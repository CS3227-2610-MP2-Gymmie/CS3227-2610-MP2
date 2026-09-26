package gymmie.persistence.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.persistence.repository.BookingRepository;

/**
 * SQLite Booking storage using only the connection supplied to each operation.
 */
public final class SqliteBookingRepository implements BookingRepository {
    private static final String SELECT =
            "SELECT id, session_id, member_id, booked_at, status, cancellation_reason FROM booking";

    @Override
    public Optional<Booking> findById(Connection connection, long id)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE id = ?",
                SqliteBookingRepository::map, id).stream().findFirst();
    }

    @Override
    public Optional<Booking> findByMemberAndSession(Connection connection, long memberId, long sessionId)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE member_id = ? AND session_id = ?",
                SqliteBookingRepository::map, memberId, sessionId).stream().findFirst();
    }

    @Override
    public List<Booking> findByMemberId(Connection connection, long memberId)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE member_id = ? ORDER BY id",
                SqliteBookingRepository::map, memberId);
    }

    @Override
    public List<Booking> findBySessionId(Connection connection, long sessionId)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE session_id = ? ORDER BY id",
                SqliteBookingRepository::map, sessionId);
    }

    @Override
    public int countBookedBySessionId(Connection connection, long sessionId) throws SQLException {
        return SqliteQueries.read(connection,
                "SELECT COUNT(*) AS total FROM booking WHERE session_id = ? AND status = 'BOOKED'",
                row -> row.getInt("total"), sessionId).getFirst();
    }

    @Override
    public long nextId(Connection connection) throws SQLException {
        return SqliteQueries.read(connection, "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM booking",
                row -> row.getLong("next_id")).getFirst();
    }

    @Override
    public void insert(Connection connection, Booking booking) throws SQLException {
        SqliteQueries.writeOne(connection,
                "INSERT INTO booking (id, session_id, member_id, booked_at, status, cancellation_reason) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                booking.id(), booking.sessionId(), booking.memberId(), booking.bookedAt(),
                booking.status(), booking.cancellationReason());
    }

    @Override
    public void update(Connection connection, Booking booking) throws SQLException {
        Booking existing = findById(connection, booking.id())
                .orElseThrow(() -> new SQLException("Booking does not exist"));
        if (!existing.bookedAt().equals(booking.bookedAt())) {
            throw new SQLException("Booking time cannot change");
        }
        SqliteQueries.writeOne(connection,
                "UPDATE booking SET status = ?, cancellation_reason = ? WHERE id = ? AND member_id = ? "
                        + "AND session_id = ?",
                booking.status(), booking.cancellationReason(), booking.id(), booking.memberId(), booking.sessionId());
    }

    @Override
    public void reactivate(Connection connection, long bookingId, LocalDateTime bookedAt) throws SQLException {
        SqliteQueries.writeOne(connection,
                "UPDATE booking SET status = 'BOOKED', cancellation_reason = NULL, booked_at = ? "
                        + "WHERE id = ? AND status = 'CANCELLED'",
                bookedAt, bookingId);
    }

    private static Booking map(ResultSet row) throws SQLException {
        String reason = row.getString("cancellation_reason");
        return new Booking(row.getLong("id"), row.getLong("session_id"), row.getLong("member_id"),
                LocalDateTime.parse(row.getString("booked_at")), BookingStatus.valueOf(row.getString("status")),
                reason == null ? null : CancellationReason.valueOf(reason));
    }
}
