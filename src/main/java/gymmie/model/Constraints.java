package gymmie.model;

import gymmie.model.exception.ValidationException;

/** Shared validation for immutable domain records. */
public final class Constraints {
    private Constraints() {
    }

    /** Rejects a missing field value. */
    public static void required(Object value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
    }

    /** Rejects a non-positive identifier. */
    public static void positiveId(long value, String field) {
        if (value <= 0) {
            throw new ValidationException(field + " must be positive");
        }
    }

    /** Checks an inclusive numeric range. */
    public static void range(int value, int minimum, int maximum, String field) {
        if (value < minimum || value > maximum) {
            throw new ValidationException(field + " must be between " + minimum + " and " + maximum);
        }
    }

    /** Checks the Unicode code-point length of a required string. */
    public static void length(String value, int minimum, int maximum, String field) {
        required(value, field);
        range(value.codePointCount(0, value.length()), minimum, maximum, field + " length");
    }
}
