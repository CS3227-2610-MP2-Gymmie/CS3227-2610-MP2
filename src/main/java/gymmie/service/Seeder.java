package gymmie.service;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.AccountRepository;

/**
 * Creates the initial Manager before application services become available.
 */
public final class Seeder {
    private final AccountRepository accounts;
    private final UnitOfWork unitOfWork;
    private final PasswordHasher hasher;

    /**
     * Creates a seeder using the application's account and password boundaries.
     *
     * @param accounts account storage.
     * @param unitOfWork top-level transaction boundary.
     * @param hasher password hashing boundary.
     */
    public Seeder(AccountRepository accounts, UnitOfWork unitOfWork, PasswordHasher hasher) {
        this.accounts = Objects.requireNonNull(accounts);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.hasher = Objects.requireNonNull(hasher);
    }

    /**
     * Seeds the documented manager / manager123 credentials when the username is absent.
     *
     * <p>An existing active Manager is preserved, including its password. A conflicting or invalid
     * account is never overwritten. This method owns its transaction and runs after schema initialization.
     *
     * @throws ConflictException if the reserved username belongs to an invalid, inactive or non-Manager account.
     * @throws Exception if hashing, storage or transaction completion fails.
     */
    public void seed() throws Exception {
        unitOfWork.inTransaction(connection -> {
            Optional<Account> existing;
            try {
                existing = accounts.findByUsername(connection, Account.SEEDED_MANAGER_USERNAME);
            } catch (SQLException exception) {
                // Manually deactivated Managers cannot be materialized by the account model.
                if (exception.getCause() instanceof ValidationException validation) {
                    throw new ConflictException("Cannot seed Manager: username 'manager' has invalid account data: "
                            + validation.getMessage());
                }
                throw exception;
            }
            if (existing.isPresent()) {
                Account manager = existing.orElseThrow();
                if (manager.role() != Role.MANAGER || !manager.active()) {
                    throw new ConflictException(
                            "Cannot seed Manager: username 'manager' must belong to an active Manager");
                }
                return null;
            }
            long id = 1;
            // Repository results are ordered by ID; use the first available positive identifier.
            for (Account account : accounts.findAll(connection)) {
                if (account.id() > id) {
                    break;
                }
                if (account.id() == id) {
                    id++;
                }
            }
            accounts.insert(connection, new Account(id, Account.SEEDED_MANAGER_USERNAME,
                    hasher.hash("manager123"), "Manager", Role.MANAGER, true));
            return null;
        });
    }
}
