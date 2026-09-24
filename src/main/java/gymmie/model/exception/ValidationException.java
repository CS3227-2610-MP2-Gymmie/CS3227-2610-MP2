package gymmie.model.exception;

/** Indicates that a field or record violates its domain constraints. */
public final class ValidationException extends DomainException {
    /**
     * Creates a validation failure.
     *
     * @param message explanation of the invalid value.
     */
    public ValidationException(String message) {
        super(message);
    }
}
