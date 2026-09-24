package gymmie.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.model.exception.ValidationException;

class PasswordHashTest {
    @ParameterizedTest
    @ValueSource(ints = {8, 128})
    void acceptsAndVerifiesPasswordBoundaries(int length) {
        String password = "🔑".repeat(length);
        PasswordHash hash = PasswordHash.fromPassword(password);
        assertTrue(hash.matches(password));
        assertFalse(hash.matches("different-password"));
        assertFalse(hash.matches(null));
        assertFalse(hash.matches("short"));
        assertFalse(hash.matches("x".repeat(129)));
        assertEquals(hash, new PasswordHash(hash.hash(), hash.salt()));
        assertFalse(hash.toString().contains(hash.hash()));
        assertFalse(hash.toString().contains(hash.salt()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7, 129})
    void rejectsPasswordLengthsOutsideBoundaries(int length) {
        assertThrows(ValidationException.class, () -> PasswordHash.fromPassword("x".repeat(length)));
    }

    @Test
    void saltsIdenticalPasswordsIndependently() {
        PasswordHash first = PasswordHash.fromPassword("password123");
        PasswordHash second = PasswordHash.fromPassword("password123");
        assertNotEquals(first.salt(), second.salt());
        assertNotEquals(first.hash(), second.hash());
    }

    @Test
    void rejectsMissingPasswordsAndMalformedStoredHashes() {
        assertThrows(ValidationException.class, () -> PasswordHash.fromPassword(null));
        assertThrows(ValidationException.class, () -> new PasswordHash(null, null));
        assertThrows(ValidationException.class, () -> new PasswordHash("not base64!", "salt"));
        assertThrows(ValidationException.class, () -> new PasswordHash("YQ==", "YQ=="));
        assertThrows(ValidationException.class, () -> new PasswordHash("A".repeat(43) + "=", null));
    }
}
