# Implementation Report

- **Issue reference:** [#122](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/issues/122)
- **Title:** Build a cross-platform release JAR.
- **Date:** 2026-09-29.
- **Branch:** implement-issue-122-release-jar.
- **Base commit:** 61e5f803a2755bfb91a971afbe15947a35d67800.
- **Skill invocation:** Explicit.

## Summary

| Skills used | Verification passed on first run | Final verification result | Tests added or changed | Docs updated | Number of user corrections |
| --- | --- | --- | --- | --- | --- |
| implement-issue (explicit) | No | Plain checks omit release packaging; the opt-in release test, `./gradlew run`, human macOS JAR check, and independent review passed | `ReleaseJarTest` | docs/DeveloperGuide.md | 2 |

## Acceptance criteria

| Criterion | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation) |
| --- | --- | --- |
| Add resolvable release configurations for SQLite/runtime dependencies and Windows, macOS aarch64, and Linux JavaFX jars, without disrupting `./gradlew run`. | completed | The opt-in release test verifies the archive's natives; `./gradlew run` reached `:run` and was stopped afterward. |
| Add `releaseJar` with the requested archive name, application entry point, native-access manifest attribute, and main source output. | completed | Archive is `build/libs/gymmie-release.jar`; `jar tf` shows `gymmie/Launcher.class`, `glass.dll`, `libglass.dylib`, and `libglass.so`; manifest inspection shows `Main-Class: gymmie.Launcher` and `Enable-Native-Access: ALL-UNNAMED`. Size: 26,406,123 bytes (25.2 MiB). |
| Run the release-JAR content test in the Ubuntu, macOS, and Windows CI matrix. | completed | The workflow runs `./gradlew test -PreleaseTests=true --tests gymmie.release.ReleaseJarTest`; this opt-in test depends on `releaseJar`. |
| Document the release JAR command and output in the Developer Guide. | completed | Added the `./gradlew releaseJar` command and `build/libs/gymmie-release.jar` output to `docs/DeveloperGuide.md`. |
| Keep `./gradlew run` working. | completed | `./gradlew run` entered the long-running `:run` task and was stopped afterward. |
| Verify release-JAR contents in an opt-in test. | completed | `./gradlew test -PreleaseTests=true --tests gymmie.release.ReleaseJarTest` passed after running `releaseJar`. |
| From a temporary folder, confirm the bundled JavaFX launches the login window on this Mac. | completed | Human check with JavaFX hidden from the JDK confirmed the JAR loaded JavaFX from its bundled classes and natives and started on macOS aarch64. See Review responses. |

## Workflow checklist

- [x] Inspected relevant instructions, source, tests, documentation, conventions, and repository skills before editing.
- [x] Mapped criteria to intended changes and checks.
- [x] Kept changes in scope. Unexpected files and why: none.
- [x] Ran verification on the final code state; reran checks after relevant later edits.
- [x] Other tools used besides skills above: Git, Gradle, `jar`, `unzip`, shell.
- [x] No commit, push, or external action occurred without authorization.

## Verification runs

| # | Command | Result | What was fixed next |
| --- | --- | --- | --- |
| 1 | `git fetch upstream` and `git switch -c implement-issue-122-release-jar upstream/master` | Passed after requesting filesystem access for Git metadata. | None. |
| 2 | `./gradlew check` | Passed. | None. |
| 3 | `./gradlew releaseJar` | Passed. | None. |
| 4 | `jar tf build/libs/gymmie-release.jar`; inspect manifest and file size | Passed; required JavaFX and SQLite native entries, launcher class, and manifest attributes are present; 26,406,123 bytes. | None. |
| 5 | `./gradlew run` | Reached `:run`; stopped with Ctrl-C after launch. | None. |
| 6 | `java -jar /Users/shawnnygoh/github-repos/CS3227-MP2/CS3227-2610-MP2/build/libs/gymmie-release.jar` from `/private/tmp/gymmie-release-smoke` | Process remained running with host GUI access, but desktop inventory did not expose the JavaFX login window; stopped with Ctrl-C. | GUI visibility could not be verified in this environment. |
| 7 | `./gradlew shadowJar` | Passed. | None. |
| 8 | `git diff --check` | Passed. | None. |
| 9 | `./gradlew check` after adding `ReleaseJarTest` | Passed; test, Checkstyle, and release-JAR packaging completed. | Moved the test out of the ignored `build/` package directory. |
| 10 | `./gradlew check` after moving `ReleaseJarTest` to `gymmie.release` | Passed; test, Checkstyle, and release-JAR packaging completed against the tracked test source. | None. |
| 11 | `./gradlew check --dry-run` | Passed; task graph did not include `releaseJar`. | Made release packaging conditional on `-PreleaseTests=true`. |
| 12 | `./gradlew check` | Passed; no `releaseJar` task ran. | None. |
| 13 | `./gradlew test --dry-run` | Passed; task graph did not include `releaseJar`. | None. |
| 14 | `./gradlew test -PreleaseTests=true --tests gymmie.release.ReleaseJarTest` | Passed; `releaseJar` was in the graph and the packaging test passed. | None. |
| 15 | `./gradlew run` | Reached `:run`; stopped with Ctrl-C after launch. | None. |
| 16 | Human JAR launch with `--limit-modules java.se,jdk.unsupported,jdk.unsupported.desktop,jdk.localedata,jdk.charsets` | Passed per user confirmation: bundled JavaFX classes and natives loaded and the app started on macOS aarch64. | Corrected the earlier smoke-test assessment; it used a JDK with JavaFX built in. |

## Self-assessment (self-assessed)

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Acceptance criteria met | 5 | Build, archive, CI, Developer Guide, standard run task, and the human macOS bundled-JavaFX launch check are complete. |
| Test adequacy | 5 | Opt-in `ReleaseJarTest` checks the requested archive metadata, platform natives, SQLite natives, and launcher; plain `check` does not build the release archive. |
| Scope and design fit | 5 | Changes are limited to `build.gradle`, the requested CI workflow, the Developer Guide, the packaging test, and this implementation report. |
| Consistency with repository conventions | 5 | Used the JavaFX version variable for all classifiers, left existing task definitions intact, and passed `git diff --check`. |

## Independent review



### Round 1

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Resolvable release configurations without changing runtimeClasspath | 5 | `build.gradle:41–76` isolates runtime dependencies and the three JavaFX classifiers. Existing JAR contains the requested JavaFX and SQLite natives. |
| releaseJar name, entry point, native access, and main output | 5 | Inspected `gymmie-release.jar`: correct manifest attributes, launcher, and all existing compiled main classes and source resources. |
| Release build in all three CI environments | 5 | Added `./gradlew releaseJar` within the existing Ubuntu, macOS, and Windows matrix. |
| Developer Guide command and output | 5 | `docs/DeveloperGuide.md:51` documents the command and exact output path. |
| Keep ./gradlew run working | 4 | Development configuration remains unchanged; report records reaching `:run`. Launch was not independently repeated during this read-only review. |
| Visually confirm packaged login window from a temporary folder | 1 | Report explicitly records that the login window was never visually confirmed. |
| Acceptance criteria met | 3 | Packaging, CI, and documentation are supported; required visual verification remains incomplete. |
| Test adequacy | 2 | No tests added or updated. Existing reports show 350 tests, 45 skipped, and zero failures; they do not verify release packaging. |
| Scope and design fit | 5 | Changes are confined to build configuration, CI, documentation, and the untracked implementation report. |
| Consistency with repository conventions | 2 | Diff whitespace checks pass, but omission of changed-behavior tests violates `AGENTS.md:26`. |

**Overall:** fail — Required visual verification remains incomplete, and release packaging lacks the tests mandated by repository conventions.

**Blocking:**

- `evals/implement-issue/reports/issue-122-release-jar.md:25`: Complete the explicit acceptance criterion by launching the release JAR from a temporary folder on the Mac, visually confirming the login window, and closing it; a running process alone does not establish this.
- `build.gradle:85–102`: Add repeatable packaging tests covering the required manifest, application resources, platform natives, and runtime-classpath isolation. Building the archive and manually inspecting it do not satisfy the requirement to add or update tests in `AGENTS.md:26`.

**Non-blocking:** None.

Reviewed: 2026-09-29; base: upstream/master; model: gpt-6-astra; HEAD: 61e5f80; Uncommitted changes: yes.


### Round 2

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Resolvable release configurations without changing runtimeClasspath | 5 | `build.gradle:41` isolates runtime dependencies and the three JavaFX platform configurations. Required natives are present in the existing archive. |
| releaseJar name, entry point, native access, and main output | 5 | Inspected `gymmie-release.jar`: correct manifest attributes, launcher, compiled main classes, and source resources. |
| Release build in Ubuntu, macOS, and Windows CI | 5 | `.github/workflows/gradle.yml` runs `./gradlew releaseJar` within the existing three-platform matrix. |
| Developer Guide command and output | 5 | `docs/DeveloperGuide.md:51` documents the command and exact archive path. |
| Keep ./gradlew run working | 4 | Development configuration remains unchanged; the report records reaching `:run`. Not independently launched during this read-only review. |
| Test release contents and configuration isolation | 4 | Untracked `src/test/java/gymmie/release/ReleaseJarTest.java` checks metadata, launcher, natives, and isolation. Existing passing results reference its previous `gymmie.build` package. |
| Visually confirm packaged login window from a temporary folder | 1 | The report explicitly records that the login window was never visually confirmed. |
| Acceptance criteria met | 3 | Packaging, CI, documentation, and automated assertions are present; required visual verification remains incomplete. |
| Test adequacy | 4 | Packaging assertions cover the principal requirements; execution evidence predates the test’s package move. |
| Scope and design fit | 5 | Changes remain confined to packaging, CI, documentation, the packaging test, and the report. |
| Consistency with repository conventions | 4 | Tests are included and whitespace checks pass; reported verification does not establish a check run after the package move. |

**Overall:** fail — The required visual confirmation of the packaged login window remains incomplete.

**Blocking:**

- `evals/implement-issue/reports/issue-122-release-jar.md:26`: Complete the explicit acceptance criterion by launching the release JAR from a temporary folder on this Mac, visually confirming the login window, and closing it; a running process alone does not satisfy this criterion.

**Non-blocking:**

- Refresh verification evidence after moving `ReleaseJarTest` to `gymmie.release`; the available XML result still names `gymmie.build.ReleaseJarTest`, so it does not verify the final source location.

Reviewed: 2026-09-29; base: upstream/master; model: gpt-6-astra; HEAD: 61e5f80; Uncommitted changes: yes.


### Round 3

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Resolvable release configurations without changing runtimeClasspath | 5 | `build.gradle` isolates runtime dependencies and all three JavaFX platform configurations; inspected archive contains the required natives. |
| releaseJar name, entry point, native access, and main output | 5 | `gymmie-release.jar` has both required manifest attributes, all 128 compiled main files, and all 18 application resources. |
| Release build in Ubuntu, macOS, and Windows CI | 5 | `.github/workflows/gradle.yml` adds `./gradlew releaseJar` within the existing three-platform matrix. |
| Developer Guide command and output | 5 | `docs/DeveloperGuide.md:51` documents the command and exact archive path. |
| Keep ./gradlew run working | 4 | Development configuration remains unchanged; report records reaching `:run`. Launch was not independently repeated. |
| Test release contents and configuration isolation | 5 | `ReleaseJarTest` checks manifest attributes, launcher, platform natives, SQLite natives, and configuration isolation; current-package XML records a passing test. |
| Visually confirm packaged login window from a temporary folder | 1 | Report explicitly states that the login window was never visually confirmed. |
| Acceptance criteria met | 3 | Build, packaging, CI, documentation, and automated checks are supported; required visual verification remains incomplete. |
| Test adequacy | 4 | Packaging assertions pass; existing results show 351 tests, 45 skipped, and no failures. GUI launch remains unverified. |
| Scope and design fit | 5 | Changes are confined to packaging configuration, CI, documentation, the packaging test, and the report. |
| Consistency with repository conventions | 5 | Changed-behavior tests are included; current-package results postdate the source move, Checkstyle reports contain no errors, and diff whitespace checks pass. Builds were not rerun during this read-only review. |

**Overall:** fail — The explicit acceptance criterion requiring visual confirmation of the packaged login window remains unmet.

**Blocking:**

- `evals/implement-issue/reports/issue-122-release-jar.md:26`: Launch the release JAR from a temporary folder on this Mac, visually confirm the login window, then close it and record the result. A process remaining alive does not establish that the required window opened.

**Non-blocking:** None.

Reviewed: 2026-09-29; base: upstream/master; model: gpt-6-astra; HEAD: 61e5f80; Uncommitted changes: yes.


### Round 4

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Resolvable release configurations without disrupting development | 5 | `build.gradle` isolates runtime dependencies and Windows, macOS aarch64, and Linux JavaFX configurations; required natives exist in the archive. |
| `releaseJar` name, entry point, native access, and main output | 5 | Inspected `gymmie-release.jar`: correct manifest attributes, all 128 compiled main files, and all 18 application resources. |
| Release build in Ubuntu, macOS, and Windows CI | 5 | The matrix runs the opt-in release test, which depends on `releaseJar`. |
| Developer Guide command and output | 5 | `docs/DeveloperGuide.md:51` documents the command, exact output path, and supported platforms. |
| Keep `./gradlew run` working | 4 | Development dependencies remain unchanged; the report records successful startup. Not independently relaunched. |
| Verify release contents in an opt-in test | 5 | `ReleaseJarTest` checks manifest attributes, launcher, and three-platform JavaFX/SQLite natives; current XML records one passing, unskipped test. |
| Confirm bundled JavaFX launches from a temporary folder on this Mac | 4 | The report records human confirmation resolving the smoke-test finding, with JDK-provided JavaFX hidden. Not independently repeated. |
| Acceptance criteria met | 5 | Current implementation, inspected artifacts, test results, and recorded human verification support all seven criteria. |
| Test adequacy | 4 | Opt-in packaging assertions pass and execute across the CI matrix; they check representative archive entries. |
| Scope and design fit | 5 | Changes remain confined to packaging, CI, documentation, the release test, and the implementation report. |
| Consistency with repository conventions | 5 | Added behavior tests; report records `check`; existing Checkstyle reports contain no errors; diff whitespace checks pass. Builds were not rerun during this read-only review. |

**Overall:** pass — No blocking defects, unmet criteria, or repository-convention violations were found.

**Blocking:** None.

**Non-blocking:**

- `evals/implement-issue/reports/issue-122-release-jar.md:22` still describes a direct `releaseJar` CI command, while CI now invokes the opt-in test; its self-assessment also retains the superseded statement that visual confirmation remains unavailable.

Reviewed: 2026-09-29; base: upstream/master; model: gpt-6-astra; HEAD: 61e5f80; Uncommitted changes: yes.

## Review responses

| Round | Finding | Fixed or rejected | Reason or change |
| --- | --- | --- | --- |
| 1 | Missing repeatable release packaging tests. | Fixed | Added `ReleaseJarTest`, configured Gradle to build the release archive before JUnit tests, and reran `./gradlew check` successfully. |
| 1 | No visual confirmation of login window. | Resolved by human check | Resolved by human check: with the JDK's built-in JavaFX hidden (--limit-modules java.se,jdk.unsupported,jdk.unsupported.desktop,jdk.localedata,jdk.charsets), the JAR loaded JavaFX from its own classes and natives and the app started on macOS aarch64. The earlier smoke run used a jdk+fx JDK, so it did not exercise the bundled JavaFX. |
| 2 | Verification evidence predates the tracked package move. | Fixed | Reran `./gradlew check`; `build/test-results/test/TEST-gymmie.release.ReleaseJarTest.xml` now corresponds to the tracked test class. |
| Follow-up | The release test made plain `test` and `check` depend on `releaseJar`. | Fixed | The test is now enabled only with `-PreleaseTests=true`; the release task dependency and JAR path are configured only when enabled. |
| Follow-up | The test included a separate release-platform isolation property and assertion. | Fixed | Removed the property, configuration list, and assertion; `./gradlew run` is the standard-runtime check. |
| 4 | Report described a direct releaseJar CI step and said visual confirmation remained unavailable. | Fixed | Updated the CI criterion and evidence to describe the opt-in test. The human macOS check confirmed the bundled JavaFX launch, and the self-assessment now records completion. |

## User corrections

| Correction | Change made |
| --- | --- |
| The test task should depend on `releaseJar` only for the opt-in release test. | Made `ReleaseJarTest` opt-in via `gymmie.releaseTests`, and conditionally configured the `test` task dependency and JAR path. |
| Remove the circular platform-isolation property and assertion. | Removed `gymmie.releasePlatformConfigurationsIsolated`, `releasePlatformConfigurations`, and its assertion; retained `./gradlew run` as the runtime check. |

## Notes for reflection

- **Where the skill helped:** Tracked each acceptance criterion through artifact inspection, verification, and the implementation report.
- **Where it needed guidance:** Neither the agent nor the reviewer noticed the smoke test ran on a JDK with JavaFX built in.
- **What to change:** Hide the JDK's built-in JavaFX when smoke-testing the release JAR so the test exercises its bundled classes and natives.
