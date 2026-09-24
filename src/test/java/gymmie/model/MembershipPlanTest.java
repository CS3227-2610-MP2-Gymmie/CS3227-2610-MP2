package gymmie.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import gymmie.model.exception.ValidationException;

class MembershipPlanTest {
    @ParameterizedTest
    @CsvSource({"1, 0", "365, 1000000"})
    void acceptsInclusiveBoundariesAndFreePlans(int duration, int price) {
        MembershipPlan plan = new MembershipPlan(1, "Plan", duration, price, false);
        assertEquals(duration, plan.durationDays());
        assertEquals(price, plan.priceCents());
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "366, 0", "1, -1", "1, 1000001"})
    void rejectsValuesOutsideBoundaries(int duration, int price) {
        assertThrows(ValidationException.class, () -> new MembershipPlan(1, "Plan", duration, price, false));
    }

    @Test
    void rejectsInvalidIdentityAndMissingName() {
        assertThrows(ValidationException.class, () -> new MembershipPlan(0, "Plan", 30, 100, false));
        assertThrows(ValidationException.class, () -> new MembershipPlan(1, null, 30, 100, false));
        assertThrows(ValidationException.class, () -> new MembershipPlan(1, " ", 30, 100, false));
    }
}
