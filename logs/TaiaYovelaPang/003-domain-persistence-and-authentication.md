# Domain Models, Persistence, and Authentication

## Goal
Build Gymmie's domain model, repository contracts, SQLite implementations, and authentication services while following project conventions. Resolve review feedback and IDE warnings, and verify behavior with automated tests.

## Scope
Eight user-assistant exchanges, including this logging request, covering related backend implementation tasks and review corrections.

## Key prompts
- “Follow the coding conventions and OOP” required validated, immutable domain records with consistent responsibilities and documentation.
- “Repository methods accept the Connection they operate on” required nested persistence operations to share the caller's transaction rather than open another connection.
- “Implement SQLite repository classes for all five repository interfaces” requested durable storage connected to the existing database boundary, with history protection and atomic cancellation updates.
- “Is there a way to make this function less arrowheaded coding style?” requested flatter control flow in the SQL read helper without changing resource cleanup or exception handling.
- “Add PasswordHasher, AuthService, UserSession, and Permissions” requested authentication, logout, session state, own-password changes, and service-layer authorization.

## Decisions and corrections
- Added account, plan, membership, session, and booking records; enforced documented field limits, integer cents, local-time comparisons, purchase snapshots, and non-overlapping active memberships through the Member aggregate.
- Represented passwords with salted hashes. Authentication reused the existing format; hashing parameters remain implementation choices, not prescribed requirements.
- Fixed four initial Java warnings by removing an unused return value, inlining plan-name validation, simplifying booking validation, and using a functional Optional expression. Left the missing User Guide warning unchanged as requested.
- Defined five connection-taking repository interfaces, then implemented SQLite storage. Accounts, memberships, and bookings have no deletion API; plan/session deletion atomically refuses records with purchase/booking history, including cancelled history.
- Wired startup to `data/gymmie.db`. File-backed tests verified reopening, retained history, and rollback of membership/session cancellation and earlier booking updates after an injected SQLite failure. Application-scale performance verification remains separate as requested.
- Extracted `mapRows` to reduce nesting. Later added narrowly scoped SQL-inspection suppressions after verifying fixed templates and bound parameters, plus a stylesheet null check producing an IOException.
- Added credential-free session snapshots, distinct deactivated-account errors, current-password verification, and post-commit session publication. Permissions reload persisted account state and enforce exact roles and ownership; future workflow services must invoke these guards.
- Corrected Checkstyle formatting findings. Final `./gradlew check` passed with 116 tests. Sandbox-blocked Gradle-cache and GitHub-identity access were retried with approval. No commits or pushes were performed.

## Files created or modified
- `src/main/java/gymmie/model/*.java` and `model/exception/*.java`
- `src/main/java/gymmie/persistence/repository/*.java`, including `package-info.java`
- `src/main/java/gymmie/persistence/sqlite/*.java`
- `src/main/java/gymmie/persistence/Persistence.java`
- `src/main/java/gymmie/service/*.java` and `service/exception/*.java`
- `src/main/java/gymmie/App.java`
- `src/test/java/gymmie/model/*Test.java`
- `src/test/java/gymmie/persistence/sqlite/SqliteRepositoriesTest.java`
- `src/test/java/gymmie/service/AuthServiceTest.java` and `PasswordHasherTest.java`
- `docs/DeveloperGuide.md`
