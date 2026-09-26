package gymmie.trainer.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import gymmie.model.Constraints;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.model.exception.ValidationException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.service.Permissions;

/** Creates sessions owned by the currently authenticated Trainer. */
public final class TrainingSessionService {
    private final TrainingSessionRepository sessions;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final Clock clock;

    /** Creates a service using the application's local clock and transaction boundary. */
    public TrainingSessionService(TrainingSessionRepository sessions, UnitOfWork unitOfWork,
            Permissions permissions, Clock clock) {
        this.sessions = Objects.requireNonNull(sessions);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
        this.clock = Objects.requireNonNull(clock);
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
            return sessions.create(connection, trainer.id(), startsAt, durationMinutes, capacity, description);
        });
    }
}
