package gymmie.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import gymmie.model.Account;
import gymmie.model.Member;
import gymmie.model.Membership;
import gymmie.model.Role;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.MembershipPlanRepository;
import gymmie.persistence.repository.MembershipRepository;

/** Reads the signed-in Member's current membership without changing history. */
public final class MembershipStatusService {
    private final MembershipRepository memberships;
    private final MembershipPlanRepository plans;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final Clock clock;

    /** Creates a read service with an explicit clock for local-date coverage. */
    public MembershipStatusService(MembershipRepository memberships, MembershipPlanRepository plans,
            UnitOfWork unitOfWork, Permissions permissions, Clock clock) {
        this.memberships = memberships;
        this.plans = plans;
        this.unitOfWork = unitOfWork;
        this.permissions = permissions;
        this.clock = clock;
    }

    /**
     * Derives current coverage from the authenticated Member's complete history.
     *
     * @return current plan and inclusive expiry, or empty when inactive.
     * @throws Exception if authorization or storage access fails.
     */
    public Optional<CurrentMembership> currentMembership() throws Exception {
        return unitOfWork.inTransaction(connection -> {
            Account account = permissions.requireRole(connection, Role.MEMBER);
            Member member = new Member(account, memberships.findByMemberId(connection, account.id()));
            Optional<Membership> current = member.activeMembershipOn(LocalDate.now(clock));
            if (current.isEmpty()) {
                return Optional.empty();
            }
            Membership membership = current.orElseThrow();
            String planName = plans.findById(connection, membership.planId()).orElseThrow().name();
            return Optional.of(new CurrentMembership(planName, membership.expiryDate()));
        });
    }

    /**
     * Read-only details of the membership covering today.
     *
     * @param planName current catalogue name, including archived plans.
     * @param expiryDate last covered local date.
     */
    public record CurrentMembership(String planName, LocalDate expiryDate) {
    }
}
