package gymmie.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Membership;
import gymmie.model.MembershipStatus;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.persistence.Database;
import gymmie.persistence.Persistence;

class InMemoryDatabaseTest {
    @Test
    void connectionsShareCommittedRecordsButFixturesAreIsolated() throws Exception {
        try (InMemoryDatabase first = new InMemoryDatabase(); InMemoryDatabase second = new InMemoryDatabase()) {
            Persistence persistence = first.persistence();
            Account account = new AccountBuilder().build();
            persistence.unitOfWork().inTransaction(connection -> {
                persistence.accounts().insert(connection, account);
                return null;
            });
            try (Connection connection = first.database().openConnection()) {
                assertEquals(account, persistence.accounts().findById(connection, account.id()).orElseThrow());
                assertThrows(SQLException.class, () -> persistence.bookings().insert(connection,
                        new BookingBuilder().build()));
            }
            second.persistence().unitOfWork().inTransaction(connection -> {
                assertTrue(second.persistence().accounts().findAll(connection).isEmpty());
                return null;
            });
        }
    }

    @Test
    void closingLastConnectionDestroysDatabase() throws Exception {
        Database database;
        try (InMemoryDatabase fixture = new InMemoryDatabase()) {
            database = fixture.database();
        }
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeQuery("SELECT * FROM account"));
        }
    }

    @ParameterizedTest
    @EnumSource(value = CancellationReason.class, names = {
        "TRAINER_CANCELLED_SESSION", "MEMBERSHIP_CANCELLED", "ACCOUNT_DEACTIVATED"
    })
    void cancellationChangesCommitTogetherAndRollBackOnLaterBookingFailure(CancellationReason reason) throws Exception {
        try (InMemoryDatabase fixture = new InMemoryDatabase()) {
            Persistence persistence = fixture.persistence();
            seed(persistence, reason);
            try (Connection connection = fixture.database().openConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("CREATE TRIGGER reject_second_booking BEFORE UPDATE ON booking "
                        + "WHEN OLD.id = 2 BEGIN SELECT RAISE(ABORT, 'simulated write failure'); END");
            }
            assertThrows(SQLException.class, () -> cancel(persistence, reason));
            assertState(persistence, false, reason);
            try (Connection connection = fixture.database().openConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("DROP TRIGGER reject_second_booking");
            }
            cancel(persistence, reason);
            assertState(persistence, true, reason);
        }
    }

    private void seed(Persistence persistence, CancellationReason reason) throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, new AccountBuilder().build());
            persistence.accounts().insert(connection, new AccountBuilder().withId(2).withUsername("trainer")
                    .withRole(Role.TRAINER).build());
            persistence.accounts().insert(connection, new AccountBuilder().withId(3).withUsername("second_member")
                    .build());
            persistence.plans().insert(connection, new MembershipPlanBuilder().build());
            persistence.memberships().insert(connection, new MembershipBuilder().build());
            persistence.sessions().insert(connection, new TrainingSessionBuilder().build());
            persistence.sessions().insert(connection, new TrainingSessionBuilder().withId(2).build());
            persistence.bookings().insert(connection, new BookingBuilder().build());
            boolean trainerCancellation = reason == CancellationReason.TRAINER_CANCELLED_SESSION;
            persistence.bookings().insert(connection, new BookingBuilder().withId(2)
                    .withSessionId(trainerCancellation ? 1 : 2).withMemberId(trainerCancellation ? 3 : 1).build());
            persistence.bookings().insert(connection, new BookingBuilder().withId(3).withMemberId(3)
                    .withSessionId(trainerCancellation ? 2 : 1).build());
            return null;
        });
    }

    // Exercises repository composition, not an application cancellation service.
    private void cancel(Persistence persistence, CancellationReason reason) throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            if (reason == CancellationReason.TRAINER_CANCELLED_SESSION) {
                TrainingSession session = persistence.sessions().findById(connection, 1).orElseThrow();
                persistence.sessions().update(connection, new TrainingSessionBuilder(session).withCancelled(true)
                        .build());
            } else if (reason == CancellationReason.MEMBERSHIP_CANCELLED) {
                Membership membership = persistence.memberships().findById(connection, 1).orElseThrow();
                persistence.memberships().update(connection, new MembershipBuilder(membership)
                        .withStatus(MembershipStatus.CANCELLED).build());
            } else {
                Account account = persistence.accounts().findById(connection, 1).orElseThrow();
                persistence.accounts().update(connection, new AccountBuilder(account).withActive(false).build());
            }
            for (Booking booking : reason == CancellationReason.TRAINER_CANCELLED_SESSION
                    ? persistence.bookings().findBySessionId(connection, 1)
                    : persistence.bookings().findByMemberId(connection, 1)) {
                TrainingSession session = persistence.sessions().findById(connection, booking.sessionId())
                        .orElseThrow();
                if (!session.hasStartedAt(LocalDateTime.now(TestClocks.fixed()))) {
                    persistence.bookings().update(connection, new BookingBuilder(booking)
                            .withStatus(BookingStatus.CANCELLED).withCancellationReason(reason).build());
                }
            }
            return null;
        });
    }

    private void assertState(Persistence persistence, boolean cancelled, CancellationReason reason) throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            assertEquals(!(cancelled && reason == CancellationReason.ACCOUNT_DEACTIVATED),
                    persistence.accounts().findById(connection, 1).orElseThrow().active());
            assertEquals(cancelled && reason == CancellationReason.MEMBERSHIP_CANCELLED,
                    persistence.memberships().findById(connection, 1).orElseThrow().status()
                            == MembershipStatus.CANCELLED);
            assertEquals(cancelled && reason == CancellationReason.TRAINER_CANCELLED_SESSION,
                    persistence.sessions().findById(connection, 1).orElseThrow().cancelled());
            for (Booking booking : reason == CancellationReason.TRAINER_CANCELLED_SESSION
                    ? persistence.bookings().findBySessionId(connection, 1)
                    : persistence.bookings().findByMemberId(connection, 1)) {
                assertEquals(cancelled ? BookingStatus.CANCELLED : BookingStatus.BOOKED, booking.status());
                assertEquals(cancelled ? reason : null, booking.cancellationReason());
            }
            assertEquals(BookingStatus.BOOKED,
                    persistence.bookings().findById(connection, 3).orElseThrow().status());
            assertFalse(persistence.sessions().findById(connection, 2).orElseThrow().cancelled());
            return null;
        });
    }
}
