package gymmie.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.model.Membership;
import gymmie.model.TrainingSession;

class TestClocksTest {
    @Test
    void defaultClockUsesSystemZoneAndFixtureWallTime() {
        Clock clock = TestClocks.fixed();
        assertEquals(ZoneId.systemDefault(), clock.getZone());
        assertEquals(TestClocks.LOCAL_TIME, LocalDateTime.now(clock));
        assertEquals(clock.instant(), clock.instant());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Asia/Singapore", "America/Los_Angeles", "UTC"})
    void localBoundariesAreDeterministicAcrossZones(String zoneName) {
        ZoneId originalZone = ZoneId.systemDefault();
        Clock clock = TestClocks.at(TestClocks.LOCAL_TIME, ZoneId.of(zoneName));
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        Membership membership = new MembershipBuilder().withExpiryDate(today).build();
        assertFalse(membership.isActiveOn(today.minusDays(1)));
        assertTrue(membership.isActiveOn(today));
        assertFalse(membership.isActiveOn(today.plusDays(1)));
        TrainingSession session = new TrainingSessionBuilder().withStartsAt(now).build();
        assertFalse(session.hasStartedAt(now.minusNanos(1)));
        assertTrue(session.hasStartedAt(now));
        assertTrue(session.hasStartedAt(now.plusNanos(1)));
        assertEquals(originalZone, ZoneId.systemDefault());
        assertEquals(TestClocks.LOCAL_TIME, now);
    }
}
