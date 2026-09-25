package gymmie.testutil;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Fixed local wall-clock values for tests; never changes the JVM's default time zone. */
public final class TestClocks {
    /** Default local date and time shared by fixture builders. */
    public static final LocalDateTime LOCAL_TIME = LocalDateTime.of(2026, 9, 25, 12, 0);

    private TestClocks() {}

    /** Returns a fixed clock at the default fixture time in the system's local zone. */
    public static Clock fixed() {
        return at(LOCAL_TIME, ZoneId.systemDefault());
    }

    /**
     * Freezes a local wall time in an explicit zone, without altering production time handling.
     *
     * <p>Use LocalDate.now(clock) or LocalDateTime.now(clock) with the model's explicit-time methods.
     * Java's normal zone rules resolve daylight-saving gaps and overlaps.
     *
     * @param localTime local wall time to freeze.
     * @param zone zone used to interpret the local time.
     * @return an immutable fixed clock.
     */
    public static Clock at(LocalDateTime localTime, ZoneId zone) {
        return Clock.fixed(localTime.atZone(zone).toInstant(), zone);
    }
}
