package gymmie.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.model.PasswordHash;
import gymmie.model.exception.ValidationException;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void remainsCompatibleWithExistingHashesAndGeneratesFreshSalts() {
        String password = "existing-password";
        PasswordHash existing = PasswordHash.fromPassword(password);
        PasswordHash fresh = hasher.hash(password);
        assertTrue(hasher.verify(password, existing));
        assertTrue(fresh.matches(password));
        assertNotEquals(existing.salt(), fresh.salt());
        assertNotEquals(existing.hash(), fresh.hash());
        assertNotEquals(password, fresh.hash());
        assertFalse(hasher.verify("incorrect-password", fresh));
        assertFalse(hasher.verify(null, fresh));
        assertFalse(hasher.verify("short", fresh));
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 128})
    void acceptsDocumentedPasswordLengthBoundaries(int length) {
        String password = "🔑".repeat(length);
        assertTrue(hasher.verify(password, hasher.hash(password)));
    }

    @Test
    void rejectsMissingOrInvalidPasswordLengths() {
        assertThrows(ValidationException.class, () -> hasher.hash(null));
        assertThrows(ValidationException.class, () -> hasher.hash("short"));
        assertThrows(ValidationException.class, () -> hasher.hash("x".repeat(129)));
    }
}
