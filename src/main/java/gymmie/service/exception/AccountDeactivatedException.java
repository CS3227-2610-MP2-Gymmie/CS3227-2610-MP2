package gymmie.service.exception;

/** Distinguishes a deactivated account from invalid credentials. */
public final class AccountDeactivatedException extends AuthenticationException {
    /** Creates a deactivated-account failure without exposing credentials. */
    public AccountDeactivatedException() {
        super("This account is deactivated");
    }
}
