package gymmie.manager.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Constraints;
import gymmie.model.PasswordHash;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.AccountRepository;
import gymmie.persistence.repository.BookingRepository;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.service.PasswordHasher;
import gymmie.service.Permissions;

/**
 * Provides Manager-authorized provisioning and lifecycle management for Trainer and Member accounts.
 *
 * <p>Enforces Manager permissions on all account operations. Accounts are soft-deactivated rather than
 * hard-deleted to preserve audit history and historical memberships, sessions, and bookings.
 */
public final class AccountProvisioningService {
    private final AccountRepository accounts;
    private final BookingRepository bookings;
    private final TrainingSessionRepository sessions;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final PasswordHasher hasher;
    private final Clock clock;

    /**
     * Creates an account provisioning service with repository, transaction, authorization, and time boundaries.
     *
     * @param accounts account storage.
     * @param bookings booking storage.
     * @param sessions training session storage.
     * @param unitOfWork top-level transaction boundary.
     * @param permissions service-layer authorization boundary.
     * @param hasher password hashing boundary.
     * @param clock system time provider for evaluating session start times.
     */
    public AccountProvisioningService(AccountRepository accounts, BookingRepository bookings,
            TrainingSessionRepository sessions, UnitOfWork unitOfWork, Permissions permissions,
            PasswordHasher hasher, Clock clock) {
        this.accounts = Objects.requireNonNull(accounts);
        this.bookings = Objects.requireNonNull(bookings);
        this.sessions = Objects.requireNonNull(sessions);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
        this.hasher = Objects.requireNonNull(hasher);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Lists all accounts for administration, including deactivated accounts.
     *
     * @return all accounts in identifier order.
     * @throws Exception if authorization or storage access fails.
     */
    public List<Account> getAllAccounts() throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            return accounts.findAll(connection);
        });
    }

    /**
     * Lists only accounts whose active flag is true.
     *
     * @return active accounts in identifier order.
     * @throws Exception if authorization or storage access fails.
     */
    public List<Account> getActiveAccounts() throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            return accounts.findAllActive(connection);
        });
    }

    /**
     * Finds an account by its identifier, including deactivated accounts.
     *
     * @param accountId account identifier.
     * @return the matching account, or empty if absent.
     * @throws Exception if authorization or storage access fails.
     */
    public Optional<Account> findById(long accountId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            return accounts.findById(connection, accountId);
        });
    }

    /**
     * Finds an account by its case-insensitive username, including deactivated accounts.
     *
     * @param username login name to match.
     * @return the matching account, or empty if absent.
     * @throws Exception if authorization or storage access fails.
     */
    public Optional<Account> findByUsername(String username) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            return accounts.findByUsername(connection, username);
        });
    }

    /**
     * Provisions a new Trainer or Member account.
     *
     * @param username unique login name (3–30 ASCII alphanumeric characters, hyphens or underscores).
     * @param password plaintext password (8–128 characters).
     * @param displayName display name (1–100 characters).
     * @param role account role, restricted to Trainer or Member.
     * @return the created active account.
     * @throws Exception if authorization, validation, conflict or persistence fails.
     */
    public Account create(String username, String password, String displayName, Role role) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            Constraints.required(role, "Role");
            if (role != Role.TRAINER && role != Role.MEMBER) {
                throw new ValidationException("Only Trainer and Member accounts can be provisioned");
            }
            Constraints.required(username, "Username");
            if (accounts.findByUsername(connection, username).isPresent()) {
                throw new ConflictException("An account with username '" + username + "' already exists");
            }
            PasswordHash passwordHash = hasher.hash(password);
            long id = accounts.nextId(connection);
            Account account = new Account(id, username, passwordHash, displayName, role, true);
            accounts.insert(connection, account);
            return account;
        });
    }

    /**
     * Updates the display name of an existing account.
     *
     * @param accountId identifier of the account to edit.
     * @param displayName replacement display name (1–100 characters).
     * @return the updated account.
     * @throws Exception if authorization, validation, conflict or persistence fails.
     */
    public Account edit(long accountId, String displayName) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            Account existing = accounts.findById(connection, accountId)
                    .orElseThrow(() -> new ConflictException("Account not found"));
            Account updated = existing.withDisplayName(displayName);
            accounts.update(connection, updated);
            return updated;
        });
    }

    /**
     * Deactivates an account and cancels all its future bookings in the same transaction.
     *
     * <p>The seeded Manager account cannot be deactivated. Cancelled bookings record
     * {@link CancellationReason#ACCOUNT_DEACTIVATED} as their cancellation reason.
     *
     * @param accountId identifier of the account to deactivate.
     * @return the deactivated account.
     * @throws Exception if authorization, validation, conflict or persistence fails.
     */
    public Account deactivate(long accountId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            Account existing = accounts.findById(connection, accountId)
                    .orElseThrow(() -> new ConflictException("Account not found"));
            if (!existing.active()) {
                throw new ConflictException("This account is already deactivated.");
            }
            Account deactivated = new Account(existing.id(), existing.username(), existing.password(),
                    existing.displayName(), existing.role(), false);
            accounts.update(connection, deactivated);

            LocalDateTime now = LocalDateTime.now(clock);
            for (Booking booking : bookings.findByMemberId(connection, accountId)) {
                if (booking.status() == BookingStatus.BOOKED) {
                    TrainingSession session = sessions.findById(connection, booking.sessionId()).orElseThrow();
                    if (!session.hasStartedAt(now)) {
                        bookings.update(connection, new Booking(booking.id(), booking.sessionId(),
                                booking.memberId(), booking.bookedAt(), BookingStatus.CANCELLED,
                                CancellationReason.ACCOUNT_DEACTIVATED));
                    }
                }
            }
            return deactivated;
        });
    }

    /**
     * Reactivates a deactivated account without re-creating any cancelled bookings.
     *
     * @param accountId identifier of the account to reactivate.
     * @return the reactivated account.
     * @throws Exception if authorization, conflict or persistence fails.
     */
    public Account reactivate(long accountId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            Account existing = accounts.findById(connection, accountId)
                    .orElseThrow(() -> new ConflictException("Account not found"));
            if (existing.active()) {
                throw new ConflictException("This account is already active.");
            }
            Account reactivated = new Account(existing.id(), existing.username(), existing.password(),
                    existing.displayName(), existing.role(), true);
            accounts.update(connection, reactivated);
            return reactivated;
        });
    }
}
