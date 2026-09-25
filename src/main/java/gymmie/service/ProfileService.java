package gymmie.service;

import java.util.Objects;

import gymmie.model.Account;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.AccountRepository;

/**
 * Shared account profile operations with authorization and atomic persistence.
 *
 * <p>The shared authentication monitor serializes these operations with login, logout and
 * password changes. Session snapshots are refreshed only after a successful commit.
 */
public final class ProfileService {
    private final AccountRepository accounts;
    private final UnitOfWork unitOfWork;
    private final UserSession session;
    private final AuthService auth;
    private final Permissions permissions;

    /** Creates profile services using the same accounts, session and authentication service as the application. */
    public ProfileService(AccountRepository accounts, UnitOfWork unitOfWork,
            UserSession session, AuthService auth) {
        this.accounts = Objects.requireNonNull(accounts);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.session = Objects.requireNonNull(session);
        this.auth = Objects.requireNonNull(auth);
        permissions = new Permissions(accounts, session);
    }

    /**
     * Changes the shared display name for any authenticated account.
     *
     * @param displayName replacement name under the Account rules.
     * @throws Exception if authorization, validation or persistence fails.
     */
    public void changeOwnDisplayName(String displayName) throws Exception {
        synchronized (auth) {
            Account updated = unitOfWork.inTransaction(connection -> {
                Account account = permissions.requireAuthenticated(connection).withDisplayName(displayName);
                accounts.update(connection, account);
                return account;
            });
            session.establish(updated);
        }
    }
}
