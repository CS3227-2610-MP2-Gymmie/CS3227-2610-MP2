package gymmie.model;

/** Membership lifecycle values; expiry is also evaluated against the local date. */
public enum MembershipStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED
}
