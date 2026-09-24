package gymmie.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;

class MembershipTest {
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate EXPIRY = START.plusDays(30);

    @Test
    void activeCoverageIncludesStartAndExpiryButNotAdjacentDates() {
        Membership membership = membership(MembershipStatus.ACTIVE, 100, 30);
        assertFalse(membership.isActiveOn(START.minusDays(1)));
        assertTrue(membership.isActiveOn(START));
        assertTrue(membership.isActiveOn(EXPIRY));
        assertFalse(membership.isActiveOn(EXPIRY.plusDays(1)));
    }

    @Test
    void cancellationAndPersistedExpiryDisableCoverage() {
        assertFalse(membership(MembershipStatus.CANCELLED, 100, 30).isActiveOn(START));
        assertFalse(membership(MembershipStatus.EXPIRED, 100, 30).isActiveOn(START));
    }

    @Test
    void purchaseSnapshotsTermsAndUsesLocalDate() {
        LocalDate before = LocalDate.now();
        MembershipPlan plan = new MembershipPlan(1, "Original", 30, 4990, false);
        Membership purchase = Membership.purchase(1, 1, plan);
        LocalDate after = LocalDate.now();
        MembershipPlan editedPlan = new MembershipPlan(plan.id(), "Edited", 365, 9900, true);

        assertEquals(30, purchase.snapshotDurationDays());
        assertEquals(4990, purchase.snapshotPriceCents());
        assertEquals(editedPlan.id(), purchase.planId());
        assertTrue(purchase.startDate().equals(before) || purchase.startDate().equals(after));
        assertEquals(purchase.startDate().plusDays(30), purchase.expiryDate());
        assertTrue(purchase.isActive());
    }

    @Test
    void rejectsArchivedPurchasesButAllowsHistoryForArchivedPlans() {
        MembershipPlan archived = new MembershipPlan(1, "Archived", 30, 4990, true);
        assertThrows(ConflictException.class, () -> Membership.purchase(1, 1, archived));
        assertEquals(archived.id(), membership(MembershipStatus.ACTIVE, 4990, 30).planId());
    }

    @ParameterizedTest
    @CsvSource({"-1, 30", "1000001, 30", "0, 0", "0, 366"})
    void validatesSnapshotsWhenLoadingHistory(int price, int duration) {
        assertThrows(ValidationException.class, () -> membership(MembershipStatus.ACTIVE, price, duration));
    }

    @Test
    void rejectsReversedDatesAndMissingState() {
        assertThrows(ValidationException.class, () -> new Membership(1, 1, 1, EXPIRY, START,
                MembershipStatus.ACTIVE, 0, 30));
        assertThrows(ValidationException.class, () -> membership(null, 0, 30));
        assertThrows(ValidationException.class, () -> new Membership(1, 1, 1, null, EXPIRY,
                MembershipStatus.ACTIVE, 0, 30));
        assertThrows(ValidationException.class, () -> membership(MembershipStatus.ACTIVE, 0, 30).isActiveOn(null));
    }

    private static Membership membership(MembershipStatus status, int price, int duration) {
        return new Membership(1, 1, 1, START, EXPIRY, status, price, duration);
    }
}
