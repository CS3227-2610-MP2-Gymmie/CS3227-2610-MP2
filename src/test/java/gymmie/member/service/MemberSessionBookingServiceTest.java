package gymmie.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.MembershipPlan;
import gymmie.model.MembershipStatus;
import gymmie.model.Role;
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
import gymmie.testutil.TrainingSessionBuilder;

class MemberSessionBookingServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-26T04:00:00Z"),
            ZoneId.of("Asia/Singapore"));
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 12, 0);
    private InMemoryDatabase fixture;
    private Persistence persistence;
    private UserSession userSession;
    private AuthService auth;
    private Account member;
    private Account trainer;
    private MemberSessionBookingService service;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        userSession = new UserSession();
        auth = new AuthService(persistence.accounts(), persistence.unitOfWork(), userSession, new PasswordHasher());
        member = account(1, "member_one", Role.MEMBER);
        trainer = account(2, "trainer_one", Role.TRAINER);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, member);
            persistence.accounts().insert(connection, trainer);
            persistence.accounts().insert(connection, account(3, "member_two", Role.MEMBER));
            persistence.plans().insert(connection, new MembershipPlan(1, "Test plan", 30, 4990, false));
            persistence.sessions().insert(connection, new TrainingSessionBuilder().withId(1)
                    .withTrainerId(trainer.id()).withStartsAt(NOW.plusHours(1)).build());
            return null;
        });
        auth.login(member.username(), AccountBuilder.DEFAULT_PASSWORD);
        service = new MemberSessionBookingService(persistence.memberships(), persistence.sessions(),
                persistence.bookings(), persistence.unitOfWork(), new Permissions(persistence.accounts(), userSession),
                CLOCK);
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    void booksEligibleSessionAndPersistsItInMemberHistory() throws Exception {
        addMembership(TODAY.plusDays(1));

        Booking booking = service.book(1);

        assertEquals(new Booking(1, 1, member.id(), NOW, BookingStatus.BOOKED, null), booking);
        assertEquals(List.of(booking), persistence.unitOfWork().inTransaction(connection ->
                persistence.bookings().findByMemberId(connection, member.id())));
    }

    @Test
    void permitsSessionStartingOnInclusiveMembershipExpiryDate() throws Exception {
        addMembership(TODAY);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.sessions().update(connection, new TrainingSessionBuilder().withId(1)
                    .withTrainerId(trainer.id()).withStartsAt(NOW.plusHours(10)).build());
            return null;
        });

        assertEquals(BookingStatus.BOOKED, service.book(1).status());
    }

    @Test
    void rejectsBookingWithoutAnActiveMembership() {
        ConflictException failure = assertThrows(ConflictException.class, () -> service.book(1));
        assertEquals("An active membership is required to book sessions", failure.getMessage());
        assertNoBookings();
    }

    @Test
    void rejectsFullStartedAndAfterExpirySessions() throws Exception {
        addMembership(TODAY);
        insertBooking(2, 3, 1, BookingStatus.BOOKED);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.sessions().update(connection, new TrainingSessionBuilder().withId(1)
                    .withTrainerId(trainer.id()).withStartsAt(NOW.plusHours(1)).withCapacity(1).build());
            return null;
        });
        assertRejected("This session is full");

        persistence.unitOfWork().inTransaction(connection -> {
            persistence.sessions().update(connection, new TrainingSessionBuilder().withId(1)
                    .withTrainerId(trainer.id()).withStartsAt(NOW.minusNanos(1)).build());
            return null;
        });
        assertRejected("This session has already started");

        persistence.unitOfWork().inTransaction(connection -> {
            persistence.sessions().update(connection, new TrainingSessionBuilder().withId(1)
                    .withTrainerId(trainer.id()).withStartsAt(NOW.plusDays(1)).build());
            return null;
        });
        assertRejected("Your membership expires before this session starts");
    }

    @Test
    void rejectsAnActiveDuplicateBooking() throws Exception {
        addMembership(TODAY.plusDays(1));
        insertBooking(1, 1, 1, BookingStatus.BOOKED);

        assertRejected("You have already booked this session");
        assertEquals(BookingStatus.BOOKED, bookingsForMember().getFirst().status());
    }

    @Test
    void rebooksCancelledSessionAndReplacesBookingTime() throws Exception {
        addMembership(TODAY.plusDays(1));
        insertBooking(1, 1, 1, BookingStatus.CANCELLED);
        LocalDateTime originalBookedAt = bookingsForMember().getFirst().bookedAt();

        Booking rebooked = service.book(1);

        assertNotEquals(originalBookedAt, rebooked.bookedAt());
        assertEquals(new Booking(1, 1, member.id(), NOW, BookingStatus.BOOKED, null), rebooked);
        assertEquals(List.of(rebooked), bookingsForMember());
    }

    @Test
    void rejectsRebookingWhenTheSessionIsFullAndKeepsCancellationHistory() throws Exception {
        addMembership(TODAY.plusDays(1));
        insertBooking(1, 1, 1, BookingStatus.CANCELLED);
        insertBooking(2, 3, 1, BookingStatus.BOOKED);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.sessions().update(connection, new TrainingSessionBuilder().withId(1)
                    .withTrainerId(trainer.id()).withStartsAt(NOW.plusHours(1)).withCapacity(1).build());
            return null;
        });

        assertRejected("This session is full");
        assertEquals(BookingStatus.CANCELLED, bookingsForMember().getFirst().status());
    }

    @Test
    void rejectsRebookingWithAnExpiredMembership() throws Exception {
        addMembership(TODAY.minusDays(1));
        insertBooking(1, 1, 1, BookingStatus.CANCELLED);

        assertRejected("An active membership is required to book sessions");
        assertEquals(BookingStatus.CANCELLED, bookingsForMember().getFirst().status());
    }

    @Test
    void rejectsRebookingWhenSessionStartsAfterActiveMembershipExpiry() throws Exception {
        addMembership(TODAY);
        insertBooking(1, 1, 1, BookingStatus.CANCELLED);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.sessions().update(connection, new TrainingSessionBuilder().withId(1)
                    .withTrainerId(trainer.id()).withStartsAt(NOW.plusDays(1)).build());
            return null;
        });

        assertRejected("Your membership expires before this session starts");
        assertEquals(BookingStatus.CANCELLED, bookingsForMember().getFirst().status());
    }

    @Test
    void persistenceFailureDoesNotPublishAPartialBooking() throws Exception {
        addMembership(TODAY.plusDays(1));
        persistence.unitOfWork().inTransaction(connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TRIGGER fail_booking_insert BEFORE INSERT ON booking "
                        + "BEGIN SELECT RAISE(ABORT, 'booking write failed'); END");
            }
            return null;
        });

        assertThrows(Exception.class, () -> service.book(1));
        assertNoBookings();
    }

    @Test
    void requiresAnAuthenticatedMember() throws Exception {
        auth.logout();
        assertThrows(AuthenticationException.class, () -> service.book(1));
        auth.login(trainer.username(), AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> service.book(1));
        assertNoBookings();
    }

    private void addMembership(LocalDate expiryDate) throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.memberships().insert(connection, new MembershipBuilder().withMemberId(member.id())
                    .withExpiryDate(expiryDate).withStatus(MembershipStatus.ACTIVE).build());
            return null;
        });
    }

    private void insertBooking(long bookingId, long bookingMemberId, long sessionId, BookingStatus status)
            throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.bookings().insert(connection, new BookingBuilder().withId(bookingId)
                    .withSessionId(sessionId).withMemberId(bookingMemberId).withStatus(status)
                    .withCancellationReason(status == BookingStatus.CANCELLED
                            ? gymmie.model.CancellationReason.MEMBER_CANCELLED_BOOKING : null)
                    .build());
            return null;
        });
    }

    private void assertRejected(String message) {
        ConflictException failure = assertThrows(ConflictException.class, () -> service.book(1));
        assertEquals(message, failure.getMessage());
    }

    private void assertNoBookings() {
        assertTrue(bookingsForMember().isEmpty());
    }

    private List<Booking> bookingsForMember() {
        try {
            return persistence.unitOfWork().inTransaction(connection ->
                    persistence.bookings().findByMemberId(connection, member.id()));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static Account account(long id, String username, Role role) {
        return new AccountBuilder().withId(id).withUsername(username).withDisplayName(username)
                .withRole(role).withActive(true).build();
    }
}
