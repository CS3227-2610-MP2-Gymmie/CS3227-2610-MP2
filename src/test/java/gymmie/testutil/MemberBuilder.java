package gymmie.testutil;

import java.util.List;

import gymmie.model.Account;
import gymmie.model.Member;
import gymmie.model.Membership;

/**
 * Builds Member fixtures with valid, deterministic defaults and explicit overrides.
 *
 * <p>Identifiers are fixed; assign distinct IDs and matching references when inserting multiple records.
 */
public final class MemberBuilder {
    private Account account = new AccountBuilder().build();
    private List<Membership> memberships = List.of();

    /** Creates a builder with valid defaults. */
    public MemberBuilder() {}

    /**
     * Copies an existing record for lifecycle changes.
     *
     * @param source record to copy.
     */
    public MemberBuilder(Member source) {
        account = source.account();
        memberships = source.memberships();
    }

    /**
     * Overrides account.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MemberBuilder withAccount(Account value) {
        account = value;
        return this;
    }

    /**
     * Overrides memberships.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public MemberBuilder withMemberships(List<Membership> value) {
        memberships = value;
        return this;
    }

    /**
     * Builds a record using the production constructor and its validation.
     *
     * @return the fixture record.
     */
    public Member build() {
        return new Member(account, memberships);
    }
}
