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
        service = new TrainingSessionService(persistence.sessions(), persistence.unitOfWork(),
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
        var next = restarted.getTrainingSessionService().create(LocalDateTime.now().plusDays(2), 30, 5, null);
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
        var advanced = new TrainingSessionService(persistence.sessions(), persistence.unitOfWork(),
                context.getPermissions(), Clock.offset(CLOCK, java.time.Duration.ofDays(1)));
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
