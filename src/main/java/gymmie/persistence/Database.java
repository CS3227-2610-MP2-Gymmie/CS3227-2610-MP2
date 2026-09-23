package gymmie.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Creates configured SQLite connections for Gymmie.
 *
 * <p>SQLite applies foreign-key enforcement per connection, so every connection
 * created here enables it before being returned. WAL mode is also requested for
 * file-backed databases to support concurrent readers and writers.
 *
 * <p>A {@code jdbc:sqlite::memory:} URL creates a separate empty database for
 * each connection. Therefore, the connection-opening forms of
 * {@link SchemaInitializer#initialize()} and
 * {@link UnitOfWork#inTransaction(TransactionCallback)} cannot be used with an
 * in-memory database; use the supplied-connection overloads instead.
 */
public final class Database {
    private static final String DEFAULT_DATABASE_PATH = "data/gymmie.db";

    private final String jdbcUrl;
    private final Path databasePath;

    /**
     * Creates a database configuration for Gymmie's default local database.
     */
    public Database() {
        this(Path.of(DEFAULT_DATABASE_PATH));
    }

    /**
     * Creates a database configuration for a file-backed SQLite database.
     *
     * @param databasePath path of the SQLite database file.
     */
    public Database(Path databasePath) {
        this.jdbcUrl = "jdbc:sqlite:" + Objects.requireNonNull(databasePath);
        this.databasePath = databasePath;
    }

    /**
     * Creates a database configuration for a JDBC SQLite URL.
     *
     * @param jdbcUrl SQLite JDBC URL, such as {@code jdbc:sqlite::memory:}.
     */
    public Database(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
        this.databasePath = null;
    }

    /**
     * Opens a connection with SQLite foreign keys enabled and WAL mode requested.
     *
     * @return a newly opened and configured SQLite connection.
     * @throws SQLException if the database directory cannot be created, the
     *         connection cannot be opened, or either SQLite setting fails.
     */
    public Connection openConnection() throws SQLException {
        createDatabaseDirectory();
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try {
            configureConnection(connection);
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    private void createDatabaseDirectory() throws SQLException {
        if (databasePath == null) {
            return;
        }

        Path parent = databasePath.toAbsolutePath().getParent();
        try {
            Files.createDirectories(parent);
        } catch (java.io.IOException exception) {
            throw new SQLException("Unable to create the database directory", exception);
        }
    }

    private static void configureConnection(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
        }
    }
}
