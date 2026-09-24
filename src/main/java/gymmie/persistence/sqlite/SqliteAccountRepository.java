package gymmie.persistence.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import gymmie.model.Account;
import gymmie.model.PasswordHash;
import gymmie.model.Role;
import gymmie.persistence.repository.AccountRepository;

/**
 * SQLite Account storage using only the connection supplied to each operation.
 */
public final class SqliteAccountRepository implements AccountRepository {
    private static final String SELECT =
            "SELECT id, username, password_hash, salt, display_name, role, active FROM account";

    @Override
    public Optional<Account> findById(Connection connection, long id)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE id = ?",
                SqliteAccountRepository::map, id).stream().findFirst();
    }

    @Override
    public Optional<Account> findByUsername(Connection connection, String username)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE username = ? COLLATE NOCASE",
                SqliteAccountRepository::map, username).stream().findFirst();
    }

    @Override
    public List<Account> findAll(Connection connection)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " ORDER BY id", SqliteAccountRepository::map);
    }

    @Override
    public List<Account> findAllActive(Connection connection)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE active = 1 ORDER BY id", SqliteAccountRepository::map);
    }

    @Override
    public void insert(Connection connection, Account account) throws SQLException {
        SqliteQueries.writeOne(connection,
                "INSERT INTO account (id, username, password_hash, salt, display_name, role, active) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                account.id(), account.username(), account.password().hash(), account.password().salt(),
                account.displayName(), account.role(), account.active());
    }

    @Override
    public void update(Connection connection, Account account) throws SQLException {
        SqliteQueries.writeOne(connection,
                "UPDATE account SET password_hash = ?, salt = ?, display_name = ?, active = ? WHERE id = "
                        + "? AND username = ? COLLATE BINARY AND role = ?",
                account.password().hash(), account.password().salt(), account.displayName(), account.active(),
                account.id(), account.username(), account.role());
    }

    private static Account map(ResultSet row) throws SQLException {
        return new Account(row.getLong("id"), row.getString("username"),
                new PasswordHash(row.getString("password_hash"), row.getString("salt")),
                row.getString("display_name"), Role.valueOf(row.getString("role")), row.getBoolean("active"));
    }
}
