package gymmie.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Provides an all-or-nothing transaction boundary for persistence operations.
 */
public final class UnitOfWork {
    private final Database database;

    /**
     * Creates a unit of work that opens a connection for each transaction.
     *
     * @param database database that supplies transaction connections.
     */
    public UnitOfWork(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    /**
     * Executes work in a transaction and closes its connection afterward.
     *
     * @param callback work to execute.
     * @param <T> result type returned by the work.
     * @return the callback result after a successful commit.
     * @throws Exception if the callback or transaction cannot complete.
     */
    public <T> T inTransaction(TransactionCallback<T> callback) throws Exception {
        try (Connection connection = database.openConnection()) {
            return inTransaction(connection, callback);
        }
    }

    /**
     * Executes work in a transaction on a supplied connection without closing it.
     *
     * @param connection connection on which the transaction should run.
     * @param callback work to execute.
     * @param <T> result type returned by the work.
     * @return the callback result after a successful commit.
     * @throws Exception if the callback or transaction cannot complete.
     */
    public <T> T inTransaction(Connection connection, TransactionCallback<T> callback)
            throws Exception {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(callback);
        if (!connection.getAutoCommit()) {
            return callback.execute(connection);
        }

        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            T result = callback.execute(connection);
            connection.commit();
            return result;
        } catch (Exception | Error exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            restoreAutoCommit(connection, originalAutoCommit);
        }
    }

    private static void rollback(Connection connection, Throwable originalException) {
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
