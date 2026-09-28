# Implementation Report

- **Issue reference:** [#109](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/issues/109)
- **Title:** Enter plan prices in dollars, not cents.
- **Date:** 2026-09-29.
- **Branch:** implement-issue-109-plan-prices-dollars.
- **Base commit:** 9d42181467475f488667ce19d28bb41f237efb9d.
- **Skill invocation:** Explicit.

## Summary

| Skills used                                                     | Verification passed on first run | Final verification result                                     | Tests added or changed                                                        | Docs updated      | Number of user corrections |
| --------------------------------------------------------------- | -------------------------------- | ------------------------------------------------------------- | ----------------------------------------------------------------------------- | ----------------- | -------------------------- |
| implement-issue, update-user-guide, checkstyle-compliance-check | Yes                              | Passed: `./gradlew check` and `./gradlew test -PuiTests=true` | ManagerPlansControllerTest; ManagerPlansNavigationTest; DisplayFormattersTest | docs/UserGuide.md | 0                          |

## Acceptance criteria

| Criterion                                                                                                                | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation)                                                                                                                                                                      |
| ------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| The input is SGD dollars with at most 2 decimal places, e.g. "50" or "49.90". Reject negatives and more than 2 decimals. | completed                                                     | ManagerPlansControllerTest.parsePriceCentsConvertsFiftyDollarsToCents, parsePriceCentsConvertsFortyNineNinetyToCents, parsePriceCentsRejectsNegativeValues, parsePriceCentsRejectsMoreThanTwoDecimalPlaces.      |
| Editing shows dollars ("49.90"). Storage stays in cents.                                                                 | completed                                                     | ManagerPlansNavigationTest.editPlanLoadsExistingValuesAndSavesUpdates checks "30.00" on load and saves 6000 cents from "60.00"; DisplayFormattersTest checks DisplayFormatters.dollars(4990) -> "49.90".         |
| The label and prompt text say dollars. Tests for 50, 49.90, 0, -1 and 49.999.                                            | completed                                                     | Plans.fxml label updated to "Price in SGD dollars (e.g. 49.90)" and promptText "e.g. 50 or 49.90"; ManagerPlansNavigationTest checks promptText; ManagerPlansControllerTest covers 50, 49.90, 0, -1, and 49.999. |

## Workflow checklist

- [x] Inspected relevant instructions, source, tests, documentation, conventions, and repository skills before editing.
- [x] Mapped criteria to intended changes and checks.
- [x] Kept changes in scope. Unexpected files and why: none.
- [x] Ran verification on the final code state; reran checks after relevant later edits.
- [x] Other tools used besides skills above: GitHub CLI, Gradle, shell.
- [x] No commit, push, or external action occurred without authorization.

## Verification runs

| #   | Command                         | Result  | What was fixed next |
| --- | ------------------------------- | ------- | ------------------- |
| 1   | `./gradlew check`               | Passed. | None.               |
| 2   | `./gradlew test -PuiTests=true` | Passed. | None.               |

## Self-assessment (self-assessed)

| Area                                    | Score (1–5) | Evidence                                                                                                                                                                              |
| --------------------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acceptance criteria met                 | 5           | All criteria met: dollars input, at most 2 decimal places, negative rejection, editing in dollars, storage in cents, label/prompt in dollars, and tests for 50, 49.90, 0, -1, 49.999. |
| Test adequacy                           | 5           | Dedicated unit tests for parsePriceCents edge cases, DisplayFormatters dollar tests, and UI integration test updates.                                                                 |
| Scope and design fit                    | 5           | Minimal, surgical changes to ManagerPlansController, DisplayFormatters, Plans.fxml, docs/UserGuide.md, and tests.                                                                     |
| Consistency with repository conventions | 5           | Checkstyle clean, Javadoc conventions followed, all tests pass.                                                                                                                       |

## User corrections

| Correction | Change made |
| ---------- | ----------- |
| None       | None.       |

## Notes for reflection

- **Where the skill helped:** Clear acceptance criteria mapping ensured all required test inputs (50, 49.90, 0, -1, 49.999) and documentation changes were verified.
- **Where it needed guidance:** None.
- **What to change:** None.
