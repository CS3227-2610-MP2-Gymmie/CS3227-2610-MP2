package gymmie.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;

/**
 * An immutable Member profile and its complete membership history.
 *
 * <p>Repositories must supply the full history. Construction prevents overlapping active membership
 * periods, so advancing the local clock cannot introduce a second active membership.
 *
 * @param account account with the Member role.
 * @param memberships all memberships owned by this Member, defensively copied.
 */
public record Member(Account account, List<Membership> memberships) {
    /**
     * Validates ownership, membership identities and active date ranges.
     *
     * @throws ValidationException if the account is not a Member or the history contains invalid ownership or nulls.
     * @throws ConflictException if membership identifiers or active date ranges overlap.
     */
    public Member {
        Constraints.required(account, "Account");
        Constraints.required(memberships, "Memberships");
        if (account.role() != Role.MEMBER) {
            throw new ValidationException("A Member requires an account with the Member role");
        }
        Set<Long> identifiers = new HashSet<>();
        for (Membership membership : memberships) {
            Constraints.required(membership, "Membership");
            if (membership.memberId() != account.id()) {
                throw new ValidationException("Membership belongs to another account");
            }
            if (!identifiers.add(membership.id())) {
                throw new ConflictException("Membership identifiers must be unique");
            }
        }
        memberships = List.copyOf(memberships);
        List<Membership> activePeriods = memberships.stream()
                .filter(membership -> membership.status() == MembershipStatus.ACTIVE)
                .sorted(Comparator.comparing(Membership::startDate))
                .toList();
        for (int index = 1; index < activePeriods.size(); index++) {
            if (!activePeriods.get(index).startDate().isAfter(activePeriods.get(index - 1).expiryDate())) {
                throw new ConflictException("A Member cannot hold overlapping active memberships");
            }
        }
    }

    /** Returns the active membership using the local system date. */
    public Optional<Membership> activeMembership() {
        return activeMembershipOn(LocalDate.now());
    }

    /**
     * Finds the membership covering a local date.
     *
     * @param date date to evaluate.
     * @return the sole active membership, or empty if no membership covers the date.
     */
    public Optional<Membership> activeMembershipOn(LocalDate date) {
        Constraints.required(date, "Date");
        return memberships.stream().filter(membership -> membership.isActiveOn(date)).findFirst();
    }

    /** Returns the current plan identifier derived from the active membership. */
    public OptionalLong planId() {
        return activeMembership().stream().mapToLong(Membership::planId).findFirst();
    }

    /** Returns membership status using the local system date, independently of account activation. */
    public MemberStatus status() {
        return activeMembership().isPresent() ? MemberStatus.ACTIVE : MemberStatus.INACTIVE;
    }

    /**
     * Creates a new aggregate containing an additional membership while preserving all invariants.
     *
     * @param membership membership to add.
     * @return a new Member; this instance remains unchanged even when validation fails.
     * @throws ValidationException if the membership is null or belongs to another Member.
     * @throws ConflictException if the membership duplicates an identifier or overlaps an active period.
     */
    public Member withMembership(Membership membership) {
        List<Membership> updated = new ArrayList<>(memberships);
        updated.add(membership);
        return new Member(account, updated);
    }
}
