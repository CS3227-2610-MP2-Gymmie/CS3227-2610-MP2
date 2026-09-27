package gymmie.manager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;
import gymmie.persistence.Persistence;
import gymmie.service.AuthService;
import gymmie.service.PasswordHasher;
import gymmie.service.Permissions;
import gymmie.service.UserSession;
import gymmie.service.exception.AuthenticationException;
import gymmie.service.exception.AuthorizationException;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.BookingBuilder;
import gymmie.testutil.InMemoryDatabase;
import gymmie.testutil.TestClocks;
import gymmie.testutil.TrainingSessionBuilder;

class AccountProvisioningServiceTest {
    private InMemoryDatabase fixture;
    private Persistence persistence;
    private UserSession session;
    private AuthService auth;
    private PasswordHasher hasher;
    private Clock clock;
    private Account manager;
    private Account trainer;
    private Account member;
    private Account deactivatedMember;
    private AccountProvisioningService service;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        session = new UserSession();
        hasher = new PasswordHasher();
        clock = TestClocks.fixed();
        auth = new AuthService(persistence.accounts(), persistence.unitOfWork(), session, hasher);

        // Standard seeded Manager with username "manager"
        manager = new AccountBuilder().withId(1).withUsername("manager").withRole(Role.MANAGER).build();
        trainer = new AccountBuilder().withId(2).withUsername("trainer_bob").withRole(Role.TRAINER).build();
        member = new AccountBuilder().withId(3).withUsername("member_alice").withRole(Role.MEMBER).build();
        deactivatedMember = new AccountBuilder().withId(4).withUsername("member_charlie")
                .withRole(Role.MEMBER).withActive(false).build();

        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, manager);
            persistence.accounts().insert(connection, trainer);
            persistence.accounts().insert(connection, member);
            persistence.accounts().insert(connection, deactivatedMember);
            return null;
        });

        auth.login(manager.username(), AccountBuilder.DEFAULT_PASSWORD);
        Permissions permissions = new Permissions(persistence.accounts(), session);
        service = new AccountProvisioningService(persistence.accounts(), persistence.bookings(),
                persistence.sessions(), persistence.unitOfWork(), permissions, hasher, clock);
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    void getAllAccountsReturnsAllAccountsIncludingDeactivated() throws Exception {
        List<Account> accounts = service.getAllAccounts();
        assertEquals(4, accounts.size());
        assertEquals("manager", accounts.get(0).username());
        assertEquals("member_charlie", accounts.get(3).username());
        assertFalse(accounts.get(3).active());
    }

    @Test
    void getActiveAccountsReturnsOnlyActiveAccounts() throws Exception {
        List<Account> accounts = service.getActiveAccounts();
        assertEquals(3, accounts.size());
        assertTrue(accounts.stream().allMatch(Account::active));
    }

    @Test
    void findByIdReturnsAccountWhenPresent() throws Exception {
        assertTrue(service.findById(1).isPresent());
        assertEquals("manager", service.findById(1).orElseThrow().username());
        assertTrue(service.findById(4).isPresent());
        assertFalse(service.findById(4).orElseThrow().active());
        assertTrue(service.findById(999).isEmpty());
    }

    @Test
    void findByUsernameReturnsAccountWhenPresent() throws Exception {
        assertTrue(service.findByUsername("MEMBER_ALICE").isPresent());
        assertEquals(3, service.findByUsername("MEMBER_ALICE").orElseThrow().id());
        assertTrue(service.findByUsername("nonexistent").isEmpty());
    }

    @Test
    void createProvisionsTrainerAccountSuccessfully() throws Exception {
        Account created = service.create("trainer_carol", "secret123", "Carol Danvers", Role.TRAINER);
        assertNotNull(created);
        assertEquals(5, created.id());
        assertEquals("trainer_carol", created.username());
        assertEquals("Carol Danvers", created.displayName());
        assertEquals(Role.TRAINER, created.role());
        assertTrue(created.active());
        assertTrue(hasher.verify("secret123", created.password()));

        Account persisted = service.findById(created.id()).orElseThrow();
        assertEquals("Carol Danvers", persisted.displayName());
    }

    @Test
    void createProvisionsMemberAccountSuccessfully() throws Exception {
        Account created = service.create("member_dave", "password888", "Dave Smith", Role.MEMBER);
        assertNotNull(created);
        assertEquals(Role.MEMBER, created.role());
        assertTrue(created.active());
    }

    @Test
    void createRejectsManagerRole() {
        assertThrows(ValidationException.class, () ->
                service.create("new_manager", "password123", "New Manager", Role.MANAGER));
    }

    @Test
    void createRejectsNullRole() {
        assertThrows(ValidationException.class, () ->
                service.create("new_user", "password123", "New User", null));
    }

    @Test
    void createRejectsDuplicateUsernameCaseInsensitive() {
        assertThrows(ConflictException.class, () ->
                service.create("TRAINER_BOB", "password123", "Duplicate Bob", Role.TRAINER));
    }

    @Test
    void createRejectsInvalidUsername() {
        assertThrows(ValidationException.class, () ->
                service.create("ab", "password123", "Short Name", Role.TRAINER));
        assertThrows(ValidationException.class, () ->
                service.create("invalid username!", "password123", "Special Chars", Role.TRAINER));
    }

    @Test
    void createRejectsInvalidPassword() {
        assertThrows(ValidationException.class, () ->
                service.create("valid_user", "short", "Valid User", Role.MEMBER));
    }

    @Test
    void editUpdatesDisplayNameSuccessfully() throws Exception {
        Account updated = service.edit(2, "Robert Coach");
        assertEquals("Robert Coach", updated.displayName());

        Account persisted = service.findById(2).orElseThrow();
        assertEquals("Robert Coach", persisted.displayName());
        assertEquals("trainer_bob", persisted.username());
        assertEquals(Role.TRAINER, persisted.role());
    }

    @Test
    void editRejectsMissingAccount() {
        assertThrows(ConflictException.class, () -> service.edit(999, "Nobody"));
    }

    @Test
    void editRejectsInvalidDisplayName() {
        assertThrows(ValidationException.class, () -> service.edit(2, ""));
    }

    @Test
    void deactivateDeactivatesAccountAndCancelsFutureBookings() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        TrainingSession futureSession = new TrainingSessionBuilder().withId(1).withTrainerId(2)
                .withStartsAt(now.plusDays(1)).build();
        TrainingSession pastSession = new TrainingSessionBuilder().withId(2).withTrainerId(2)
                .withStartsAt(now.minusDays(1)).build();
        TrainingSession anotherFutureSession = new TrainingSessionBuilder().withId(3).withTrainerId(2)
                .withStartsAt(now.plusDays(2)).build();

        Booking futureBooking = new BookingBuilder().withId(1).withMemberId(3).withSessionId(1)
                .withStatus(BookingStatus.BOOKED).build();
        Booking pastBooking = new BookingBuilder().withId(2).withMemberId(3).withSessionId(2)
                .withStatus(BookingStatus.BOOKED).build();
        Booking alreadyCancelled = new BookingBuilder().withId(3).withMemberId(3).withSessionId(3)
                .withStatus(BookingStatus.CANCELLED)
                .withCancellationReason(CancellationReason.MEMBER_CANCELLED_BOOKING).build();

        persistence.unitOfWork().inTransaction(connection -> {
            persistence.sessions().insert(connection, futureSession);
            persistence.sessions().insert(connection, pastSession);
            persistence.sessions().insert(connection, anotherFutureSession);
            persistence.bookings().insert(connection, futureBooking);
            persistence.bookings().insert(connection, pastBooking);
            persistence.bookings().insert(connection, alreadyCancelled);
            return null;
        });

        Account deactivated = service.deactivate(3);
        assertFalse(deactivated.active());

        // Future booking should be cancelled with ACCOUNT_DEACTIVATED
        Booking updatedFuture = persistence.unitOfWork().inTransaction(connection ->
                persistence.bookings().findById(connection, 1).orElseThrow());
        assertEquals(BookingStatus.CANCELLED, updatedFuture.status());
        assertEquals(CancellationReason.ACCOUNT_DEACTIVATED, updatedFuture.cancellationReason());

        // Past booking remains BOOKED
        Booking updatedPast = persistence.unitOfWork().inTransaction(connection ->
                persistence.bookings().findById(connection, 2).orElseThrow());
        assertEquals(BookingStatus.BOOKED, updatedPast.status());

        // Already cancelled booking preserves original reason
        Booking updatedAlreadyCancelled = persistence.unitOfWork().inTransaction(connection ->
                persistence.bookings().findById(connection, 3).orElseThrow());
        assertEquals(CancellationReason.MEMBER_CANCELLED_BOOKING, updatedAlreadyCancelled.cancellationReason());
    }

    @Test
    void deactivatePreventsDeactivatingSeededManager() {
        assertThrows(ValidationException.class, () -> service.deactivate(1));
    }

    @Test
    void deactivateRejectsAlreadyDeactivatedAccount() {
        assertThrows(ConflictException.class, () -> service.deactivate(4));
    }

    @Test
    void deactivateRejectsMissingAccount() {
        assertThrows(ConflictException.class, () -> service.deactivate(999));
    }

    @Test
    void reactivateRestoresInactiveAccountWithoutRecreatingBookings() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        TrainingSession futureSession = new TrainingSessionBuilder().withId(1).withTrainerId(2)
                .withStartsAt(now.plusDays(1)).build();
        Booking cancelledBooking = new BookingBuilder().withId(1).withMemberId(4).withSessionId(1)
                .withStatus(BookingStatus.CANCELLED)
                .withCancellationReason(CancellationReason.ACCOUNT_DEACTIVATED).build();

        persistence.unitOfWork().inTransaction(connection -> {
            persistence.sessions().insert(connection, futureSession);
            persistence.bookings().insert(connection, cancelledBooking);
            return null;
        });

        Account reactivated = service.reactivate(4);
        assertTrue(reactivated.active());

        Account persisted = service.findById(4).orElseThrow();
        assertTrue(persisted.active());

        // Bookings remain cancelled
        Booking booking = persistence.unitOfWork().inTransaction(connection ->
                persistence.bookings().findById(connection, 1).orElseThrow());
        assertEquals(BookingStatus.CANCELLED, booking.status());
        assertEquals(CancellationReason.ACCOUNT_DEACTIVATED, booking.cancellationReason());
    }

    @Test
    void reactivateRejectsAlreadyActiveAccount() {
        assertThrows(ConflictException.class, () -> service.reactivate(2));
    }

    @Test
    void reactivateRejectsMissingAccount() {
        assertThrows(ConflictException.class, () -> service.reactivate(999));
    }

    @Test
    void unauthorizedWhenNotLoggedIn() throws Exception {
        auth.logout();
        assertThrows(AuthenticationException.class, () -> service.getAllAccounts());
        assertThrows(AuthenticationException.class, () -> service.getActiveAccounts());
        assertThrows(AuthenticationException.class, () -> service.findById(1));
        assertThrows(AuthenticationException.class, () -> service.findByUsername("manager"));
        assertThrows(AuthenticationException.class, () ->
                service.create("test_user", "password123", "Test", Role.MEMBER));
        assertThrows(AuthenticationException.class, () -> service.edit(2, "Test"));
        assertThrows(AuthenticationException.class, () -> service.deactivate(2));
        assertThrows(AuthenticationException.class, () -> service.reactivate(4));
    }

    @Test
    void unauthorizedWhenLoggedInAsTrainerOrMember() throws Exception {
        auth.login(trainer.username(), AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> service.getAllAccounts());
        assertThrows(AuthorizationException.class, () -> service.getActiveAccounts());
        assertThrows(AuthorizationException.class, () -> service.findById(1));
        assertThrows(AuthorizationException.class, () ->
                service.create("test_user", "password123", "Test", Role.MEMBER));

        auth.login(member.username(), AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> service.getAllAccounts());
        assertThrows(AuthorizationException.class, () -> service.deactivate(2));
        assertThrows(AuthorizationException.class, () -> service.reactivate(4));
    }
}
