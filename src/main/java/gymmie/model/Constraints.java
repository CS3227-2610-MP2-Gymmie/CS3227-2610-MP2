package gymmie.model;

import gymmie.model.exception.ValidationException;

/** Shared validation for immutable domain records. */
final class Constraints {
    private Constraints() {
    }

    static void required(Object value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
    }

    static void positiveId(long value, String field) {
        if (value <= 0) {
            throw new ValidationException(field + " must be positive");
        }
    }

    static void range(int value, int minimum, int maximum, String field) {
        if (value < minimum || value > maximum) {
            throw new ValidationException(field + " must be between " + minimum + " and " + maximum);
        }
    }

    static void length(String value, int minimum, int maximum, String field) {
        required(value, field);
        range(value.codePointCount(0, value.length()), minimum, maximum, field + " length");
    }
}
