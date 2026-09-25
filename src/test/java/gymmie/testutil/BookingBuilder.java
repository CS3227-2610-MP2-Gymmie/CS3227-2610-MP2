package gymmie.testutil;

import java.time.LocalDateTime;

import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;

/**
 * Builds Booking fixtures with valid, deterministic defaults and explicit overrides.
 *
 * <p>Identifiers are fixed; assign distinct IDs and matching references when inserting multiple records.
 */
@SuppressWarnings("unused") // Reusable fixture overrides need not all be used by the current tests.
public final class BookingBuilder {
    private long id = 1;
    private long sessionId = 1;
    private long memberId = 1;
    private LocalDateTime bookedAt = TestClocks.LOCAL_TIME;
    private BookingStatus status = BookingStatus.BOOKED;
    private CancellationReason cancellationReason = null;

    /** Creates a builder with valid defaults. */
    public BookingBuilder() {}

    /**
     * Copies an existing record for lifecycle changes.
     *
     * @param source record to copy.
     */
    public BookingBuilder(Booking source) {
        id = source.id();
        sessionId = source.sessionId();
        memberId = source.memberId();
        bookedAt = source.bookedAt();
        status = source.status();
        cancellationReason = source.cancellationReason();
    }

    /**
     * Overrides id.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public BookingBuilder withId(long value) {
        id = value;
        return this;
    }

    /**
     * Overrides sessionId.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public BookingBuilder withSessionId(long value) {
        sessionId = value;
        return this;
    }

    /**
     * Overrides memberId.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public BookingBuilder withMemberId(long value) {
        memberId = value;
        return this;
    }

    /**
     * Overrides bookedAt.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public BookingBuilder withBookedAt(LocalDateTime value) {
        bookedAt = value;
        return this;
    }

    /**
     * Overrides status.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public BookingBuilder withStatus(BookingStatus value) {
        status = value;
        return this;
    }

    /**
     * Overrides cancellationReason.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public BookingBuilder withCancellationReason(CancellationReason value) {
        cancellationReason = value;
        return this;
    }

    /**
     * Builds a record using the production constructor and its validation.
     *
     * @return the fixture record.
     */
    public Booking build() {
        return new Booking(id, sessionId, memberId, bookedAt, status, cancellationReason);
    }
}
