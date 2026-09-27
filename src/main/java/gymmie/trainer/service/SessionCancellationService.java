package gymmie.trainer.service;

import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.AccountRepository;
import gymmie.persistence.repository.BookingRepository;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.service.Permissions;
import gymmie.service.exception.AuthorizationException;

/** Authorizes and atomically cancels owned future sessions while retaining booking history. */
public final class SessionCancellationService {
    private final TrainingSessionRepository sessions;
    private final BookingRepository bookings;
    private final AccountRepository accounts;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final Clock clock;

    /** Creates the session cancellation boundary with a local-time clock. */
    public SessionCancellationService(TrainingSessionRepository sessions, BookingRepository bookings,
            AccountRepository accounts, UnitOfWork unitOfWork, Permissions permissions, Clock clock) {
        this.sessions = Objects.requireNonNull(sessions);
        this.bookings = Objects.requireNonNull(bookings);
        this.accounts = Objects.requireNonNull(accounts);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Loads a consistent snapshot to display before asking for confirmation.
     *
     * @param sessionId session to review.
     * @return the session and current bookings, with Member display names only.
     * @throws Exception if authorization, eligibility or persistence fails.
     */
    public Preview preview(long sessionId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            var session = requireCancellable(connection, sessionId);
            var current = currentBookings(connection, sessionId);
            List<String> names = new ArrayList<>();
            for (var booking : current) {
                names.add(accounts.findById(connection, booking.memberId()).orElseThrow().displayName());
            }
            return new Preview(session, current, names);
        });
    }

    /**
     * Cancels a confirmed, unchanged snapshot and its current bookings in one transaction.
     *
     * @param preview snapshot shown to the Trainer before confirmation.
     * @param reason required explanation retained for Members and across restarts.
     * @return number of newly cancelled bookings.
     * @throws Exception if authorization, validation, a stale preview or persistence prevents cancellation.
     */
    public int cancel(Preview preview, String reason) throws Exception {
        Objects.requireNonNull(preview);
        return unitOfWork.inTransaction(connection -> {
            var session = requireCancellable(connection, preview.session().id());
            if (reason == null || reason.isBlank()) {
                throw new ValidationException("Enter a reason for cancelling the session");
            }
            var current = currentBookings(connection, session.id());
            if (!session.equals(preview.session()) || !current.equals(preview.bookings())) {
                throw new ConflictException("The session or bookings changed. Review cancellation again");
            }
            sessions.update(connection, new TrainingSession(session.id(), session.trainerId(), session.startsAt(),
                    session.durationMinutes(), session.capacity(), session.description(), true, reason.strip()));
            for (var booking : current) {
                bookings.update(connection, new Booking(booking.id(), booking.sessionId(), booking.memberId(),
                        booking.bookedAt(), BookingStatus.CANCELLED, CancellationReason.TRAINER_CANCELLED_SESSION));
            }
            return current.size();
        });
    }

    private TrainingSession requireCancellable(Connection connection, long sessionId) throws Exception {
        permissions.requireRole(connection, Role.TRAINER);
        var session = sessions.findById(connection, sessionId)
                .orElseThrow(() -> new AuthorizationException("Session is unavailable"));
        permissions.requireOwner(connection, Role.TRAINER, session.trainerId());
        if (session.cancelled()) {
            throw new ConflictException("This session has already been cancelled");
        }
        if (session.hasStartedAt(LocalDateTime.now(clock))) {
            throw new ConflictException("A session cannot be cancelled once it has started");
        }
        return session;
    }

    private List<Booking> currentBookings(Connection connection, long sessionId) throws Exception {
        return bookings.findBySessionId(connection, sessionId).stream()
                .filter(booking -> booking.status() == BookingStatus.BOOKED).toList();
    }

    /** Immutable confirmation details, with booking identities retained for a stale-preview check. */
    public record Preview(TrainingSession session, List<Booking> bookings, List<String> memberNames) {
        /** Copies lists so the confirmation snapshot cannot change after it is displayed. */
        public Preview {
            Objects.requireNonNull(session);
            bookings = List.copyOf(bookings);
            memberNames = List.copyOf(memberNames);
        }
    }
}
