package gymmie.model;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import gymmie.model.exception.ValidationException;

/**
 * A PBKDF2-HMAC-SHA256 password hash using 600,000 iterations and a random 128-bit salt.
 *
 * <p>Only the Base64 hash and salt are persisted. Plaintext is validated before hashing and never retained.
 *
 * @param hash Base64-encoded 256-bit hash.
 * @param salt Base64-encoded 128-bit salt.
 */
public record PasswordHash(String hash, String salt) {
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Validates persisted hash material without applying plaintext password length rules to it.
     *
     * @throws ValidationException if the hash or salt is absent or malformed.
     */
    public PasswordHash {
        validateEncoded(hash, 32, "Password hash");
        validateEncoded(salt, 16, "Password salt");
    }

    /**
     * Validates and hashes a password of 8–128 Unicode code points.
     *
     * @param password plaintext password used only for this operation.
     * @return salted hash suitable for persistence.
     * @throws ValidationException if the password violates the length constraint.
     */
    public static PasswordHash fromPassword(String password) {
        Constraints.length(password, 8, 128, "Password");
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        return new PasswordHash(Base64.getEncoder().encodeToString(derive(password, saltBytes)),
                Base64.getEncoder().encodeToString(saltBytes));
    }

    /**
     * Checks a candidate without retaining it or revealing credential material.
     *
     * @param candidate plaintext candidate password.
     * @return whether the candidate matches, or false for an invalid length or null.
     */
    public boolean matches(String candidate) {
        if (candidate == null) {
            return false;
        }
        int length = candidate.codePointCount(0, candidate.length());
        return length >= 8 && length <= 128
                && MessageDigest.isEqual(Base64.getDecoder().decode(hash),
                        derive(candidate, Base64.getDecoder().decode(salt)));
    }

    @Override
    public String toString() {
        return "PasswordHash[redacted]";
    }

    private static byte[] derive(String password, byte[] salt) {
        PBEKeySpec specification = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            specification.clearPassword();
        }
    }

    private static void validateEncoded(String value, int bytes, String field) {
        Constraints.required(value, field);
        try {
            if (Base64.getDecoder().decode(value).length == bytes) {
                return;
            }
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(field + " must be valid Base64");
        }
        throw new ValidationException(field + " has an invalid length");
    }
}
