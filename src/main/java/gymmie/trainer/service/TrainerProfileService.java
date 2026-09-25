package gymmie.trainer.service;

import java.util.List;
import java.util.Objects;

import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.AccountRepository;
import gymmie.service.AuthService;
import gymmie.service.Permissions;
import gymmie.service.UserSession;
import gymmie.trainer.model.TrainerProfile;
import gymmie.trainer.persistence.TrainerProfileRepository;

/**
 * Trainer own-profile operations with authorization and atomic persistence.
 *
 * <p>The shared authentication monitor serializes these operations with login, logout and
 * password changes. Session snapshots are refreshed only after a successful commit.
 */
public final class TrainerProfileService {
    private final AccountRepository accounts;
    private final TrainerProfileRepository profiles;
    private final UnitOfWork unitOfWork;
    private final UserSession session;
    private final AuthService auth;
    private final Permissions permissions;

    /** Creates profile services using the same accounts, session and authentication service as the application. */
    public TrainerProfileService(AccountRepository accounts, TrainerProfileRepository profiles, UnitOfWork unitOfWork,
            UserSession session, AuthService auth) {
        this.accounts = Objects.requireNonNull(accounts);
        this.profiles = Objects.requireNonNull(profiles);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.session = Objects.requireNonNull(session);
        this.auth = Objects.requireNonNull(auth);
        permissions = new Permissions(accounts, session);
    }

    /**
     * Reads the authenticated Trainer's profile without exposing credentials.
     *
     * @return current Trainer profile.
     * @throws Exception if the caller is not an active Trainer or persistence fails.
     */
    public TrainerView getOwnTrainerProfile() throws Exception {
        synchronized (auth) {
            return unitOfWork.inTransaction(connection -> {
                Account account = permissions.requireRole(connection, Role.TRAINER);
                return view(account, profiles.findByAccountId(connection, account.id()));
            });
        }
    }

    /**
     * Updates only the authenticated Trainer's shared display name and Trainer details.
     *
     * @param displayName shared display name, without separate Trainer identity rules.
     * @param synopsis biography, which may be empty.
     * @param specializations specialization tags, which may be empty.
     * @return committed profile without credentials.
     * @throws Exception if authorization, validation or persistence fails.
     */
    public TrainerView updateOwnTrainerProfile(String displayName, String synopsis, List<String> specializations)
            throws Exception {
        synchronized (auth) {
            SavedProfile saved = unitOfWork.inTransaction(connection -> {
                Account account = permissions.requireRole(connection, Role.TRAINER).withDisplayName(displayName);
                TrainerProfile profile = new TrainerProfile(account.id(), synopsis, specializations);
                accounts.update(connection, account);
                profiles.save(connection, profile);
                return new SavedProfile(account, profile);
            });
            session.refresh(saved.account());
            return view(saved.account(), saved.profile());
        }
    }

    private record SavedProfile(Account account, TrainerProfile profile) {
    }

    private static TrainerView view(Account account, TrainerProfile profile) {
        return new TrainerView(account.username(), account.displayName(),
                profile.synopsis(), profile.specializations());
    }

    /**
     * Credential-free data shown in the Trainer editor.
     *
     * @param username immutable login name.
     * @param displayName shared account display name.
     * @param synopsis Trainer biography.
     * @param specializations immutable ordered specialization tags.
     */
    public record TrainerView(String username, String displayName, String synopsis, List<String> specializations) {
        /** Takes a defensive copy of the tags. */
        public TrainerView {
            specializations = List.copyOf(specializations);
        }
    }
}
