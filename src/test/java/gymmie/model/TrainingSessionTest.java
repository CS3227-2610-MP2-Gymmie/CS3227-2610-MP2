package gymmie.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import gymmie.model.exception.ValidationException;

class TrainingSessionTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 10, 0);

    @ParameterizedTest
    @CsvSource({"15, 1", "240, 50"})
    void acceptsInclusiveBoundariesAndOptionalDescription(int duration, int capacity) {
        TrainingSession session = new TrainingSession(1, 1, START, duration, capacity, null, false);
        assertEquals(duration, session.durationMinutes());
        assertEquals(capacity, session.capacity());
        assertNull(session.description());
    }

    @ParameterizedTest
    @CsvSource({"14, 1", "241, 1", "15, 0", "15, 51"})
    void rejectsValuesOutsideBoundaries(int duration, int capacity) {
        assertThrows(ValidationException.class, () ->
                new TrainingSession(1, 1, START, duration, capacity, "", false));
    }

    @Test
    void treatsExactStartAsStartedAndAllowsHistoricalSessions() {
        TrainingSession session = new TrainingSession(1, 1, START, 60, 10, "Strength", true);
        assertFalse(session.hasStartedAt(START.minusNanos(1)));
        assertTrue(session.hasStartedAt(START));
        assertTrue(session.hasStartedAt(START.plusNanos(1)));
        TrainingSession past = new TrainingSession(2, 1, LocalDateTime.now().minusDays(1), 60, 10, null, false);
        TrainingSession future = new TrainingSession(3, 1, LocalDateTime.now().plusDays(1), 60, 10, null, false);
        assertTrue(past.hasStarted());
        assertFalse(future.hasStarted());
    }

    @Test
    void rejectsInvalidIdentityAndMissingTimes() {
        assertThrows(ValidationException.class, () -> new TrainingSession(0, 1, START, 60, 10, null, false));
        assertThrows(ValidationException.class, () -> new TrainingSession(1, 0, START, 60, 10, null, false));
        assertThrows(ValidationException.class, () -> new TrainingSession(1, 1, null, 60, 10, null, false));
        assertThrows(ValidationException.class, () ->
                new TrainingSession(1, 1, START, 60, 10, null, false).hasStartedAt(null));
    }
}
