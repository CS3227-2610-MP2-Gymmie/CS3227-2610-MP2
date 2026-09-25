package gymmie.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DisplayFormattersTest {
    @Test
    void pricesKeepExactCentsIncludingFreePlansAndLargeAmounts() {
        assertEquals("SGD 0.00", DisplayFormatters.price(0));
        assertEquals("SGD 0.01", DisplayFormatters.price(1));
        assertEquals("SGD 49.90", DisplayFormatters.price(4990));
        assertEquals("SGD 10000.00", DisplayFormatters.price(1_000_000));
        assertEquals("SGD 92233720368547758.07", DisplayFormatters.price(Long.MAX_VALUE));
    }

    @Test
    void displayConventionsAreIndependentOfMachineLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            assertEquals("SGD 49.90", DisplayFormatters.price(4990));
            assertEquals("29 Feb 2024", DisplayFormatters.date(LocalDate.of(2024, 2, 29)));
            assertEquals("00:00", DisplayFormatters.time(LocalTime.MIDNIGHT));
            assertEquals("23:59", DisplayFormatters.time(LocalTime.MAX));
            assertEquals("25 Sep 2026, 15:04",
                    DisplayFormatters.dateTime(LocalDateTime.of(2026, 9, 25, 15, 4, 59)));
        } finally {
            Locale.setDefault(original);
        }
    }
}
