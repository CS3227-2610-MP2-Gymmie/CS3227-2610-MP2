package gymmie.persistence.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import gymmie.model.MembershipPlan;

/**
 * Persists membership plans using the caller-owned connection under this package's transaction contract.
 *
 * <p>Purchased plans are archived by updating their archived flag, never deleted. Archival and
 * restoration preserve all membership snapshots. Archived plans remain accessible for renewal.
 */
public interface MembershipPlanRepository {
    /**
     * Finds a plan, including an archived plan.
     *
     * @param connection caller-owned connection.
     * @param id plan identifier.
     * @return the plan, or empty if absent.
     * @throws SQLException if the query fails.
     */
    Optional<MembershipPlan> findById(Connection connection, long id) throws SQLException;

    /**
     * Lists all plans for administration, including archived plans.
     *
     * @param connection caller-owned connection.
     * @return all plans in identifier order.
     * @throws SQLException if the query fails.
     */
    List<MembershipPlan> findAll(Connection connection) throws SQLException;

    /**
     * Lists unarchived plans available for new purchases.
     *
     * @param connection caller-owned connection.
     * @return unarchived plans in identifier order.
     * @throws SQLException if the query fails.
     */
    List<MembershipPlan> findAllAvailable(Connection connection) throws SQLException;

    /**
     * Inserts a plan with integer-cent pricing.
     *
     * @param connection caller-owned connection.
     * @param plan new plan.
     * @throws SQLException if the identifier already exists or the insert fails.
     */
    void insert(Connection connection, MembershipPlan plan) throws SQLException;

    /**
     * Updates catalogue fields or archives/restores a plan without changing purchased snapshots.
     *
     * @param connection caller-owned connection.
     * @param plan replacement state for the existing plan.
     * @throws SQLException if the plan is absent or the update fails.
     */
    void update(Connection connection, MembershipPlan plan) throws SQLException;

    /**
     * Deletes a plan only if no membership has ever referenced it, regardless of membership status.
     *
     * <p>The history guard and deletion must be atomic on the supplied connection, for example with
     * a conditional DELETE. A separate, unprotected check followed by deletion is not sufficient.
     * Related memberships must never be deleted or modified by this operation.
     *
     * @param connection caller-owned connection.
     * @param id plan identifier.
     * @return true if deleted; false if absent or any purchase history exists, with no changes made.
     * @throws SQLException if the guarded deletion fails.
     */
    boolean deleteIfUnpurchased(Connection connection, long id) throws SQLException;
}
