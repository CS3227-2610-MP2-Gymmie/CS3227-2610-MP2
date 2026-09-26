package gymmie.member.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import gymmie.model.Account;
import gymmie.model.Member;
import gymmie.model.Membership;
import gymmie.model.MembershipPlan;
import gymmie.model.Role;
import gymmie.model.exception.ConflictException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.MembershipPlanRepository;
import gymmie.persistence.repository.MembershipRepository;
import gymmie.service.Permissions;

/** Provides an authenticated Member's available plans and atomic membership purchases. */
public final class MembershipPurchaseService {
    private final MembershipPlanRepository plans;
    private final MembershipRepository memberships;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final Clock clock;

    /** Creates the purchase service with a clock for local purchase dates. */
    public MembershipPurchaseService(MembershipPlanRepository plans, MembershipRepository memberships,
            UnitOfWork unitOfWork, Permissions permissions, Clock clock) {
        this.plans = Objects.requireNonNull(plans);
        this.memberships = Objects.requireNonNull(memberships);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Lists the unarchived plans available to the signed-in Member.
     *
     * @return available plans in catalogue order.
     * @throws Exception if authorization or storage access fails.
     */
    public List<MembershipPlan> availablePlans() throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MEMBER);
            return plans.findAllAvailable(connection);
        });
    }

    /**
     * Purchases an available plan for the signed-in Member.
     *
     * @param planId selected plan identifier.
     * @return the active membership with purchase-time terms.
     * @throws Exception if authorization, availability, membership rules or persistence fail.
     */
    public Membership purchase(long planId) throws Exception {
        LocalDate purchaseDate = LocalDate.now(clock);
        return unitOfWork.inTransaction(connection -> {
            Account account = permissions.requireRole(connection, Role.MEMBER);
            Member member = new Member(account, memberships.findByMemberId(connection, account.id()));
            if (member.activeMembershipOn(purchaseDate).isPresent()) {
                throw new ConflictException("A Member can hold only one active membership at a time");
            }
            MembershipPlan plan = plans.findById(connection, planId)
                    .filter(candidate -> !candidate.archived())
                    .orElseThrow(() -> new ConflictException("This membership plan is no longer available"));
            Membership purchase = Membership.purchase(memberships.nextId(connection), account.id(), plan,
                    purchaseDate);
            member.withMembership(purchase);
            memberships.insert(connection, purchase);
            return purchase;
        });
    }
}
