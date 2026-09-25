package gymmie.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Member;
import gymmie.model.Membership;
import gymmie.model.MembershipPlan;
import gymmie.model.TrainingSession;
import gymmie.model.exception.ValidationException;

class EntityBuildersTest {
    @Test
    void defaultsAreValidAndCopyBuildersPreserveAllFields() {
        Account account = new AccountBuilder().build();
        MembershipPlan plan = new MembershipPlanBuilder().build();
        Membership membership = new MembershipBuilder().build();
        TrainingSession session = new TrainingSessionBuilder().build();
        Booking booking = new BookingBuilder().build();
        Member member = new MemberBuilder().withMemberships(List.of(membership)).build();
        assertTrue(account.password().matches(AccountBuilder.DEFAULT_PASSWORD));
        assertEquals(account, new AccountBuilder(account).build());
        assertEquals(plan, new MembershipPlanBuilder(plan).build());
        assertEquals(membership, new MembershipBuilder(membership).build());
        assertEquals(session, new TrainingSessionBuilder(session).build());
        assertEquals(booking, new BookingBuilder(booking).build());
        assertEquals(member, new MemberBuilder(member).build());
        assertEquals(account.id(), membership.memberId());
        assertEquals(plan.id(), membership.planId());
        assertEquals(session.id(), booking.sessionId());
        assertEquals(account.id(), booking.memberId());
    }

    @Test
    void buildersDoNotMutatePreviouslyBuiltRecords() {
        AccountBuilder builder = new AccountBuilder();
        Account original = builder.build();
        Account updated = builder.withActive(false).build();
        assertTrue(original.active());
        assertFalse(updated.active());
        assertTrue(new AccountBuilder().build().active());
        ArrayList<Membership> history = new ArrayList<>();
        history.add(new MembershipBuilder().build());
        Member member = new MemberBuilder().withMemberships(history).build();
        history.clear();
        assertEquals(1, member.memberships().size());
    }

    @Test
    void overridesRetainProductionValidationAndCancellationHistory() {
        assertThrows(ValidationException.class, () -> new AccountBuilder().withId(0).build());
        assertThrows(ValidationException.class, () -> new MembershipPlanBuilder().withDurationDays(0).build());
        assertThrows(ValidationException.class, () -> new MembershipBuilder().withMemberId(0).build());
        assertThrows(ValidationException.class, () -> new TrainingSessionBuilder().withCapacity(0).build());
        assertThrows(ValidationException.class, () -> new MemberBuilder().withAccount(null).build());
        assertThrows(ValidationException.class, () -> new BookingBuilder().withStatus(BookingStatus.CANCELLED).build());
        Booking original = new BookingBuilder().build();
        for (CancellationReason reason : CancellationReason.values()) {
            Booking cancelled = new BookingBuilder(original).withStatus(BookingStatus.CANCELLED)
                    .withCancellationReason(reason).build();
            assertEquals(reason, cancelled.cancellationReason());
            assertEquals(original.bookedAt(), cancelled.bookedAt());
            assertEquals(original.memberId(), cancelled.memberId());
            assertEquals(original.sessionId(), cancelled.sessionId());
        }
    }
}
