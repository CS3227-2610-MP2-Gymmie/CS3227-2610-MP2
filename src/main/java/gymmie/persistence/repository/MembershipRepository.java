package gymmie.persistence.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import gymmie.model.Membership;

/**
 * Persists membership history using the caller-owned connection under this package's transaction contract.
 *
 * <p>Memberships are never deleted. Cancellation updates lifecycle state while retaining purchase
 * evidence and snapshots. Services load complete Member history to enforce the single active
 * membership rule and cancel related future bookings in the same transaction.
 */
public interface MembershipRepository {
    /**
     * Finds a membership, including cancelled and expired history.
     *
     * @param connection caller-owned connection.
     * @param id membership identifier.
     * @return the membership, or empty if absent.
     * @throws SQLException if the query fails.
     */
    Optional<Membership> findById(Connection connection, long id) throws SQLException;

    /**
     * Lists complete membership history for reporting.
     *
     * @param connection caller-owned connection.
     * @return all memberships in identifier order, regardless of status or dates.
     * @throws SQLException if the query fails.
     */
    List<Membership> findAll(Connection connection) throws SQLException;

    /**
     * Loads a Member's complete history for constructing the Member aggregate.
     *
     * @param connection caller-owned connection.
     * @param memberId Member account identifier.
     * @return all owned memberships in identifier order, including cancelled, expired and future records.
     * @throws SQLException if the query fails.
     */
    List<Membership> findByMemberId(Connection connection, long memberId) throws SQLException;

    /**
     * Inserts a membership with its purchase-time price and duration snapshots.
     *
     * @param connection caller-owned connection.
     * @param membership new membership whose Member and plan already exist.
     * @throws SQLException if the identifier exists, a reference is missing or the insert fails.
     */
    void insert(Connection connection, Membership membership) throws SQLException;

    /**
     * Updates expiry or lifecycle state while retaining the original purchase terms and ownership.
     *
     * <p>Changes to member ID, plan ID, start date, snapshot price or snapshot duration are rejected.
     * A plan catalogue edit must never overwrite an existing membership's snapshots.
     *
     * @param connection caller-owned connection.
     * @param membership replacement state for the existing membership.
     * @throws SQLException if the membership is absent, immutable fields change or the update fails.
     */
    void update(Connection connection, Membership membership) throws SQLException;
}
