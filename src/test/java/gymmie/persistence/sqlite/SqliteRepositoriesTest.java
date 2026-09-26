package gymmie.persistence.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Membership;
import gymmie.model.MembershipPlan;
import gymmie.model.MembershipStatus;
import gymmie.model.PasswordHash;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.persistence.Database;
import gymmie.persistence.Persistence;

class SqliteRepositoriesTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 12, 0);
    private static final PasswordHash PASSWORD = new PasswordHash("A".repeat(43) + "=", "A".repeat(22) + "==");
    private static final Account MEMBER = new Account(1, "Member_One", PASSWORD, "会员 🏋", Role.MEMBER, true);
    private static final Account TRAINER = new Account(2, "trainer", PASSWORD, "Trainer", Role.TRAINER, true);
    private static final Account OTHER_MEMBER = new Account(3, "member_two", PASSWORD, "Other", Role.MEMBER, true);
    private static final MembershipPlan PLAN = new MembershipPlan(1, "Member's plan", 30, 4990, false);
    private static final Membership MEMBERSHIP = new Membership(1, 1, 1, LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 10, 1), MembershipStatus.ACTIVE, 4990, 30);
    private static final TrainingSession SESSION = new TrainingSession(1, 2, NOW.plusDays(1).plusNanos(123456789),
            60, 10, "Strength ' and \"balance\"", false);
    private static final TrainingSession OTHER_SESSION = new TrainingSession(2, 2, NOW.plusDays(2),
            15, 1, null, false);
    private static final TrainingSession PAST_SESSION = new TrainingSession(3, 2, NOW.minusDays(1),
            240, 50, "", false);
    private static final Booking BOOKING = new Booking(1, 1, 1, NOW.minusDays(2).plusNanos(987654321),
            BookingStatus.BOOKED, null);
    private static final Booking OTHER_BOOKING = new Booking(2, 2, 1, NOW.minusDays(2), BookingStatus.BOOKED, null);
    private static final Booking PAST_BOOKING = new Booking(3, 3, 1, NOW.minusDays(3), BookingStatus.BOOKED, null);
    private static final Booking OTHER_MEMBER_BOOKING = new Booking(4, 1, 3, NOW.minusDays(2),
            BookingStatus.BOOKED, null);

    @TempDir
    Path temporaryDirectory;

    private Database database;
    private Persistence persistence;
    private Path databasePath;

    @BeforeEach
    void setUp() throws Exception {
        databasePath = temporaryDirectory.resolve("data/gymmie.db");
        database = new Database(databasePath);
        persistence = new Persistence(database);
        persistence.initialize();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, OTHER_MEMBER);
            persistence.accounts().insert(connection, TRAINER);
            persistence.accounts().insert(connection, MEMBER);
            persistence.plans().insert(connection, PLAN);
            persistence.memberships().insert(connection, MEMBERSHIP);
            persistence.sessions().insert(connection, PAST_SESSION);
            persistence.sessions().insert(connection, OTHER_SESSION);
            persistence.sessions().insert(connection, SESSION);
            persistence.bookings().insert(connection, OTHER_MEMBER_BOOKING);
            persistence.bookings().insert(connection, PAST_BOOKING);
            persistence.bookings().insert(connection, OTHER_BOOKING);
            persistence.bookings().insert(connection, BOOKING);
            return null;
        });
    }

    @Test
    void insertedRecordsSurviveReopeningWithAllFieldsIntact() throws Exception {
        assertTrue(Files.isRegularFile(databasePath));
        Persistence reopened = reopen();
        reopened.unitOfWork().inTransaction(connection -> {
            assertEquals(List.of(MEMBER, TRAINER, OTHER_MEMBER), reopened.accounts().findAll(connection));
            assertEquals(PLAN, reopened.plans().findById(connection, 1).orElseThrow());
            assertEquals(MEMBERSHIP, reopened.memberships().findById(connection, 1).orElseThrow());
            assertEquals(List.of(SESSION, OTHER_SESSION, PAST_SESSION), reopened.sessions().findAll(connection));
            assertEquals(BOOKING, reopened.bookings().findById(connection, 1).orElseThrow());
            assertEquals(List.of(BOOKING, OTHER_MEMBER_BOOKING), reopened.bookings().findBySessionId(connection, 1));
            return null;
        });
    }

    @Test
    void updatesInEveryDomainSurviveReopeningAndPreserveReferences() throws Exception {
        Account inactive = new Account(1, MEMBER.username(),
                new PasswordHash("B".repeat(43) + "=", "C".repeat(22) + "=="), "Updated", Role.MEMBER, false);
        MembershipPlan archived = new MembershipPlan(1, "Updated plan", 365, 1_000_000, true);
        Membership renewed = new Membership(1, 1, 1, MEMBERSHIP.startDate(), MEMBERSHIP.expiryDate().plusDays(30),
                MembershipStatus.ACTIVE, 4990, 30);
        TrainingSession rescheduled = new TrainingSession(1, 2, SESSION.startsAt().plusDays(3), 120, 20, null, false);
        Booking cancelled = cancelled(BOOKING, CancellationReason.ACCOUNT_DEACTIVATED);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().update(connection, inactive);
            persistence.plans().update(connection, archived);
            persistence.memberships().update(connection, renewed);
            persistence.sessions().update(connection, rescheduled);
            persistence.bookings().update(connection, cancelled);
            return null;
        });
        Persistence reopened = reopen();
        reopened.unitOfWork().inTransaction(connection -> {
            assertEquals(inactive, reopened.accounts().findByUsername(connection, "MEMBER_ONE").orElseThrow());
            assertEquals(List.of(TRAINER, OTHER_MEMBER), reopened.accounts().findAllActive(connection));
            assertEquals(archived, reopened.plans().findById(connection, 1).orElseThrow());
            assertTrue(reopened.plans().findAllAvailable(connection).isEmpty());
            assertEquals(renewed, reopened.memberships().findByMemberId(connection, 1).getFirst());
            assertEquals(rescheduled, reopened.sessions().findById(connection, 1).orElseThrow());
            assertEquals(cancelled, reopened.bookings().findByMemberAndSession(connection, 1, 1).orElseThrow());
            assertEquals(1, reopened.bookings().countBookedBySessionId(connection, 1));
            return null;
        });
    }

    @Test
    void historyPreventsDeletionEvenAfterCancellationAndExpiry() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            for (MembershipStatus status : MembershipStatus.values()) {
                persistence.memberships().update(connection, membershipWithStatus(status));
                assertFalse(persistence.plans().deleteIfUnpurchased(connection, 1));
            }
            for (Booking booking : persistence.bookings().findBySessionId(connection, 1)) {
                persistence.bookings().update(connection,
                        cancelled(booking, CancellationReason.TRAINER_CANCELLED_SESSION));
            }
            assertEquals(0, persistence.bookings().countBookedBySessionId(connection, 1));
            assertFalse(persistence.sessions().deleteIfNeverBooked(connection, 1));
            assertEquals(2, persistence.bookings().findBySessionId(connection, 1).size());
            assertEquals(PLAN, persistence.plans().findById(connection, 1).orElseThrow());
            assertEquals(SESSION, persistence.sessions().findById(connection, 1).orElseThrow());
            return null;
        });
    }

    @Test
    void unusedPlansAndSessionsCanBeDeletedWithoutChangingHistory() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.plans().insert(connection, new MembershipPlan(2, "Unused", 1, 0, false));
            persistence.sessions().insert(connection, new TrainingSession(4, 2, NOW, 15, 1, null, false));
            assertTrue(persistence.plans().deleteIfUnpurchased(connection, 2));
            assertTrue(persistence.sessions().deleteIfNeverBooked(connection, 4));
            assertFalse(persistence.plans().deleteIfUnpurchased(connection, 2));
            assertFalse(persistence.sessions().deleteIfNeverBooked(connection, 4));
            return null;
        });
        Persistence reopened = reopen();
        reopened.unitOfWork().inTransaction(connection -> {
            assertEquals(List.of(PLAN), reopened.plans().findAll(connection));
            assertTrue(reopened.sessions().findById(connection, 4).isEmpty());
            assertEquals(BOOKING, reopened.bookings().findById(connection, 1).orElseThrow());
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cancellationCommitsAffectedBookingsAtomically(boolean membershipCancellation) throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            cancel(connection, membershipCancellation);
            return null;
        });
        Persistence reopened = reopen();
        reopened.unitOfWork().inTransaction(connection -> {
            CancellationReason reason = membershipCancellation ? CancellationReason.MEMBERSHIP_CANCELLED
                    : CancellationReason.TRAINER_CANCELLED_SESSION;
            assertEquals(cancelled(BOOKING, reason), reopened.bookings().findById(connection, 1).orElseThrow());
            assertEquals(PAST_BOOKING, reopened.bookings().findById(connection, 3).orElseThrow());
            if (membershipCancellation) {
                assertEquals(membershipWithStatus(MembershipStatus.CANCELLED),
                        reopened.memberships().findById(connection, 1).orElseThrow());
                assertEquals(cancelled(OTHER_BOOKING, reason),
                        reopened.bookings().findById(connection, 2).orElseThrow());
                assertEquals(OTHER_MEMBER_BOOKING, reopened.bookings().findById(connection, 4).orElseThrow());
            } else {
                assertTrue(reopened.sessions().findById(connection, 1).orElseThrow().cancelled());
                assertEquals(OTHER_BOOKING, reopened.bookings().findById(connection, 2).orElseThrow());
                assertEquals(cancelled(OTHER_MEMBER_BOOKING, reason),
                        reopened.bookings().findById(connection, 4).orElseThrow());
            }
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void persistenceFailureRollsBackSourceAndAlreadyUpdatedBookings(boolean membershipCancellation) throws Exception {
        int failingBooking = membershipCancellation ? 2 : 4;
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER reject_booking_update BEFORE UPDATE ON booking WHEN NEW.id = "
                    + failingBooking + " BEGIN SELECT RAISE(ABORT, 'simulated disk write failure'); END");
        }
        assertThrows(SQLException.class, () -> persistence.unitOfWork().inTransaction(connection -> {
            cancel(connection, membershipCancellation);
            return null;
        }));
        Persistence reopened = reopen();
        reopened.unitOfWork().inTransaction(connection -> {
            assertEquals(MEMBERSHIP, reopened.memberships().findById(connection, 1).orElseThrow());
            assertEquals(SESSION, reopened.sessions().findById(connection, 1).orElseThrow());
            assertEquals(List.of(BOOKING, OTHER_BOOKING, PAST_BOOKING),
                    reopened.bookings().findByMemberId(connection, 1));
            assertEquals(OTHER_MEMBER_BOOKING, reopened.bookings().findById(connection, 4).orElseThrow());
            return null;
        });
    }

    @Test
    void repositoriesUseCallerTransactionWithoutCommittingOrClosingIt() throws Exception {
        try (Connection connection = database.openConnection(); Connection observer = database.openConnection()) {
            connection.setAutoCommit(false);
            MembershipPlan added = new MembershipPlan(2, "Uncommitted", 1, 0, false);
            persistence.plans().insert(connection, added);
            assertEquals(added, persistence.plans().findById(connection, 2).orElseThrow());
            assertTrue(persistence.plans().findById(observer, 2).isEmpty());
            assertFalse(connection.isClosed());
            assertFalse(connection.getAutoCommit());
            connection.rollback();
            assertTrue(persistence.plans().findById(connection, 2).isEmpty());
            connection.setAutoCommit(true);
            assertTrue(persistence.plans().findById(observer, 2).isEmpty());
        }
    }

    @Test
    void nestedWorkRollsBackWithOuterTransactionAcrossRepositories() throws Exception {
        assertThrows(SQLException.class, () -> persistence.unitOfWork().inTransaction(outer -> {
            persistence.plans().update(outer, new MembershipPlan(1, "Changed", 60, 0, true));
            persistence.unitOfWork().inTransaction(outer, inner -> {
                cancel(inner, true);
                return null;
            });
            persistence.bookings().insert(outer, BOOKING);
            return null;
        }));
        Persistence reopened = reopen();
        reopened.unitOfWork().inTransaction(connection -> {
            assertEquals(PLAN, reopened.plans().findById(connection, 1).orElseThrow());
            assertEquals(MEMBERSHIP, reopened.memberships().findById(connection, 1).orElseThrow());
            assertEquals(BOOKING, reopened.bookings().findById(connection, 1).orElseThrow());
            return null;
        });
    }

    @Test
    void rejectsDuplicateIdentitiesUsernamesAndBookingPairsWithoutReplacingRows() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            assertThrows(SQLException.class, () -> persistence.accounts().insert(connection, MEMBER));
            assertThrows(SQLException.class, () -> persistence.plans().insert(connection, PLAN));
            assertThrows(SQLException.class, () -> persistence.memberships().insert(connection, MEMBERSHIP));
            assertThrows(SQLException.class, () -> persistence.sessions().insert(connection, SESSION));
            assertThrows(SQLException.class, () -> persistence.bookings().insert(connection, BOOKING));
            assertThrows(SQLException.class, () -> persistence.accounts().insert(connection,
                    new Account(4, "MEMBER_ONE", PASSWORD, "Duplicate", Role.TRAINER, true)));
            persistence.bookings().update(connection, cancelled(BOOKING, CancellationReason.MEMBER_CANCELLED_BOOKING));
            assertThrows(SQLException.class, () -> persistence.bookings().insert(connection,
                    new Booking(5, 1, 1, NOW, BookingStatus.BOOKED, null)));
            assertEquals(MEMBER, persistence.accounts().findById(connection, 1).orElseThrow());
            assertEquals(PLAN, persistence.plans().findById(connection, 1).orElseThrow());
            assertEquals(MEMBERSHIP, persistence.memberships().findById(connection, 1).orElseThrow());
            assertEquals(SESSION, persistence.sessions().findById(connection, 1).orElseThrow());
            return null;
        });
    }

    @Test
    void foreignKeysRejectMissingReferences() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            assertThrows(SQLException.class, () -> persistence.memberships().insert(connection,
                    new Membership(2, 999, 1, MEMBERSHIP.startDate(), MEMBERSHIP.expiryDate(),
                            MembershipStatus.ACTIVE, 4990, 30)));
            assertThrows(SQLException.class, () -> persistence.memberships().insert(connection,
                    new Membership(2, 1, 999, MEMBERSHIP.startDate(), MEMBERSHIP.expiryDate(),
                            MembershipStatus.ACTIVE, 4990, 30)));
            assertThrows(SQLException.class, () -> persistence.sessions().insert(connection,
                    new TrainingSession(4, 999, NOW, 60, 10, null, false)));
            assertThrows(SQLException.class, () -> persistence.bookings().insert(connection,
                    new Booking(5, 999, 1, NOW, BookingStatus.BOOKED, null)));
            assertThrows(SQLException.class, () -> persistence.bookings().insert(connection,
                    new Booking(5, 1, 999, NOW, BookingStatus.BOOKED, null)));
            return null;
        });
    }

    @Test
    void rejectsMissingUpdateTargetsInsteadOfInsertingThem() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            assertThrows(SQLException.class, () -> persistence.accounts().update(connection,
                    new Account(99, "missing", PASSWORD, "Missing", Role.MEMBER, true)));
            assertThrows(SQLException.class, () -> persistence.plans().update(connection,
                    new MembershipPlan(99, "Missing", 1, 0, false)));
            assertThrows(SQLException.class, () -> persistence.memberships().update(connection,
                    new Membership(99, 1, 1, MEMBERSHIP.startDate(), MEMBERSHIP.expiryDate(),
                            MembershipStatus.ACTIVE, 4990, 30)));
            assertThrows(SQLException.class, () -> persistence.sessions().update(connection,
                    new TrainingSession(99, 2, NOW, 60, 10, null, false)));
            assertThrows(SQLException.class, () -> persistence.bookings().update(connection,
                    new Booking(99, 1, 1, NOW, BookingStatus.BOOKED, null)));
            return null;
        });
    }

    @Test
    void reactivatesCancelledBookingWithNewTimeAndClearsItsReason() throws Exception {
        LocalDateTime rebookedAt = NOW.plusMinutes(5);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.bookings().update(connection,
                    cancelled(BOOKING, CancellationReason.MEMBER_CANCELLED_BOOKING));
            persistence.bookings().reactivate(connection, BOOKING.id(), rebookedAt);
            Booking reactivated = persistence.bookings().findById(connection, BOOKING.id()).orElseThrow();
            assertEquals(new Booking(BOOKING.id(), BOOKING.sessionId(), BOOKING.memberId(), rebookedAt,
                    BookingStatus.BOOKED, null), reactivated);
            return null;
        });
    }

    @Test
    void reactivateRejectsMissingAndNonCancelledBookings() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            assertThrows(SQLException.class, () -> persistence.bookings().reactivate(connection, 99, NOW));
            assertThrows(SQLException.class, () -> persistence.bookings().reactivate(connection, BOOKING.id(), NOW));
            assertEquals(BOOKING, persistence.bookings().findById(connection, BOOKING.id()).orElseThrow());
            return null;
        });
    }

    @Test
    void rejectsChangesToHistoricalOwnershipAndPurchaseTerms() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            assertThrows(SQLException.class, () -> persistence.accounts().update(connection,
                    new Account(1, "member_one", PASSWORD, "Renamed", Role.MEMBER, true)));
            assertThrows(SQLException.class, () -> persistence.accounts().update(connection,
                    new Account(1, MEMBER.username(), PASSWORD, MEMBER.displayName(), Role.TRAINER, true)));
            assertThrows(SQLException.class, () -> persistence.memberships().update(connection,
                    new Membership(1, 3, 1, MEMBERSHIP.startDate(), MEMBERSHIP.expiryDate(),
                            MembershipStatus.ACTIVE, 4990, 30)));
            assertThrows(SQLException.class, () -> persistence.memberships().update(connection,
                    new Membership(1, 1, 1, MEMBERSHIP.startDate(), MEMBERSHIP.expiryDate(),
                            MembershipStatus.ACTIVE, 0, 30)));
            assertThrows(SQLException.class, () -> persistence.memberships().update(connection,
                    new Membership(1, 1, 1, MEMBERSHIP.startDate(), MEMBERSHIP.expiryDate(),
                            MembershipStatus.ACTIVE, 4990, 365)));
            assertThrows(SQLException.class, () -> persistence.memberships().update(connection,
                    new Membership(1, 1, 1, MEMBERSHIP.startDate().plusDays(1), MEMBERSHIP.expiryDate(),
                            MembershipStatus.ACTIVE, 4990, 30)));
            assertThrows(SQLException.class, () -> persistence.sessions().update(connection,
                    new TrainingSession(1, 3, NOW, 60, 10, null, false)));
            assertThrows(SQLException.class, () -> persistence.bookings().update(connection,
                    new Booking(1, 1, 1, BOOKING.bookedAt().plusNanos(1), BookingStatus.BOOKED, null)));
            assertThrows(SQLException.class, () -> persistence.bookings().update(connection,
                    new Booking(1, 2, 1, BOOKING.bookedAt(), BookingStatus.BOOKED, null)));
            assertThrows(SQLException.class, () -> persistence.bookings().update(connection,
                    new Booking(1, 1, 3, BOOKING.bookedAt(), BookingStatus.BOOKED, null)));
            assertEquals(MEMBER, persistence.accounts().findById(connection, 1).orElseThrow());
            assertEquals(MEMBERSHIP, persistence.memberships().findById(connection, 1).orElseThrow());
            assertEquals(BOOKING, persistence.bookings().findById(connection, 1).orElseThrow());
            return null;
        });
    }

    @Test
    void filteredQueriesRespectOrderingHistoryAndNanosecondCutoffs() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            assertEquals(List.of(SESSION, OTHER_SESSION), persistence.sessions().findUpcoming(connection, NOW));
            assertEquals(List.of(OTHER_SESSION), persistence.sessions().findUpcoming(connection, SESSION.startsAt()));
            assertEquals(List.of(SESSION, OTHER_SESSION),
                    persistence.sessions().findUpcoming(connection, SESSION.startsAt().minusNanos(1)));
            cancel(connection, false);
            assertEquals(List.of(OTHER_SESSION), persistence.sessions().findUpcoming(connection, NOW));
            assertEquals(3, persistence.sessions().findByTrainerId(connection, 2).size());
            assertEquals(List.of(MEMBERSHIP), persistence.memberships().findAll(connection));
            assertEquals(List.of(PLAN), persistence.plans().findAllAvailable(connection));
            assertThrows(UnsupportedOperationException.class, () ->
                    persistence.accounts().findAll(connection).clear());
            return null;
        });
    }

    @Test
    void absentLookupsReturnEmptyResults() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            assertTrue(persistence.accounts().findById(connection, 99).isEmpty());
            assertTrue(persistence.accounts().findByUsername(connection, "absent").isEmpty());
            assertTrue(persistence.plans().findById(connection, 99).isEmpty());
            assertTrue(persistence.memberships().findById(connection, 99).isEmpty());
            assertTrue(persistence.memberships().findByMemberId(connection, 99).isEmpty());
            assertTrue(persistence.sessions().findById(connection, 99).isEmpty());
            assertTrue(persistence.sessions().findByTrainerId(connection, 99).isEmpty());
            assertTrue(persistence.bookings().findById(connection, 99).isEmpty());
            assertTrue(persistence.bookings().findByMemberAndSession(connection, 99, 99).isEmpty());
            assertTrue(persistence.bookings().findByMemberId(connection, 99).isEmpty());
            assertTrue(persistence.bookings().findBySessionId(connection, 99).isEmpty());
            assertEquals(0, persistence.bookings().countBookedBySessionId(connection, 99));
            return null;
        });
    }

    @Test
    void preparedStatementsPreserveLiteralTextAndLongIdentifiers() throws Exception {
        Account account = new Account(3_000_000_000L, "large_id", PASSWORD,
                "'); DROP TABLE account; --", Role.MEMBER, true);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, account);
            assertEquals(account, persistence.accounts().findById(connection, account.id()).orElseThrow());
            assertEquals(4, persistence.accounts().findAll(connection).size());
            return null;
        });
    }

    @Test
    void malformedStoredRecordsAreReportedAsPersistenceFailures() throws Exception {
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE membership SET status = 'INVALID' WHERE id = 1");
            assertThrows(SQLException.class, () -> persistence.memberships().findById(connection, 1));
        }
    }

    private Persistence reopen() throws SQLException {
        Persistence reopened = new Persistence(new Database(databasePath));
        reopened.initialize();
        return reopened;
    }

    private void cancel(Connection connection, boolean membershipCancellation) throws SQLException {
        List<Booking> bookings;
        CancellationReason reason;
        if (membershipCancellation) {
            persistence.memberships().update(connection, membershipWithStatus(MembershipStatus.CANCELLED));
            bookings = persistence.bookings().findByMemberId(connection, 1);
            reason = CancellationReason.MEMBERSHIP_CANCELLED;
        } else {
            persistence.sessions().update(connection, new TrainingSession(1, SESSION.trainerId(), SESSION.startsAt(),
                    SESSION.durationMinutes(), SESSION.capacity(), SESSION.description(), true));
            bookings = persistence.bookings().findBySessionId(connection, 1);
            reason = CancellationReason.TRAINER_CANCELLED_SESSION;
        }
        for (Booking booking : bookings) {
            TrainingSession session = persistence.sessions().findById(connection, booking.sessionId()).orElseThrow();
            if (booking.status() == BookingStatus.BOOKED && !session.hasStartedAt(NOW)) {
                persistence.bookings().update(connection, cancelled(booking, reason));
            }
        }
    }

    private static Membership membershipWithStatus(MembershipStatus status) {
        return new Membership(1, 1, 1, MEMBERSHIP.startDate(), MEMBERSHIP.expiryDate(), status, 4990, 30);
    }

    private static Booking cancelled(Booking booking, CancellationReason reason) {
        return new Booking(booking.id(), booking.sessionId(), booking.memberId(), booking.bookedAt(),
                BookingStatus.CANCELLED, reason);
    }
}
