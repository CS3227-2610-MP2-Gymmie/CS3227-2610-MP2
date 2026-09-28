# Implementation Report

- **Issue reference:** [#115](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/issues/115)
- **Title:** User Guide.
- **Date:** 2026-09-29.
- **Branch:** implement-issue-115-user-guide.
- **Base commit:** 61e5f8078310c1f5a5e3f53835a6435c4db28c11.
- **Skill invocation:** Explicit.

## Summary

| Skills used                                  | Verification passed on first run | Final verification result | Tests added or changed                  | Docs updated       | Number of user corrections |
| -------------------------------------------- | -------------------------------- | ------------------------- | --------------------------------------- | ------------------ | -------------------------- |
| implement-issue, update-user-guide, checkstyle-compliance-check | Yes                              | Passed: `./gradlew check` | None (documentation only)               | docs/UserGuide.md  | 0                          |

## Acceptance criteria

| Criterion                                                                                       | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation)                                                                                                                  |
| ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Accuracy: Username rules documented from Account model.                                         | completed                                                     | Documented in Sections 1.3, 5.4, and 8.1: 3–30 characters, `[A-Za-z0-9_-]`, globally unique across roles, matched case-insensitively, stored as typed.     |
| Accuracy: Membership purchase explains in-person payment handling at gym (no app payment).      | completed                                                     | Documented in Section 7.2 and 9: purchases record the membership immediately; payment handling and fee collection happen offline at gym reception desk.    |
| Accuracy: Known limitations documented (plan switching, payments, attendance, renewal date).    | completed                                                     | Documented in Section 9: no direct plan switching, no payments/revenue, no attendance check-in, renewal keeps original start date, rebooking overwrite.     |
| Readability: Feature table for each role with performance order.                                | completed                                                     | Added action tables to Sections 5.1 (Manager), 6.1 (Trainer), and 7.1 (Member) listing features in order of performance (create -> view -> edit -> delete).|
| Completeness: Quick start, Verify application, Saving data, and Resetting workspace sections.   | completed                                                     | Detailed in Sections 1 (Quick start & launch options), 3 (Automated & manual walkthroughs), and 8.4–8.6 (SQLite storage, safe inspection, workspace reset).  |
| Table of contents synchronized.                                                                 | completed                                                     | Fully verified all 41 section anchor links in table of contents matching headings.                                                                           |

## Workflow checklist

- [x] Inspected relevant instructions, source, tests, documentation, conventions, and repository skills before editing.
- [x] Mapped criteria to intended changes and checks.
- [x] Kept changes in scope. Unexpected files and why: none.
- [x] Ran verification on the final code state; reran checks after relevant later edits.
- [x] Other tools used besides skills above: GitHub CLI, Gradle, shell.
- [x] No commit, push, or external action occurred without authorization.

## Verification runs

| #   | Command           | Result  | What was fixed next |
| --- | ----------------- | ------- | ------------------- |
| 1   | `./gradlew check` | Passed. | None.               |

## Self-assessment (self-assessed)

| Area                                    | Score (1–5) | Evidence                                                                                                            |
| --------------------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------- |
| Acceptance criteria met                 | 5           | Addressed every bullet point under Accuracy, Readability, and Completeness from Issue #115.                         |
| Test adequacy                           | 5           | Documentation task; `./gradlew check` and all unit tests verified.                                                  |
| Scope and design fit                    | 5           | Updated UserGuide.md thoroughly while preserving visual layout and screenshot placeholders.                         |
| Consistency with repository conventions | 5           | Follows SE-EDU and Markdown formatting conventions.                                                                 |

## User corrections

| Correction | Change made |
| ---------- | ----------- |
| None       | None.       |

## Notes for reflection

- **Where the skill helped:** Guided accurate role-based structuring and verified table of contents synchronization.
- **Where it needed guidance:** None.
- **What to change:** None.
