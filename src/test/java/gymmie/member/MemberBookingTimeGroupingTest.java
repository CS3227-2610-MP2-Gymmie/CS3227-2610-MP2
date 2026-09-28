package gymmie.member;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import gymmie.member.service.MemberBookingHistoryService.MemberBooking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;

/** Checks status grouping, the start-time boundary, and ordering for all three booking sections. */
class MemberBookingTimeGroupingTest {
    @Test
    void classifiesBookingsBeforeAtAndAfterTheCurrentTime() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 26, 12, 0);
        MemberBooking before = booking(1, now.minusMinutes(1));
        MemberBooking at = booking(2, now);
        MemberBooking after = booking(3, now.plusMinutes(5));
        MemberBooking cancelledAt = cancelled(4, now);
        MemberBooking cancelledAfter = cancelled(5, now.plusDays(1));
        MemberBooking soonest = booking(6, now.plusMinutes(1));
        MemberBooking middle = booking(7, now.plusMinutes(3));

        List<MemberBooking> bookings = List.of(before, at, after, cancelledAt, cancelledAfter, middle, soonest);
        assertEquals(List.of(soonest, middle, after), MemberBookingsController.upcomingBookings(bookings, now));
        assertEquals(List.of(at, before), MemberBookingsController.pastBookings(bookings, now));
        assertEquals(List.of(cancelledAfter, cancelledAt), MemberBookingsController.cancelledBookings(bookings));
    }

    private static MemberBooking booking(long id, LocalDateTime startsAt) {
        return new MemberBooking(id, startsAt, "Trainer", "Workout", 60, BookingStatus.BOOKED, null, null);
    }

    private static MemberBooking cancelled(long id, LocalDateTime startsAt) {
        return new MemberBooking(id, startsAt, "Trainer", "Workout", 60, BookingStatus.CANCELLED,
                CancellationReason.MEMBER_CANCELLED_BOOKING, null);
    }
}
