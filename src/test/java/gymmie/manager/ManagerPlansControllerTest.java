package gymmie.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import gymmie.model.exception.ValidationException;

class ManagerPlansControllerTest {
    @Test
    void parsePriceCentsConvertsFiftyDollarsToCents() throws Exception {
        assertEquals(5000, ManagerPlansController.parsePriceCents("50"));
    }

    @Test
    void parsePriceCentsConvertsFortyNineNinetyToCents() throws Exception {
        assertEquals(4990, ManagerPlansController.parsePriceCents("49.90"));
    }

    @Test
    void parsePriceCentsAllowsZeroDollars() throws Exception {
        assertEquals(0, ManagerPlansController.parsePriceCents("0"));
        assertEquals(0, ManagerPlansController.parsePriceCents("0.00"));
    }

    @Test
    void parsePriceCentsRejectsNegativeValues() {
        assertThrows(ValidationException.class, () -> ManagerPlansController.parsePriceCents("-1"));
        assertThrows(ValidationException.class, () -> ManagerPlansController.parsePriceCents("-0.01"));
    }

    @Test
    void parsePriceCentsRejectsMoreThanTwoDecimalPlaces() {
        assertThrows(ValidationException.class, () -> ManagerPlansController.parsePriceCents("49.999"));
        assertThrows(ValidationException.class, () -> ManagerPlansController.parsePriceCents("50.000"));
    }

    @Test
    void parsePriceCentsAcceptsCurrencySymbolsAndWhitespace() throws Exception {
        assertEquals(5000, ManagerPlansController.parsePriceCents("$50"));
        assertEquals(4990, ManagerPlansController.parsePriceCents("SGD 49.90"));
        assertEquals(1250, ManagerPlansController.parsePriceCents("  12.50  "));
    }

    @Test
    void parsePriceCentsRejectsBlankAndInvalidInputs() {
        assertThrows(ValidationException.class, () -> ManagerPlansController.parsePriceCents(null));
        assertThrows(ValidationException.class, () -> ManagerPlansController.parsePriceCents(""));
        assertThrows(ValidationException.class, () -> ManagerPlansController.parsePriceCents("   "));
        assertThrows(ValidationException.class, () -> ManagerPlansController.parsePriceCents("abc"));
        assertThrows(ValidationException.class, () -> ManagerPlansController.parsePriceCents("1e2"));
    }
}
