package gymmie.model;

import java.time.LocalDate;

import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;

/**
 * A membership retaining its purchase-time terms independently of the plan catalogue.
 *
 * <p>The expiry date is inclusive. Date ranges may exceed the snapshot duration after renewal.
 *
 * @param id positive membership identifier.
 * @param memberId owning Member's account identifier.
 * @param planId purchased plan identifier.
 * @param startDate first covered local date.
 * @param expiryDate last covered local date.
 * @param status persisted lifecycle state.
 * @param snapshotPriceCents purchase price in integer cents.
 * @param snapshotDurationDays purchase duration in days.
 */
public record Membership(long id, long memberId, long planId, LocalDate startDate, LocalDate expiryDate,
        MembershipStatus status, int snapshotPriceCents, int snapshotDurationDays) {
    /**
     * Validates a membership, including historical records.
     *
     * @throws ValidationException if fields or date ordering are invalid.
     */
    public Membership {
        Constraints.positiveId(id, "Membership ID");
        Constraints.positiveId(memberId, "Member ID");
        Constraints.positiveId(planId, "Plan ID");
        Constraints.required(startDate, "Start date");
        Constraints.required(expiryDate, "Expiry date");
        Constraints.required(status, "Membership status");
        if (expiryDate.isBefore(startDate)) {
            throw new ValidationException("Membership expiry must not precede its start date");
        }
        Constraints.range(snapshotPriceCents, 0, 1_000_000, "Snapshot price");
        Constraints.range(snapshotDurationDays, 1, 365, "Snapshot duration");
    }

    /**
     * Takes an immutable snapshot for a new purchase starting on the local system date.
     *
     * @param id membership identifier.
     * @param memberId purchasing Member's account identifier.
     * @param plan offering being purchased.
     * @return a membership with the purchased terms.
     * @throws ConflictException if the plan is archived.
     * @throws ValidationException if any required value is invalid.
     */
    public static Membership purchase(long id, long memberId, MembershipPlan plan) {
        return purchase(id, memberId, plan, LocalDate.now());
    }

    /**
     * Takes an immutable snapshot for a new purchase on the supplied local date.
     *
     * @param id membership identifier.
     * @param memberId purchasing Member's account identifier.
     * @param plan offering being purchased.
     * @param startDate purchase date.
     * @return a membership with the purchased terms.
     * @throws ConflictException if the plan is archived.
     * @throws ValidationException if any required value is invalid.
     */
    public static Membership purchase(long id, long memberId, MembershipPlan plan, LocalDate startDate) {
        Constraints.required(plan, "Plan");
        Constraints.required(startDate, "Start date");
        if (plan.archived()) {
            throw new ConflictException("An archived plan cannot be purchased");
        }
        return new Membership(id, memberId, plan.id(), startDate, startDate.plusDays(plan.durationDays()),
                MembershipStatus.ACTIVE, plan.priceCents(), plan.durationDays());
    }

    /**
     * Renews this membership using its saved duration, preserving any remaining coverage.
     *
     * @param today local renewal date.
     * @return renewed membership with the same plan and purchase terms.
     * @throws ConflictException if this membership was cancelled.
     * @throws ValidationException if today is null.
     */
    public Membership renew(LocalDate today) {
        Constraints.required(today, "Renewal date");
        if (status == MembershipStatus.CANCELLED) {
            throw new ConflictException("A cancelled membership cannot be renewed");
        }
        LocalDate renewalStart = expiryDate.isAfter(today) ? expiryDate : today;
        return new Membership(id, memberId, planId, startDate, renewalStart.plusDays(snapshotDurationDays),
                MembershipStatus.ACTIVE, snapshotPriceCents, snapshotDurationDays);
    }

    /**
     * Cancels this membership immediately while retaining its purchase history.
     *
     * @return this membership with cancelled lifecycle state.
     * @throws ConflictException if this membership is already cancelled.
     */
    public Membership cancel() {
        if (status == MembershipStatus.CANCELLED) {
            throw new ConflictException("This membership is already cancelled");
        }
        return new Membership(id, memberId, planId, startDate, expiryDate, MembershipStatus.CANCELLED,
                snapshotPriceCents, snapshotDurationDays);
    }

    /** Returns whether this membership is active on the local system date. */
    public boolean isActive() {
        return isActiveOn(LocalDate.now());
    }

    /**
     * Evaluates coverage including the expiry date, without modifying persisted history.
     *
     * @param date local date to evaluate.
     * @return whether this membership covers that date.
     */
    public boolean isActiveOn(LocalDate date) {
        Constraints.required(date, "Date");
        return status == MembershipStatus.ACTIVE && !date.isBefore(startDate) && !date.isAfter(expiryDate);
    }
}
