package gymmie.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.persistence.repository.AccountRepository;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;

/**
 * Service-layer authorization using current persisted account state on the operation's connection.
 *
 * <p>Roles have no implicit hierarchy: a Manager does not inherit Trainer or Member permissions.
 * Ownership identifiers must come from the loaded resource, not a user-supplied claim of ownership.
 * Checks and subsequent writes belong in the same transaction.
 */
public final class Permissions {
    private final AccountRepository accounts;
    private final UserSession session;

    /**
     * Creates the authorization boundary for an application session.
     *
     * @param accounts account repository using caller-owned connections.
     * @param session current application session.
     */
    public Permissions(AccountRepository accounts, UserSession session) {
        this.accounts = Objects.requireNonNull(accounts);
        this.session = Objects.requireNonNull(session);
    }

    /**
     * Reloads the authenticated account and rejects missing or deactivated accounts.
     *
     * @param connection connection of the protected operation.
     * @return current active account.
     * @throws SQLException if the account lookup fails.
     * @throws AuthenticationException if login is missing or its account no longer exists.
     * @throws AccountDeactivatedException if the account was deactivated after login.
     */
    public Account requireAuthenticated(Connection connection) throws SQLException {
        long id = session.requireUser().accountId();
        Account account = accounts.findById(connection, id).orElse(null);
        if (account == null) {
            session.clear();
            throw new AuthenticationException("The authenticated account no longer exists");
        }
        if (!account.active()) {
            session.clear();
            throw new AccountDeactivatedException();
        }
        return account;
    }

    /**
     * Requires an exact role, with no implicit permission inheritance.
     *
     * @param connection connection of the protected operation.
     * @param role required role.
     * @return current authorized account.
     * @throws SQLException if the account lookup fails.
     * @throws AuthenticationException if the session is missing or its account is unavailable.
     * @throws AuthorizationException if the account has a different role.
     */
    public Account requireRole(Connection connection, Role role) throws SQLException {
        Objects.requireNonNull(role);
        Account account = requireAuthenticated(connection);
        if (account.role() != role) {
            throw new AuthorizationException("This operation requires the " + role + " role");
        }
        return account;
    }

    /**
     * Requires an exact role and ownership, for Member history and Trainer-managed sessions.
     *
     * @param connection connection of the protected operation.
     * @param role required role.
     * @param ownerAccountId owner identifier from the stored resource being accessed.
     * @return current authorized owner.
     * @throws SQLException if the account lookup fails.
     * @throws AuthenticationException if the session is missing or its account is unavailable.
     * @throws AuthorizationException if the role or owner does not match.
     */
    public Account requireOwner(Connection connection, Role role, long ownerAccountId) throws SQLException {
        Account account = requireRole(connection, role);
        if (account.id() != ownerAccountId) {
            throw new AuthorizationException("You can only access your own records");
        }
        return account;
    }
}
