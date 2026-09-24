package gymmie.model;

import java.time.LocalDateTime;

import gymmie.model.exception.ValidationException;

/**
 * A Trainer's session, including historical and cancelled sessions.
 *
 * <p>Services check that new or rescheduled sessions are in the future. Construction permits past
 * sessions so that persisted history can be loaded.
 *
 * @param id positive session identifier.
 * @param trainerId owning Trainer's account identifier.
 * @param startsAt start time in the local system time zone.
 * @param durationMinutes duration from 15 to 240 minutes.
 * @param capacity capacity from 1 to 50 Members.
 * @param description optional description; null is preserved for database round trips.
 * @param cancelled whether the session was cancelled.
 */
public record TrainingSession(long id, long trainerId, LocalDateTime startsAt, int durationMinutes,
        int capacity, String description, boolean cancelled) {
    /**
     * Validates the session's fields.
     *
     * @throws ValidationException if required fields or numeric constraints are invalid.
     */
    public TrainingSession {
        Constraints.positiveId(id, "Session ID");
        Constraints.positiveId(trainerId, "Trainer ID");
        Constraints.required(startsAt, "Session start");
        Constraints.range(durationMinutes, 15, 240, "Session duration");
        Constraints.range(capacity, 1, 50, "Session capacity");
    }

    /** Returns whether the session has started according to the local system clock. */
    public boolean hasStarted() {
        return hasStartedAt(LocalDateTime.now());
    }

    /**
     * Evaluates the start cut-off, including the exact start instant.
     *
     * @param time local time to evaluate.
     * @return whether the session has already started.
     */
    public boolean hasStartedAt(LocalDateTime time) {
        Constraints.required(time, "Time");
        return !time.isBefore(startsAt);
    }
}
