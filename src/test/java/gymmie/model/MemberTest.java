package gymmie.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;

class MemberTest {
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final PasswordHash PASSWORD = new PasswordHash("A".repeat(43) + "=", "A".repeat(22) + "==");

    @Test
    void derivesPlanAndStatusFromHistoryWithoutChangingAccountActivation() {
        Member member = new Member(account(Role.MEMBER, false), List.of());
        assertEquals(MemberStatus.INACTIVE, member.status());
        assertTrue(member.planId().isEmpty());

        Membership purchase = Membership.purchase(1, 1, new MembershipPlan(2, "Plan", 30, 4990, false));
        Member subscribed = member.withMembership(purchase);
        assertEquals(MemberStatus.ACTIVE, subscribed.status());
        assertEquals(2, subscribed.planId().orElseThrow());
        assertEquals(purchase, subscribed.activeMembership().orElseThrow());
        assertFalse(subscribed.account().active());
        assertTrue(member.memberships().isEmpty());
    }

    @Test
    void rejectsOverlappingMembershipsRegardlessOfInputOrderOrCurrentDate() {
        Membership first = membership(1, START, START.plusDays(30), MembershipStatus.ACTIVE);
        Membership nested = membership(2, START.plusDays(1), START.plusDays(2), MembershipStatus.ACTIVE);
        assertThrows(ConflictException.class, () -> member(List.of(nested, first)));

        LocalDate future = LocalDate.now().plusYears(1);
        assertThrows(ConflictException.class, () -> member(List.of(
                membership(1, future, future.plusDays(30), MembershipStatus.ACTIVE),
                membership(2, future.plusDays(30), future.plusDays(60), MembershipStatus.ACTIVE))));
    }

    @Test
    void permitsSequentialCoverageButRejectsSharedExpiryDayAndPreservesOriginalOnFailure() {
        Membership first = membership(1, START, START.plusDays(30), MembershipStatus.ACTIVE);
        Member member = member(List.of(first));
        Membership overlapping = membership(2, START.plusDays(30), START.plusDays(60), MembershipStatus.ACTIVE);
        assertThrows(ConflictException.class, () -> member.withMembership(overlapping));
        assertEquals(List.of(first), member.memberships());

        Membership second = membership(2, START.plusDays(31), START.plusDays(60), MembershipStatus.ACTIVE);
        Member sequential = member.withMembership(second);
        assertEquals(first, sequential.activeMembershipOn(START.plusDays(30)).orElseThrow());
        assertEquals(second, sequential.activeMembershipOn(START.plusDays(31)).orElseThrow());
        assertTrue(sequential.activeMembershipOn(START.minusDays(1)).isEmpty());
        assertTrue(sequential.activeMembershipOn(START.plusDays(61)).isEmpty());
    }

    @Test
    void retainsCancelledHistoryWithoutBlockingReplacement() {
        Membership cancelled = membership(1, START, START.plusDays(30), MembershipStatus.CANCELLED);
        Membership replacement = membership(2, START, START.plusDays(30), MembershipStatus.ACTIVE);
        Member member = member(List.of(cancelled, replacement));
        assertEquals(replacement, member.activeMembershipOn(START).orElseThrow());
        assertEquals(2, member.memberships().size());
    }

    @Test
    void defensivelyCopiesMembershipHistory() {
        List<Membership> history = new ArrayList<>();
        Member member = member(history);
        history.add(membership(1, START, START.plusDays(30), MembershipStatus.ACTIVE));
        assertTrue(member.memberships().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> member.memberships().add(history.getFirst()));
    }

    @Test
    void rejectsWrongRoleOwnershipDuplicateIdsAndMissingHistory() {
        Membership membership = membership(1, START, START.plusDays(30), MembershipStatus.ACTIVE);
        assertThrows(ValidationException.class, () -> new Member(account(Role.TRAINER, true), List.of()));
        assertThrows(ValidationException.class, () -> new Member(account(Role.MANAGER, true), List.of()));
        assertThrows(ValidationException.class, () -> member(List.of(new Membership(1, 2, 1, START,
                START.plusDays(30), MembershipStatus.ACTIVE, 0, 30))));
        assertThrows(ConflictException.class, () -> member(List.of(membership, membership)));
        assertThrows(ValidationException.class, () -> member(null));
        assertThrows(ValidationException.class, () -> member(Arrays.asList(membership, null)));
        assertThrows(ValidationException.class, () -> member(List.of()).activeMembershipOn(null));
    }

    @Test
    void expiredAndCancelledHistoryDerivesInactiveStatus() {
        Membership past = membership(1, START.minusYears(100), START.minusYears(99), MembershipStatus.ACTIVE);
        Member member = member(List.of(past));
        assertEquals(MemberStatus.INACTIVE, member.status());
        assertTrue(member.planId().isEmpty());
        LocalDate today = LocalDate.now();
        Member cancelled = member(List.of(membership(1, today, today.plusDays(30), MembershipStatus.CANCELLED)));
        assertEquals(MemberStatus.INACTIVE, cancelled.status());
        assertTrue(cancelled.planId().isEmpty());
    }

    private static Account account(Role role, boolean active) {
        return new Account(1, "member", PASSWORD, "Member", role, active);
    }

    private static Member member(List<Membership> memberships) {
        return new Member(account(Role.MEMBER, true), memberships);
    }

    private static Membership membership(long id, LocalDate start, LocalDate expiry, MembershipStatus status) {
        return new Membership(id, 1, 1, start, expiry, status, 100, 30);
    }
}
