package gymmie.persistence.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

/** Executes prepared statements without taking ownership of the connection or transaction. */
final class SqliteQueries {
    private SqliteQueries() {
    }

    // Repository callers supply fixed SQL templates; all runtime values are bound separately.
    @SuppressWarnings("SqlSourceToSinkFlow")
    static <T> List<T> read(Connection connection, String sql, RowMapper<T> mapper, Object... parameters)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet rows = statement.executeQuery()) {
                return mapRows(rows, mapper);
            }
        }
    }

    // Repository callers supply fixed SQL templates; all runtime values are bound separately.
    @SuppressWarnings("SqlSourceToSinkFlow")
    static int write(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            return statement.executeUpdate();
        }
    }

    static void writeOne(Connection connection, String sql, Object... parameters) throws SQLException {
        if (write(connection, sql, parameters) != 1) {
            throw new SQLException("Record is missing or immutable fields do not match");
        }
    }

    private static <T> List<T> mapRows(ResultSet rows, RowMapper<T> mapper) throws SQLException {
        List<T> result = new ArrayList<>();
        while (rows.next()) {
            try {
                result.add(mapper.map(rows));
            } catch (RuntimeException exception) {
                throw new SQLException("Stored record contains invalid domain values", exception);
            }
        }
        return List.copyOf(result);
    }

    private static void bind(PreparedStatement statement, Object[] parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            Object value = parameters[index];
            if (value instanceof Enum<?> enumeration) {
                value = enumeration.name();
            } else if (value instanceof TemporalAccessor) {
                value = value.toString();
            } else if (value instanceof Boolean flag) {
                value = flag ? 1 : 0;
            }
            statement.setObject(index + 1, value);
        }
    }

    /** Maps one current result row to an immutable domain value. */
    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet row) throws SQLException;
    }
}
