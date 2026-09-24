package gymmie.persistence.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import gymmie.model.Booking;

/**
 * Persists bookings using the caller-owned connection under this package's transaction contract.
 *
 * <p>Bookings are never deleted. Cancelled bookings retain their reason, ownership and session
 * reference, so a session's booking history remains available even after its last booking is cancelled.
 */
public interface BookingRepository {
    /**
     * Finds a booking, including cancelled bookings.
     *
     * @param connection caller-owned connection.
     * @param id booking identifier.
     * @return the booking, or empty if absent.
     * @throws SQLException if the query fails.
     */
    Optional<Booking> findById(Connection connection, long id) throws SQLException;

    /**
     * Finds a Member's booking for a session, including a cancelled booking.
     *
     * @param connection caller-owned connection.
     * @param memberId Member account identifier.
     * @param sessionId session identifier.
     * @return the unique matching booking, or empty if absent.
     * @throws SQLException if the query fails.
     */
    Optional<Booking> findByMemberAndSession(Connection connection, long memberId, long sessionId) throws SQLException;

    /**
     * Lists a Member's complete booking history for display or transactional cancellation.
     *
     * @param connection caller-owned connection.
     * @param memberId Member account identifier.
     * @return the Member's bookings in identifier order, including cancelled bookings.
     * @throws SQLException if the query fails.
     */
    List<Booking> findByMemberId(Connection connection, long memberId) throws SQLException;

    /**
     * Lists all bookings for a session, including cancelled bookings needed for history checks.
     *
     * @param connection caller-owned connection.
     * @param sessionId session identifier.
     * @return the session's bookings in identifier order, regardless of status.
     * @throws SQLException if the query fails.
     */
    List<Booking> findBySessionId(Connection connection, long sessionId) throws SQLException;

    /**
     * Counts only BOOKED reservations for capacity checks, excluding cancelled bookings.
     *
     * <p>This count must not be used to decide whether a session has ever had a booking.
     *
     * @param connection caller-owned connection.
     * @param sessionId session identifier.
     * @return the current booking count, or zero if there are no booked reservations.
     * @throws SQLException if the query fails.
     */
    int countBookedBySessionId(Connection connection, long sessionId) throws SQLException;

    /**
     * Inserts a booking with a unique Member/session pair, including cancelled booking history.
     *
     * @param connection caller-owned connection.
     * @param booking new booking whose Member and session already exist.
     * @throws SQLException if the identifier or pair exists, a reference is missing or the insert fails.
     */
    void insert(Connection connection, Booking booking) throws SQLException;

    /**
     * Updates booking status and its cancellation reason while retaining historical identity.
     *
     * <p>Changes to member ID, session ID or booking time are rejected.
     *
     * @param connection caller-owned connection.
     * @param booking replacement state for the existing booking.
     * @throws SQLException if the booking is absent, immutable fields change or the update fails.
     */
    void update(Connection connection, Booking booking) throws SQLException;
}
