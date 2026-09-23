---
name: manage-membership-plans
description: Use when implementing or modifying Manager-role CRUD functionality for membership plans (name, duration, price in cents) in the Gymmie application.
---

## When to use this

- Building the initial Manager domain, service, and repository flow for membership plans.
- Adding a new plan attribute or validation rule to an existing plan CRUD flow.
- Extending or debugging plan create/read/update/archive operations.

## What to build

- A `MembershipPlan` domain model (pure Java, no JavaFX/DB imports) with fields: `id`, `name`, `durationDays`, `priceCents`, `isArchived`, `createdAt`.
- Invariant validation: non-blank and unique name, duration > 0 days, priceCents >= 0.
- A `MembershipPlanRepository` interface in the domain layer defining `save`, `findById`, `findByName`, `findAllActive`, `findAll`, and `archive`.
- A `MembershipPlanService` in the Application Services layer that enforces Manager authorization and business validation before delegating to the repository. No plan is ever hard-deleted (see `soft-delete-archive-pattern` skill).
- (Optional/Next Step) A thin Manager UI controller (FXML + CSS) that delegates all actions to `MembershipPlanService`.

## Prerequisites

- Domain layer package structure exists (`gymmie.domain.plan`).
- Storage adapter (H2) available or stubbed.
- Session context / role-check mechanism available in Application Services.

## Steps

1. Branch off latest `upstream/master` naming the branch after the issue (e.g. `feat/plan-management`).
2. Define `MembershipPlan` domain model and `MembershipPlanRepository` interface without any JavaFX/DB imports.
3. Implement `MembershipPlanService` with validation logic and manager role verification.
4. Implement `archive` logic adhering to the `soft-delete-archive-pattern`.
5. Write unit tests: valid creation, invalid invariant cases (blank name, negative price, zero duration), name collisions, archive toggling, and non-manager unauthorized access.
6. Verify `./gradlew check` passes Checkstyle and unit tests.
7. Open PR tagged with `Fixes #<issue-id>` and log session in `logs/<github-username>/`.

## Verification environments

- `./gradlew test` passes all unit and service tests.
- `./gradlew check` passes Checkstyle with zero violations.

## Definition of done

- [ ] Pure Java domain model and repository interface committed.
- [ ] Service enforces invariants and Manager-only permission.
- [ ] Soft-archiving implemented; no hard-delete method exposed to service.
- [ ] Unit tests cover positive, negative, and unauthorized paths.
- [ ] `./gradlew check` passes.
- [ ] Interaction log recorded.
