package gymmie.persistence;

import java.sql.SQLException;
import java.util.Objects;

import gymmie.persistence.repository.AccountRepository;
import gymmie.persistence.repository.BookingRepository;
import gymmie.persistence.repository.MembershipPlanRepository;
import gymmie.persistence.repository.MembershipRepository;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.persistence.sqlite.SqliteAccountRepository;
import gymmie.persistence.sqlite.SqliteBookingRepository;
import gymmie.persistence.sqlite.SqliteMembershipPlanRepository;
import gymmie.persistence.sqlite.SqliteMembershipRepository;
import gymmie.persistence.sqlite.SqliteTrainingSessionRepository;

/**
 * Wires the application's database boundary and stateless SQLite repositories.
 *
 * <p>Only top-level application code owns this composition. Nested work receives repository
 * interfaces and the current transaction connection, not this object's UnitOfWork.
 */
public final class Persistence {
    private final SchemaInitializer schemaInitializer;
    private final UnitOfWork unitOfWork;
    private final AccountRepository accounts = new SqliteAccountRepository();
    private final MembershipPlanRepository plans = new SqliteMembershipPlanRepository();
    private final MembershipRepository memberships = new SqliteMembershipRepository();
    private final TrainingSessionRepository sessions = new SqliteTrainingSessionRepository();
    private final BookingRepository bookings = new SqliteBookingRepository();

    /** Creates the application's storage boundary for data/gymmie.db. */
    public Persistence() {
        this(new Database());
    }

    /**
     * Creates a boundary for a supplied database, allowing isolated file-backed tests.
     *
     * @param database shared database configuration.
     */
    public Persistence(Database database) {
        Objects.requireNonNull(database);
        schemaInitializer = new SchemaInitializer(database);
        unitOfWork = new UnitOfWork(database);
    }

    /**
     * Initializes persistent storage before application services run.
     *
     * @throws SQLException if opening or initializing the database fails.
     */
    public void initialize() throws SQLException {
        schemaInitializer.initialize();
    }

    /** Returns the transaction boundary for top-level application services. */
    public UnitOfWork unitOfWork() {
        return unitOfWork;
    }

    /** Returns account storage without exposing connection-opening operations. */
    public AccountRepository accounts() {
        return accounts;
    }

    /** Returns membership-plan storage. */
    public MembershipPlanRepository plans() {
        return plans;
    }

    /** Returns membership history storage. */
    public MembershipRepository memberships() {
        return memberships;
    }

    /** Returns training-session storage. */
    public TrainingSessionRepository sessions() {
        return sessions;
    }

    /** Returns booking history storage. */
    public BookingRepository bookings() {
        return bookings;
    }
}
