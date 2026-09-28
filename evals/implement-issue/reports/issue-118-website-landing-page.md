# Implementation Report

- **Issue reference:** [#118](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/issues/118)
- **Title:** Create GitHub Pages website landing page.
- **Date:** 2026-09-29.
- **Branch:** implement-issue-118-website-landing-page.
- **Base commit:** 706756fd2366861f67f53d1eec5b4a8e268a0cbe.
- **Skill invocation:** Explicit.

## Summary

| Skills used                                  | Verification passed on first run | Final verification result | Tests added or changed                         | Docs updated  | Number of user corrections |
| -------------------------------------------- | -------------------------------- | ------------------------- | ---------------------------------------------- | ------------- | -------------------------- |
| implement-issue, checkstyle-compliance-check | Yes                              | Passed: `./gradlew check` | None (documentation/website landing page only) | docs/index.md | 0                          |

## Acceptance criteria

| Criterion                                                   | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation)                                                                                                                  |
| ----------------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Create GitHub Pages website landing page (`docs/index.md`). | completed                                                     | Created `docs/index.md` with project description, screenshot placeholder, guide links, role features table, quick start, project links, and team allocation. |
| Do not modify UserGuide.md or DeveloperGuide.md.            | completed                                                     | Verified clean working tree; only `docs/index.md` created in `docs/`.                                                                                        |

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
| Acceptance criteria met                 | 5           | Created `docs/index.md` adhering to the required structure, linking guides, roles, setup, and repository resources. |
| Test adequacy                           | 5           | Non-code documentation task; existing test suite and checkstyle pass.                                               |
| Scope and design fit                    | 5           | Surgical addition of landing page without touching active guides.                                                   |
| Consistency with repository conventions | 5           | Formatted according to GitHub Pages Markdown conventions.                                                           |

## User corrections

| Correction | Change made |
| ---------- | ----------- |
| None       | None.       |

## Notes for reflection

- **Where the skill helped:** Maintained scope isolation to prevent merge conflicts with ongoing concurrent documentation work.
- **Where it needed guidance:** None.
- **What to change:** None.
