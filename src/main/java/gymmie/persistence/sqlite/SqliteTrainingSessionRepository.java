package gymmie.persistence.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import gymmie.model.TrainingSession;
import gymmie.persistence.repository.TrainingSessionRepository;

/**
 * SQLite TrainingSession storage using only the connection supplied to each operation.
 */
public final class SqliteTrainingSessionRepository implements TrainingSessionRepository {
    private static final String SELECT =
            "SELECT id, trainer_id, starts_at, duration_minutes, capacity, description, cancelled "
                    + "FROM training_session";

    @Override
    public Optional<TrainingSession> findById(Connection connection, long id)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE id = ?",
                SqliteTrainingSessionRepository::map, id).stream().findFirst();
    }

    @Override
    public List<TrainingSession> findAll(Connection connection)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " ORDER BY id", SqliteTrainingSessionRepository::map);
    }

    @Override
    public List<TrainingSession> findByTrainerId(Connection connection, long trainerId)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE trainer_id = ? ORDER BY id",
                SqliteTrainingSessionRepository::map, trainerId);
    }

    @Override
    public List<TrainingSession> findUpcoming(Connection connection, LocalDateTime now) throws SQLException {
        Objects.requireNonNull(now);
        // Parse ISO timestamps before comparison to retain nanoseconds and support optional seconds.
        return SqliteQueries.read(connection, SELECT + " WHERE cancelled = 0 ORDER BY id",
                SqliteTrainingSessionRepository::map).stream()
                .filter(session -> session.startsAt().isAfter(now)).toList();
    }

    @Override
    public void insert(Connection connection, TrainingSession session) throws SQLException {
        SqliteQueries.writeOne(connection,
                "INSERT INTO training_session (id, trainer_id, starts_at, duration_minutes, capacity, "
                        + "description, cancelled) VALUES (?, ?, ?, ?, ?, ?, ?)",
                session.id(), session.trainerId(), session.startsAt(), session.durationMinutes(),
                session.capacity(), session.description(), session.cancelled());
    }

    @Override
    public void update(Connection connection, TrainingSession session) throws SQLException {
        SqliteQueries.writeOne(connection,
                "UPDATE training_session SET starts_at = ?, duration_minutes = ?, capacity = ?, "
                        + "description = ?, cancelled = ? WHERE id = ? AND trainer_id = ?",
                session.startsAt(), session.durationMinutes(), session.capacity(), session.description(),
                session.cancelled(), session.id(), session.trainerId());
    }

    @Override
    public boolean deleteIfNeverBooked(Connection connection, long id) throws SQLException {
        return SqliteQueries.write(connection,
                "DELETE FROM training_session WHERE id = ? AND NOT EXISTS (SELECT 1 FROM booking WHERE "
                        + "session_id = ?)",
                id, id) == 1;
    }

    private static TrainingSession map(ResultSet row) throws SQLException {
        return new TrainingSession(row.getLong("id"), row.getLong("trainer_id"),
                LocalDateTime.parse(row.getString("starts_at")), row.getInt("duration_minutes"),
                row.getInt("capacity"), row.getString("description"), row.getBoolean("cancelled"));
    }
}
