package gymmie.member.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import gymmie.model.Member;
import gymmie.model.Membership;
import gymmie.model.MembershipStatus;

/** Selects a Member's current or latest eligible membership for renewal. */
final class MembershipRenewalSelection {
    private MembershipRenewalSelection() {
    }

    /** Finds current coverage, or the latest started non-cancelled membership. */
    static Optional<Membership> findTarget(Member member, LocalDate date) {
        return member.activeMembershipOn(date).or(() -> member.memberships().stream()
                .filter(membership -> !membership.startDate().isAfter(date))
                .filter(membership -> membership.status() != MembershipStatus.CANCELLED)
                .max(Comparator.comparing(Membership::startDate).thenComparingLong(Membership::id)));
    }
}
