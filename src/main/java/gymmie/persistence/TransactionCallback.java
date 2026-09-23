package gymmie.persistence;

import java.sql.Connection;

/**
 * Work executed within a {@link UnitOfWork} transaction.
 *
 * @param <T> result type returned by the transaction.
 */
@FunctionalInterface
public interface TransactionCallback<T> {
    /**
     * Executes work using the transaction's connection.
     *
     * @param connection connection whose transaction is in progress.
     * @return the transaction result.
     * @throws Exception if the work cannot complete successfully.
     */
    T execute(Connection connection) throws Exception;
}
