package gymmie.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gymmie.manager.service.AccountProvisioningService;
import gymmie.member.service.MemberBookingCancellationService;
import gymmie.member.service.MemberBookingHistoryService;
import gymmie.member.service.MemberBookingHistoryService.MemberBooking;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.persistence.Persistence;
import gymmie.service.AuthService;
import gymmie.service.PasswordHasher;
import gymmie.service.Permissions;
import gymmie.service.UserSession;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.InMemoryDatabase;

class TrainerBookingVisibilityTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-29T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private InMemoryDatabase fixture;
    private Persistence persistence;
    private AuthService auth;
    private Permissions permissions;
    private AccountProvisioningService accounts;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        UserSession userSession = new UserSession();
        PasswordHasher hasher = new PasswordHasher();
        auth = new AuthService(persistence.accounts(), persistence.unitOfWork(), userSession, hasher);
        permissions = new Permissions(persistence.accounts(), userSession);
        accounts = new AccountProvisioningService(persistence.accounts(), persistence.bookings(),
                persistence.sessions(), persistence.unitOfWork(), permissions, hasher, CLOCK);
        persistence.unitOfWork().inTransaction(connection -> {
            for (int id = 1; id <= 4; id++) {
                persistence.accounts().insert(connection, new AccountBuilder().withId(id).withUsername("user_" + id)
                        .withRole(id == 1 ? Role.MANAGER : id == 2 ? Role.MEMBER : Role.TRAINER).build());
            }
            return null;
        });
        addBooking(1, 3, NOW.plusDays(1), null);
        addBooking(2, 3, NOW.minusDays(1), null);
        addBooking(3, 3, NOW, null);
        addBooking(4, 3, NOW.plusDays(2), CancellationReason.MEMBER_CANCELLED_BOOKING);
        addBooking(5, 3, NOW.plusDays(2), CancellationReason.TRAINER_CANCELLED_SESSION);
        addBooking(6, 3, NOW.plusDays(2), CancellationReason.MEMBERSHIP_CANCELLED);
        addBooking(7, 3, NOW.plusDays(2), CancellationReason.ACCOUNT_DEACTIVATED);
        addBooking(8, 4, NOW.plusDays(1), null);
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    void trainerToggleMovesOnlyUpcomingBookingsAndPreservesStoredHistory() throws Exception {
        List<Booking> stored = storedBookings();
        List<MemberBooking> original = history(CLOCK);
        assertEquals(List.of(1L, 8L), upcomingIds(original, NOW));
        for (int cycle = 0; cycle < 2; cycle++) {
            toggleTrainer(false);
            List<MemberBooking> inactive = history(CLOCK);
            assertEquals(List.of(8L), upcomingIds(inactive, NOW));
            assertEquals(5, MemberBookingsController.cancelledBookings(inactive).size());
            assertEquals(BookingStatus.CANCELLED, inactive.getFirst().status());
            assertEquals(CancellationReason.TRAINER_ACCOUNT_DEACTIVATED,
                    inactive.getFirst().cancellationReason());
            assertNull(inactive.getFirst().trainerCancellationReason());
            assertEquals(original.subList(1, original.size()), inactive.subList(1, inactive.size()));
            assertEquals(stored, storedBookings());
            toggleTrainer(true);
            assertEquals(original, history(CLOCK));
            assertEquals(stored, storedBookings());
        }
    }

    @Test
    void reactivationAfterStartDoesNotRestoreExpiredBookingToUpcoming() throws Exception {
        toggleTrainer(false);
        Clock later = Clock.offset(CLOCK, java.time.Duration.ofDays(1));
        List<MemberBooking> before = history(later);
        toggleTrainer(true);
        List<MemberBooking> after = history(later);
        assertEquals(before, after);
        assertEquals(List.of(), upcomingIds(after, NOW.plusDays(1)));
        assertEquals(4, MemberBookingsController.pastBookings(after, NOW.plusDays(1)).size());
    }

    @Test
    void explicitCancellationWhileTrainerInactiveIsNotRestored() throws Exception {
        toggleTrainer(false);
        auth.login("user_2", AccountBuilder.DEFAULT_PASSWORD);
        new MemberBookingCancellationService(persistence.bookings(), persistence.sessions(),
                persistence.unitOfWork(), permissions, CLOCK).cancel(1);
        toggleTrainer(true);
        assertEquals(CancellationReason.MEMBER_CANCELLED_BOOKING, history(CLOCK).getFirst().cancellationReason());
        assertEquals(List.of(8L), upcomingIds(history(CLOCK), NOW));
    }

    @Test
    void historyStillRequiresAuthenticatedMember() throws Exception {
        assertThrows(AuthenticationException.class, () -> reader(CLOCK).bookingHistory());
        auth.login("user_1", AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> reader(CLOCK).bookingHistory());
        auth.login("user_3", AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> reader(CLOCK).bookingHistory());
    }

    private void addBooking(long id, long trainerId, LocalDateTime startsAt,
            CancellationReason reason) throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.sessions().insert(connection, new TrainingSession(id, trainerId, startsAt, 60, 10,
                    "Workout", reason == CancellationReason.TRAINER_CANCELLED_SESSION,
                    reason == CancellationReason.TRAINER_CANCELLED_SESSION ? "Unavailable" : null));
            persistence.bookings().insert(connection, new Booking(id, id, 2, NOW.minusDays(5),
                    reason == null ? BookingStatus.BOOKED : BookingStatus.CANCELLED, reason));
            return null;
        });
    }

    private void toggleTrainer(boolean active) throws Exception {
        auth.login("user_1", AccountBuilder.DEFAULT_PASSWORD);
        if (active) {
            accounts.reactivate(3);
        } else {
            accounts.deactivate(3);
        }
    }

    private List<Booking> storedBookings() throws Exception {
        return persistence.unitOfWork().inTransaction(connection ->
                persistence.bookings().findByMemberId(connection, 2));
    }

    private List<MemberBooking> history(Clock clock) throws Exception {
        auth.login("user_2", AccountBuilder.DEFAULT_PASSWORD);
        return reader(clock).bookingHistory();
    }

    private MemberBookingHistoryService reader(Clock clock) {
        return new MemberBookingHistoryService(persistence.bookings(), persistence.sessions(), persistence.accounts(),
                persistence.unitOfWork(), permissions, clock);
    }

    private static List<Long> upcomingIds(List<MemberBooking> history, LocalDateTime now) {
        return MemberBookingsController.upcomingBookings(history, now).stream().map(MemberBooking::bookingId).toList();
    }
}
