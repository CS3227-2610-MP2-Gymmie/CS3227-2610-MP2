package gymmie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.AppContext;
import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.model.exception.ValidationException;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.TrainingSessionBuilder;
import gymmie.trainer.service.TrainingSessionService;

class TrainingSessionServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-26T04:00:00Z"),
            ZoneId.of("Asia/Singapore"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 12, 0);
    @TempDir
    Path directory;
    private AppContext context;
    private Account trainer;
    private TrainingSessionService service;

    @BeforeEach
    void setUp() throws Exception {
        context = new AppContext(directory.resolve("sessions.db"));
        trainer = new AccountBuilder().withId(2).withRole(Role.TRAINER).build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection, trainer);
            return null;
        });
        context.getUserSession().establish(trainer);
        var persistence = context.getPersistence();
        service = new TrainingSessionService(persistence.sessions(), persistence.bookings(), persistence.unitOfWork(),
                context.getPermissions(), CLOCK);
    }

    @Test
    void createsOwnedUpcomingSessionsAndRetainsAllFieldsAfterRestart() throws Exception {
        var first = service.create(NOW.plusNanos(1), 15, 1, null);
        var second = service.create(NOW.plusDays(1), 240, 50, "Strength\nand mobility");
        var third = service.create(NOW.plusDays(2), 60, 10, "");
        assertEquals(trainer.id(), first.trainerId());
        assertFalse(first.cancelled());
        assertNotEquals(first.id(), second.id());
        AppContext restarted = new AppContext(directory.resolve("sessions.db"));
        var stored = restarted.getPersistence().unitOfWork().inTransaction(connection ->
                restarted.getPersistence().sessions().findUpcoming(connection, NOW));
        assertEquals(List.of(first, second, third), stored);
        restarted.getUserSession().establish(trainer);
        var persistence = restarted.getPersistence();
        var restartedService = new TrainingSessionService(persistence.sessions(), persistence.bookings(),
                persistence.unitOfWork(), restarted.getPermissions(), CLOCK);
        var next = restartedService.create(NOW.plusDays(3), 30, 5, null);
        assertTrue(next.id() > third.id());
    }

    @Test
    void rejectsMissingPastAndExactCurrentLocalTimeWithoutWriting() throws Exception {
        assertThrows(ValidationException.class, () -> service.create(null, 60, 10, null));
        for (LocalDateTime start : List.of(NOW.minusNanos(1), NOW, NOW.minusHours(1))) {
            assertThrows(ValidationException.class, () -> service.create(start, 60, 10, null));
        }
        assertEmpty();
    }

    @Test
    void rejectsOutOfRangeDurationAndCapacityWithoutWriting() throws Exception {
        for (int minutes : new int[]{-1, 0, 14, 241, Integer.MAX_VALUE}) {
            assertThrows(ValidationException.class, () -> service.create(NOW.plusDays(1), minutes, 10, null));
        }
        for (int capacity : new int[]{-1, 0, 51, Integer.MAX_VALUE}) {
            assertThrows(ValidationException.class, () -> service.create(NOW.plusDays(1), 60, capacity, null));
        }
        assertEmpty();
    }

    @Test
    void rejectsUnauthenticatedAndOtherRolesIncludingStaleTrainerRole() throws Exception {
        context.getUserSession().clear();
        assertThrows(AuthenticationException.class, () -> service.create(NOW.plusDays(1), 60, 10, null));
        for (Role role : List.of(Role.MANAGER, Role.MEMBER)) {
            Account other = new AccountBuilder().withId(role.ordinal() + 10).withUsername("other_" + role.name())
                    .withRole(role).build();
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                context.getPersistence().accounts().insert(connection, other);
                return null;
            });
            context.getUserSession().establish(other);
            assertThrows(AuthorizationException.class, () -> service.create(NOW.plusDays(1), 60, 10, null));
            context.getUserSession().establish(new AccountBuilder(other).withRole(Role.TRAINER).build());
            assertThrows(AuthorizationException.class, () -> service.create(NOW.plusDays(1), 60, 10, null));
        }
        assertEmpty();
    }

    @Test
    void rejectsDeactivatedTrainerAndClearsAuthentication() throws Exception {
        replaceTrainer(new AccountBuilder(trainer).withActive(false).build());
        assertThrows(AccountDeactivatedException.class, () -> service.create(NOW.plusDays(1), 60, 10, null));
        assertFalse(context.getUserSession().isAuthenticated());
        assertEmpty();
    }

    @Test
    void listsOnlyOwnUncancelledFutureSessionsInStartOrderUsingLocalTime() throws Exception {
        var other = new AccountBuilder().withId(3).withUsername("otherTrainer").withRole(Role.TRAINER).build();
        var later = new TrainingSessionBuilder().withId(1).withStartsAt(NOW.plusDays(1)).build();
        var next = new TrainingSessionBuilder().withId(2).withStartsAt(NOW.plusNanos(1)).build();
        var tied = new TrainingSessionBuilder(next).withId(3).build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection, other);
            for (var session : List.of(later, next, tied,
                    new TrainingSessionBuilder().withId(4).withStartsAt(NOW).build(),
                    new TrainingSessionBuilder().withId(5).withStartsAt(NOW.minusNanos(1)).build(),
                    new TrainingSessionBuilder().withId(6).withStartsAt(NOW.minusHours(1)).build(),
                    new TrainingSessionBuilder(later).withId(7).withCancelled(true).build(),
                    new TrainingSessionBuilder(later).withId(8).withTrainerId(other.id()).build())) {
                context.getPersistence().sessions().insert(connection, session);
            }
            return null;
        });
        assertEquals(List.of(next, tied, later), service.getOwnUpcomingSessions());
        context.getUserSession().establish(other);
        assertEquals(List.of(8L), service.getOwnUpcomingSessions().stream().map(session -> session.id()).toList());
        context.getUserSession().establish(trainer);
        var persistence = context.getPersistence();
        var advanced = new TrainingSessionService(persistence.sessions(), persistence.bookings(),
                persistence.unitOfWork(), context.getPermissions(), Clock.offset(CLOCK, java.time.Duration.ofDays(1)));
        assertTrue(advanced.getOwnUpcomingSessions().isEmpty());
    }

    @Test
    void upcomingReadRejectsMissingWrongStaleAndDeactivatedRoles() throws Exception {
        assertTrue(service.getOwnUpcomingSessions().isEmpty());
        context.getUserSession().clear();
        assertThrows(AuthenticationException.class, service::getOwnUpcomingSessions);
        for (Role role : List.of(Role.MANAGER, Role.MEMBER)) {
            var other = new AccountBuilder().withId(role.ordinal() + 10).withUsername("reader_" + role.name())
                    .withRole(role).build();
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                context.getPersistence().accounts().insert(connection, other);
                return null;
            });
            context.getUserSession().establish(new AccountBuilder(other).withRole(Role.TRAINER).build());
            assertThrows(AuthorizationException.class, service::getOwnUpcomingSessions);
            context.getUserSession().establish(other);
            assertThrows(AuthorizationException.class, service::getOwnUpcomingSessions);
        }
        replaceTrainer(new AccountBuilder(trainer).withActive(false).build());
        context.getUserSession().establish(trainer);
        assertThrows(AccountDeactivatedException.class, service::getOwnUpcomingSessions);
        assertFalse(context.getUserSession().isAuthenticated());
    }

    @Test
    void editsAtBookedCapacityPreserveAllBookingsAcrossRestart() throws Exception {
        var session = service.create(NOW.plusDays(1), 60, 10, "Original");
        var persistence = context.getPersistence();
        persistence.unitOfWork().inTransaction(connection -> {
            for (int id = 3; id <= 5; id++) {
                persistence.accounts().insert(connection, new AccountBuilder().withId(id)
                        .withUsername("member" + id).withRole(Role.MEMBER).build());
                var builder = new gymmie.testutil.BookingBuilder().withId(id).withMemberId(id)
                        .withSessionId(session.id());
                if (id == 5) {
                    builder.withStatus(gymmie.model.BookingStatus.CANCELLED)
                            .withCancellationReason(gymmie.model.CancellationReason.MEMBER_CANCELLED_BOOKING);
                }
                persistence.bookings().insert(connection, builder.build());
            }
            return null;
        });
        var bookings = persistence.unitOfWork().inTransaction(connection ->
                persistence.bookings().findBySessionId(connection, session.id()));
        assertThrows(ValidationException.class, () ->
                        service.edit(session.id(), NOW.plusDays(2), 30, 1, "Rejected"));
        assertEquals(session, persistence.unitOfWork().inTransaction(connection ->
                persistence.sessions().findById(connection, session.id()).orElseThrow()));
        var edited = service.edit(session.id(), NOW.plusDays(2), 240, 2, "Corrected");
        assertEquals(session.id(), edited.id());
        assertEquals(trainer.id(), edited.trainerId());
        assertEquals(NOW.plusDays(2), edited.startsAt());
        assertEquals(240, edited.durationMinutes());
        assertEquals(2, edited.capacity());
        assertEquals("Corrected", edited.description());
        var restarted = new AppContext(directory.resolve("sessions.db")).getPersistence();
        assertEquals(edited, restarted.unitOfWork().inTransaction(connection ->
                restarted.sessions().findById(connection, session.id()).orElseThrow()));
        assertEquals(bookings, restarted.unitOfWork().inTransaction(connection ->
                restarted.bookings().findBySessionId(connection, session.id())));
    }

    @Test
    void editsRejectInvalidDetailsAndCancelledSessionsWithoutWriting() throws Exception {
        var session = service.create(NOW.plusDays(1), 60, 10, "Original");
        assertThrows(ValidationException.class, () -> service.edit(session.id(), null, 60, 10, null));
        for (var start : List.of(NOW.minusNanos(1), NOW)) {
            assertThrows(ValidationException.class, () -> service.edit(session.id(), start, 60, 10, null));
        }
        for (int duration : new int[]{14, 241}) {
            assertThrows(ValidationException.class, () ->
                            service.edit(session.id(), NOW.plusDays(1), duration, 10, null));
        }
        for (int capacity : new int[]{0, 51}) {
            assertThrows(ValidationException.class, () ->
                            service.edit(session.id(), NOW.plusDays(1), 60, capacity, null));
        }
        assertEquals(List.of(session), service.getOwnUpcomingSessions());
        var edited = service.edit(session.id(), NOW.plusNanos(1), 15, 1, null);
        assertEquals(NOW.plusNanos(1), edited.startsAt());
        var cancelled = new TrainingSessionBuilder(edited).withCancelled(true).build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().sessions().update(connection, cancelled);
            return null;
        });
        assertThrows(ValidationException.class, () ->
                        service.edit(session.id(), NOW.plusDays(2), 30, 5, "Rejected"));
        assertEquals(cancelled, context.getPersistence().unitOfWork().inTransaction(connection ->
                context.getPersistence().sessions().findById(connection, session.id()).orElseThrow()));
    }

    @Test
    void editsRequireCurrentTrainerRoleOwnershipAndActiveAuthentication() throws Exception {
        var session = service.create(NOW.plusDays(1), 60, 10, "Original");
        context.getUserSession().clear();
        assertThrows(AuthenticationException.class, () ->
                        service.edit(session.id(), NOW.plusDays(2), 30, 5, null));
        for (Role role : Role.values()) {
            var other = new AccountBuilder().withId(role.ordinal() + 10).withUsername("editor_" + role)
                    .withRole(role).build();
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                context.getPersistence().accounts().insert(connection, other);
                return null;
            });
            context.getUserSession().establish(other);
            assertThrows(AuthorizationException.class, () ->
                            service.edit(session.id(), NOW.plusDays(2), 30, 5, null));
            if (role != Role.TRAINER) {
                context.getUserSession().establish(new AccountBuilder(other).withRole(Role.TRAINER).build());
                assertThrows(AuthorizationException.class, () ->
                                service.edit(session.id(), NOW.plusDays(2), 30, 5, null));
            }
        }
        context.getUserSession().establish(trainer);
        assertThrows(AuthorizationException.class, () -> service.edit(999, NOW.plusDays(2), 30, 5, null));
        replaceTrainer(new AccountBuilder(trainer).withActive(false).build());
        assertThrows(AccountDeactivatedException.class, () ->
                        service.edit(session.id(), NOW.plusDays(2), 30, 5, null));
        assertFalse(context.getUserSession().isAuthenticated());
        assertEquals(session, context.getPersistence().unitOfWork().inTransaction(connection ->
                context.getPersistence().sessions().findById(connection, session.id()).orElseThrow()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsOverlapsInEitherDirectionAndContainmentWithoutWriting(boolean editing) throws Exception {
        LocalDateTime start = NOW.plusDays(1);
        var occupied = service.create(start, 60, 10, "Occupied");
        var target = editing ? service.create(start.plusDays(1), 30, 5, "Original") : null;
        var before = service.getOwnUpcomingSessions();
        // Equal, contained, containing, left overlap, right overlap, and sub-minute overlap.
        var starts = List.of(start, start.plusMinutes(15), start.minusMinutes(30),
                start.minusMinutes(30), start.plusMinutes(30), start.plusMinutes(60).minusNanos(1));
        int[] durations = {60, 15, 120, 60, 60, 15};
        for (int index = 0; index < starts.size(); index++) {
            LocalDateTime proposed = starts.get(index);
            int duration = durations[index];
            var error = assertThrows(ValidationException.class, () -> {
                if (editing) {
                    service.edit(target.id(), proposed, duration, 5, "Rejected");
                } else {
                    service.create(proposed, duration, 5, "Rejected");
                }
            });
            assertTrue(error.getMessage().contains("overlaps with your session #" + occupied.id()));
            assertEquals(before, service.getOwnUpcomingSessions());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void allowsAdjacentSessionsAndIgnoresCancelledAndOtherTrainers(boolean editing) throws Exception {
        LocalDateTime start = NOW.plusDays(1);
        var occupied = service.create(start, 60, 10, "Occupied");
        var other = new AccountBuilder().withId(3).withUsername("otherTrainer").withRole(Role.TRAINER).build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            var persistence = context.getPersistence();
            persistence.accounts().insert(connection, other);
            persistence.sessions().insert(connection, new TrainingSessionBuilder(occupied).withId(100)
                    .withStartsAt(start.minusMinutes(60)).withDurationMinutes(180).withCancelled(true).build());
            persistence.sessions().insert(connection, new TrainingSessionBuilder(occupied).withId(101)
                    .withStartsAt(start.minusMinutes(60)).withDurationMinutes(180).withTrainerId(other.id()).build());
            return null;
        });
        var target = editing ? service.create(start.plusDays(1), 30, 5, "Original") : null;
        for (var adjacent : List.of(start.minusMinutes(60), start.plusMinutes(60))) {
            var saved = editing ? service.edit(target.id(), adjacent, 60, 5, "Adjacent")
                    : service.create(adjacent, 60, 5, "Adjacent");
            assertEquals(adjacent, saved.startsAt());
            // An unchanged schedule must not clash with itself.
            assertEquals(saved, service.edit(saved.id(), saved.startsAt(), 60, 5, "Adjacent"));
        }
    }

    @Test
    void durationExtensionCannotOverlapAndFailedEditPreservesBookings() throws Exception {
        LocalDateTime start = NOW.plusDays(1).withHour(23).withMinute(30);
        var target = service.create(start, 30, 10, "Original");
        service.create(start.plusMinutes(30), 60, 10, "After midnight");
        var booking = new gymmie.testutil.BookingBuilder().withSessionId(target.id()).withMemberId(3).build();
        var persistence = context.getPersistence();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, new AccountBuilder().withId(3).withUsername("member")
                    .withRole(Role.MEMBER).build());
            persistence.bookings().insert(connection, booking);
            return null;
        });
        assertThrows(ValidationException.class, () -> service.edit(target.id(), start, 31, 10, "Rejected"));
        assertEquals(target, persistence.unitOfWork().inTransaction(connection ->
                persistence.sessions().findById(connection, target.id()).orElseThrow()));
        assertEquals(List.of(booking), persistence.unitOfWork().inTransaction(connection ->
                persistence.bookings().findBySessionId(connection, target.id())));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ongoingSessionsStillBlockFutureOverlaps(boolean editing) throws Exception {
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().sessions().insert(connection, new TrainingSessionBuilder().withId(100)
                    .withTrainerId(trainer.id()).withStartsAt(NOW.minusMinutes(30)).withDurationMinutes(60).build());
            return null;
        });
        var target = editing ? service.create(NOW.plusDays(1), 60, 5, "Original") : null;
        assertThrows(ValidationException.class, () -> {
            if (editing) {
                service.edit(target.id(), NOW.plusMinutes(1), 15, 5, null);
            } else {
                service.create(NOW.plusMinutes(1), 15, 5, null);
            }
        });
    }

    @Test
    void deletesUnusedSessionAcrossRestartWithoutChangingOtherSessions() throws Exception {
        var unused = service.create(NOW.plusDays(1), 60, 10, "Unused");
        var retained = service.create(NOW.plusDays(2), 60, 10, "Retained");
        service.delete(unused.id());
        var restarted = new AppContext(directory.resolve("sessions.db")).getPersistence();
        assertEquals(List.of(retained), restarted.unitOfWork().inTransaction(connection ->
                restarted.sessions().findAll(connection)));
        assertThrows(AuthorizationException.class, () -> service.delete(unused.id()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletionRejectsAnyBookingHistoryWithoutChangingSessionOrBooking(boolean cancelled) throws Exception {
        var session = service.create(NOW.plusDays(1), 60, 10, "Retained");
        var builder = new gymmie.testutil.BookingBuilder().withSessionId(session.id()).withMemberId(3);
        if (cancelled) {
            builder.withStatus(gymmie.model.BookingStatus.CANCELLED)
                    .withCancellationReason(gymmie.model.CancellationReason.MEMBER_CANCELLED_BOOKING);
        }
        var booking = builder.build();
        var persistence = context.getPersistence();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, new AccountBuilder().withId(3).withUsername("member")
                    .withRole(Role.MEMBER).build());
            persistence.bookings().insert(connection, booking);
            return null;
        });
        var error = assertThrows(ValidationException.class, () -> service.delete(session.id()));
        assertTrue(error.getMessage().contains("booking history"));
        var restarted = new AppContext(directory.resolve("sessions.db")).getPersistence();
        assertEquals(session, restarted.unitOfWork().inTransaction(connection ->
                restarted.sessions().findById(connection, session.id()).orElseThrow()));
        assertEquals(List.of(booking), restarted.unitOfWork().inTransaction(connection ->
                restarted.bookings().findBySessionId(connection, session.id())));
    }

    @Test
    void deletionRequiresActiveAuthenticationCurrentTrainerRoleAndOwnership() throws Exception {
        var session = service.create(NOW.plusDays(1), 60, 10, "Retained");
        context.getUserSession().clear();
        assertThrows(AuthenticationException.class, () -> service.delete(session.id()));
        for (Role role : Role.values()) {
            var other = new AccountBuilder().withId(role.ordinal() + 10).withUsername("deleter_" + role)
                    .withRole(role).build();
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                context.getPersistence().accounts().insert(connection, other);
                return null;
            });
            context.getUserSession().establish(other);
            assertThrows(AuthorizationException.class, () -> service.delete(session.id()));
            if (role != Role.TRAINER) {
                context.getUserSession().establish(new AccountBuilder(other).withRole(Role.TRAINER).build());
                assertThrows(AuthorizationException.class, () -> service.delete(session.id()));
            }
        }
        context.getUserSession().establish(trainer);
        assertThrows(AuthorizationException.class, () -> service.delete(999));
        replaceTrainer(new AccountBuilder(trainer).withActive(false).build());
        assertThrows(AccountDeactivatedException.class, () -> service.delete(session.id()));
        assertFalse(context.getUserSession().isAuthenticated());
        assertEquals(session, context.getPersistence().unitOfWork().inTransaction(connection ->
                context.getPersistence().sessions().findById(connection, session.id()).orElseThrow()));
    }

    private void replaceTrainer(Account replacement) throws Exception {
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().update(connection, replacement);
            return null;
        });
    }

    private void assertEmpty() throws Exception {
        assertTrue(context.getPersistence().unitOfWork().inTransaction(connection ->
                context.getPersistence().sessions().findAll(connection)).isEmpty());
    }
}
