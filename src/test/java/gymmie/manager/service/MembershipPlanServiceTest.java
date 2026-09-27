package gymmie.manager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gymmie.model.Account;
import gymmie.model.Membership;
import gymmie.model.MembershipPlan;
import gymmie.model.Role;
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
import gymmie.testutil.InMemoryDatabase;
import gymmie.testutil.MembershipBuilder;
import gymmie.testutil.MembershipPlanBuilder;

class MembershipPlanServiceTest {
    private InMemoryDatabase fixture;
    private Persistence persistence;
    private UserSession session;
    private AuthService auth;
    private Account manager;
    private Account trainer;
    private Account member;
    private MembershipPlanService service;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new InMemoryDatabase();
        persistence = fixture.persistence();
        session = new UserSession();
        auth = new AuthService(persistence.accounts(), persistence.unitOfWork(), session, new PasswordHasher());
        manager = new AccountBuilder().withId(1).withUsername("manager_user").withRole(Role.MANAGER).build();
        trainer = new AccountBuilder().withId(2).withUsername("trainer_user").withRole(Role.TRAINER).build();
        member = new AccountBuilder().withId(3).withUsername("member_user").withRole(Role.MEMBER).build();

        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, manager);
            persistence.accounts().insert(connection, trainer);
            persistence.accounts().insert(connection, member);
            persistence.plans().insert(connection,
                    new MembershipPlanBuilder().withId(1).withName("Monthly Standard").withDurationDays(30)
                            .withPriceCents(4990).withArchived(false).build());
            persistence.plans().insert(connection,
                    new MembershipPlanBuilder().withId(2).withName("Legacy Annual").withDurationDays(365)
                            .withPriceCents(49900).withArchived(true).build());
            return null;
        });

        auth.login(manager.username(), AccountBuilder.DEFAULT_PASSWORD);
        Permissions permissions = new Permissions(persistence.accounts(), session);
        service = new MembershipPlanService(persistence.plans(), persistence.unitOfWork(), permissions);
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    void getAllPlansReturnsAllPlansIncludingArchived() throws Exception {
        List<MembershipPlan> plans = service.getAllPlans();
        assertEquals(2, plans.size());
        assertEquals("Monthly Standard", plans.get(0).name());
        assertEquals("Legacy Annual", plans.get(1).name());
    }

    @Test
    void getAvailablePlansReturnsOnlyUnarchivedPlans() throws Exception {
        List<MembershipPlan> plans = service.getAvailablePlans();
        assertEquals(1, plans.size());
        assertEquals("Monthly Standard", plans.getFirst().name());
        assertFalse(plans.getFirst().archived());
    }

    @Test
    void findByIdReturnsPlanWhenPresent() throws Exception {
        assertTrue(service.findById(1).isPresent());
        assertEquals("Monthly Standard", service.findById(1).orElseThrow().name());
        assertTrue(service.findById(2).isPresent());
        assertTrue(service.findById(2).orElseThrow().archived());
        assertTrue(service.findById(999).isEmpty());
    }

    @Test
    void createAddsNewPlanWithNextIdAndInvariants() throws Exception {
        MembershipPlan created = service.create("Weekly Trial", 7, 1500);
        assertEquals(3, created.id());
        assertEquals("Weekly Trial", created.name());
        assertEquals(7, created.durationDays());
        assertEquals(1500, created.priceCents());
        assertFalse(created.archived());

        List<MembershipPlan> available = service.getAvailablePlans();
        assertEquals(2, available.size());
    }

    @Test
    void createRejectsInvalidInvariants() {
        assertThrows(ValidationException.class, () -> service.create("", 30, 5000));
        assertThrows(ValidationException.class, () -> service.create("   ", 30, 5000));
        assertThrows(ValidationException.class, () -> service.create("Zero duration", 0, 5000));
        assertThrows(ValidationException.class, () -> service.create("Negative duration", -1, 5000));
        assertThrows(ValidationException.class, () -> service.create("Excessive duration", 366, 5000));
        assertThrows(ValidationException.class, () -> service.create("Negative price", 30, -1));
        assertThrows(ValidationException.class, () -> service.create("Excessive price", 30, 1_000_001));
    }

    @Test
    void createRejectsDuplicatePlanNameCaseInsensitive() {
        assertThrows(ConflictException.class, () -> service.create("monthly standard", 15, 2000));
        assertThrows(ConflictException.class, () -> service.create("MONTHLY STANDARD", 15, 2000));
        assertThrows(ConflictException.class, () -> service.create("legacy annual", 15, 2000));
    }

    @Test
    void editUpdatesCatalogueFieldsOfUnarchivedPlan() throws Exception {
        MembershipPlan updated = service.edit(1, "Monthly Premium", 31, 5990);
        assertEquals(1, updated.id());
        assertEquals("Monthly Premium", updated.name());
        assertEquals(31, updated.durationDays());
        assertEquals(5990, updated.priceCents());
        assertFalse(updated.archived());

        MembershipPlan reloaded = service.findById(1).orElseThrow();
        assertEquals("Monthly Premium", reloaded.name());
        assertEquals(31, reloaded.durationDays());
        assertEquals(5990, reloaded.priceCents());
    }

    @Test
    void editPreservesSameNameWithoutDuplicateCollision() throws Exception {
        MembershipPlan updated = service.edit(1, "Monthly Standard", 60, 8990);
        assertEquals("Monthly Standard", updated.name());
        assertEquals(60, updated.durationDays());
    }

    @Test
    void editRejectsDuplicateNameWithAnotherPlan() {
        assertThrows(ConflictException.class, () -> service.edit(1, "Legacy Annual", 30, 4990));
    }

    @Test
    void editRejectsArchivedPlan() {
        assertThrows(ConflictException.class, () -> service.edit(2, "New Annual", 365, 49900));
    }

    @Test
    void editRejectsInvalidInvariants() {
        assertThrows(ValidationException.class, () -> service.edit(1, "", 30, 5000));
        assertThrows(ValidationException.class, () -> service.edit(1, "Valid Name", 0, 5000));
        assertThrows(ValidationException.class, () -> service.edit(1, "Valid Name", 30, -1));
    }

    @Test
    void editRejectsNonExistentPlan() {
        assertThrows(ConflictException.class, () -> service.edit(999, "Valid Name", 30, 5000));
    }

    @Test
    void archiveDisablesPurchasesAndExcludesFromAvailable() throws Exception {
        MembershipPlan archived = service.archive(1);
        assertTrue(archived.archived());

        List<MembershipPlan> available = service.getAvailablePlans();
        assertTrue(available.isEmpty());

        List<MembershipPlan> all = service.getAllPlans();
        assertEquals(2, all.size());
        assertTrue(all.getFirst().archived());
    }

    @Test
    void archiveRejectsAlreadyArchivedPlan() {
        ConflictException ex = assertThrows(ConflictException.class, () -> service.archive(2));
        assertEquals("This plan is already archived.", ex.getMessage());
    }

    @Test
    void restoreReactivatesArchivedPlan() throws Exception {
        MembershipPlan restored = service.restore(2);
        assertFalse(restored.archived());

        List<MembershipPlan> available = service.getAvailablePlans();
        assertEquals(2, available.size());
    }

    @Test
    void restoreRejectsUnarchivedPlan() {
        ConflictException ex = assertThrows(ConflictException.class, () -> service.restore(1));
        assertEquals("This plan is not archived.", ex.getMessage());
    }

    @Test
    void deleteIfUnpurchasedRemovesUnusedPlan() throws Exception {
        boolean deleted = service.deleteIfUnpurchased(1);
        assertTrue(deleted);
        assertTrue(service.findById(1).isEmpty());
    }

    @Test
    void deleteIfUnpurchasedFailsWhenPlanHasPurchases() throws Exception {
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.memberships().insert(connection,
                    new MembershipBuilder().withId(1).withMemberId(3).withPlanId(1).build());
            return null;
        });

        boolean deleted = service.deleteIfUnpurchased(1);
        assertFalse(deleted);
        assertTrue(service.findById(1).isPresent());
    }

    @Test
    void deleteOrArchiveDeletesNeverPurchasedPlan() throws Exception {
        boolean hardDeleted = service.deleteOrArchive(1);
        assertTrue(hardDeleted);
        assertTrue(service.findById(1).isEmpty());
    }

    @Test
    void deleteOrArchiveArchivesPurchasedPlanAndPreservesSnapshots() throws Exception {
        Membership membership = new MembershipBuilder().withId(1).withMemberId(3).withPlanId(1)
                .withSnapshotPriceCents(4990).withSnapshotDurationDays(30).build();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.memberships().insert(connection, membership);
            return null;
        });

        boolean hardDeleted = service.deleteOrArchive(1);
        assertFalse(hardDeleted);

        MembershipPlan archivedPlan = service.findById(1).orElseThrow();
        assertTrue(archivedPlan.archived());

        persistence.unitOfWork().inTransaction(connection -> {
            Membership loaded = persistence.memberships().findById(connection, 1).orElseThrow();
            assertEquals(4990, loaded.snapshotPriceCents());
            assertEquals(30, loaded.snapshotDurationDays());
            return null;
        });
    }

    @Test
    void nonManagerRolesAreRejectedAcrossOperations() throws Exception {
        auth.login(trainer.username(), AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> service.getAllPlans());
        assertThrows(AuthorizationException.class, () -> service.getAvailablePlans());
        assertThrows(AuthorizationException.class, () -> service.findById(1));
        assertThrows(AuthorizationException.class, () -> service.create("Plan", 30, 5000));
        assertThrows(AuthorizationException.class, () -> service.edit(1, "Plan", 30, 5000));
        assertThrows(AuthorizationException.class, () -> service.archive(1));
        assertThrows(AuthorizationException.class, () -> service.restore(2));
        assertThrows(AuthorizationException.class, () -> service.deleteIfUnpurchased(1));
        assertThrows(AuthorizationException.class, () -> service.deleteOrArchive(1));

        auth.login(member.username(), AccountBuilder.DEFAULT_PASSWORD);
        assertThrows(AuthorizationException.class, () -> service.getAllPlans());
        assertThrows(AuthorizationException.class, () -> service.create("Plan", 30, 5000));
    }

    @Test
    void unauthenticatedCallersAreRejected() {
        auth.logout();
        assertThrows(AuthenticationException.class, () -> service.getAllPlans());
        assertThrows(AuthenticationException.class, () -> service.create("Plan", 30, 5000));
    }
}
