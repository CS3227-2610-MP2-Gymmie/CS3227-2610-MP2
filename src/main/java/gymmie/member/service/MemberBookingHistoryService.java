package gymmie.member.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import gymmie.model.Account;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Role;
import gymmie.model.exception.ConflictException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.AccountRepository;
import gymmie.persistence.repository.BookingRepository;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.service.Permissions;

/** Reads the signed-in Member's complete booking history, including cancellation reasons. */
public final class MemberBookingHistoryService {
    private final BookingRepository bookings;
    private final AccountRepository accounts;
    private final TrainingSessionRepository sessions;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;

    /** Creates the Member booking-history reader. */
    public MemberBookingHistoryService(BookingRepository bookings, TrainingSessionRepository sessions,
            AccountRepository accounts,
            UnitOfWork unitOfWork, Permissions permissions) {
        this.bookings = Objects.requireNonNull(bookings);
        this.sessions = Objects.requireNonNull(sessions);
        this.accounts = Objects.requireNonNull(accounts);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
    }

    /**
     * Lists all of the signed-in Member's bookings and session details.
     *
     * @return booking history in booking identifier order.
     * @throws Exception if authorization or persistence fails.
     */
    public List<MemberBooking> bookingHistory() throws Exception {
        return unitOfWork.inTransaction(connection -> {
            Account account = permissions.requireRole(connection, Role.MEMBER);
            ArrayList<MemberBooking> history = new ArrayList<>();
            for (var booking : bookings.findByMemberId(connection, account.id())) {
                var session = sessions.findById(connection, booking.sessionId())
                        .orElseThrow(() -> new ConflictException("A booked session is no longer available"));
                String trainerName = accounts.findById(connection, session.trainerId())
                        .orElseThrow(() -> new ConflictException("A session Trainer is no longer available"))
                        .displayName();
                history.add(new MemberBooking(booking.id(), session.startsAt(), trainerName,
                        session.description(), session.durationMinutes(), booking.status(),
                        booking.cancellationReason()));
            }
            return List.copyOf(history);
        });
    }

    /** Read-only details needed to display a Member's booking history. */
    public record MemberBooking(long bookingId, LocalDateTime startsAt, String trainerName, String description,
            int durationMinutes, BookingStatus status,
            CancellationReason cancellationReason) {
    }
}
