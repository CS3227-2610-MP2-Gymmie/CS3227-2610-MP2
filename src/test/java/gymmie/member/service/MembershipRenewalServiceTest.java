package gymmie.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;
import java.sql.Statement;
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
import gymmie.service.AuthService;
import gymmie.service.PasswordHasher;
import gymmie.service.Permissions;
import gymmie.service.UserSession;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.InMemoryDatabase;

class MembershipRenewalServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final int DURATION_DAYS = 30;
    private static final int PRICE_CENTS = 4990;
    private InMemoryDatabase fixture;
    private Persistence persistence;
    private UserSession session;
    private AuthService auth;
    private Account member;
    private MembershipRenewalService service;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        session = new UserSession();
        auth = new AuthService(persistence.accounts(), persistence.unitOfWork(), session, new PasswordHasher());
        member = new AccountBuilder().build();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, member);
            persistence.plans().insert(connection,
                    new MembershipPlan(1, "Monthly", DURATION_DAYS, PRICE_CENTS, false));
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
    void renewalWithRemainingTimeExtendsFromExistingExpiry() throws Exception {
        Membership existing = membership(1, member.id(), TODAY.minusDays(20), TODAY.plusDays(10),
                MembershipStatus.ACTIVE);
        insert(existing);

        Membership renewed = service.renew(1);

        assertEquals(TODAY.plusDays(40), renewed.expiryDate());
        assertEquals(existing.planId(), renewed.planId());
    }

    @Test
    void expiredMembershipRenewalExtendsFromToday() throws Exception {
        insert(membership(1, member.id(), TODAY.minusDays(40), TODAY.minusDays(10), MembershipStatus.EXPIRED));

        Membership renewed = service.renew(1);

        assertEquals(TODAY.plusDays(DURATION_DAYS), renewed.expiryDate());
        assertEquals(MembershipStatus.ACTIVE, renewed.status());
    }

    @Test
    void membershipExpiringTodayRenewsFromToday() throws Exception {
        insert(membership(1, member.id(), TODAY.minusDays(DURATION_DAYS), TODAY, MembershipStatus.ACTIVE));

        Membership renewed = service.renew(1);

        assertEquals(TODAY.plusDays(DURATION_DAYS), renewed.expiryDate());
    }

    @Test
    void renewalRejectsWhenMemberHasNoMembership() {
        assertThrows(ConflictException.class, () -> service.renew(1));
    }

    @Test
    void renewalRejectsCancelledMembership() throws Exception {
        insert(membership(1, member.id(), TODAY.minusDays(40), TODAY.minusDays(10), MembershipStatus.CANCELLED));

        assertThrows(ConflictException.class, () -> service.renew(1));
    }

    @Test
    void renewalRejectsFutureOnlyMembership() throws Exception {
        insert(membership(1, member.id(), TODAY.plusDays(1), TODAY.plusDays(31), MembershipStatus.ACTIVE));

        assertThrows(ConflictException.class, () -> service.renew(1));
    }

    @Test
    void holderCanRenewArchivedPlanWithoutChangingPlanOrPurchaseSnapshots() throws Exception {
        Membership existing = membership(1, member.id(), TODAY.minusDays(20), TODAY.plusDays(10),
                MembershipStatus.ACTIVE);
        insert(existing);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.plans().update(connection, new MembershipPlan(1, "Archived monthly", 90, 9900, true));
            return null;
        });

        Membership renewed = service.renew(1);
        Membership stored = findMembership(existing.id());

        assertEquals(existing.expiryDate().plusDays(DURATION_DAYS), renewed.expiryDate());
        assertEquals(existing.planId(), stored.planId());
        assertEquals(existing.snapshotPriceCents(), stored.snapshotPriceCents());
        assertEquals(existing.snapshotDurationDays(), stored.snapshotDurationDays());
        assertEquals(existing.startDate(), stored.startDate());
    }

    @Test
    void renewalRequiresMemberRoleAndAuthentication() throws Exception {
        insert(membership(1, member.id(), TODAY.minusDays(20), TODAY.plusDays(10), MembershipStatus.ACTIVE));
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection,
                    new AccountBuilder().withId(2).withUsername("manager")
                            .withRole(Role.MANAGER).build());
            persistence.accounts().insert(connection,
                    new AccountBuilder().withId(3).withUsername("trainer")
                            .withRole(Role.TRAINER).build());
            return null;
        });

        for (String username : List.of("manager", "trainer")) {
            Account unauthorized = persistence.unitOfWork().inTransaction(connection ->
                    persistence.accounts().findByUsername(connection, username).orElseThrow());
            auth.login(unauthorized.username(), AccountBuilder.DEFAULT_PASSWORD);
            assertThrows(AuthorizationException.class, () -> service.renew(1));
        }
        auth.logout();
        assertThrows(AuthenticationException.class, () -> service.renew(1));
        assertEquals(TODAY.plusDays(10), findMembership(1).expiryDate());
    }

    @Test
    void renewalChangesOnlyTheSignedInMembersMembership() throws Exception {
        Account otherMember = new AccountBuilder().withId(2).withUsername("other_member").build();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, otherMember);
            return null;
        });
        Membership owned = membership(1, member.id(), TODAY.minusDays(20), TODAY.plusDays(10),
                MembershipStatus.ACTIVE);
        Membership notOwned = membership(2, otherMember.id(), TODAY.minusDays(20), TODAY.plusDays(15),
                MembershipStatus.ACTIVE);
        insert(owned);
        insert(notOwned);

        assertThrows(ConflictException.class, () -> service.renew(notOwned.id()));
        Membership renewed = service.renew(owned.id());

        assertEquals(owned.id(), renewed.id());
        assertEquals(TODAY.plusDays(40), findMembership(owned.id()).expiryDate());
        assertEquals(notOwned, findMembership(notOwned.id()));
    }

    @Test
    void currentMembershipWinsOverCancelledLaterHistory() throws Exception {
        Membership current = membership(1, member.id(), TODAY.minusDays(20), TODAY.plusDays(10),
                MembershipStatus.ACTIVE);
        Membership cancelled = membership(2, member.id(), TODAY.minusDays(5), TODAY.plusDays(5),
                MembershipStatus.CANCELLED);
        insert(current);
        insert(cancelled);

        Membership renewed = service.renew(current.id());

        assertEquals(current.id(), renewed.id());
        assertEquals(cancelled, findMembership(cancelled.id()));
        assertEquals(current.expiryDate().plusDays(DURATION_DAYS), findMembership(current.id()).expiryDate());
    }

    @Test
    void expiredArchivedMembershipRemainsRenewableWhenLaterMembershipWasCancelled() throws Exception {
        Membership expired = membership(1, member.id(), TODAY.minusDays(40), TODAY.minusDays(10),
                MembershipStatus.EXPIRED);
        Membership cancelled = new Membership(2, member.id(), 2, TODAY.minusDays(5), TODAY.plusDays(25),
                MembershipStatus.CANCELLED, 5990, 30);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.plans().update(connection, new MembershipPlan(1, "Archived monthly", 30, PRICE_CENTS, true));
            persistence.plans().insert(connection, new MembershipPlan(2, "Cancelled plan", 30, 5990, false));
            persistence.memberships().insert(connection, expired);
            persistence.memberships().insert(connection, cancelled);
            return null;
        });

        Membership renewed = service.renew(expired.id());

        assertEquals(expired.id(), renewed.id());
        assertEquals(TODAY.plusDays(DURATION_DAYS), renewed.expiryDate());
        assertEquals(cancelled, findMembership(cancelled.id()));
    }

    @Test
    void renewalRejectsOverlapWithFutureActiveMembership() throws Exception {
        Membership current = membership(1, member.id(), TODAY.minusDays(20), TODAY.plusDays(10),
                MembershipStatus.ACTIVE);
        Membership future = membership(2, member.id(), TODAY.plusDays(11), TODAY.plusDays(41),
                MembershipStatus.ACTIVE);
        insert(current);
        insert(future);

        assertThrows(ConflictException.class, () -> service.renew(current.id()));

        assertEquals(current, findMembership(current.id()));
        assertEquals(future, findMembership(future.id()));
    }

    @Test
    void persistenceFailureRollsBackMembershipRenewal() throws Exception {
        Membership existing = membership(1, member.id(), TODAY.minusDays(20), TODAY.plusDays(10),
                MembershipStatus.ACTIVE);
        insert(existing);
        try (var connection = fixture.database().openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER reject_membership_renewal BEFORE UPDATE ON membership "
                    + "BEGIN SELECT RAISE(ABORT, 'simulated persistence failure'); END");
        }

        assertThrows(SQLException.class, () -> service.renew(existing.id()));

        assertEquals(existing, findMembership(existing.id()));
    }

    private MembershipRenewalService service() {
        return new MembershipRenewalService(persistence.memberships(), persistence.unitOfWork(),
                new Permissions(persistence.accounts(), session),
                Clock.fixed(Instant.parse("2026-09-26T00:00:00Z"), ZoneOffset.UTC));
    }

    private Membership membership(long id, long memberId, LocalDate startDate, LocalDate expiryDate,
            MembershipStatus status) {
        return new Membership(id, memberId, 1, startDate, expiryDate, status, PRICE_CENTS, DURATION_DAYS);
    }

    private void insert(Membership membership) throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.memberships().insert(connection, membership);
            return null;
        });
    }

    private Membership findMembership(long id) throws Exception {
        return persistence.unitOfWork().inTransaction(connection ->
                persistence.memberships().findById(connection, id).orElseThrow());
    }
}
