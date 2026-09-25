package gymmie.testutil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import gymmie.persistence.Database;
import gymmie.persistence.Persistence;

/**
 * Owns an isolated, schema-initialized SQLite database shared across service connections.
 *
 * <p>Use one fixture per test in try-with-resources, or close it in AfterEach. Close all borrowed
 * connections before the fixture: SQLite releases the database when its last connection closes.
 * No application accounts or other seed data are inserted automatically.
 */
public final class InMemoryDatabase implements AutoCloseable {
    private final Database database;
    private final Persistence persistence;
    private final Connection keeper;

    /**
     * Opens a unique named in-memory database and applies the production schema.
     *
     * @throws SQLException if opening or initializing the database fails.
     */
    public InMemoryDatabase() throws SQLException {
        database = new Database("jdbc:sqlite:file:gymmie-test-" + UUID.randomUUID() + "?mode=memory&cache=shared");
        keeper = database.openConnection();
        persistence = new Persistence(database);
        try {
            persistence.initialize();
        } catch (SQLException | RuntimeException | Error exception) {
            try {
                keeper.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    /** Returns the configuration for additional caller-owned connections. */
    public Database database() {
        return database;
    }

    /** Returns real repositories and the production transaction boundary. */
    public Persistence persistence() {
        return persistence;
    }

    @Override
    public void close() throws SQLException {
        keeper.close();
    }
}
