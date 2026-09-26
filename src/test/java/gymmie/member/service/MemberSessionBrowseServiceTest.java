package gymmie.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gymmie.member.service.MemberSessionBrowseService.BrowseSession;
import gymmie.model.Account;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Role;
import gymmie.persistence.Persistence;
import gymmie.service.AuthService;
import gymmie.service.PasswordHasher;
import gymmie.service.Permissions;
import gymmie.service.UserSession;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.BookingBuilder;
import gymmie.testutil.InMemoryDatabase;
import gymmie.testutil.TrainingSessionBuilder;

class MemberSessionBrowseServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-26T04:00:00Z"),
            ZoneId.of("Asia/Singapore"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 12, 0);
    private InMemoryDatabase fixture;
    private Persistence persistence;
    private UserSession session;
    private AuthService auth;
    private Account member;
    private Account trainer;
    private Account secondTrainer;
    private Account inactiveTrainer;
    private MemberSessionBrowseService service;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        session = new UserSession();
        auth = new AuthService(persistence.accounts(), persistence.unitOfWork(), session, new PasswordHasher());
        member = account(1, "member_one", "Member One", Role.MEMBER, true);
        trainer = account(2, "trainer_one", "Bri Coach", Role.TRAINER, true);
        secondTrainer = account(3, "trainer_two", "Ari Coach", Role.TRAINER, true);
        inactiveTrainer = account(4, "trainer_old", "Inactive Coach", Role.TRAINER, false);
        persistence.unitOfWork().inTransaction(connection -> {
            for (Account account : List.of(member, trainer, secondTrainer, inactiveTrainer,
                    account(5, "member_two", "Member Two", Role.MEMBER, true),
                    account(6, "member_three", "Member Three", Role.MEMBER, true))) {
                persistence.accounts().insert(connection, account);
            }
            return null;
        });
        auth.login(member.username(), AccountBuilder.DEFAULT_PASSWORD);
        service = new MemberSessionBrowseService(persistence.accounts(), persistence.sessions(),
                persistence.bookings(), persistence.unitOfWork(), new Permissions(persistence.accounts(), session),
                CLOCK);
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    void listsOrderedUpcomingSessionsWithAllDetailsAndCurrentBookingCounts() throws Exception {
        var later = new TrainingSessionBuilder().withId(10).withTrainerId(trainer.id())
                .withStartsAt(NOW.plusHours(2)).withDurationMinutes(75).withCapacity(8)
                .withDescription("Strength and mobility").build();
        var earlier = new TrainingSessionBuilder().withId(20).withTrainerId(secondTrainer.id())
                .withStartsAt(NOW.plusHours(1)).withDurationMinutes(30).withCapacity(4)
                .withDescription("Intro session").build();
        var past = new TrainingSessionBuilder().withId(30).withTrainerId(trainer.id())
                .withStartsAt(NOW.minusNanos(1)).build();
        var cancelled = new TrainingSessionBuilder().withId(40).withTrainerId(trainer.id())
                .withStartsAt(NOW.plusHours(3)).withCancelled(true).build();
        var inactiveTrainerSession = new TrainingSessionBuilder().withId(50).withTrainerId(inactiveTrainer.id())
                .withStartsAt(NOW.plusHours(4)).build();
        persistence.unitOfWork().inTransaction(connection -> {
            for (var trainingSession : List.of(later, earlier, past, cancelled, inactiveTrainerSession)) {
                persistence.sessions().insert(connection, trainingSession);
            }
            persistence.bookings().insert(connection, booking(1, later.id(), member.id(), BookingStatus.BOOKED));
            persistence.bookings().insert(connection, booking(2, later.id(), 5, BookingStatus.BOOKED));
            persistence.bookings().insert(connection, new BookingBuilder().withId(3).withSessionId(later.id())
                    .withMemberId(6).withStatus(BookingStatus.CANCELLED)
                    .withCancellationReason(CancellationReason.MEMBER_CANCELLED_BOOKING).build());
            persistence.bookings().insert(connection, new BookingBuilder().withId(4).withSessionId(earlier.id())
                    .withMemberId(member.id()).withStatus(BookingStatus.CANCELLED)
                    .withCancellationReason(CancellationReason.MEMBER_CANCELLED_BOOKING).build());
            persistence.bookings().insert(connection, booking(5, earlier.id(), 5, BookingStatus.BOOKED));
            return null;
        });

        List<BrowseSession> results = service.upcomingSessions();

        assertEquals(List.of(20L, 10L), results.stream().map(BrowseSession::sessionId).toList());
        assertEquals(new BrowseSession(20, secondTrainer.id(), secondTrainer.displayName(), earlier.startsAt(),
                30, "Intro session", 4, 1, false), results.get(0));
        assertEquals(new BrowseSession(10, trainer.id(), trainer.displayName(), later.startsAt(),
                75, "Strength and mobility", 8, 2, true), results.get(1));
        assertTrue(results.stream().noneMatch(item -> item.sessionId() == past.id()
                || item.sessionId() == cancelled.id() || item.sessionId() == inactiveTrainerSession.id()));
    }

    @Test
    void rejectsUnauthenticatedNonMemberAndDeactivatedMemberCallers() throws Exception {
        auth.logout();
        assertThrows(AuthenticationException.class, () -> service.upcomingSessions());

        Account otherTrainer = trainer;
        auth.login(otherTrainer.username(), AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> service.upcomingSessions());
        Account manager = account(10, "manager_one", "Manager One", Role.MANAGER, true);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, manager);
            return null;
        });
        auth.login(manager.username(), AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> service.upcomingSessions());

        auth.login(member.username(), AccountBuilder.DEFAULT_PASSWORD);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().update(connection, new AccountBuilder(member).withActive(false).build());
            return null;
        });
        assertThrows(AccountDeactivatedException.class, () -> service.upcomingSessions());
        assertFalse(session.isAuthenticated());
    }

    private static Account account(long id, String username, String displayName, Role role, boolean active) {
        return new AccountBuilder().withId(id).withUsername(username).withDisplayName(displayName)
                .withRole(role).withActive(active).build();
    }

    private static gymmie.model.Booking booking(long id, long sessionId, long memberId, BookingStatus status) {
        return new BookingBuilder().withId(id).withSessionId(sessionId).withMemberId(memberId)
                .withStatus(status).build();
    }
}
