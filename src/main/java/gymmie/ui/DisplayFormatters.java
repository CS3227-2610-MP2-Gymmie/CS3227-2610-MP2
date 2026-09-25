package gymmie.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Locale-independent presentation of stored cents and local dates and times. */
public final class DisplayFormatters {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private DisplayFormatters() {
    }

    /** Returns an SGD price with exactly two decimal places, without floating-point conversion. */
    public static String price(long cents) {
        return "SGD " + BigDecimal.valueOf(cents, 2).toPlainString();
    }

    /** Returns a local date such as 25 Sep 2026; no timezone conversion is performed. */
    public static String date(LocalDate date) {
        return DATE.format(date);
    }

    /** Returns a local time in 24-hour HH:mm format, omitting seconds. */
    public static String time(LocalTime time) {
        return TIME.format(time);
    }

    /** Returns the date and time of a local timestamp, without changing its timezone. */
    public static String dateTime(LocalDateTime dateTime) {
        return date(dateTime.toLocalDate()) + ", " + time(dateTime.toLocalTime());
    }
}
