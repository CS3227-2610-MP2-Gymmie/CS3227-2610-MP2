package gymmie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.service.PasswordHasher;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;

class AppContextTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void freshStartupSeedsManagerAndReopeningPreservesChangedPassword() throws Exception {
        Path path = temporaryDirectory.resolve("nested/gymmie.db");
        AppContext first = new AppContext(path);
        assertFalse(first.getUserSession().isAuthenticated());
        assertEquals(Role.MANAGER, first.getAuthService().login("manager", "manager123").role());
        first.getAuthService().changeOwnPassword("manager123", "replacement123");
        AppContext reopened = new AppContext(path);
        assertFalse(reopened.getUserSession().isAuthenticated());
        assertThrows(AuthenticationException.class, () ->
                reopened.getAuthService().login("manager", "manager123"));
        assertEquals(Role.MANAGER, reopened.getAuthService().login("manager", "replacement123").role());
    }

    @Test
    void authenticatedRolesResolveToTheirDashboardAndLogoutClearsIdentity() throws Exception {
        AppContext context = new AppContext(temporaryDirectory.resolve("roles.db"));
        var password = new PasswordHasher().hash("password123");
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection,
                    new Account(2, "trainer", password, "Trainer", Role.TRAINER, true));
            context.getPersistence().accounts().insert(connection,
                    new Account(3, "member", password, "Member", Role.MEMBER, true));
            context.getPersistence().accounts().insert(connection,
                    new Account(4, "inactive", password, "Inactive", Role.MEMBER, false));
            return null;
        });
        assertEquals("Manager dashboard",
                Router.dashboardTitle(context.getAuthService().login("manager", "manager123").role()));
        assertEquals("Trainer dashboard",
                Router.dashboardTitle(context.getAuthService().login("trainer", "password123").role()));
        assertEquals("Gym User dashboard",
                Router.dashboardTitle(context.getAuthService().login("member", "password123").role()));
        context.getAuthService().logout();
        assertFalse(context.getUserSession().isAuthenticated());
        Exception deactivated = assertThrows(AccountDeactivatedException.class, () ->
                context.getAuthService().login("inactive", "password123"));
        assertEquals("This account is deactivated. Please contact a Manager.",
                LoginController.failureMessage(deactivated));
        Exception incorrect = assertThrows(AuthenticationException.class, () ->
                context.getAuthService().login("inactive", "wrongpassword"));
        assertEquals("Invalid username or password.", LoginController.failureMessage(incorrect));
        assertFalse(context.getUserSession().isAuthenticated());
    }
}
