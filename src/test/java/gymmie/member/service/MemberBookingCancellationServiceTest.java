package gymmie.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.MembershipPlan;
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
import gymmie.testutil.BookingBuilder;
import gymmie.testutil.InMemoryDatabase;
import gymmie.testutil.MembershipBuilder;

class MemberBookingCancellationServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-26T04:00:00Z"),
            ZoneId.of("Asia/Singapore"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 12, 0);
    private InMemoryDatabase fixture;
    private Persistence persistence;
    private UserSession userSession;
    private AuthService auth;
    private Account member;
    private MemberBookingCancellationService service;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        userSession = new UserSession();
        auth = new AuthService(persistence.accounts(), persistence.unitOfWork(), userSession, new PasswordHasher());
        member = account(1, "member_one", Role.MEMBER);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, member);
            persistence.accounts().insert(connection, account(2, "member_two", Role.MEMBER));
            persistence.accounts().insert(connection, account(3, "trainer_one", Role.TRAINER));
            persistence.plans().insert(connection, new MembershipPlan(1, "Test plan", 30, 4990, false));
            persistence.sessions().insert(connection,
                    new TrainingSession(1, 3, NOW.plusHours(1), 60, 1, "Workout", false));
            persistence.memberships().insert(connection, new MembershipBuilder().withId(1).withMemberId(1).build());
            persistence.memberships().insert(connection, new MembershipBuilder().withId(2).withMemberId(2).build());
            persistence.bookings().insert(connection, booking(1, 1, BookingStatus.BOOKED));
            return null;
        });
        auth.login(member.username(), AccountBuilder.DEFAULT_PASSWORD);
        service = new MemberBookingCancellationService(persistence.bookings(), persistence.sessions(),
                persistence.unitOfWork(), new Permissions(persistence.accounts(), userSession), CLOCK);
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    void cancellationReleasesCapacityForAnotherMember() throws Exception {
        service.cancel(1);

        Booking cancelled = bookingForMember(1);
        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        assertEquals(CancellationReason.MEMBER_CANCELLED_BOOKING, cancelled.cancellationReason());
        int currentBookings = persistence.unitOfWork().inTransaction(connection ->
                persistence.bookings().countBookedBySessionId(connection, 1));
        assertEquals(0, currentBookings);

        auth.logout();
        auth.login("member_two", AccountBuilder.DEFAULT_PASSWORD);
        MemberSessionBookingService bookingService = new MemberSessionBookingService(persistence.memberships(),
                persistence.sessions(), persistence.bookings(), persistence.unitOfWork(),
                new Permissions(persistence.accounts(), userSession), CLOCK);
        assertEquals(BookingStatus.BOOKED, bookingService.book(1).status());
    }

    @Test
    void rejectsCancellationAtOrAfterSessionStartAndPreservesBooking() throws Exception {
        for (LocalDateTime startsAt : new LocalDateTime[] {NOW, NOW.minusNanos(1)}) {
            persistence.unitOfWork().inTransaction(connection -> {
                persistence.sessions().update(connection,
                        new TrainingSession(1, 3, startsAt, 60, 1, "Workout", false));
                return null;
            });

            ConflictException failure = assertThrows(ConflictException.class, () -> service.cancel(1));

            assertEquals("A booking cannot be cancelled after the session has started", failure.getMessage());
            assertEquals(BookingStatus.BOOKED, bookingForMember(1).status());
        }
    }

    @Test
    void rejectsAnotherMembersExistingBooking() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.bookings().insert(connection, booking(2, 2, BookingStatus.BOOKED));
            return null;
        });

        assertThrows(ConflictException.class, () -> service.cancel(2));
        assertEquals(BookingStatus.BOOKED, bookingForMember(2).status());
    }

    @Test
    void requiresAuthenticatedMember() throws Exception {
        auth.logout();
        assertThrows(AuthenticationException.class, () -> service.cancel(1));
        auth.login("trainer_one", AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> service.cancel(1));
        assertEquals(BookingStatus.BOOKED, bookingForMember(1).status());
    }

    private Booking booking(long id, long memberId, BookingStatus status) {
        return new BookingBuilder().withId(id).withSessionId(1).withMemberId(memberId).withStatus(status)
                .withCancellationReason(status == BookingStatus.CANCELLED
                        ? CancellationReason.MEMBER_CANCELLED_BOOKING : null)
                .build();
    }

    private Booking bookingForMember(long memberId) {
        try {
            return persistence.unitOfWork().inTransaction(connection ->
                    persistence.bookings().findByMemberId(connection, memberId).stream().findFirst().orElseThrow());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static Account account(long id, String username, Role role) {
        return new AccountBuilder().withId(id).withUsername(username).withDisplayName(username)
                .withRole(role).withActive(true).build();
    }
}
