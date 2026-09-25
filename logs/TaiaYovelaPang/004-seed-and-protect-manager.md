# Seed and Protect the First-Run Manager

## Goal
Create Gymmie's first-run Manager with username `manager` and password `manager123`, protect it from deactivation, and follow the application's password-storage rules.

## Scope
Five user-assistant exchanges, including the logging request, focused on one Manager-seeding task and its recovery behavior.

## Key prompts
> Add Seeder logic for the first-run Manager account

This requested startup initialization with fixed credentials, deactivation protection, and hashed password storage while leaving the seeding API and first-run detection open to implementation choices.

> In the case where the table has one inactive account (for example someone added it manually to the data table) but no manger account was seeded. How do I get my application to work?

This exposed a recovery gap in the initial empty-table check: an unrelated inactive account could prevent creation of a usable Manager.

## Decisions and corrections
- Initially implemented seeding only when the account table was empty, in a transaction after schema initialization. The user subsequently approved replacing this condition with a case-insensitive lookup for the `manager` username.
- Final behavior: when that username is absent, create the active Manager using the first available positive account ID, preserving unrelated accounts, including inactive ones.
- Preserve an existing active Manager and its current password. Report a clear conflict when the reserved username belongs to another role or an inactive Manager; do not overwrite, reactivate, or reset credentials.
- Identify the protected seed by its immutable username and Manager role. Enforce deactivation protection in the account model, including after password or display-name changes.
- Use the existing password hasher: PBKDF2-HMAC-SHA256, 600,000 iterations, and a fresh random 128-bit salt. Persist only hash and salt.
- Testing revealed that manually deactivated Managers fail model validation during repository reads, with the failure wrapped in a SQL exception. Corrected the seeder to translate that validation failure into a conflict while preserving ordinary database errors.
- Clarified recovery: a manually deactivated `manager` blocks startup until its database `active` flag is restored to `1`; its existing password remains unchanged. A deactivated Manager with another username does not prevent seeding.
- Updated documentation and tests; renamed the ordinary Manager authentication fixture to separate it from the protected seed. Corrected one Checkstyle formatting violation. All nine final seeder tests and the full `./gradlew check` passed. No commits were made.

## Files created or modified
- `src/main/java/gymmie/service/Seeder.java` (created)
- `src/test/java/gymmie/service/SeederTest.java` (created)
- `src/main/java/gymmie/App.java`
- `src/main/java/gymmie/model/Account.java`
- `src/main/java/gymmie/persistence/repository/AccountRepository.java`
- `src/test/java/gymmie/model/AccountTest.java`
- `src/test/java/gymmie/service/AuthServiceTest.java`
- `README.md`
