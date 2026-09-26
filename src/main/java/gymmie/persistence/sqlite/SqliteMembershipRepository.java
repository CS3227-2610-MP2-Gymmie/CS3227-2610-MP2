package gymmie.persistence.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import gymmie.model.Membership;
import gymmie.model.MembershipStatus;
import gymmie.persistence.repository.MembershipRepository;

/**
 * SQLite Membership storage using only the connection supplied to each operation.
 */
public final class SqliteMembershipRepository implements MembershipRepository {
    private static final String SELECT =
            "SELECT id, member_id, plan_id, start_date, expiry_date, status, snapshot_price_cents, "
                    + "snapshot_duration_days FROM membership";

    @Override
    public Optional<Membership> findById(Connection connection, long id)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE id = ?",
                SqliteMembershipRepository::map, id).stream().findFirst();
    }

    @Override
    public List<Membership> findAll(Connection connection)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " ORDER BY id", SqliteMembershipRepository::map);
    }

    @Override
    public List<Membership> findByMemberId(Connection connection, long memberId)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE member_id = ? ORDER BY id",
                SqliteMembershipRepository::map, memberId);
    }

    @Override
    public long nextId(Connection connection) throws SQLException {
        return SqliteQueries.read(connection, "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM membership",
                row -> row.getLong("next_id")).getFirst();
    }

    @Override
    public void insert(Connection connection, Membership membership) throws SQLException {
        SqliteQueries.writeOne(connection,
                "INSERT INTO membership (id, member_id, plan_id, start_date, expiry_date, status, "
                        + "snapshot_price_cents, snapshot_duration_days) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                membership.id(), membership.memberId(), membership.planId(), membership.startDate(),
                membership.expiryDate(), membership.status(), membership.snapshotPriceCents(),
                membership.snapshotDurationDays());
    }

    @Override
    public void update(Connection connection, Membership membership) throws SQLException {
        SqliteQueries.writeOne(connection,
                "UPDATE membership SET expiry_date = ?, status = ? WHERE id = ? AND member_id = ? AND "
                        + "plan_id = ? AND start_date = ? AND snapshot_price_cents = ? AND snapshot_duration_days = "
                        + "?",
                membership.expiryDate(), membership.status(), membership.id(), membership.memberId(),
                membership.planId(), membership.startDate(), membership.snapshotPriceCents(),
                membership.snapshotDurationDays());
    }

    private static Membership map(ResultSet row) throws SQLException {
        return new Membership(row.getLong("id"), row.getLong("member_id"), row.getLong("plan_id"),
                LocalDate.parse(row.getString("start_date")), LocalDate.parse(row.getString("expiry_date")),
                MembershipStatus.valueOf(row.getString("status")), row.getInt("snapshot_price_cents"),
                row.getInt("snapshot_duration_days"));
    }
}
