# Implement Account Provisioning Service

## Goal

Implement `AccountProvisioningService` providing Manager-authorized provisioning and lifecycle management for Trainer and Member accounts using `AccountRepository.nextId()`, adhering to the soft-delete archive pattern, addressing issue #96.

## Scope

One focused implementation task on the `issue-4-account-service` branch covering service domain logic, application wiring, and comprehensive unit tests.

## Key prompts

- "Ok, lets go back to what we were doing, the code changes have been merged, lets move on to the fourth issue." — Directed pulling upstream master containing the merged Issue 3 (#95), creating branch `issue-4-account-service`, and implementing Issue #96.

## Decisions and corrections

- Implemented `AccountProvisioningService` in package `gymmie.manager.service` enforcing Manager-only access (`permissions.requireRole(connection, Role.MANAGER)`) for all queries and mutations.
- Applied the soft-delete archive pattern: `create` validates unique username (case-insensitive), hashes passwords via `PasswordHasher`, restricts provisioned roles to `TRAINER` and `MEMBER`, and assigns IDs via `AccountRepository.nextId()`.
- Implemented `edit` to update display names while preserving immutable username and role constraints.
- Implemented `deactivate` to set `active = false` and atomically cancel all upcoming future bookings for the deactivated account with `CancellationReason.ACCOUNT_DEACTIVATED`, while preventing deactivation of the seeded Manager account.
- Implemented `reactivate` to restore inactive accounts without re-creating previously cancelled bookings.
- Supported administrative read queries (`getAllAccounts`) vs active-only read queries (`getActiveAccounts`).
- Registered `accountProvisioningService` in `AppContext` and exposed `getAccountProvisioningService()`.
- Created comprehensive unit tests in `AccountProvisioningServiceTest` verifying CRUD, deactivation booking cancellation, seeded-manager protection, and authorization checks.

## Files created or modified

- logs/naa-siuuuu-ff/004-account-provisioning-service.md
- src/main/java/gymmie/AppContext.java
- src/main/java/gymmie/manager/service/AccountProvisioningService.java
- src/test/java/gymmie/manager/service/AccountProvisioningServiceTest.java
