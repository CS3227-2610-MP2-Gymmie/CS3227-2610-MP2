package gymmie.testutil;

import java.time.LocalDateTime;

import gymmie.model.TrainingSession;

/**
 * Builds TrainingSession fixtures with valid, deterministic defaults and explicit overrides.
 *
 * <p>Identifiers are fixed; assign distinct IDs and matching references when inserting multiple records.
 */
@SuppressWarnings("unused") // Reusable fixture overrides need not all be used by the current tests.
public final class TrainingSessionBuilder {
    private long id = 1;
    private long trainerId = 2;
    private LocalDateTime startsAt = TestClocks.LOCAL_TIME.plusDays(1);
    private int durationMinutes = 60;
    private int capacity = 10;
    private String description = "Test session";
    private boolean cancelled = false;

    /** Creates a builder with valid defaults. */
    public TrainingSessionBuilder() {}

    /**
     * Copies an existing record for lifecycle changes.
     *
     * @param source record to copy.
     */
    public TrainingSessionBuilder(TrainingSession source) {
        id = source.id();
        trainerId = source.trainerId();
        startsAt = source.startsAt();
        durationMinutes = source.durationMinutes();
        capacity = source.capacity();
        description = source.description();
        cancelled = source.cancelled();
    }

    /**
     * Overrides id.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public TrainingSessionBuilder withId(long value) {
        id = value;
        return this;
    }

    /**
     * Overrides trainerId.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public TrainingSessionBuilder withTrainerId(long value) {
        trainerId = value;
        return this;
    }

    /**
     * Overrides startsAt.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public TrainingSessionBuilder withStartsAt(LocalDateTime value) {
        startsAt = value;
        return this;
    }

    /**
     * Overrides durationMinutes.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public TrainingSessionBuilder withDurationMinutes(int value) {
        durationMinutes = value;
        return this;
    }

    /**
     * Overrides capacity.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public TrainingSessionBuilder withCapacity(int value) {
        capacity = value;
        return this;
    }

    /**
     * Overrides description.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public TrainingSessionBuilder withDescription(String value) {
        description = value;
        return this;
    }

    /**
     * Overrides cancelled.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public TrainingSessionBuilder withCancelled(boolean value) {
        cancelled = value;
        return this;
    }

    /**
     * Builds a record using the production constructor and its validation.
     *
     * @return the fixture record.
     */
    public TrainingSession build() {
        return new TrainingSession(id, trainerId, startsAt, durationMinutes, capacity, description, cancelled);
    }
}
