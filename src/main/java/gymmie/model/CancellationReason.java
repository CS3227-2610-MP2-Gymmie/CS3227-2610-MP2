package gymmie.model;

/** Reasons retained when a booking is cancelled. */
public enum CancellationReason {
    TRAINER_CANCELLED_SESSION,
    MEMBER_CANCELLED_BOOKING,
    MEMBERSHIP_CANCELLED,
    /** Display-only reason derived from an inactive Trainer; never persisted on a booking. */
    TRAINER_ACCOUNT_DEACTIVATED,
    ACCOUNT_DEACTIVATED
}
