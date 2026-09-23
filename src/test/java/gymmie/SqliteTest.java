package gymmie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

class SqliteTest {
    @Test
    void sqliteDriverExecutesInMemoryQuery() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }
    }
}
