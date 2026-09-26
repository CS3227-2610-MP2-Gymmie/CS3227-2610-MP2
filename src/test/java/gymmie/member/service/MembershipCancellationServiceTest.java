package gymmie.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Membership;
import gymmie.model.MembershipPlan;
import gymmie.model.MembershipStatus;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.model.exception.ConflictException;
import gymmie.persistence.Persistence;
import gymmie.service.AuthService;
import gymmie.service.PasswordHasher;
import gymmie.service.Permissions;
import gymmie.service.UserSession;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.InMemoryDatabase;

class MembershipCancellationServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 12, 0);
    private InMemoryDatabase fixture;
    private Persistence persistence;
    private UserSession session;
    private AuthService auth;
    private Account member;
    private MembershipCancellationService service;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        session = new UserSession();
        auth = new AuthService(persistence.accounts(), persistence.unitOfWork(), session, new PasswordHasher());
        member = new AccountBuilder().build();
        Account otherMember = new AccountBuilder().withId(2).withUsername("other-member").build();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, member);
            persistence.accounts().insert(connection, otherMember);
            for (int id = 11; id <= 14; id++) {
                persistence.accounts().insert(connection, new AccountBuilder().withId(id)
                        .withUsername("trainer-" + id).withRole(Role.TRAINER).build());
            }
            persistence.plans().insert(connection, new MembershipPlan(1, "Monthly", 30, 4990, false));
            persistence.memberships().insert(connection, activeMembership(1, member.id()));
            persistence.sessions().insert(connection, trainingSession(1, NOW.plusHours(1)));
            persistence.sessions().insert(connection, trainingSession(2, NOW.minusHours(1)));
            persistence.sessions().insert(connection, trainingSession(3, NOW.plusHours(2)));
            persistence.sessions().insert(connection, trainingSession(4, NOW.plusHours(3)));
            persistence.bookings().insert(connection, booked(1, 1, member.id()));
            persistence.bookings().insert(connection, booked(2, 2, member.id()));
            persistence.bookings().insert(connection, booked(3, 1, otherMember.id()));
            persistence.bookings().insert(connection, booked(4, 3, member.id()));
            persistence.bookings().insert(connection, new Booking(5, 4, member.id(), NOW.minusDays(1),
                    BookingStatus.CANCELLED, CancellationReason.MEMBER_CANCELLED_BOOKING));
            return null;
        });
        auth.login(member.username(), AccountBuilder.DEFAULT_PASSWORD);
        service = service();
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    void cancellationImmediatelyUpdatesMembershipAndOnlyOwnFutureBookings() throws Exception {
        assertEquals(2, service.cancel(1));

        Membership cancelled = findMembership();
        assertEquals(MembershipStatus.CANCELLED, cancelled.status());
        assertEquals(4990, cancelled.snapshotPriceCents());
        assertEquals(30, cancelled.snapshotDurationDays());
        assertEquals(cancelled(1, 1, member.id(), CancellationReason.MEMBERSHIP_CANCELLED), findBooking(1));
        assertEquals(booked(2, 2, member.id()), findBooking(2));
        assertEquals(booked(3, 1, 2), findBooking(3));
        assertEquals(cancelled(4, 3, member.id(), CancellationReason.MEMBERSHIP_CANCELLED), findBooking(4));
        assertEquals(CancellationReason.MEMBER_CANCELLED_BOOKING, findBooking(5).cancellationReason());
    }

    @Test
    void bookingPersistenceFailureRollsBackMembershipAndEarlierBookingUpdates() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TRIGGER fail_second_booking BEFORE UPDATE ON booking "
                        + "WHEN OLD.id = 4 BEGIN SELECT RAISE(ABORT, 'forced failure'); END");
            }
            return null;
        });

        assertThrows(Exception.class, () -> service.cancel(1));

        assertEquals(MembershipStatus.ACTIVE, findMembership().status());
        assertEquals(BookingStatus.BOOKED, findBooking(1).status());
    }

    @Test
    void rejectsStaleMembershipAndRequiresMemberAuthorization() throws Exception {
        assertThrows(ConflictException.class, () -> service.cancel(99));
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection,
                    new AccountBuilder().withId(3).withUsername("manager").withRole(Role.MANAGER).build());
            return null;
        });
        auth.login("manager", AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> service.cancel(1));
        auth.logout();
        assertThrows(AuthenticationException.class, () -> service.cancel(1));
        assertEquals(MembershipStatus.ACTIVE, findMembership().status());
    }

    private MembershipCancellationService service() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC);
        return new MembershipCancellationService(persistence.memberships(), persistence.bookings(),
                persistence.sessions(), persistence.unitOfWork(), new Permissions(persistence.accounts(), session),
                clock);
    }

    private Membership findMembership() throws Exception {
        return persistence.unitOfWork().inTransaction(connection ->
                persistence.memberships().findById(connection, 1).orElseThrow());
    }

    private Booking findBooking(long id) throws Exception {
        return persistence.unitOfWork().inTransaction(connection ->
                persistence.bookings().findById(connection, id).orElseThrow());
    }

    private static Membership activeMembership(long id, long memberId) {
        return new Membership(id, memberId, 1, TODAY.minusDays(1), TODAY.plusDays(30),
                MembershipStatus.ACTIVE, 4990, 30);
    }

    private static TrainingSession trainingSession(long id, LocalDateTime startsAt) {
        return new TrainingSession(id, 10 + id, startsAt, 60, 10, "Workout", false);
    }

    private static Booking booked(long id, long sessionId, long memberId) {
        return new Booking(id, sessionId, memberId, NOW.minusDays(1), BookingStatus.BOOKED, null);
    }

    private static Booking cancelled(long id, long sessionId, long memberId, CancellationReason reason) {
        return new Booking(id, sessionId, memberId, NOW.minusDays(1), BookingStatus.CANCELLED, reason);
    }
}
