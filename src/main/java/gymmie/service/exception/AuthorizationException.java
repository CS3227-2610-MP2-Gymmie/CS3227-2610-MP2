package gymmie.service.exception;

import gymmie.model.exception.DomainException;

/** Indicates that an authenticated account lacks the required role or ownership. */
public final class AuthorizationException extends DomainException {
    /**
     * Creates an authorization failure.
     *
     * @param message explanation of the denied operation.
     */
    public AuthorizationException(String message) {
        super(message);
    }
}
