package gymmie.persistence.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import gymmie.model.TrainingSession;

/**
 * Persists sessions using the caller-owned connection under this package's transaction contract.
 *
 * <p>Sessions with any booking history are cancelled by updating their cancelled flag, never deleted.
 * Services validate scheduling and capacity, and cancel related bookings in the same transaction.
 */
public interface TrainingSessionRepository {
    /**
     * Finds a session, including past and cancelled sessions.
     *
     * @param connection caller-owned connection.
     * @param id session identifier.
     * @return the session, or empty if absent.
     * @throws SQLException if the query fails.
     */
    Optional<TrainingSession> findById(Connection connection, long id) throws SQLException;

    /**
     * Lists all sessions, including past and cancelled history.
     *
     * @param connection caller-owned connection.
     * @return all sessions in identifier order.
     * @throws SQLException if the query fails.
     */
    List<TrainingSession> findAll(Connection connection) throws SQLException;

    /**
     * Lists a Trainer's complete session history.
     *
     * @param connection caller-owned connection.
     * @param trainerId Trainer account identifier.
     * @return the Trainer's sessions in identifier order, including past and cancelled sessions.
     * @throws SQLException if the query fails.
     */
    List<TrainingSession> findByTrainerId(Connection connection, long trainerId) throws SQLException;

    /**
     * Lists uncancelled sessions starting strictly after the supplied local system time.
     *
     * @param connection caller-owned connection.
     * @param now local system time captured by the caller for consistent cut-off comparisons.
     * @return upcoming sessions in identifier order.
     * @throws SQLException if the query fails.
     */
    List<TrainingSession> findUpcoming(Connection connection, LocalDateTime now) throws SQLException;

    /**
     * Inserts a session.
     *
     * @param connection caller-owned connection.
     * @param session new session whose Trainer account already exists.
     * @throws SQLException if the identifier exists, the Trainer is missing or the insert fails.
     */
    void insert(Connection connection, TrainingSession session) throws SQLException;

    /**
     * Updates session details or cancellation state, preserving Trainer ownership and all bookings.
     *
     * @param connection caller-owned connection.
     * @param session replacement state for the existing session.
     * @throws SQLException if the session is absent, Trainer ownership changes or the update fails.
     */
    void update(Connection connection, TrainingSession session) throws SQLException;

    /**
     * Deletes a session only if it has never had a booking, including a subsequently cancelled booking.
     *
     * <p>The history guard and deletion must be atomic on the supplied connection, for example with
     * a conditional DELETE. Related bookings must never be deleted or modified by this operation.
     *
     * @param connection caller-owned connection.
     * @param id session identifier.
     * @return true if deleted; false if absent or any booking history exists, with no changes made.
     * @throws SQLException if the guarded deletion fails.
     */
    boolean deleteIfNeverBooked(Connection connection, long id) throws SQLException;
}
