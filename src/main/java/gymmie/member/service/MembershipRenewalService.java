package gymmie.member.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;

import gymmie.model.Account;
import gymmie.model.Member;
import gymmie.model.Membership;
import gymmie.model.Role;
import gymmie.model.exception.ConflictException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.MembershipRepository;
import gymmie.service.Permissions;

/** Renews the signed-in Member's current or latest started membership without changing its plan. */
public final class MembershipRenewalService {
    private final MembershipRepository memberships;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final Clock clock;

    /** Creates the renewal service with a clock for local renewal dates. */
    public MembershipRenewalService(MembershipRepository memberships, UnitOfWork unitOfWork,
            Permissions permissions, Clock clock) {
        this.memberships = Objects.requireNonNull(memberships);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Renews the displayed membership if it is still current for the signed-in Member.
     *
     * @param membershipId displayed membership identifier.
     * @return the renewed membership with its extended expiry.
     * @throws Exception if authorization or persistence fails, or the membership cannot be renewed.
     */
    public Membership renew(long membershipId) throws Exception {
        LocalDate today = LocalDate.now(clock);
        return unitOfWork.inTransaction(connection -> {
            Account account = permissions.requireRole(connection, Role.MEMBER);
            Member member = new Member(account, memberships.findByMemberId(connection, account.id()));
            Membership current = MembershipRenewalSelection.findTarget(member, today)
                    .orElseThrow(() -> new ConflictException("There is no membership available to renew"));
            if (current.id() != membershipId) {
                throw new ConflictException("This membership has changed. Refresh and try again.");
            }
            Membership renewed = current.renew(today);
            ArrayList<Membership> updatedHistory = new ArrayList<>(member.memberships());
            updatedHistory.replaceAll(membership -> membership.id() == renewed.id() ? renewed : membership);
            new Member(account, updatedHistory);
            memberships.update(connection, renewed);
            return renewed;
        });
    }
}
