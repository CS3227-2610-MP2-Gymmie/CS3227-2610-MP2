package gymmie.service;

import java.util.Objects;

import gymmie.model.PasswordHash;
import gymmie.model.exception.ValidationException;

/**
 * Password hashing boundary for services, compatible with existing stored PasswordHash values.
 *
 * <p>The current PBKDF2 algorithm and parameters are implementation choices, not product requirements.
 * This class holds no password state and delegates to the existing constant-time hash comparison.
 */
public final class PasswordHasher {
    /**
     * Validates the 8–128-character policy and produces a fresh salted hash.
     *
     * @param password plaintext used only during this call.
     * @return hash material suitable for persistence.
     * @throws ValidationException if the password is missing or violates the length constraint.
     */
    public PasswordHash hash(String password) {
        return PasswordHash.fromPassword(password);
    }

    /**
     * Verifies a password without retaining the supplied plaintext.
     *
     * @param password candidate password, which may be null.
     * @param storedHash persisted password hash.
     * @return whether the candidate matches; invalid candidate lengths return false.
     */
    public boolean verify(String password, PasswordHash storedHash) {
        return Objects.requireNonNull(storedHash).matches(password);
    }
}
