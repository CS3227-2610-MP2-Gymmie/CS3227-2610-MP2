package gymmie.model.exception;

/** Indicates that individually valid records conflict with existing domain state. */
public final class ConflictException extends DomainException {
    /**
     * Creates a conflict failure.
     *
     * @param message explanation of the conflicting state.
     */
    public ConflictException(String message) {
        super(message);
    }
}
