package gymmie.service;

import java.util.Objects;
import java.util.Optional;

import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;

/**
 * In-memory authenticated identity for one application session, with no password or hash material.
 *
 * <p>Only services in this package can establish or clear authentication. The snapshot supports
 * display and routing; protected operations must revalidate the persisted account through Permissions.
 */
public final class UserSession {
    private volatile Principal principal;

    /** Returns the current authenticated identity, or empty after logout and before login. */
    public Optional<Principal> currentUser() {
        return Optional.ofNullable(principal);
    }

    /** Returns whether an authenticated identity has been established locally. */
    public boolean isAuthenticated() {
        return principal != null;
    }

    /**
     * Requires an established identity without treating its cached role as current authorization.
     *
     * @return current session identity.
     * @throws AuthenticationException if no user is signed in.
     */
    public Principal requireUser() {
        return currentUser().orElseThrow(() -> new AuthenticationException("Please log in first"));
    }

    /**
     * Refreshes an existing identity after a role service commits its own-profile update.
     *
     * <p>This cannot sign in a user or switch the session to another account.
     *
     * @param account committed state of the already authenticated account.
     * @throws AuthenticationException if the session is absent or belongs to another account.
     * @throws AccountDeactivatedException if the account is inactive.
     */
    public synchronized void refresh(Account account) {
        Objects.requireNonNull(account);
        Principal current = requireUser();
        if (current.accountId() != account.id() || !current.username().equals(account.username())) {
            throw new AuthenticationException("The authenticated account has changed");
        }
        establish(account);
    }

    synchronized void establish(Account account) {
        Objects.requireNonNull(account);
        if (!account.active()) {
            throw new AccountDeactivatedException();
        }
        principal = new Principal(account.id(), account.username(), account.displayName(), account.role());
    }

    synchronized void clear() {
        principal = null;
    }

    /**
     * Public session snapshot without credential material.
     *
     * @param accountId authenticated account identifier.
     * @param username username preserved as entered.
     * @param displayName display name captured at authentication.
     * @param role role captured at authentication, for dashboard routing only.
     */
    public record Principal(long accountId, String username, String displayName, Role role) {
    }
}
