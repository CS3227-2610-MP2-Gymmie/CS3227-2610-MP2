package gymmie.service.exception;

import gymmie.model.exception.DomainException;

/** Indicates missing authentication or invalid credentials without exposing secret values. */
public class AuthenticationException extends DomainException {
    /**
     * Creates an authentication failure.
     *
     * @param message explanation safe to display to the user.
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
