package gymmie.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Applies Gymmie's initial schema and manages the SQLite schema version.
 */
public final class SchemaInitializer {
    /** The schema version created by the current schema resource. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final String SCHEMA_RESOURCE = "/gymmie/db/schema.sql";

    private final Database database;

    /**
     * Creates an initializer for a database.
     *
     * @param database database whose schema should be initialized.
     */
    public SchemaInitializer(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    /**
     * Initializes the configured database using a dedicated connection.
     *
     * @throws SQLException if the schema cannot be read or applied.
     */
    public void initialize() throws SQLException {
        try (Connection connection = database.openConnection()) {
            initialize(connection);
        }
    }

    /**
     * Initializes a supplied connection, which is useful for in-memory SQLite databases.
     *
     * @param connection configured SQLite connection to initialize.
     * @throws SQLException if the schema cannot be read or applied.
     */
    public void initialize(Connection connection) throws SQLException {
        Objects.requireNonNull(connection);
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            int version = readUserVersion(connection);
            if (version == 0) {
                applySchema(connection);
                setUserVersion(connection, CURRENT_SCHEMA_VERSION);
            } else if (version > CURRENT_SCHEMA_VERSION) {
                throw new SQLException("Database schema version is newer than this application");
            } else if (version == CURRENT_SCHEMA_VERSION) {
                // The database already has the current schema.
            } else {
                throw new SQLException("No migration path from schema version " + version
                        + " to " + CURRENT_SCHEMA_VERSION);
            }
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            restoreAutoCommit(connection, originalAutoCommit);
        }
    }

    private static int readUserVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA user_version")) {
            if (!resultSet.next()) {
                throw new SQLException("SQLite did not return a schema version");
            }
            return resultSet.getInt(1);
        }
    }

    private static void applySchema(Connection connection) throws SQLException {
        String schema = readSchema();
        try (Statement statement = connection.createStatement()) {
            // This splitter requires statements without semicolons in trigger bodies or string
            // literals. Replace it with a real SQL script parser before adding such statements.
            for (String sql : schema.split(";")) {
                String trimmedSql = sql.strip();
                if (!trimmedSql.isEmpty()) {
                    statement.execute(trimmedSql);
                }
            }
        }
    }

    private static String readSchema() throws SQLException {
        try (InputStream input = SchemaInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) {
                throw new SQLException("Missing database schema resource: " + SCHEMA_RESOURCE);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new SQLException("Unable to read database schema resource", exception);
        }
    }

    private static void setUserVersion(Connection connection, int version) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA user_version = " + version);
        }
    }

    private static void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private static void restoreAutoCommit(Connection connection, boolean originalAutoCommit)
            throws SQLException {
        if (connection.getAutoCommit() != originalAutoCommit) {
            connection.setAutoCommit(originalAutoCommit);
        }
    }
}
