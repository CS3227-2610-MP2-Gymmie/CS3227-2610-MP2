package gymmie.manager.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import gymmie.model.Constraints;
import gymmie.model.MembershipPlan;
import gymmie.model.Role;
import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.MembershipPlanRepository;
import gymmie.service.Permissions;

/**
 * Provides Manager-authorized CRUD operations for membership plans.
 *
 * <p>Enforces Manager permissions on all catalogue modifications and queries. Purchased plans
 * are soft-archived rather than hard-deleted to preserve historical membership snapshots.
 */
public final class MembershipPlanService {
    private final MembershipPlanRepository plans;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;

    /**
     * Creates a plan service with the application's transaction and authorization boundaries.
     *
     * @param plans membership plan repository.
     * @param unitOfWork top-level transaction boundary.
     * @param permissions service-layer authorization boundary.
     */
    public MembershipPlanService(MembershipPlanRepository plans, UnitOfWork unitOfWork, Permissions permissions) {
        this.plans = Objects.requireNonNull(plans);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
    }

    /**
     * Lists all membership plans for administration, including archived plans.
     *
     * @return all plans in identifier order.
     * @throws Exception if authorization or storage access fails.
     */
    public List<MembershipPlan> getAllPlans() throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            return plans.findAll(connection);
        });
    }

    /**
     * Lists only unarchived membership plans available for new purchases.
     *
     * @return available plans in identifier order.
     * @throws Exception if authorization or storage access fails.
     */
    public List<MembershipPlan> getAvailablePlans() throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            return plans.findAllAvailable(connection);
        });
    }

    /**
     * Finds a plan by its identifier, including archived plans.
     *
     * @param planId plan identifier.
     * @return the matching plan, or empty if absent.
     * @throws Exception if authorization or storage access fails.
     */
    public Optional<MembershipPlan> findById(long planId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            return plans.findById(connection, planId);
        });
    }

    /**
     * Creates a new membership plan with a caller-assigned positive identifier from the repository.
     *
     * @param name non-blank, unique plan name.
     * @param durationDays plan duration in days (1 to 365).
     * @param priceCents plan price in SGD cents (>= 0).
     * @return the created plan.
     * @throws Exception if authorization, validation, conflict or persistence fails.
     */
    public MembershipPlan create(String name, int durationDays, int priceCents) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            Constraints.required(name, "Plan name");
            if (name.isBlank()) {
                throw new ValidationException("Plan name must not be blank");
            }
            String trimmedName = name.strip();
            ensureUniqueName(connection, trimmedName, 0);
            long id = plans.nextId(connection);
            MembershipPlan plan = new MembershipPlan(id, trimmedName, durationDays, priceCents, false);
            plans.insert(connection, plan);
            return plan;
        });
    }

    /**
     * Updates catalogue fields of an existing unarchived plan.
     *
     * @param planId identifier of the plan to edit.
     * @param name replacement non-blank, unique plan name.
     * @param durationDays replacement duration in days (1 to 365).
     * @param priceCents replacement price in SGD cents (>= 0).
     * @return the updated plan.
     * @throws Exception if authorization, validation, conflict or persistence fails.
     */
    public MembershipPlan edit(long planId, String name, int durationDays, int priceCents) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            MembershipPlan existing = plans.findById(connection, planId)
                    .orElseThrow(() -> new ConflictException("Membership plan not found"));
            if (existing.archived()) {
                throw new ConflictException("An archived plan cannot be edited");
            }
            Constraints.required(name, "Plan name");
            if (name.isBlank()) {
                throw new ValidationException("Plan name must not be blank");
            }
            String trimmedName = name.strip();
            ensureUniqueName(connection, trimmedName, planId);
            MembershipPlan replacement = new MembershipPlan(existing.id(), trimmedName, durationDays,
                    priceCents, false);
            plans.update(connection, replacement);
            return replacement;
        });
    }

    /**
     * Archives an unarchived plan to disable new purchases while retaining member history.
     *
     * @param planId identifier of the plan to archive.
     * @return the archived plan.
     * @throws Exception if authorization, conflict or persistence fails.
     */
    public MembershipPlan archive(long planId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            MembershipPlan existing = plans.findById(connection, planId)
                    .orElseThrow(() -> new ConflictException("Membership plan not found"));
            if (existing.archived()) {
                throw new ConflictException("This plan is already archived.");
            }
            MembershipPlan archived = new MembershipPlan(existing.id(), existing.name(), existing.durationDays(),
                    existing.priceCents(), true);
            plans.update(connection, archived);
            return archived;
        });
    }

    /**
     * Restores an archived plan so it becomes available for new purchases again.
     *
     * @param planId identifier of the plan to restore.
     * @return the restored plan.
     * @throws Exception if authorization, conflict or persistence fails.
     */
    public MembershipPlan restore(long planId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            MembershipPlan existing = plans.findById(connection, planId)
                    .orElseThrow(() -> new ConflictException("Membership plan not found"));
            if (!existing.archived()) {
                throw new ConflictException("This plan is not archived.");
            }
            MembershipPlan restored = new MembershipPlan(existing.id(), existing.name(), existing.durationDays(),
                    existing.priceCents(), false);
            plans.update(connection, restored);
            return restored;
        });
    }

    /**
     * Deletes a plan if never purchased, or archives it if purchase history exists.
     *
     * <p>Adheres to the soft-delete-archive pattern: never-purchased plans are safely removed,
     * while purchased plans are archived without modifying historical membership snapshots.
     *
     * @param planId identifier of the plan to remove or archive.
     * @return true if hard-deleted because unpurchased; false if archived due to purchase history.
     * @throws Exception if authorization, conflict or persistence fails.
     */
    public boolean deleteOrArchive(long planId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            MembershipPlan existing = plans.findById(connection, planId)
                    .orElseThrow(() -> new ConflictException("Membership plan not found"));
            if (plans.deleteIfUnpurchased(connection, planId)) {
                return true;
            }
            if (existing.archived()) {
                throw new ConflictException("This plan is already archived.");
            }
            MembershipPlan archived = new MembershipPlan(existing.id(), existing.name(), existing.durationDays(),
                    existing.priceCents(), true);
            plans.update(connection, archived);
            return false;
        });
    }

    /**
     * Deletes a plan only if it has never been purchased by any member.
     *
     * @param planId identifier of the plan to delete.
     * @return true if deleted; false if absent or purchase history exists.
     * @throws Exception if authorization or persistence fails.
     */
    public boolean deleteIfUnpurchased(long planId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.MANAGER);
            plans.findById(connection, planId)
                    .orElseThrow(() -> new ConflictException("Membership plan not found"));
            return plans.deleteIfUnpurchased(connection, planId);
        });
    }

    private void ensureUniqueName(Connection connection, String name, long excludedId) throws SQLException {
        for (MembershipPlan existing : plans.findAll(connection)) {
            if (existing.id() != excludedId && existing.name().equalsIgnoreCase(name)) {
                throw new ConflictException("A membership plan with the name '" + name + "' already exists");
            }
        }
    }
}
