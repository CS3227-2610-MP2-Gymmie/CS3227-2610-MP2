---
name: soft-delete-archive-pattern
description: Use when implementing any "removal" operation for entities (membership plans, trainer accounts, member accounts) in Gymmie to ensure archiving rather than hard-deleting is applied consistently.
---

## When to use this

- Implementing, reviewing, or refactoring any "delete" operation for plans or accounts.
- Adding a new domain entity that requires a deactivation/removal lifecycle.
- Auditing storage access to eliminate accidental hard-delete calls.

## What to build

- A consistent `isArchived` boolean flag (or status enum) across archivable entities (`MembershipPlan`, `TrainerAccount`, `MemberAccount`).
- Repository contract methods named `archive(id)` and `restore(id)` instead of `delete(id)`.
- Query separation: default queries fetch only active records (`findAllActive()`), while reporting/admin queries can fetch all records (`findAll()`).
- Data integrity guard: historical payment records, booking logs, and attendance rows referencing an archived entity must remain intact.

## Prerequisites

- Target entity domain model and repository interface defined.
- H2 database migration/schema includes the archiving column.

## Steps

1. Add `isArchived` flag to the entity domain model.
2. Expose `archive(id)` on the repository; ensure no hard `delete` method is exposed to Application Services.
3. Update standard selection queries to filter out archived records by default.
4. Enforce that only Manager roles can execute archive operations.
5. Write tests verifying that:
   - Archiving hides the entity from default active queries.
   - The entity remains queryable via explicit administrative lookups.
   - Associated historical records (e.g. payment logs) still resolve without foreign key failures.
   - Non-Manager callers cannot archive entities.
6. Verify `./gradlew check` passes.

## Verification environments

- `./gradlew test` passes repository and service lifecycle tests.

## Definition of done

- [ ] No application service invokes a hard delete on archivable entities.
- [ ] Default read operations exclude archived entities.
- [ ] Historical references to archived records remain intact.
- [ ] Manager authorization verified for archiving.
- [ ] `./gradlew check` passes.
- [ ] Interaction log recorded.
