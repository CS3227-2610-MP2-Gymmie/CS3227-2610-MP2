package gymmie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import gymmie.AppContext;
import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.model.exception.ValidationException;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;

class ProfileServiceTest {
    @TempDir
    Path directory;
    private AppContext context;
    private Account trainer;

    @BeforeEach
    void setUp() throws Exception {
        context = new AppContext(directory.resolve("profiles.db"));
        var hash = new PasswordHasher().hash("password123");
        trainer = new Account(2, "TrainerLogin", hash, "Original name", Role.TRAINER, true);
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection, trainer);
            context.getPersistence().accounts().insert(connection,
                    new Account(3, "other-trainer", hash, "Other Trainer", Role.TRAINER, true));
            context.getPersistence().accounts().insert(connection,
                    new Account(4, "member", hash, "Member", Role.MEMBER, true));
            return null;
        });
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void displayNameCapabilityIsSharedAcrossRoles(Role role) throws Exception {
        String username = username(role);
        context.getAuthService().login(username, role == Role.MANAGER ? "manager123" : "password123");
        long accountId = context.getUserSession().requireUser().accountId();
        Account original = loadAccount(accountId);
        Account other = loadAccount(3);
        String name = "😀".repeat(100);
        context.getProfileService().changeOwnDisplayName(name);
        assertEquals(name, context.getUserSession().requireUser().displayName());
        assertEquals(username, context.getUserSession().requireUser().username());
        assertEquals(original.withDisplayName(name), loadAccount(accountId));
        assertEquals(other, loadAccount(3));
        for (String invalid : new String[]{"", name + "x"}) {
            assertThrows(ValidationException.class, () -> context.getProfileService().changeOwnDisplayName(invalid));
            assertEquals(original.withDisplayName(name), loadAccount(accountId));
            assertEquals(name, context.getUserSession().requireUser().displayName());
        }
        AppContext restarted = new AppContext(directory.resolve("profiles.db"));
        restarted.getAuthService().login(username, role == Role.MANAGER ? "manager123" : "password123");
        assertEquals(name, restarted.getUserSession().requireUser().displayName());
        context.getAuthService().logout();
        assertThrows(AuthenticationException.class, () -> context.getProfileService().changeOwnDisplayName("Name"));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void failedWritePreservesStoredAccountAndSession(Role role) throws Exception {
        context.getAuthService().login(username(role), role == Role.MANAGER ? "manager123" : "password123");
        var principal = context.getUserSession().requireUser();
        Account original = loadAccount(principal.accountId());
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            try (var statement = connection.createStatement()) {
                // Test-only SQL uses the temporary database created by this fixture.
                //noinspection SqlNoDataSourceInspection
                statement.execute("CREATE TRIGGER reject_name BEFORE UPDATE ON account "
                        + "BEGIN SELECT RAISE(ABORT, 'simulated failure'); END");
            }
            return null;
        });
        assertThrows(SQLException.class, () -> context.getProfileService().changeOwnDisplayName("Replacement"));
        assertEquals(original, loadAccount(principal.accountId()));
        assertEquals(principal, context.getUserSession().requireUser());
    }

    @Test
    void deactivationAfterLoginRejectsNameChangeAndClearsSession() throws Exception {
        context.getAuthService().login("member", "password123");
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            try (var statement = connection.createStatement()) {
                // Test-only SQL uses the temporary database created by this fixture.
                //noinspection SqlNoDataSourceInspection
                statement.execute("UPDATE account SET active = 0 WHERE id = 4");
            }
            return null;
        });
        Account original = loadAccount(4);
        assertThrows(AccountDeactivatedException.class, () -> context.getProfileService().changeOwnDisplayName("New"));
        assertFalse(context.getUserSession().isAuthenticated());
        assertEquals(original, loadAccount(4));
    }

    private String username(Role role) {
        return switch (role) {
            case MANAGER -> "manager";
            case TRAINER -> trainer.username();
            case MEMBER -> "member";
        };
    }

    private Account loadAccount(long id) throws Exception {
        return context.getPersistence().unitOfWork().inTransaction(connection ->
                context.getPersistence().accounts().findById(connection, id).orElseThrow());
    }

    @Test
    void sessionRefreshCannotLogInOrSwitchAccounts() throws Exception {
        UserSession session = context.getUserSession();
        assertThrows(AuthenticationException.class, () -> session.refresh(trainer));
        context.getAuthService().login("manager", "manager123");
        UserSession.Principal before = session.requireUser();
        assertThrows(AuthenticationException.class, () -> session.refresh(trainer));
        assertEquals(before, session.requireUser());
    }

}
