package gymmie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.model.Account;
import gymmie.model.PasswordHash;
import gymmie.model.Role;
import gymmie.model.exception.ValidationException;
import gymmie.persistence.Database;
import gymmie.persistence.Persistence;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;

class AuthServiceTest {
    private static final String PASSWORD = "original-password";
    private static PasswordHash passwordHash;

    @TempDir
    Path directory;

    private Persistence persistence;
    private Database database;
    private UserSession session;
    private AuthService auth;
    private Permissions permissions;

    @BeforeAll
    static void hashFixturePassword() {
        passwordHash = new PasswordHasher().hash(PASSWORD);
    }

    @BeforeEach
    void setUp() throws Exception {
        database = new Database(directory.resolve("data/gymmie.db"));
        persistence = new Persistence(database);
        persistence.initialize();
        session = new UserSession();
        auth = new AuthService(persistence.accounts(), persistence.unitOfWork(), session, new PasswordHasher());
        permissions = new Permissions(persistence.accounts(), session);
        persistence.unitOfWork().inTransaction(connection -> {
            for (Role role : Role.values()) {
                persistence.accounts().insert(connection, account(role, true));
            }
            return null;
        });
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void loginEstablishesCredentialFreeIdentityForEveryRole(Role role) throws Exception {
        Account expected = account(role, true);
        UserSession.Principal principal = auth.login(expected.username().toUpperCase(Locale.ROOT), PASSWORD);
        assertTrue(session.isAuthenticated());
        assertEquals(expected.id(), principal.accountId());
        assertEquals(expected.username(), principal.username());
        assertEquals(expected.displayName(), principal.displayName());
        assertEquals(role, principal.role());
        assertEquals(principal, session.requireUser());
        assertFalse(principal.toString().contains(PASSWORD));
        assertFalse(principal.toString().contains(passwordHash.hash()));
        assertFalse(principal.toString().contains(passwordHash.salt()));
        assertEquals(expected, load(role));
    }

    @Test
    void logoutClearsStateAndIsIdempotent() throws Exception {
        assertTrue(session.currentUser().isEmpty());
        assertThrows(AuthenticationException.class, session::requireUser);
        auth.login("Member", PASSWORD);
        auth.logout();
        auth.logout();
        assertFalse(session.isAuthenticated());
        assertTrue(session.currentUser().isEmpty());
        assertThrows(AuthenticationException.class, session::requireUser);
        assertEquals(account(Role.MEMBER, true), load(Role.MEMBER));
    }

    @Test
    void invalidCredentialsClearOldIdentityAndUseOneFailureMessage() throws Exception {
        auth.login("Manager", PASSWORD);
        AuthenticationException unknown = assertThrows(AuthenticationException.class, () ->
                auth.login("unknown", PASSWORD));
        assertFalse(session.isAuthenticated());
        AuthenticationException incorrect = assertThrows(AuthenticationException.class, () ->
                auth.login("Member", "wrong-password"));
        assertEquals(unknown.getClass(), incorrect.getClass());
        assertEquals(unknown.getMessage(), incorrect.getMessage());
        assertThrows(AuthenticationException.class, () -> auth.login(null, PASSWORD));
        assertThrows(AuthenticationException.class, () -> auth.login("Member", null));
        assertThrows(AuthenticationException.class, () -> auth.login("Member", "short"));
        assertFalse(session.isAuthenticated());
    }

    @Test
    void deactivatedLoginIsDistinctOnlyAfterPasswordVerification() throws Exception {
        deactivate(Role.MEMBER);
        AuthenticationException incorrect = assertThrows(AuthenticationException.class, () ->
                auth.login("Member", "wrong-password"));
        assertEquals(AuthenticationException.class, incorrect.getClass());
        assertThrows(AccountDeactivatedException.class, () -> auth.login("Member", PASSWORD));
        assertFalse(session.isAuthenticated());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyRoleCanChangeOnlyItsOwnPasswordAndNewPasswordSurvivesRestart(Role role) throws Exception {
        Account original = account(role, true);
        auth.login(original.username(), PASSWORD);
        auth.changeOwnPassword(PASSWORD, "replacement-password");
        Account updated = load(role);
        assertEquals(original.id(), updated.id());
        assertEquals(original.username(), updated.username());
        assertEquals(original.role(), updated.role());
        assertNotEquals(original.password(), updated.password());
        assertTrue(updated.password().matches("replacement-password"));
        assertFalse(updated.password().matches(PASSWORD));
        for (Role other : Role.values()) {
            if (other != role) {
                assertEquals(account(other, true), load(other));
            }
        }
        Persistence restarted = new Persistence(new Database(directory.resolve("data/gymmie.db")));
        restarted.initialize();
        UserSession restartedSession = new UserSession();
        AuthService restartedAuth = new AuthService(restarted.accounts(), restarted.unitOfWork(),
                restartedSession, new PasswordHasher());
        assertFalse(restartedSession.isAuthenticated());
        assertThrows(AuthenticationException.class, () -> restartedAuth.login(original.username(), PASSWORD));
        assertEquals(original.id(), restartedAuth.login(original.username(), "replacement-password").accountId());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7, 129})
    void rejectsInvalidNewPasswordsWithoutChangingStoredState(int length) throws Exception {
        auth.login("Member", PASSWORD);
        UserSession.Principal before = session.requireUser();
        assertThrows(ValidationException.class, () -> auth.changeOwnPassword(PASSWORD, "x".repeat(length)));
        assertEquals(account(Role.MEMBER, true), load(Role.MEMBER));
        assertEquals(before, session.requireUser());
    }

    @Test
    void passwordChangeRequiresLoginAndCorrectCurrentPassword() throws Exception {
        assertThrows(AuthenticationException.class, () -> auth.changeOwnPassword(PASSWORD, "replacement-password"));
        auth.login("Member", PASSWORD);
        assertThrows(AuthenticationException.class, () ->
                auth.changeOwnPassword("wrong-password", "replacement-password"));
        assertEquals(account(Role.MEMBER, true), load(Role.MEMBER));
        assertThrows(ValidationException.class, () -> auth.changeOwnPassword(PASSWORD, null));
        auth.logout();
        assertThrows(AuthenticationException.class, () -> auth.changeOwnPassword(PASSWORD, "replacement-password"));
    }

    @Test
    void deactivationAfterLoginRevokesProtectedAccessAndPasswordChange() throws Exception {
        auth.login("Member", PASSWORD);
        deactivate(Role.MEMBER);
        assertThrows(AccountDeactivatedException.class, () -> auth.changeOwnPassword(PASSWORD, "replacement-password"));
        assertFalse(session.isAuthenticated());
        assertEquals(passwordHash, load(Role.MEMBER).password());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void permissionsEnforceExactRolesAndOwnershipBeforeWrites(Role role) throws Exception {
        Account actor = account(role, true);
        auth.login(actor.username(), PASSWORD);
        persistence.unitOfWork().inTransaction(connection -> {
            assertEquals(actor, permissions.requireAuthenticated(connection));
            assertEquals(actor, permissions.requireRole(connection, role));
            assertEquals(actor, permissions.requireOwner(connection, role, actor.id()));
            assertThrows(AuthorizationException.class, () -> permissions.requireOwner(connection, role, 999));
            for (Role required : Role.values()) {
                if (required != role) {
                    assertThrows(AuthorizationException.class, () -> {
                        permissions.requireRole(connection, required);
                        persistence.accounts().update(connection, new Account(actor.id(), actor.username(),
                                actor.password(), "Unauthorized change", actor.role(), true));
                    });
                }
            }
            assertEquals(actor, persistence.accounts().findById(connection, actor.id()).orElseThrow());
            return null;
        });
    }

    @Test
    void permissionsRejectMissingAndDeactivatedSessions() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            assertThrows(AuthenticationException.class, () -> permissions.requireRole(connection, Role.MANAGER));
            assertThrows(AuthenticationException.class, () -> permissions.requireOwner(connection, Role.MEMBER, 3));
            return null;
        });
        auth.login("Manager", PASSWORD);
        deactivate(Role.MANAGER);
        persistence.unitOfWork().inTransaction(connection -> {
            assertThrows(AccountDeactivatedException.class, () -> permissions.requireRole(connection, Role.MANAGER));
            return null;
        });
        assertFalse(session.isAuthenticated());
    }

    @Test
    void permissionsUseCurrentRoleRatherThanCachedDashboardRole() throws Exception {
        auth.login("Manager", PASSWORD);
        // Simulates external account maintenance; ordinary profile updates cannot change roles.
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE account SET role = 'MEMBER' WHERE id = 1");
        }
        persistence.unitOfWork().inTransaction(connection -> {
            assertThrows(AuthorizationException.class, () -> permissions.requireRole(connection, Role.MANAGER));
            assertEquals(Role.MEMBER, permissions.requireRole(connection, Role.MEMBER).role());
            return null;
        });
    }

    @Test
    void removedAccountsCannotKeepUsingSessions() throws Exception {
        auth.login("Member", PASSWORD);
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM account WHERE id = 3");
        }
        persistence.unitOfWork().inTransaction(connection -> {
            assertThrows(AuthenticationException.class, () -> permissions.requireAuthenticated(connection));
            return null;
        });
        assertFalse(session.isAuthenticated());
    }

    @Test
    void failedPasswordWritePreservesOldCredentialsAndSession() throws Exception {
        auth.login("Member", PASSWORD);
        UserSession.Principal before = session.requireUser();
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER reject_password BEFORE UPDATE ON account "
                    + "BEGIN SELECT RAISE(ABORT, 'simulated write failure'); END");
        }
        assertThrows(SQLException.class, () -> auth.changeOwnPassword(PASSWORD, "replacement-password"));
        assertEquals(account(Role.MEMBER, true), load(Role.MEMBER));
        assertEquals(before, session.requireUser());
    }

    @Test
    void failedLoginReadNeverLeavesAnAuthenticatedIdentity() throws Exception {
        auth.login("Member", PASSWORD);
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE account RENAME TO unavailable_account");
        }
        assertThrows(SQLException.class, () -> auth.login("Member", PASSWORD));
        assertFalse(session.isAuthenticated());
    }

    @Test
    void usernamesRemainGloballyUniqueAcrossRolesIncludingDeactivatedAccounts() throws Exception {
        deactivate(Role.MEMBER);
        persistence.unitOfWork().inTransaction(connection -> {
            assertThrows(SQLException.class, () -> persistence.accounts().insert(connection,
                    new Account(4, "mEMBER", passwordHash, "Duplicate", Role.TRAINER, true)));
            assertEquals("Member",
                    persistence.accounts().findByUsername(connection, "MEMBER").orElseThrow().username());
            return null;
        });
    }

    private void deactivate(Role role) throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().update(connection, account(role, false));
            return null;
        });
    }

    private Account load(Role role) throws Exception {
        return persistence.unitOfWork().inTransaction(connection ->
                persistence.accounts().findById(connection, role.ordinal() + 1).orElseThrow());
    }

    private static Account account(Role role, boolean active) {
        String username = role.name().substring(0, 1) + role.name().substring(1).toLowerCase(Locale.ROOT);
        return new Account(role.ordinal() + 1, username, passwordHash, username + " Display", role, active);
    }
}
