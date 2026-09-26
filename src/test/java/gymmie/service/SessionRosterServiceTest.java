package gymmie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gymmie.AppContext;
import gymmie.model.Account;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Role;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.BookingBuilder;
import gymmie.trainer.service.SessionRosterService;

class SessionRosterServiceTest {
    @TempDir
    Path directory;
    private AppContext context;
    private Account trainer;
    private SessionRosterService service;
    private long sessionId;

    @BeforeEach
    void setUp() throws Exception {
        context = new AppContext(directory.resolve("roster.db"));
        trainer = new AccountBuilder().withId(2).withRole(Role.TRAINER).build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection, trainer);
            return null;
        });
        context.getUserSession().establish(trainer);
        sessionId = context.getTrainingSessionService().create(LocalDateTime.now().plusDays(1), 60, 10, null).id();
        service = context.getSessionRosterService();
    }

    @Test
    void returnsOnlyBookedDisplayNamesForSelectedSessionAndPreservesDuplicateNames() throws Exception {
        assertTrue(service.getRoster(sessionId).isEmpty());
        long otherSession = context.getTrainingSessionService()
                .create(LocalDateTime.now().plusDays(2), 60, 10, null).id();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            for (int id = 3; id <= 7; id++) {
                var member = new AccountBuilder().withId(id).withUsername("private_login_" + id)
                        .withDisplayName(id == 3 ? "Zoe" : "Alex").withRole(Role.MEMBER).build();
                context.getPersistence().accounts().insert(connection, member);
                var booking = new BookingBuilder().withId(id).withMemberId(id)
                        .withSessionId(id == 7 ? otherSession : sessionId);
                if (id == 6) {
                    booking.withStatus(BookingStatus.CANCELLED)
                            .withCancellationReason(CancellationReason.MEMBER_CANCELLED_BOOKING);
                }
                context.getPersistence().bookings().insert(connection, booking.build());
            }
            return null;
        });
        assertEquals(List.of("Alex", "Alex", "Zoe"), service.getRoster(sessionId));
        assertEquals(List.of("Alex"), service.getRoster(otherSession));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            var member = context.getPersistence().accounts().findById(connection, 3).orElseThrow();
            context.getPersistence().accounts().update(connection,
                    new AccountBuilder(member).withDisplayName("Updated name").build());
            var booking = context.getPersistence().bookings().findById(connection, 4).orElseThrow();
            context.getPersistence().bookings().update(connection, new BookingBuilder(booking)
                    .withStatus(BookingStatus.CANCELLED)
                    .withCancellationReason(CancellationReason.MEMBER_CANCELLED_BOOKING).build());
            return null;
        });
        assertEquals(List.of("Alex", "Updated name"), service.getRoster(sessionId));
    }

    @Test
    void rejectsOtherTrainersMissingSessionsAndUnauthenticatedRequests() throws Exception {
        assertThrows(AuthorizationException.class, () -> service.getRoster(Long.MAX_VALUE));
        var other = new AccountBuilder(trainer).withId(3).withUsername("other_trainer").build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection, other);
            return null;
        });
        context.getUserSession().establish(other);
        assertThrows(AuthorizationException.class, () -> service.getRoster(sessionId));
        context.getUserSession().clear();
        assertThrows(AuthenticationException.class, () -> service.getRoster(sessionId));
    }

    @Test
    void rejectsOtherRolesStaleRoleClaimsAndDeactivatedTrainer() throws Exception {
        for (Role role : List.of(Role.MANAGER, Role.MEMBER)) {
            var other = new AccountBuilder().withId(role.ordinal() + 10).withUsername("other_" + role)
                    .withRole(role).build();
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                context.getPersistence().accounts().insert(connection, other);
                return null;
            });
            context.getUserSession().establish(other);
            assertThrows(AuthorizationException.class, () -> service.getRoster(sessionId));
            context.getUserSession().establish(new AccountBuilder(other).withRole(Role.TRAINER).build());
            assertThrows(AuthorizationException.class, () -> service.getRoster(sessionId));
        }
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().update(connection,
                    new AccountBuilder(trainer).withActive(false).build());
            return null;
        });
        context.getUserSession().establish(trainer);
        assertThrows(AccountDeactivatedException.class, () -> service.getRoster(sessionId));
        assertFalse(context.getUserSession().isAuthenticated());
    }
}
