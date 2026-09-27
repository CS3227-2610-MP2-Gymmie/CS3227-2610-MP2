# Implement Membership Plan Management Service

## Goal

Implement `MembershipPlanService` providing Manager-authorized CRUD operations for membership plans using `MembershipPlanRepository.nextId()`, adhering to the soft-delete archive pattern, addressing issue #94.

## Scope

One focused implementation task on the `issue-2-plan-service` branch covering service domain logic, application wiring, and comprehensive unit tests.

## Key prompts

- "Ok, the issue has been merged, now carry out step D, then work on the next issue (on a branch that already has issue 1 merged)" — Directed pulling upstream master containing the merged Issue 1 (#93), creating branch `issue-2-plan-service`, and implementing Issue #94.

## Decisions and corrections

- Implemented `MembershipPlanService` in package `gymmie.manager.service` enforcing Manager-only access (`permissions.requireRole(connection, Role.MANAGER)`) for all queries and mutations.
- Applied the soft-delete archive pattern: `create` validates invariants and uniqueness, `edit` modifies only unarchived plans, `deleteOrArchive` hard-deletes never-purchased plans via `deleteIfUnpurchased` while soft-archiving purchased plans to safeguard historical membership snapshots.
- Registered `membershipPlanService` in `AppContext` and exposed `getMembershipPlanService()` for upcoming UI integration in Issue 3.
- In `MembershipPlanServiceTest`, corrected an unauthenticated test helper to use `auth.logout()` instead of the package-private `session.clear()`, and added `throws Exception` where `auth.login()` was invoked.

## Files created or modified

- src/main/java/gymmie/AppContext.java
- src/main/java/gymmie/manager/service/MembershipPlanService.java
- src/test/java/gymmie/manager/service/MembershipPlanServiceTest.java
