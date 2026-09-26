package gymmie.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gymmie.model.Account;
import gymmie.model.Membership;
import gymmie.model.MembershipPlan;
import gymmie.model.MembershipStatus;
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
import gymmie.testutil.InMemoryDatabase;
import gymmie.testutil.MembershipPlanBuilder;

class MembershipStatusServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private InMemoryDatabase fixture;
    private Persistence persistence;
    private UserSession session;
    private AuthService auth;
    private Account member;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        session = new UserSession();
        auth = new AuthService(persistence.accounts(), persistence.unitOfWork(), session, new PasswordHasher());
        member = new AccountBuilder().build();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, member);
            persistence.plans().insert(connection, new MembershipPlanBuilder().withArchived(true).build());
            return null;
        });
        auth.login(member.username(), AccountBuilder.DEFAULT_PASSWORD);
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    void coverageIncludesBothBoundariesAndLeavesHistoryUnchanged() throws Exception {
        Membership membership = membership(1, TODAY, TODAY.plusDays(30), MembershipStatus.ACTIVE);
        insert(membership);
        for (LocalDate date : List.of(TODAY, TODAY.plusDays(30))) {
            var current = service(date).currentMembership().orElseThrow();
            assertEquals("Monthly", current.planName());
            assertEquals(TODAY.plusDays(30), current.expiryDate());
        }
        assertTrue(service(TODAY.minusDays(1)).currentMembership().isEmpty());
        assertTrue(service(TODAY.plusDays(31)).currentMembership().isEmpty());
        assertEquals(List.of(membership), persistence.unitOfWork().inTransaction(connection ->
                persistence.memberships().findByMemberId(connection, member.id())));
    }

    @Test
    void emptyExpiredCancelledAndFutureHistoryAreInactive() throws Exception {
        assertTrue(service(TODAY).currentMembership().isEmpty());
        insert(membership(1, TODAY.minusDays(30), TODAY.minusDays(1), MembershipStatus.ACTIVE));
        insert(membership(2, TODAY, TODAY.plusDays(30), MembershipStatus.CANCELLED));
        insert(membership(3, TODAY.plusDays(1), TODAY.plusDays(31), MembershipStatus.ACTIVE));
        assertTrue(service(TODAY).currentMembership().isEmpty());
    }

    @Test
    void renewableMembershipShowsExpiredArchivedPlanAndSkipsFutureMembership() throws Exception {
        insert(membership(1, TODAY.plusDays(1), TODAY.plusDays(31), MembershipStatus.ACTIVE));
        assertTrue(service(TODAY).renewableMembership().isEmpty());
        insert(membership(2, TODAY.minusDays(40), TODAY.minusDays(10), MembershipStatus.EXPIRED));
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.plans().insert(connection, new MembershipPlan(2, "Cancelled plan", 30, 5990, false));
            persistence.memberships().insert(connection,
                    new Membership(3, member.id(), 2, TODAY.minusDays(5), TODAY.plusDays(25),
                            MembershipStatus.CANCELLED, 5990, 30));
            return null;
        });

        var renewable = service(TODAY).renewableMembership().orElseThrow();

        assertEquals(2, renewable.membershipId());
        assertEquals("Monthly", renewable.planName());
        assertEquals(TODAY.minusDays(10), renewable.expiryDate());
        assertFalse(renewable.activeToday());
    }

    @Test
    void currentCoverageWinsOverOtherHistory() throws Exception {
        insert(membership(1, TODAY, TODAY.plusDays(30), MembershipStatus.ACTIVE));
        insert(membership(2, TODAY.plusDays(31), TODAY.plusDays(60), MembershipStatus.ACTIVE));
        insert(membership(3, TODAY, TODAY.plusDays(90), MembershipStatus.CANCELLED));
        assertEquals(TODAY.plusDays(30), service(TODAY).currentMembership().orElseThrow().expiryDate());
    }

    @Test
    void readsOnlySignedInMembersHistory() throws Exception {
        insert(membership(1, TODAY, TODAY.plusDays(30), MembershipStatus.ACTIVE));
        Account other = new AccountBuilder().withId(2).withUsername("other").build();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, other);
            persistence.plans().insert(connection, new MembershipPlan(2, "Other plan", 30, 5990, false));
            persistence.memberships().insert(connection,
                    new Membership(2, other.id(), 2, TODAY, TODAY.plusDays(30), MembershipStatus.ACTIVE,
                            5990, 30));
            return null;
        });
        auth.login(other.username(), AccountBuilder.DEFAULT_PASSWORD);
        assertEquals("Other plan", service(TODAY).currentMembership().orElseThrow().planName());
    }

    @Test
    void rejectsMissingSessionOtherRolesAndPersistedDeactivation() throws Exception {
        auth.logout();
        assertThrows(AuthenticationException.class, () -> service(TODAY).currentMembership());
        for (Role role : List.of(Role.MANAGER, Role.TRAINER)) {
            Account other = new AccountBuilder().withId(role.ordinal() + 10).withUsername(role.name())
                    .withRole(role).build();
            persistence.unitOfWork().inTransaction(connection -> {
                persistence.accounts().insert(connection, other);
                return null;
            });
            auth.login(other.username(), AccountBuilder.DEFAULT_PASSWORD);
            assertThrows(AuthorizationException.class, () -> service(TODAY).currentMembership());
        }
        auth.login(member.username(), AccountBuilder.DEFAULT_PASSWORD);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().update(connection, new AccountBuilder(member).withActive(false).build());
            return null;
        });
        assertThrows(AccountDeactivatedException.class, () -> service(TODAY).currentMembership());
    }

    private MembershipStatusService service(LocalDate date) {
        return new MembershipStatusService(persistence.memberships(), persistence.plans(), persistence.unitOfWork(),
                new Permissions(persistence.accounts(), session),
                Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private Membership membership(long id, LocalDate start, LocalDate expiry, MembershipStatus status) {
        return new Membership(id, member.id(), 1, start, expiry, status, 4990, 30);
    }

    private void insert(Membership membership) throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.memberships().insert(connection, membership);
            return null;
        });
    }
}
