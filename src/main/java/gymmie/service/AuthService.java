package gymmie.service;

import java.util.Objects;

import gymmie.model.Account;
import gymmie.model.PasswordHash;
import gymmie.model.exception.ValidationException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.AccountRepository;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;

/**
 * Top-level authentication operations with transaction ownership and post-commit session publication.
 *
 * <p>These methods start their own transactions and must not be called from nested transactional work.
 * Authentication changes on this service are serialized so logout cannot race its own login or password update.
 */
public final class AuthService {
    private final AccountRepository accounts;
    private final UnitOfWork unitOfWork;
    private final UserSession session;
    private final PasswordHasher hasher;
    private final Permissions permissions;

    /**
     * Creates authentication services sharing one session and account repository.
     *
     * @param accounts account storage with global case-insensitive username uniqueness.
     * @param unitOfWork top-level transaction boundary.
     * @param session session to establish or clear.
     * @param hasher password hashing boundary.
     */
    public AuthService(AccountRepository accounts, UnitOfWork unitOfWork, UserSession session,
            PasswordHasher hasher) {
        this.accounts = Objects.requireNonNull(accounts);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.session = Objects.requireNonNull(session);
        this.hasher = Objects.requireNonNull(hasher);
        this.permissions = new Permissions(accounts, session);
    }

    /**
     * Authenticates a username and password, establishing session state only after transaction success.
     *
     * <p>A failed login clears any previous identity. A deactivated account is distinguished only
     * after its password is verified; incorrect passwords and unknown usernames share one failure.
     *
     * @param username username matched case-insensitively without changing its stored spelling.
     * @param password candidate password, not retained.
     * @return authenticated identity without credential material.
     * @throws AuthenticationException if the username or password is invalid.
     * @throws AccountDeactivatedException if verified credentials belong to a deactivated account.
     * @throws Exception if the account lookup or transaction fails.
     */
    public synchronized UserSession.Principal login(String username, String password) throws Exception {
        session.clear();
        Account account = unitOfWork.inTransaction(connection -> {
            if (username == null || password == null) {
                throw invalidCredentials();
            }
            Account found = accounts.findByUsername(connection, username).orElseThrow(AuthService::invalidCredentials);
            if (!hasher.verify(password, found.password())) {
                throw invalidCredentials();
            }
            if (!found.active()) {
                throw new AccountDeactivatedException();
            }
            return found;
        });
        session.establish(account);
        return session.requireUser();
    }

    /** Clears local authentication; calling logout repeatedly is safe. */
    public synchronized void logout() {
        session.clear();
    }

    /**
     * Changes only the authenticated user's password after verifying their current password.
     *
     * <p>No target account ID is accepted. The fresh account is authorized and updated on one connection.
     * A persistence failure leaves the previous password and session identity intact.
     *
     * @param currentPassword current plaintext password used only for verification.
     * @param newPassword new plaintext password of 8–128 characters, not retained.
     * @throws AuthenticationException if login is missing, the account is unavailable or the current password is wrong.
     * @throws ValidationException if the new password violates the password policy.
     * @throws Exception if persistence or transaction completion fails.
     */
    public synchronized void changeOwnPassword(String currentPassword, String newPassword) throws Exception {
        Account updated = unitOfWork.inTransaction(connection -> {
            Account account = permissions.requireAuthenticated(connection);
            if (!hasher.verify(currentPassword, account.password())) {
                throw invalidCredentials();
            }
            PasswordHash password = hasher.hash(newPassword);
            Account replacement = new Account(account.id(), account.username(), password,
                    account.displayName(), account.role(), account.active());
            accounts.update(connection, replacement);
            return replacement;
        });
        session.establish(updated);
    }

    private static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid username or password");
    }
}
