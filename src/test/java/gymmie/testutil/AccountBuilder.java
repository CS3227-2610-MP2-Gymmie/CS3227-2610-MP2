package gymmie.testutil;

import gymmie.model.Account;
import gymmie.model.PasswordHash;
import gymmie.model.Role;

/**
 * Builds Account fixtures with valid, deterministic defaults and explicit overrides.
 *
 * <p>Identifiers are fixed; assign distinct IDs and matching references when inserting multiple records.
 */
public final class AccountBuilder {
    /** Plaintext matching the shared test-only default hash. */
    public static final String DEFAULT_PASSWORD = "test-password";

    private static final PasswordHash DEFAULT_PASSWORD_HASH = PasswordHash.fromPassword(DEFAULT_PASSWORD);

    private long id = 1;
    private String username = "test_member";
    private PasswordHash password = DEFAULT_PASSWORD_HASH;
    private String displayName = "Test Member";
    private Role role = Role.MEMBER;
    private boolean active = true;

    /** Creates a builder with valid defaults. */
    public AccountBuilder() {}

    /**
     * Copies an existing record for lifecycle changes.
     *
     * @param source record to copy.
     */
    public AccountBuilder(Account source) {
        id = source.id();
        username = source.username();
        password = source.password();
        displayName = source.displayName();
        role = source.role();
        active = source.active();
    }

    /**
     * Overrides id.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public AccountBuilder withId(long value) {
        id = value;
        return this;
    }

    /**
     * Overrides username.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public AccountBuilder withUsername(String value) {
        username = value;
        return this;
    }

    /**
     * Overrides password.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public AccountBuilder withPassword(PasswordHash value) {
        password = value;
        return this;
    }

    /**
     * Overrides displayName.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public AccountBuilder withDisplayName(String value) {
        displayName = value;
        return this;
    }

    /**
     * Overrides role.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public AccountBuilder withRole(Role value) {
        role = value;
        return this;
    }

    /**
     * Overrides active.
     *
     * @param value fixture value.
     * @return this builder.
     */
    public AccountBuilder withActive(boolean value) {
        active = value;
        return this;
    }

    /**
     * Builds a record using the production constructor and its validation.
     *
     * @return the fixture record.
     */
    public Account build() {
        return new Account(id, username, password, displayName, role, active);
    }
}
