package gymmie.model;

import gymmie.model.exception.ValidationException;

/**
 * An immutable catalogue offering whose money is stored in integer SGD cents.
 *
 * @param id positive plan identifier.
 * @param name nonblank plan name.
 * @param durationDays duration from 1 to 365 days.
 * @param priceCents price from 0 to 1,000,000 cents.
 * @param archived whether new purchases are disabled.
 */
public record MembershipPlan(long id, String name, int durationDays, int priceCents, boolean archived) {
    /**
     * Validates the plan fields.
     *
     * @throws ValidationException if any field violates its constraints.
     */
    public MembershipPlan {
        Constraints.positiveId(id, "Plan ID");
        Constraints.required(name, "Plan name");
        if (name.isBlank()) {
            throw new ValidationException("Plan name must not be blank");
        }
        Constraints.range(durationDays, 1, 365, "Plan duration");
        Constraints.range(priceCents, 0, 1_000_000, "Plan price");
    }
}
