# SQLite Build Verification

## Goal
Implement issue #17 by adding SQLite JDBC support, Java 25 native-access configuration, a combined verification task, and a real SQLite integration test.

## Scope
Three user-assistant exchanges covering one focused repository task and its commit-message follow-up.

## Key prompts
- “Implement issue #17 in this repository: \"Add SQLite build support and verification\".” This defined the focused implementation scope.
- “Also add one JUnit test that opens an in-memory SQLite connection.” This required runtime proof that the native driver loads.
- “Verification, all of which must pass before you report done.” This established the required Gradle, packaging, launch, and test checks.
- “Do not commit or push. Show me the diff and verification output first.” This constrained repository changes and required evidence before completion.

## Decisions and corrections
- Added `org.xerial:sqlite-jdbc:3.50.3.0`, a `verify` task depending on `check` and `shadowJar`, and `SqliteTest` with a `jdbc:sqlite::memory:` query.
- Checked Java 25 documentation: the launcher uses `--enable-native-access`; the executable-JAR manifest attribute is `Enable-Native-Access`, whose only supported value is `ALL-UNNAMED`.
- Because the local JDK exposes JavaFX as a named `javafx.graphics` module, application launch arguments include both `ALL-UNNAMED` and `javafx.graphics`. The manifest remains documentation-compliant.
- `verify`, the SQLite test, and fat-JAR creation passed. Plain `java -jar` still emitted a JavaFX named-module warning in the headless environment; this limitation was reported rather than hidden. No commit or push was performed.

## Files created or modified
- `build.gradle`
- `src/test/java/gymmie/SqliteTest.java`
- `build/libs/Gymmie-1.0.0-all.jar`
