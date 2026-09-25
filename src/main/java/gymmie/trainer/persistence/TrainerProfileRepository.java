package gymmie.trainer.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import gymmie.trainer.model.TrainerProfile;

/** Stores Trainer details using caller-owned connections and transactions. */
public interface TrainerProfileRepository {
    /**
     * Loads details, defaulting to an empty profile when none have been saved.
     *
     * @param connection caller-owned connection.
     * @param accountId Trainer account identifier authorized by the service.
     * @return stored or empty details.
     * @throws SQLException if reading fails.
     */
    TrainerProfile findByAccountId(Connection connection, long accountId) throws SQLException;

    /**
     * Replaces details and tags within the caller's transaction.
     *
     * @param connection caller-owned transactional connection.
     * @param profile replacement details.
     * @throws SQLException if writing fails.
     */
    void save(Connection connection, TrainerProfile profile) throws SQLException;
}
