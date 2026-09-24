package gymmie.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.model.exception.ValidationException;

class AccountTest {
    private static final PasswordHash PASSWORD = new PasswordHash(
            Base64.getEncoder().encodeToString(new byte[32]), Base64.getEncoder().encodeToString(new byte[16]));

    @ParameterizedTest
    @ValueSource(strings = {"a_1", "A-Z", "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234"})
    void acceptsUsernameBoundariesAndAllowedCharacters(String username) {
        assertEquals(username, account(username, "Member").username());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ab", "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345", "a b", "ab.c", "ébc", "会员名", "abc\n"})
    void rejectsInvalidUsernames(String username) {
        assertThrows(ValidationException.class, () -> account(username, "Member"));
    }

    @Test
    void preservesUsernameAndNormalizesIndependentlyOfLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Account account = account("IAN", "Ian");
            assertEquals("IAN", account.username());
            assertEquals("ian", account.usernameKey());
        } finally {
            Locale.setDefault(original);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void acceptsUnicodeDisplayNameBoundaries(int length) {
        String name = "🏋".repeat(length);
        assertEquals(name, account("member", name).displayName());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void rejectsDisplayNamesOutsideBoundaries(int length) {
        assertThrows(ValidationException.class, () -> account("member", "名".repeat(length)));
    }

    @Test
    void rejectsMissingAccountFieldsAndInvalidIdentity() {
        assertThrows(ValidationException.class, () -> account("member", null));
        assertThrows(ValidationException.class, () -> new Account(0, "member", PASSWORD, "M", Role.MEMBER, true));
        assertThrows(ValidationException.class, () -> new Account(1, "member", null, "M", Role.MEMBER, true));
        assertThrows(ValidationException.class, () -> new Account(1, "member", PASSWORD, "M", null, true));
    }

    private static Account account(String username, String displayName) {
        return new Account(1, username, PASSWORD, displayName, Role.MEMBER, true);
    }
}
