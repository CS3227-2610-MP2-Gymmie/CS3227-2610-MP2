package gymmie.trainer.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import gymmie.model.Constraints;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.model.exception.ValidationException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.BookingRepository;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.service.Permissions;
import gymmie.service.exception.AuthorizationException;

/** Creates, edits and lists sessions owned by the currently authenticated Trainer. */
public final class TrainingSessionService {
    private final TrainingSessionRepository sessions;
    private final BookingRepository bookings;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final Clock clock;

    /** Creates a service using the application's local clock and transaction boundary. */
    public TrainingSessionService(TrainingSessionRepository sessions, BookingRepository bookings, UnitOfWork unitOfWork,
            Permissions permissions, Clock clock) {
        this.sessions = Objects.requireNonNull(sessions);
        this.bookings = Objects.requireNonNull(bookings);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Lists only the signed-in Trainer's uncancelled sessions starting after the local cut-off.
     *
     * @return upcoming sessions ordered by start time, then identifier.
     * @throws Exception if authorization or persistence fails.
     */
    public List<TrainingSession> getOwnUpcomingSessions() throws Exception {
        return unitOfWork.inTransaction(connection -> {
            var trainer = permissions.requireRole(connection, Role.TRAINER);
            LocalDateTime now = LocalDateTime.now(clock);
            return sessions.findByTrainerId(connection, trainer.id()).stream()
                    .filter(session -> session.trainerId() == trainer.id())
                    .filter(session -> !session.cancelled() && session.startsAt().isAfter(now))
                    .sorted(Comparator.comparing(TrainingSession::startsAt).thenComparingLong(TrainingSession::id))
                    .toList();
        });
    }

    /**
     * Validates and commits a future session for the signed-in Trainer.
     *
     * @param startsAt start time in the local system time zone.
     * @param durationMinutes duration from 15 to 240 minutes.
     * @param capacity capacity from 1 to 50 Members.
     * @param description optional description.
     * @return the committed session.
     * @throws Exception if authorization, validation or persistence fails.
     */
    public TrainingSession create(LocalDateTime startsAt, int durationMinutes, int capacity,
            String description) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            var trainer = permissions.requireRole(connection, Role.TRAINER);
            Constraints.required(startsAt, "Session start");
            if (!startsAt.isAfter(LocalDateTime.now(clock))) {
                throw new ValidationException("Session start must be in the future (local system time)");
            }
            Constraints.range(durationMinutes, 15, 240, "Session duration");
            Constraints.range(capacity, 1, 50, "Session capacity");
            requireNoOverlap(connection, trainer.id(), 0, startsAt, durationMinutes);
            return sessions.create(connection, trainer.id(), startsAt, durationMinutes, capacity, description);
        });
    }

    /**
     * Updates an owned session without changing any booking records.
     *
     * @param sessionId session to edit.
     * @param startsAt future start time in the local system time zone.
     * @param durationMinutes duration from 15 to 240 minutes.
     * @param capacity capacity from 1 to 50, at least the current BOOKED count.
     * @param description optional description.
     * @return the committed session.
     * @throws Exception if authorization, validation or persistence fails.
     */
    public TrainingSession edit(long sessionId, LocalDateTime startsAt, int durationMinutes, int capacity,
            String description) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.TRAINER);
            var existing = sessions.findById(connection, sessionId)
                    .orElseThrow(() -> new AuthorizationException("Session is unavailable"));
            permissions.requireOwner(connection, Role.TRAINER, existing.trainerId());
            if (existing.cancelled()) {
                throw new ValidationException("Cancelled sessions cannot be edited");
            }
            Constraints.required(startsAt, "Session start");
            if (!startsAt.isAfter(LocalDateTime.now(clock))) {
                throw new ValidationException("Session start must be in the future (local system time)");
            }
            var replacement = new TrainingSession(existing.id(), existing.trainerId(), startsAt,
                    durationMinutes, capacity, description, existing.cancelled());
            int booked = bookings.countBookedBySessionId(connection, sessionId);
            if (capacity < booked) {
                throw new ValidationException("Session capacity must be at least the current booking count ("
                        + booked + ")");
            }
            requireNoOverlap(connection, existing.trainerId(), sessionId, startsAt, durationMinutes);
            sessions.update(connection, replacement);
            return replacement;
        });
    }

    private void requireNoOverlap(Connection connection, long trainerId, long excludedSessionId,
            LocalDateTime startsAt, int durationMinutes) throws SQLException {
        LocalDateTime endsAt = startsAt.plusMinutes(durationMinutes);
        for (var session : sessions.findByTrainerId(connection, trainerId)) {
            if (session.id() != excludedSessionId && !session.cancelled()
                    && startsAt.isBefore(session.startsAt().plusMinutes(session.durationMinutes()))
                    && session.startsAt().isBefore(endsAt)) {
                throw new ValidationException("Session overlaps with your session #" + session.id()
                        + ". Choose a different start time or duration.");
            }
        }
    }

}
