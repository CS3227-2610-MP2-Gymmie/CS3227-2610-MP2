package gymmie.testutil;

import java.time.LocalDate;

import gymmie.model.Membership;
import gymmie.model.MembershipStatus;

/**
 * Builds Membership fixtures with valid, deterministic defaults and explicit overrides.
 *
 * <p>Identifiers are fixed; assign distinct IDs and matching references when inserting multiple records.
 */
@SuppressWarnings("unused") // Reusable fixture overrides need not all be used by the current tests.
public final class MembershipBuilder {
    private long id = 1;
    private long memberId = 1;
    private long planId = 1;
    private LocalDate startDate = TestClocks.LOCAL_TIME.toLocalDate();
    private LocalDate expiryDate = TestClocks.LOCAL_TIME.toLocalDate().plusDays(30);
    private MembershipStatus status = MembershipStatus.ACTIVE;
    private int snapshotPriceCents = 4990;
    private int snapshotDurationDays = 30;

    /** Creates a builder with valid defaults. */
    public MembershipBuilder() {}

    /**
     * Copies an existing record for lifecycle changes.
     *
     * @param source record to copy.
     */
    public MembershipBuilder(Membership source) {
        id = source.id();
        memberId = source.memberId();
        planId = source.planId();
        startDate = source.startDate();
        expiryDate = source.expiryDate();
        status = source.status();
        snapshotPriceCents = source.snapshotPriceCents();
        snapshotDurationDays = source.snapshotDurationDays();
    }

    /**
     * Overrides id.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipBuilder withId(long value) {
        id = value;
        return this;
    }

    /**
     * Overrides memberId.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipBuilder withMemberId(long value) {
        memberId = value;
        return this;
    }

    /**
     * Overrides planId.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipBuilder withPlanId(long value) {
        planId = value;
        return this;
    }

    /**
     * Overrides startDate.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipBuilder withStartDate(LocalDate value) {
        startDate = value;
        return this;
    }

    /**
     * Overrides expiryDate.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipBuilder withExpiryDate(LocalDate value) {
        expiryDate = value;
        return this;
    }

    /**
     * Overrides status.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipBuilder withStatus(MembershipStatus value) {
        status = value;
        return this;
    }

    /**
     * Overrides snapshotPriceCents.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipBuilder withSnapshotPriceCents(int value) {
        snapshotPriceCents = value;
        return this;
    }

    /**
     * Overrides snapshotDurationDays.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MembershipBuilder withSnapshotDurationDays(int value) {
        snapshotDurationDays = value;
        return this;
    }

    /**
     * Builds a record using the production constructor and its validation.
     *
     * @return the fixture record.
     */
    public Membership build() {
        return new Membership(id, memberId, planId, startDate, expiryDate, status, snapshotPriceCents,
                snapshotDurationDays);
    }
}
