package gymmie.testutil;

import gymmie.model.MembershipPlan;

/**
 * Builds MembershipPlan fixtures with valid, deterministic defaults and explicit overrides.
 *
 * <p>Identifiers are fixed; assign distinct IDs and matching references when inserting multiple records.
 */
@SuppressWarnings("unused") // Reusable fixture overrides need not all be used by the current tests.
public final class MembershipPlanBuilder {
    private long id = 1;
    private String name = "Monthly";
    private int durationDays = 30;
    private int priceCents = 4990;
    private boolean archived = false;

    /** Creates a builder with valid defaults. */
    public MembershipPlanBuilder() {}

    /**
     * Copies an existing record for lifecycle changes.
     *
     * @param source record to copy.
     */
    public MembershipPlanBuilder(MembershipPlan source) {
        id = source.id();
        name = source.name();
        durationDays = source.durationDays();
        priceCents = source.priceCents();
        archived = source.archived();
    }

    /**
     * Overrides id.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipPlanBuilder withId(long value) {
        id = value;
        return this;
    }

    /**
     * Overrides name.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipPlanBuilder withName(String value) {
        name = value;
        return this;
    }

    /**
     * Overrides durationDays.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipPlanBuilder withDurationDays(int value) {
        durationDays = value;
        return this;
    }

    /**
     * Overrides priceCents.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipPlanBuilder withPriceCents(int value) {
        priceCents = value;
        return this;
    }

    /**
     * Overrides archived.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipPlanBuilder withArchived(boolean value) {
        archived = value;
        return this;
    }

    /**
     * Builds a record using the production constructor and its validation.
     *
     * @return the fixture record.
     */
    public MembershipPlan build() {
        return new MembershipPlan(id, name, durationDays, priceCents, archived);
    }
}
