package gymmie;

import java.nio.file.Path;
import java.time.Clock;

import gymmie.member.service.MemberBookingHistoryService;
import gymmie.member.service.MemberSessionBrowseService;
import gymmie.member.service.MembershipCancellationService;
import gymmie.member.service.MembershipPurchaseService;
import gymmie.member.service.MembershipRenewalService;
import gymmie.member.service.MembershipStatusService;
import gymmie.persistence.Database;
import gymmie.persistence.Persistence;
import gymmie.persistence.SchemaInitializer;
import gymmie.service.AuthService;
import gymmie.service.PasswordHasher;
import gymmie.service.Permissions;
import gymmie.service.ProfileService;
import gymmie.service.Seeder;
import gymmie.service.UserSession;
import gymmie.trainer.service.SessionRosterService;
import gymmie.trainer.service.TrainerProfileService;
import gymmie.trainer.service.TrainingSessionService;

/** Composes initialized storage and shared application services before any view is created. */
public final class AppContext {
    private final Persistence persistence;
    private final UserSession userSession;
    private final AuthService authService;
    private final Permissions permissions;
    private final MembershipStatusService membershipStatusService;
    private final MembershipPurchaseService membershipPurchaseService;
    private final MembershipRenewalService membershipRenewalService;
    private final MembershipCancellationService membershipCancellationService;
    private final MemberBookingHistoryService memberBookingHistoryService;
    private final MemberSessionBrowseService memberSessionBrowseService;
    private final ProfileService profileService;
    private final TrainerProfileService trainerProfileService;
    private final TrainingSessionService trainingSessionService;
    private final SessionRosterService sessionRosterService;

    /**
     * Initializes the application's default local database and services.
     *
     * @throws Exception if schema initialization or Manager seeding fails.
     */
    public AppContext() throws Exception {
        this(Path.of("data", "gymmie.db"));
    }

    /**
     * Initializes an isolated database before constructing repositories and services.
     *
     * @param databasePath file-backed database location.
     * @throws Exception if schema initialization or Manager seeding fails.
     */
    public AppContext(Path databasePath) throws Exception {
        Database database = new Database(databasePath);
        new SchemaInitializer(database).initialize();
        persistence = new Persistence(database);
        userSession = new UserSession();
        PasswordHasher hasher = new PasswordHasher();
        new Seeder(persistence.accounts(), persistence.unitOfWork(), hasher).seed();
        permissions = new Permissions(persistence.accounts(), userSession);
        membershipStatusService = new MembershipStatusService(persistence.memberships(), persistence.plans(),
                persistence.unitOfWork(), permissions, Clock.systemDefaultZone());
        membershipPurchaseService = new MembershipPurchaseService(persistence.plans(), persistence.memberships(),
                persistence.unitOfWork(), permissions, Clock.systemDefaultZone());
        membershipRenewalService = new MembershipRenewalService(persistence.memberships(), persistence.unitOfWork(),
                permissions, Clock.systemDefaultZone());
        membershipCancellationService = new MembershipCancellationService(persistence.memberships(),
                persistence.bookings(), persistence.sessions(), persistence.unitOfWork(), permissions,
                Clock.systemDefaultZone());
        memberBookingHistoryService = new MemberBookingHistoryService(persistence.bookings(), persistence.sessions(),
                persistence.unitOfWork(), permissions);
        memberSessionBrowseService = new MemberSessionBrowseService(persistence.accounts(), persistence.sessions(),
                persistence.bookings(), persistence.unitOfWork(), permissions, Clock.systemDefaultZone());
        authService = new AuthService(persistence.accounts(), persistence.unitOfWork(), userSession, hasher);
        profileService = new ProfileService(persistence.accounts(), persistence.unitOfWork(), userSession, authService);
        trainingSessionService = new TrainingSessionService(persistence.sessions(), persistence.bookings(),
                persistence.unitOfWork(), permissions, Clock.systemDefaultZone());
        sessionRosterService = new SessionRosterService(persistence.sessions(), persistence.bookings(),
                persistence.accounts(), persistence.unitOfWork(), permissions);
        trainerProfileService = new TrainerProfileService(persistence.accounts(), persistence.trainerProfiles(),
                persistence.unitOfWork(), userSession, authService);
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public TrainerProfileService getTrainerProfileService() {
        return trainerProfileService;
    }

    /** Returns the Trainer-authorized session creation service. */
    public TrainingSessionService getTrainingSessionService() {
        return trainingSessionService;
    }

    /** Returns the Trainer-authorized display-name roster service. */
    public SessionRosterService getSessionRosterService() {
        return sessionRosterService;
    }

    public ProfileService getProfileService() {
        return profileService;
    }

    /** Returns the protected read service for current membership coverage. */
    public MembershipStatusService getMembershipStatusService() {
        return membershipStatusService;
    }

    /** Returns the protected service for purchasing available membership plans. */
    public MembershipPurchaseService getMembershipPurchaseService() {
        return membershipPurchaseService;
    }

    /** Returns the protected service for renewing a Member's existing plan. */
    public MembershipRenewalService getMembershipRenewalService() {
        return membershipRenewalService;
    }

    /** Returns the Member-authorized atomic membership cancellation service. */
    public MembershipCancellationService getMembershipCancellationService() {
        return membershipCancellationService;
    }

    /** Returns the Member-authorized booking-history reader. */
    public MemberBookingHistoryService getMemberBookingHistoryService() {
        return memberBookingHistoryService;
    }

    /** Returns the Member-authorized reader for upcoming sessions by Trainer. */
    public MemberSessionBrowseService getMemberSessionBrowseService() {
        return memberSessionBrowseService;
    }

    public AuthService getAuthService() {
        return authService;
    }

    /** Returns the authorization boundary for protected application services as they are added. */
    @SuppressWarnings("unused")
    public Permissions getPermissions() {
        return permissions;
    }
}
