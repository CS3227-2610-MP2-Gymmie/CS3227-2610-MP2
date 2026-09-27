package gymmie.trainer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
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
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.BookingBuilder;
import gymmie.testutil.TrainingSessionBuilder;

class SessionCancellationServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T04:00:00Z"),
            ZoneId.of("Asia/Singapore"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 12, 0);
    @TempDir
    Path directory;
    private AppContext context;
    private SessionCancellationService service;
    private TrainingSession session;
    private List<Booking> originalBookings;

    @BeforeEach
    void setUp() throws Exception {
        context = new AppContext(directory.resolve("cancel.db"));
        session = new TrainingSessionBuilder().withStartsAt(NOW.plusHours(1)).build();
        var persistence = context.getPersistence();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, new AccountBuilder().withId(2).withUsername("trainer")
                    .withRole(Role.TRAINER).build());
            persistence.sessions().insert(connection, session);
            persistence.sessions().insert(connection, new TrainingSessionBuilder(session).withId(2).build());
            for (int id = 3; id <= 5; id++) {
                persistence.accounts().insert(connection, new AccountBuilder().withId(id).withUsername("member" + id)
                        .withDisplayName("Member " + id).withRole(Role.MEMBER).build());
                var booking = new BookingBuilder().withId(id).withMemberId(id).withSessionId(1);
                if (id == 5) {
                    booking.withStatus(BookingStatus.CANCELLED)
                            .withCancellationReason(CancellationReason.MEMBER_CANCELLED_BOOKING);
                }
                persistence.bookings().insert(connection, booking.build());
            }
            persistence.bookings().insert(connection,
                    new BookingBuilder().withId(6).withSessionId(2).withMemberId(3).build());
            return null;
        });
        context.getAuthService().login("trainer", AccountBuilder.DEFAULT_PASSWORD);
        service = service(CLOCK);
        originalBookings = bookings();
    }

    @Test
    void cancelsCurrentBookingsAndPersistsReasonVisibleToMembersAfterRestart() throws Exception {
        var preview = service.preview(1);
        assertEquals(session, preview.session());
        assertEquals(List.of("Member 3", "Member 4"), preview.memberNames());
        assertEquals(2, service.cancel(preview, "  Trainer is unwell  "));
        AppContext restarted = new AppContext(directory.resolve("cancel.db"));
        var stored = restarted.getPersistence().unitOfWork().inTransaction(connection ->
                restarted.getPersistence().sessions().findById(connection, 1).orElseThrow());
        assertTrue(stored.cancelled());
        assertEquals("Trainer is unwell", stored.cancellationReason());
        for (int id : new int[]{3, 4}) {
            restarted.getAuthService().login("member" + id, AccountBuilder.DEFAULT_PASSWORD);
            var history = restarted.getMemberBookingHistoryService().bookingHistory();
            var affected = history.stream().filter(booking -> booking.bookingId() == id).findFirst().orElseThrow();
            assertEquals(BookingStatus.CANCELLED, affected.status());
            assertEquals(CancellationReason.TRAINER_CANCELLED_SESSION, affected.cancellationReason());
            assertEquals("Trainer is unwell", affected.trainerCancellationReason());
        }
        assertEquals(originalBookings.get(2), bookings().get(2));
        assertEquals(BookingStatus.BOOKED, restarted.getPersistence().unitOfWork().inTransaction(connection ->
                restarted.getPersistence().bookings().findById(connection, 6).orElseThrow().status()));
        assertThrows(ConflictException.class, () -> service.cancel(preview, "Again"));
    }

    @Test
    void requiresReasonAndPreviewAloneMakesNoChanges() throws Exception {
        var preview = service.preview(1);
        for (String reason : new String[]{null, "", " \n\t "}) {
            assertThrows(ValidationException.class, () -> service.cancel(preview, reason));
        }
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void checksLocalStartBoundaryAgainAfterConfirmation(long nanosAfterStart) throws Exception {
        var preview = service.preview(1);
        Clock atStart = Clock.fixed(session.startsAt().atZone(CLOCK.getZone()).toInstant()
                .plusNanos(nanosAfterStart), CLOCK.getZone());
        var timed = service(atStart);
        if (nanosAfterStart < 0) {
            assertEquals(2, timed.cancel(preview, "Reason"));
        } else {
            assertThrows(ConflictException.class, () -> timed.preview(1));
            assertThrows(ConflictException.class, () -> timed.cancel(preview, "Reason"));
            assertUnchanged();
        }
    }

    @Test
    void rejectsWrongRolesForeignOwnersMissingSessionsAndLoggedOutUsers() throws Exception {
        var preview = service.preview(1);
        for (Role role : Role.values()) {
            var other = new AccountBuilder().withId(10 + role.ordinal()).withUsername("other" + role)
                    .withRole(role).build();
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                context.getPersistence().accounts().insert(connection, other);
                return null;
            });
            context.getAuthService().login(other.username(), AccountBuilder.DEFAULT_PASSWORD);
            assertThrows(AuthorizationException.class, () -> service.preview(1));
            assertThrows(AuthorizationException.class, () -> service.cancel(preview, "Reason"));
        }
        context.getAuthService().logout();
        assertThrows(AuthenticationException.class, () -> service.preview(1));
        assertThrows(AuthenticationException.class, () -> service.cancel(preview, "Reason"));
        context.getAuthService().login("trainer", AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> service.preview(999));
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rechecksPersistedTrainerRoleAndActivation(boolean deactivated) throws Exception {
        var preview = service.preview(1);
        // Simulate a persisted account change after the confirmation snapshot was loaded.
        execute(deactivated ? "UPDATE account SET active = 0 WHERE id = 2"
                : "UPDATE account SET role = 'MEMBER' WHERE id = 2");
        if (deactivated) {
            assertThrows(AccountDeactivatedException.class, () -> service.cancel(preview, "Reason"));
        } else {
            assertThrows(AuthorizationException.class, () -> service.cancel(preview, "Reason"));
        }
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void persistenceFailureRollsBackSessionAndEarlierBookingChanges(boolean failSession) throws Exception {
        var preview = service.preview(1);
        execute(failSession
                ? "CREATE TRIGGER fail_write BEFORE UPDATE ON training_session "
                        + "BEGIN SELECT RAISE(ABORT, 'forced session failure'); END"
                : "CREATE TRIGGER fail_write BEFORE UPDATE ON booking WHEN OLD.id = 4 "
                        + "BEGIN SELECT RAISE(ABORT, 'forced second booking failure'); END");
        assertThrows(SQLException.class, () -> service.cancel(preview, "Reason"));
        assertUnchanged();
        var restarted = new AppContext(directory.resolve("cancel.db")).getPersistence();
        assertEquals(session, restarted.unitOfWork().inTransaction(connection ->
                restarted.sessions().findById(connection, 1).orElseThrow()));
        assertEquals(originalBookings, restarted.unitOfWork().inTransaction(connection ->
                restarted.bookings().findBySessionId(connection, 1)));
    }

    @Test
    void rejectsChangedBookingsOrSessionAndAllowsFreshConfirmation() throws Exception {
        var preview = service.preview(1);
        execute("UPDATE booking SET status = 'CANCELLED', "
                + "cancellation_reason = 'MEMBER_CANCELLED_BOOKING' WHERE id = 3");
        assertThrows(ConflictException.class, () -> service.cancel(preview, "Reason"));
        var next = service.preview(1);
        execute("UPDATE training_session SET description = 'Changed' WHERE id = 1");
        assertThrows(ConflictException.class, () -> service.cancel(next, "Reason"));
        assertEquals(1, service.cancel(service.preview(1), "Reviewed"));
    }

    @Test
    void cancelsEmptySessionAndMigratesLegacyDatabaseWithoutLosingHistory() throws Exception {
        execute("ALTER TABLE training_session DROP COLUMN cancellation_reason");
        execute("PRAGMA user_version = 2");
        context = new AppContext(directory.resolve("cancel.db"));
        context.getAuthService().login("trainer", AccountBuilder.DEFAULT_PASSWORD);
        service = service(CLOCK);
        assertUnchanged();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().sessions().insert(connection,
                    new TrainingSessionBuilder(session).withId(3).build());
            return null;
        });
        assertEquals(0, service.cancel(service.preview(3), "No longer available"));
        assertUnchanged();
        new AppContext(directory.resolve("cancel.db"));
    }

    private SessionCancellationService service(Clock clock) {
        var persistence = context.getPersistence();
        return new SessionCancellationService(persistence.sessions(), persistence.bookings(), persistence.accounts(),
                persistence.unitOfWork(), context.getPermissions(), clock);
    }

    private List<Booking> bookings() throws Exception {
        return context.getPersistence().unitOfWork().inTransaction(connection ->
                context.getPersistence().bookings().findBySessionId(connection, 1));
    }

    private void assertUnchanged() throws Exception {
        var stored = context.getPersistence().unitOfWork().inTransaction(connection ->
                context.getPersistence().sessions().findById(connection, 1).orElseThrow());
        assertFalse(stored.cancelled());
        assertEquals(session, stored);
        assertEquals(originalBookings, bookings());
    }

    private void execute(String sql) throws Exception {
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute(sql);
            }
            return null;
        });
    }
}
