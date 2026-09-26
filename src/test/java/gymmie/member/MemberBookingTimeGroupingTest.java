package gymmie.member;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import gymmie.member.service.MemberBookingHistoryService.MemberBooking;
import gymmie.model.BookingStatus;

/** Checks the boundary between upcoming and already-started Member bookings. */
class MemberBookingTimeGroupingTest {
    @Test
    void classifiesBookingsBeforeAtAndAfterTheCurrentTime() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 26, 12, 0);
        MemberBooking before = booking(1, now.minusMinutes(1));
        MemberBooking at = booking(2, now);
        MemberBooking after = booking(3, now.plusMinutes(1));

        assertEquals(List.of(after), MemberBookingsController.upcomingBookings(List.of(before, at, after), now));
        assertEquals(List.of(at, before), MemberBookingsController.pastBookings(List.of(before, at, after), now));
    }

    private static MemberBooking booking(long id, LocalDateTime startsAt) {
        return new MemberBooking(id, startsAt, BookingStatus.BOOKED, null);
    }
}
