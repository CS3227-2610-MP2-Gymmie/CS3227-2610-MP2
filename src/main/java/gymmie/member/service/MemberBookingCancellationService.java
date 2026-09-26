package gymmie.member.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Role;
import gymmie.model.exception.ConflictException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.BookingRepository;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.service.Permissions;

/** Cancels an upcoming booking for the authenticated Member. */
public final class MemberBookingCancellationService {
    private final BookingRepository bookings;
    private final TrainingSessionRepository sessions;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final Clock clock;

    /** Creates the Member booking-cancellation service. */
    public MemberBookingCancellationService(BookingRepository bookings, TrainingSessionRepository sessions,
            UnitOfWork unitOfWork, Permissions permissions, Clock clock) {
        this.bookings = Objects.requireNonNull(bookings);
        this.sessions = Objects.requireNonNull(sessions);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Cancels a booking owned by the authenticated Member before its session starts.
     *
     * @param bookingId booking identifier to cancel.
     * @throws Exception if authorization or persistence fails, or the booking cannot be cancelled.
     */
    public void cancel(long bookingId) throws Exception {
        unitOfWork.inTransaction(connection -> {
            Account account = permissions.requireRole(connection, Role.MEMBER);
            var booking = bookings.findById(connection, bookingId)
                    .filter(candidate -> candidate.memberId() == account.id())
                    .orElseThrow(() -> new ConflictException("This booking is no longer available"));
            if (booking.status() != BookingStatus.BOOKED) {
                throw new ConflictException("This booking has already been cancelled");
            }
            var session = sessions.findById(connection, booking.sessionId())
                    .orElseThrow(() -> new ConflictException("This session is no longer available"));
            if (!LocalDateTime.now(clock).isBefore(session.startsAt())) {
                throw new ConflictException("A booking cannot be cancelled after the session has started");
            }
            bookings.update(connection, new Booking(booking.id(), booking.sessionId(),
                    booking.memberId(), booking.bookedAt(), BookingStatus.CANCELLED,
                    CancellationReason.MEMBER_CANCELLED_BOOKING));
            return null;
        });
    }
}
