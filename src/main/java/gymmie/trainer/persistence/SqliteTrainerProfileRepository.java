package gymmie.trainer.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import gymmie.persistence.sqlite.SqliteQueries;
import gymmie.trainer.model.TrainerProfile;

/** SQLite storage for Trainer biographies and ordered specialization tags. */
public final class SqliteTrainerProfileRepository implements TrainerProfileRepository {
    @Override
    public TrainerProfile findByAccountId(Connection connection, long accountId) throws SQLException {
        var synopses = SqliteQueries.read(connection,
                "SELECT synopsis FROM trainer_profile WHERE account_id = ?", row -> row.getString(1), accountId);
        var tags = SqliteQueries.read(connection,
                "SELECT tag FROM trainer_specialization WHERE account_id = ? ORDER BY position",
                row -> row.getString(1), accountId);
        return new TrainerProfile(accountId, synopses.isEmpty() ? "" : synopses.getFirst(), tags);
    }

    @Override
    public void save(Connection connection, TrainerProfile profile) throws SQLException {
        SqliteQueries.writeOne(connection,
                "INSERT INTO trainer_profile (account_id, synopsis) VALUES (?, ?) "
                        + "ON CONFLICT(account_id) DO UPDATE SET synopsis = excluded.synopsis",
                profile.accountId(), profile.synopsis());
        SqliteQueries.write(connection, "DELETE FROM trainer_specialization WHERE account_id = ?", profile.accountId());
        for (int position = 0; position < profile.specializations().size(); position++) {
            SqliteQueries.writeOne(connection,
                    "INSERT INTO trainer_specialization (account_id, position, tag) VALUES (?, ?, ?)",
                    profile.accountId(), position, profile.specializations().get(position));
        }
    }
}
