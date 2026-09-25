package gymmie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import gymmie.AppContext;
import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.model.exception.ValidationException;
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
        String username = switch (role) {
            case MANAGER -> "manager";
            case TRAINER -> trainer.username();
            case MEMBER -> "member";
        };
        context.getAuthService().login(username, role == Role.MANAGER ? "manager123" : "password123");
        String name = "😀".repeat(100);
        context.getProfileService().changeOwnDisplayName(name);
        assertEquals(name, context.getUserSession().requireUser().displayName());
        assertEquals(username, context.getUserSession().requireUser().username());
        assertThrows(ValidationException.class, () -> context.getProfileService().changeOwnDisplayName(name + "x"));
        context.getAuthService().logout();
        assertThrows(AuthenticationException.class, () -> context.getProfileService().changeOwnDisplayName("Name"));
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
