package gymmie.model;

import java.time.LocalDateTime;

import gymmie.model.exception.ValidationException;

/**
 * A Member's session reservation, retained after cancellation.
 *
 * @param id positive booking identifier.
 * @param sessionId reserved session identifier.
 * @param memberId booking Member's account identifier.
 * @param bookedAt local system time of the booking.
 * @param status booking lifecycle state.
 * @param cancellationReason reason required exactly when the booking is cancelled.
 */
public record Booking(long id, long sessionId, long memberId, LocalDateTime bookedAt,
        BookingStatus status, CancellationReason cancellationReason) {
    /**
     * Validates the booking's fields and cancellation consistency.
     *
     * @throws ValidationException if a required field is invalid or the reason contradicts the status.
     */
    public Booking {
        Constraints.positiveId(id, "Booking ID");
        Constraints.positiveId(sessionId, "Session ID");
        Constraints.positiveId(memberId, "Member ID");
        Constraints.required(bookedAt, "Booking time");
        Constraints.required(status, "Booking status");
        if ((status == BookingStatus.CANCELLED && cancellationReason == null)
                || (status == BookingStatus.BOOKED && cancellationReason != null)) {
            throw new ValidationException("Only cancelled bookings must have a cancellation reason");
        }
    }
}
