package gymmie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private Account member;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        session = new UserSession();
        member = new AccountBuilder().build();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, member);
            persistence.plans().insert(connection, new MembershipPlanBuilder().withArchived(true).build());
            return null;
        });
        session.establish(member);
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
        session.establish(other);
        assertEquals("Other plan", service(TODAY).currentMembership().orElseThrow().planName());
    }

    @Test
    void rejectsMissingSessionOtherRolesAndPersistedDeactivation() throws Exception {
        session.clear();
        assertThrows(AuthenticationException.class, () -> service(TODAY).currentMembership());
        for (Role role : List.of(Role.MANAGER, Role.TRAINER)) {
            Account other = new AccountBuilder().withId(role.ordinal() + 10).withUsername(role.name())
                    .withRole(role).build();
            persistence.unitOfWork().inTransaction(connection -> {
                persistence.accounts().insert(connection, other);
                return null;
            });
            session.establish(other);
            assertThrows(AuthorizationException.class, () -> service(TODAY).currentMembership());
        }
        session.establish(member);
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
