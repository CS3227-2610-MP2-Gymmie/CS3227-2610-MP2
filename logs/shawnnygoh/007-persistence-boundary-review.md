# Persistence Boundary Review

## Goal
Implement issue #18's SQLite persistence boundary, then address follow-up code-review findings for transaction ownership and nested rollback behavior.

## Scope
Nine exchanges covering one focused persistence implementation, review, correction, verification, and commit-preparation task.

## Key prompts
- “Implement issue #18 in this repository: \"Add database schema and transaction boundary\".” This requested the schema, database connection setup, schema versioning, transaction boundary, tests, and verification.
- “Fix issues found reviewing #18.” This identified nested transaction commits, nullable descriptions, migration handling, timestamp conventions, and runtime database files as review corrections.
- “What are your thoughts?” This requested an assessment of four additional review comments, including startup initialization and transaction-scoped repository behavior.
- “Fix three findings from the code review on PR #48.” This narrowed implementation to caller-owned schema transactions, nested savepoints, top-level transaction documentation, regression tests, and verification.

## Decisions and corrections
- Added five SQLite tables with foreign keys, integer-cent money, text timestamps, case-insensitive usernames, schema versioning, and a configurable database boundary.
- Corrected the initial transaction design so nested `UnitOfWork` calls use JDBC savepoints; failed nested writes roll back while outer writes can still commit.
- Corrected `SchemaInitializer` to avoid committing or rolling back a caller-owned transaction.
- Added tests proving caller transaction preservation and that SQLite leaves outer account A committed while nested account B is removed after a caught failure.
- Updated timestamp fixtures to omit timezone offsets and documented that the connection-opening transaction overload is top-level only.
- Corrected the proposed branch and commit naming to match repository conventions, using a concise hyphenated branch name without the generic `codex/` prefix.
- Repeated `./gradlew verify` successfully, including Checkstyle, tests, and `shadowJar`. No commit or push was performed.

## Files created or modified
- `.gitignore`
- `src/main/resources/gymmie/db/schema.sql`
- `src/main/java/gymmie/persistence/Database.java`
- `src/main/java/gymmie/persistence/SchemaInitializer.java`
- `src/main/java/gymmie/persistence/TransactionCallback.java`
- `src/main/java/gymmie/persistence/UnitOfWork.java`
- `src/test/java/gymmie/PersistenceBoundaryTest.java`
