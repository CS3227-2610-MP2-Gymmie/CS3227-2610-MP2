package gymmie;

import java.nio.file.Path;

import gymmie.persistence.Database;
import gymmie.persistence.Persistence;
import gymmie.persistence.SchemaInitializer;
import gymmie.service.AuthService;
import gymmie.service.PasswordHasher;
import gymmie.service.Permissions;
import gymmie.service.Seeder;
import gymmie.service.UserSession;

/** Composes initialized storage and shared application services before any view is created. */
public final class AppContext {
    private final Persistence persistence;
    private final UserSession userSession;
    private final AuthService authService;
    private final Permissions permissions;

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
        authService = new AuthService(persistence.accounts(), persistence.unitOfWork(), userSession, hasher);
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public UserSession getUserSession() {
        return userSession;
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
