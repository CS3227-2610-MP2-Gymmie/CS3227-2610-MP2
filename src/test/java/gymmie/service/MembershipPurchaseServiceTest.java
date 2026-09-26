package gymmie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
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
import gymmie.model.exception.ConflictException;
import gymmie.persistence.Persistence;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.InMemoryDatabase;
import gymmie.testutil.MembershipPlanBuilder;

class MembershipPurchaseServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private InMemoryDatabase fixture;
    private Persistence persistence;
    private UserSession session;
    private Account member;
    private MembershipPurchaseService service;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        session = new UserSession();
        member = new AccountBuilder().build();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, member);
            persistence.plans().insert(connection,
                    new MembershipPlanBuilder().withId(1).withName("Monthly").withDurationDays(30)
                            .withPriceCents(4990).build());
            persistence.plans().insert(connection,
                    new MembershipPlanBuilder().withId(2).withName("Hidden").withArchived(true).build());
            return null;
        });
        session.establish(member);
        service = service();
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    void availablePlansExcludesArchivedPlansAndRequiresMemberRole() throws Exception {
        assertEquals(List.of(1L), service.availablePlans().stream().map(MembershipPlan::id).toList());
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, new AccountBuilder().withId(2).withUsername("manager2")
                    .withRole(Role.MANAGER).build());
            persistence.accounts().insert(connection, new AccountBuilder().withId(3).withUsername("trainer")
                    .withRole(Role.TRAINER).build());
            return null;
        });
        for (Account unauthorized : persistence.unitOfWork().inTransaction(connection ->
                List.of(persistence.accounts().findById(connection, 2).orElseThrow(),
                        persistence.accounts().findById(connection, 3).orElseThrow()))) {
            session.establish(unauthorized);
            assertThrows(AuthorizationException.class, () -> service.availablePlans());
            assertThrows(AuthorizationException.class, () -> service.purchase(1));
        }
        session.clear();
        assertThrows(AuthenticationException.class, () -> service.availablePlans());
        assertThrows(AuthenticationException.class, () -> service.purchase(1));
        List<Membership> history = persistence.unitOfWork().inTransaction(connection ->
                persistence.memberships().findByMemberId(connection, member.id()));
        assertTrue(history.isEmpty());
    }

    @Test
    void purchaseActivatesMemberAndKeepsPurchaseTimeTerms() throws Exception {
        Membership purchased = service.purchase(1);
        assertEquals(member.id(), purchased.memberId());
        assertEquals(MembershipStatus.ACTIVE, purchased.status());
        assertEquals(TODAY, purchased.startDate());
        assertEquals(TODAY.plusDays(30), purchased.expiryDate());
        assertEquals(4990, purchased.snapshotPriceCents());
        assertEquals(30, purchased.snapshotDurationDays());

        persistence.unitOfWork().inTransaction(connection -> {
            persistence.plans().update(connection,
                    new MembershipPlan(1, "Changed", 90, 9900, false));
            return null;
        });
        Membership stored = persistence.unitOfWork().inTransaction(connection ->
                persistence.memberships().findById(connection, purchased.id()).orElseThrow());
        assertEquals(4990, stored.snapshotPriceCents());
        assertEquals(30, stored.snapshotDurationDays());
        boolean accountActive = persistence.unitOfWork().inTransaction(connection ->
                persistence.accounts().findById(connection, member.id()).orElseThrow().active());
        assertTrue(accountActive);
    }

    @Test
    void rejectsArchivedPlansAndSecondActivePurchaseWithoutChangingHistory() throws Exception {
        assertThrows(ConflictException.class, () -> service.purchase(2));
        assertTrue(service.purchase(1).isActiveOn(TODAY));
        assertThrows(ConflictException.class, () -> service.purchase(1));
        List<Membership> history = persistence.unitOfWork().inTransaction(connection ->
                persistence.memberships().findByMemberId(connection, member.id()));
        assertEquals(1, history.size());
        assertEquals(1, history.getFirst().planId());
    }

    @Test
    void rejectsPlanArchivedAfterItWasListed() throws Exception {
        assertEquals(1, service.availablePlans().size());
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.plans().update(connection,
                    new MembershipPlan(1, "Monthly", 30, 4990, true));
            return null;
        });
        assertThrows(ConflictException.class, () -> service.purchase(1));
        List<Membership> history = persistence.unitOfWork().inTransaction(connection ->
                persistence.memberships().findByMemberId(connection, member.id()));
        assertTrue(history.isEmpty());
    }

    private MembershipPurchaseService service() {
        return new MembershipPurchaseService(persistence.plans(), persistence.memberships(),
                persistence.unitOfWork(), new Permissions(persistence.accounts(), session),
                Clock.fixed(Instant.parse("2026-09-26T00:00:00Z"), ZoneOffset.UTC));
    }
}
