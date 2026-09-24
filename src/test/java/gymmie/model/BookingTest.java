package gymmie.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import gymmie.model.exception.ValidationException;

class BookingTest {
    private static final LocalDateTime BOOKED_AT = LocalDateTime.of(2026, 1, 1, 10, 0);

    @Test
    void acceptsBookedReservationWithoutReason() {
        Booking booking = new Booking(1, 2, 3, BOOKED_AT, BookingStatus.BOOKED, null);
        assertEquals(BOOKED_AT, booking.bookedAt());
        assertNull(booking.cancellationReason());
    }

    @ParameterizedTest
    @EnumSource(CancellationReason.class)
    void preservesEveryDocumentedCancellationReason(CancellationReason reason) {
        Booking booking = new Booking(1, 2, 3, BOOKED_AT, BookingStatus.CANCELLED, reason);
        assertEquals(reason, booking.cancellationReason());
        assertThrows(ValidationException.class, () ->
                new Booking(1, 2, 3, BOOKED_AT, BookingStatus.BOOKED, reason));
    }

    @Test
    void rejectsCancelledBookingWithoutReasonAndMissingRequiredFields() {
        assertThrows(ValidationException.class, () ->
                new Booking(1, 2, 3, BOOKED_AT, BookingStatus.CANCELLED, null));
        assertThrows(ValidationException.class, () -> new Booking(0, 2, 3, BOOKED_AT, BookingStatus.BOOKED, null));
        assertThrows(ValidationException.class, () -> new Booking(1, 0, 3, BOOKED_AT, BookingStatus.BOOKED, null));
        assertThrows(ValidationException.class, () -> new Booking(1, 2, 0, BOOKED_AT, BookingStatus.BOOKED, null));
        assertThrows(ValidationException.class, () -> new Booking(1, 2, 3, null, BookingStatus.BOOKED, null));
        assertThrows(ValidationException.class, () -> new Booking(1, 2, 3, BOOKED_AT, null, null));
    }
}
