package gymmie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gymmie.persistence.Database;
import gymmie.persistence.SchemaInitializer;
import gymmie.persistence.UnitOfWork;

class PersistenceBoundaryTest {
    private Database database;
    private Connection connection;
    private UnitOfWork unitOfWork;

    @BeforeEach
    void setUp() throws SQLException {
        database = new Database("jdbc:sqlite::memory:");
        connection = database.openConnection();
        new SchemaInitializer(database).initialize(connection);
        unitOfWork = new UnitOfWork(database);
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void schemaInitializesFreshDatabaseAndSetsVersion() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, "booking");
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("PRAGMA user_version");
                ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next());
            assertTrue(resultSet.getInt(1) > 0);
        }
    }

    @Test
    void schemaInitializerDoesNotCommitCallerTransaction() throws SQLException {
        connection.setAutoCommit(false);
        insertAccount(connection, 1, "caller-work");

        new SchemaInitializer(database).initialize(connection);

        connection.rollback();
        connection.setAutoCommit(true);
        assertEquals(0, countAccounts());
    }

    @Test
    void foreignKeyViolationIsRejected() throws SQLException {
        insertAccount(1, "trainer");
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO training_session "
                        + "(id, trainer_id, starts_at, duration_minutes, capacity, description) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setInt(1, 1);
            statement.setInt(2, 1);
            statement.setString(3, "2026-09-23T10:00:00");
            statement.setInt(4, 60);
            statement.setInt(5, 10);
            statement.setString(6, "Strength training");
            statement.executeUpdate();
        }

        assertThrows(SQLException.class, () -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO booking "
                            + "(session_id, member_id, booked_at, status) VALUES (?, ?, ?, ?)")) {
                statement.setInt(1, 1);
                statement.setInt(2, 999);
                statement.setString(3, "2026-09-23T09:00:00");
                statement.setString(4, "BOOKED");
                statement.executeUpdate();
            }
        });
    }

    @Test
    void failedUnitOfWorkRollsBackChanges() throws Exception {
        assertThrows(IllegalStateException.class, () -> unitOfWork.inTransaction(connection, transaction -> {
            insertAccount(transaction, 1, "rollback-user");
            throw new IllegalStateException("force rollback");
        }));

        assertEquals(0, countAccounts());
    }

    @Test
    void successfulUnitOfWorkCommitsChanges() throws Exception {
        unitOfWork.inTransaction(connection, transaction -> {
            insertAccount(transaction, 1, "commit-user");
            return null;
        });

        assertEquals(1, countAccounts());
    }

    @Test
    void nestedUnitOfWorkJoinsOuterTransaction() throws Exception {
        assertThrows(IllegalStateException.class, () -> unitOfWork.inTransaction(connection, outer -> {
            insertAccount(outer, 1, "outer-user");
            unitOfWork.inTransaction(connection, inner -> {
                insertAccount(inner, 2, "inner-user");
                return null;
            });
            throw new IllegalStateException("force outer rollback");
        }));

        assertEquals(0, countAccounts());
    }

    @Test
    void nestedUnitOfWorkRollsBackFailedScopeToSavepoint() throws Exception {
        unitOfWork.inTransaction(connection, outer -> {
            insertAccount(outer, 1, "outer-user");
            try {
                unitOfWork.inTransaction(connection, inner -> {
                    insertAccount(inner, 2, "inner-user");
                    throw new IllegalStateException("force nested rollback");
                });
            } catch (IllegalStateException exception) {
                assertEquals("force nested rollback", exception.getMessage());
            }
            return null;
        });

        assertEquals(1, countAccountsForUsername("outer-user"));
        assertEquals(0, countAccountsForUsername("inner-user"));
    }

    private void insertAccount(int id, String username) throws SQLException {
        insertAccount(connection, id, username);
    }

    private static void insertAccount(Connection connection, int id, String username)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO account "
                        + "(id, username, password_hash, salt, display_name, role) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setInt(1, id);
            statement.setString(2, username);
            statement.setString(3, "hash");
            statement.setString(4, "salt");
            statement.setString(5, username);
            statement.setString(6, "MEMBER");
            statement.executeUpdate();
        }
    }

    private int countAccounts() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM account");
                ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private int countAccountsForUsername(String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM account WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getInt(1);
            }
        }
    }
}
