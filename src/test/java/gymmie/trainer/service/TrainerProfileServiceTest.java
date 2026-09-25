package gymmie.trainer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.AppContext;
import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.model.exception.ValidationException;
import gymmie.service.PasswordHasher;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;

class TrainerProfileServiceTest {
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

    @Test
    void savesOwnDetailsAtomicallyAndPreservesIdentityAfterRestart() throws Exception {
        loginTrainer();
        var initial = context.getTrainerProfileService().getOwnTrainerProfile();
        assertEquals(trainer.username(), initial.username());
        assertEquals(trainer.displayName(), initial.displayName());
        assertEquals("", initial.synopsis());
        assertEquals(List.of(), initial.specializations());
        var saved = context.getTrainerProfileService().updateOwnTrainerProfile(
                "Coach 李", "Strength & mobility\nAll levels",
                List.of(" Strength ", "strength", "Mobility", "Balance, coordination"));
        assertEquals(List.of("Strength", "Mobility", "Balance, coordination"), saved.specializations());
        assertEquals("Coach 李", context.getUserSession().requireUser().displayName());
        assertEquals(trainer.withDisplayName("Coach 李"), loadTrainer());
        context.getAuthService().login("other-trainer", "password123");
        assertEquals("Other Trainer", context.getTrainerProfileService().getOwnTrainerProfile().displayName());
        assertEquals(List.of(), context.getTrainerProfileService().getOwnTrainerProfile().specializations());
        AppContext restarted = new AppContext(directory.resolve("profiles.db"));
        restarted.getAuthService().login(trainer.username(), "password123");
        assertEquals(saved, restarted.getTrainerProfileService().getOwnTrainerProfile());
        restarted.getTrainerProfileService().updateOwnTrainerProfile("Coach 李", "", List.of());
        assertEquals(List.of(), restarted.getTrainerProfileService().getOwnTrainerProfile().specializations());
        assertEquals("", restarted.getTrainerProfileService().getOwnTrainerProfile().synopsis());
    }

    @Test
    void readsAndWritesRequireAnAuthenticatedTrainer() throws Exception {
        assertThrows(AuthenticationException.class, () -> context.getTrainerProfileService().getOwnTrainerProfile());
        assertThrows(AuthenticationException.class, () ->
                context.getTrainerProfileService().updateOwnTrainerProfile("Changed", "", List.of()));
        for (String username : List.of("manager", "member")) {
            context.getAuthService().login(username, username.equals("manager") ? "manager123" : "password123");
            assertThrows(AuthorizationException.class, () -> context.getTrainerProfileService().getOwnTrainerProfile());
            assertThrows(AuthorizationException.class, () ->
                    context.getTrainerProfileService().updateOwnTrainerProfile("Changed", "", List.of()));
        }
        assertEquals(trainer, loadTrainer());
    }

    @Test
    void revalidatesRoleAndActivationOnEveryOperation() throws Exception {
        loginTrainer();
        execute("UPDATE account SET role = 'MEMBER' WHERE id = 2");
        assertThrows(AuthorizationException.class, () -> context.getTrainerProfileService().getOwnTrainerProfile());
        assertThrows(AuthorizationException.class, () ->
                context.getTrainerProfileService().updateOwnTrainerProfile("Changed", "", List.of()));
        execute("UPDATE account SET role = 'TRAINER', active = 0 WHERE id = 2");
        assertThrows(AccountDeactivatedException.class, () ->
                context.getTrainerProfileService().updateOwnTrainerProfile("Changed", "", List.of()));
        assertFalse(context.getUserSession().isAuthenticated());
        assertEquals(trainer.displayName(), loadTrainer().displayName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "too-long"})
    void rejectsInvalidDisplayNamesWithoutChangingDetails(String input) throws Exception {
        loginTrainer();
        String name = input.isEmpty() ? input : "x".repeat(101);
        assertThrows(ValidationException.class, () ->
                context.getTrainerProfileService().updateOwnTrainerProfile(name, "New biography", List.of("Strength")));
        assertEquals(trainer, loadTrainer());
        assertEquals("", context.getTrainerProfileService().getOwnTrainerProfile().synopsis());
        assertThrows(ValidationException.class, () ->
                context.getTrainerProfileService().updateOwnTrainerProfile("Valid", "", List.of(" ")));
        assertEquals(trainer, loadTrainer());
    }

    @Test
    void failedTagWriteRollsBackDisplayNameSynopsisAndExistingTags() throws Exception {
        loginTrainer();
        var before = context.getTrainerProfileService().updateOwnTrainerProfile(
                "Before", "Original", List.of("Mobility"));
        execute("CREATE TRIGGER reject_tag BEFORE INSERT ON trainer_specialization "
                + "BEGIN SELECT RAISE(ABORT, 'simulated failure'); END");
        assertThrows(SQLException.class, () ->
                context.getTrainerProfileService().updateOwnTrainerProfile("After", "Changed", List.of("Strength")));
        assertEquals(before, context.getTrainerProfileService().getOwnTrainerProfile());
        assertEquals("Before", context.getUserSession().requireUser().displayName());
    }

    @Test
    void migratesVersionOneDatabaseWithoutChangingAccounts() throws Exception {
        execute("DROP TABLE trainer_specialization");
        execute("DROP TABLE trainer_profile");
        execute("PRAGMA user_version = 1");
        AppContext upgraded = new AppContext(directory.resolve("profiles.db"));
        upgraded.getAuthService().login(trainer.username(), "password123");
        assertEquals(trainer, loadTrainer());
        assertTrue(upgraded.getTrainerProfileService().getOwnTrainerProfile().specializations().isEmpty());
        upgraded.getTrainerProfileService().updateOwnTrainerProfile("Migrated", "Biography", List.of("Strength"));
        AppContext restarted = new AppContext(directory.resolve("profiles.db"));
        restarted.getAuthService().login(trainer.username(), "password123");
        assertEquals("Migrated", restarted.getTrainerProfileService().getOwnTrainerProfile().displayName());
        assertEquals(List.of("Strength"),
                restarted.getTrainerProfileService().getOwnTrainerProfile().specializations());
    }

    private void loginTrainer() throws Exception {
        context.getAuthService().login(trainer.username(), "password123");
    }

    private Account loadTrainer() throws Exception {
        return context.getPersistence().unitOfWork().inTransaction(connection ->
                context.getPersistence().accounts().findById(connection, trainer.id()).orElseThrow());
    }

    private void execute(String sql) throws Exception {
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            return null;
        });
    }
}
