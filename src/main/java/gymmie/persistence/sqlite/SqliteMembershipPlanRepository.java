package gymmie.persistence.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import gymmie.model.MembershipPlan;
import gymmie.persistence.repository.MembershipPlanRepository;

/**
 * SQLite MembershipPlan storage using only the connection supplied to each operation.
 */
public final class SqliteMembershipPlanRepository implements MembershipPlanRepository {
    private static final String SELECT =
            "SELECT id, name, duration_days, price_cents, archived FROM membership_plan";

    @Override
    public Optional<MembershipPlan> findById(Connection connection, long id)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE id = ?",
                SqliteMembershipPlanRepository::map, id).stream().findFirst();
    }

    @Override
    public List<MembershipPlan> findAll(Connection connection)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " ORDER BY id", SqliteMembershipPlanRepository::map);
    }

    @Override
    public List<MembershipPlan> findAllAvailable(Connection connection)
            throws SQLException {
        return SqliteQueries.read(connection, SELECT + " WHERE archived = 0 ORDER BY id",
                SqliteMembershipPlanRepository::map);
    }

    @Override
    public void insert(Connection connection, MembershipPlan plan) throws SQLException {
        SqliteQueries.writeOne(connection,
                "INSERT INTO membership_plan (id, name, duration_days, price_cents, archived) VALUES (?, "
                        + "?, ?, ?, ?)",
                plan.id(), plan.name(), plan.durationDays(), plan.priceCents(), plan.archived());
    }

    @Override
    public void update(Connection connection, MembershipPlan plan) throws SQLException {
        SqliteQueries.writeOne(connection,
                "UPDATE membership_plan SET name = ?, duration_days = ?, price_cents = ?, archived = ? "
                        + "WHERE id = ?",
                plan.name(), plan.durationDays(), plan.priceCents(), plan.archived(), plan.id());
    }

    @Override
    public boolean deleteIfUnpurchased(Connection connection, long id) throws SQLException {
        return SqliteQueries.write(connection,
                "DELETE FROM membership_plan WHERE id = ? AND NOT EXISTS (SELECT 1 FROM membership WHERE "
                        + "plan_id = ?)",
                id, id) == 1;
    }

    private static MembershipPlan map(ResultSet row) throws SQLException {
        return new MembershipPlan(row.getLong("id"), row.getString("name"),
                row.getInt("duration_days"), row.getInt("price_cents"), row.getBoolean("archived"));
    }
}
