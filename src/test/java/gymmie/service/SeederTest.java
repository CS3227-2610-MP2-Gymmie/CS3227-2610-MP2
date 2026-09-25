package gymmie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;
import gymmie.persistence.Database;
import gymmie.persistence.Persistence;
import gymmie.service.exception.AuthenticationException;

class SeederTest {
    @TempDir
    Path directory;

    private Database database;
    private Persistence persistence;
    private final PasswordHasher hasher = new PasswordHasher();

    @BeforeEach
    void setUp() throws Exception {
        database = new Database(directory.resolve("gymmie.db"));
        persistence = new Persistence(database);
        persistence.initialize();
    }

    @Test
    void firstRunCreatesAnActiveManagerWithHashedCredentials() throws Exception {
        seed(persistence);
        Account manager = accounts().getFirst();
        assertEquals(1, accounts().size());
        assertEquals("manager", manager.username());
        assertEquals(Role.MANAGER, manager.role());
        assertTrue(manager.active());
        assertEquals(Role.MANAGER, auth(persistence).login("manager", "manager123").role());
        try (Connection connection = database.openConnection();
                Statement statement = connection.createStatement();
                ResultSet row = statement.executeQuery("SELECT password_hash, salt FROM account")) {
            assertTrue(row.next());
            assertEquals(manager.password().hash(), row.getString("password_hash"));
            assertEquals(manager.password().salt(), row.getString("salt"));
            assertNotEquals("manager123", row.getString("password_hash"));
            assertNotEquals("manager123", row.getString("salt"));
        }
    }

    @Test
    void repeatedStartsPreserveTheAccountAndChangedPassword() throws Exception {
        seed(persistence);
        Account original = accounts().getFirst();
        seed(persistence);
        assertEquals(List.of(original), accounts());

        AuthService auth = auth(persistence);
        auth.login("manager", "manager123");
        auth.changeOwnPassword("manager123", "replacement-password");
        Account changed = accounts().getFirst();
        Persistence restarted = new Persistence(database);
        restarted.initialize();
        seed(restarted);
        assertEquals(List.of(changed), accounts());
        assertEquals(Role.MANAGER, auth(restarted).login("manager", "replacement-password").role());
        assertThrows(AuthenticationException.class, () -> auth(restarted).login("manager", "manager123"));
        assertThrows(ValidationException.class, () -> new Account(changed.id(), changed.username(),
                changed.password(), "Renamed Manager", changed.role(), false));
        assertTrue(accounts().getFirst().active());
    }

    @Test
    void existingInactiveAccountDoesNotPreventSeedingOrGetOverwritten() throws Exception {
        Account existing = new Account(1, "member", hasher.hash("existing-password"), "Member", Role.MEMBER, false);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, existing);
            return null;
        });
        seed(persistence);
        assertEquals(2, accounts().size());
        assertEquals(existing, accounts().getFirst());
        assertFalse(accounts().getFirst().active());
        assertEquals(2, accounts().getLast().id());
        assertEquals(Role.MANAGER, auth(persistence).login("manager", "manager123").role());
    }

    @Test
    void existingMixedCaseManagerIsPreserved() throws Exception {
        Account existing = new Account(7, "MaNaGeR", hasher.hash("existing-password"), "Existing", Role.MANAGER, true);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, existing);
            return null;
        });
        seed(persistence);
        assertEquals(List.of(existing), accounts());
        assertEquals(7, auth(persistence).login("manager", "existing-password").accountId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void reservedUsernameWithAnotherRoleReportsConflictWithoutChanges(boolean active) throws Exception {
        Account existing = new Account(7, "MANAGER", hasher.hash("existing-password"), "Member", Role.MEMBER, active);
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, existing);
            return null;
        });
        ConflictException failure = assertThrows(ConflictException.class, () -> seed(persistence));
        assertTrue(failure.getMessage().contains("active Manager"));
        assertEquals(List.of(existing), accounts());
    }

    @Test
    void manuallyDeactivatedManagerReportsConflictWithoutReactivation() throws Exception {
        seed(persistence);
        Account original = accounts().getFirst();
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE account SET active = 0 WHERE username = 'manager'");
        }
        ConflictException failure = assertThrows(ConflictException.class, () -> seed(persistence));
        assertTrue(failure.getMessage().contains("cannot be deactivated"));
        try (Connection connection = database.openConnection();
                Statement statement = connection.createStatement();
                ResultSet row = statement.executeQuery("SELECT active, password_hash, salt FROM account")) {
            assertTrue(row.next());
            assertFalse(row.getBoolean("active"));
            assertEquals(original.password().hash(), row.getString("password_hash"));
            assertEquals(original.password().salt(), row.getString("salt"));
            assertFalse(row.next());
        }
    }

    @Test
    void sparseIdentifiersLeaveAnAvailableIdForTheSeed() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, new Account(Long.MAX_VALUE, "member",
                    hasher.hash("existing-password"), "Member", Role.MEMBER, true));
            return null;
        });
        seed(persistence);
        assertEquals(2, accounts().size());
        assertEquals(1, auth(persistence).login("manager", "manager123").accountId());
    }

    @Test
    void independentInstallationsUseFreshSalts() throws Exception {
        seed(persistence);
        Persistence other = new Persistence(new Database(directory.resolve("other.db")));
        other.initialize();
        seed(other);
        Account otherManager = other.unitOfWork().inTransaction(connection ->
                other.accounts().findByUsername(connection, "manager").orElseThrow());
        assertNotEquals(accounts().getFirst().password().salt(), otherManager.password().salt());
        assertNotEquals(accounts().getFirst().password().hash(), otherManager.password().hash());
        assertTrue(hasher.verify("manager123", otherManager.password()));
    }

    private void seed(Persistence storage) throws Exception {
        new Seeder(storage.accounts(), storage.unitOfWork(), hasher).seed();
    }

    private AuthService auth(Persistence storage) {
        return new AuthService(storage.accounts(), storage.unitOfWork(), new UserSession(), hasher);
    }

    private List<Account> accounts() throws Exception {
        return persistence.unitOfWork().inTransaction(connection -> persistence.accounts().findAll(connection));
    }
}
