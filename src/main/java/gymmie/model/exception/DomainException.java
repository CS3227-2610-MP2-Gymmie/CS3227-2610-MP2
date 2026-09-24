package gymmie.model.exception;

/** Base exception for rejected domain values and operations. */
public class DomainException extends RuntimeException {
    /**
     * Creates a domain failure with a user-facing explanation.
     *
     * @param message explanation of the failed rule.
     */
    public DomainException(String message) {
        super(message);
    }
}
