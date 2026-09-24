package gymmie.persistence.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import gymmie.model.Account;

/**
 * Persists accounts using the caller-owned connection under this package's transaction contract.
 *
 * <p>Accounts are never deleted. Deactivation and reactivation update the active flag while retaining
 * memberships, sessions and bookings. Services protect the seeded Manager and cancel a deactivated
 * Member's future bookings in the same transaction.
 */
public interface AccountRepository {
    /**
     * Finds an account, including a deactivated account.
     *
     * @param connection caller-owned connection.
     * @param id account identifier.
     * @return the account, or empty if absent.
     * @throws SQLException if the query fails.
     */
    Optional<Account> findById(Connection connection, long id) throws SQLException;

    /**
     * Finds an account by its ASCII case-insensitive username across all roles and activation states.
     *
     * <p>Deactivated accounts remain visible so login can distinguish them from invalid credentials.
     *
     * @param connection caller-owned connection.
     * @param username login name to match without changing its stored spelling.
     * @return the matching account, or empty if absent.
     * @throws SQLException if the query fails.
     */
    Optional<Account> findByUsername(Connection connection, String username) throws SQLException;

    /**
     * Lists all accounts for administration, including deactivated accounts.
     *
     * @param connection caller-owned connection.
     * @return all accounts in identifier order.
     * @throws SQLException if the query fails.
     */
    List<Account> findAll(Connection connection) throws SQLException;

    /**
     * Lists only accounts whose active flag is true.
     *
     * @param connection caller-owned connection.
     * @return active accounts in identifier order.
     * @throws SQLException if the query fails.
     */
    List<Account> findAllActive(Connection connection) throws SQLException;

    /**
     * Inserts an account, enforcing globally unique, case-insensitive usernames across all roles.
     *
     * @param connection caller-owned connection.
     * @param account new account containing only a password hash and salt, never plaintext.
     * @throws SQLException if the identifier or username already exists or the insert fails.
     */
    void insert(Connection connection, Account account) throws SQLException;

    /**
     * Updates a profile, password or activation flag without deleting the account.
     *
     * <p>The existing username and role must remain unchanged; changing either is rejected.
     *
     * @param connection caller-owned connection.
     * @param account replacement state for the existing account.
     * @throws SQLException if the account is absent, immutable fields change or the update fails.
     */
    void update(Connection connection, Account account) throws SQLException;
}
