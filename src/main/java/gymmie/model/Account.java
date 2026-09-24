package gymmie.model;

import java.util.Locale;

import gymmie.model.exception.ValidationException;

/**
 * An account profile; membership state belongs to {@link Member}, not the account.
 *
 * @param id positive account identifier.
 * @param username original ASCII login name, preserved as entered.
 * @param password salted password hash, never a plaintext password.
 * @param displayName display name of 1–100 Unicode code points.
 * @param role account role.
 * @param active whether login is permitted.
 */
public record Account(long id, String username, PasswordHash password, String displayName,
        Role role, boolean active) {
    /**
     * Validates the account's fields.
     *
     * @throws ValidationException if a required field or identifier is invalid.
     */
    public Account {
        Constraints.positiveId(id, "Account ID");
        Constraints.required(username, "Username");
        if (!username.matches("[A-Za-z0-9_-]{3,30}")) {
            throw new ValidationException("Username must contain 3–30 ASCII letters, digits, underscores or hyphens");
        }
        Constraints.required(password, "Password hash");
        Constraints.length(displayName, 1, 100, "Display name");
        Constraints.required(role, "Role");
    }

    /**
     * Returns a locale-independent key for case-insensitive username matching.
     *
     * @return normalized username key.
     */
    public String usernameKey() {
        return username.toLowerCase(Locale.ROOT);
    }
}
