# Implementation Report

- **Issue reference:** [#111](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/issues/111)
- **Title:** Separate cancelled bookings on My bookings (Member).
- **Date:** 2026-09-28
- **Branch:** implement-issue-111-cancelled-bookings
- **Base commit:** 52a8878468db760bc9e3d1bfcab2f5ccdcd1ae8b
- **Skill invocation:** implicit

## Summary

| Skills used | Verification passed on first run | Final verification result | Tests added or changed | Docs updated | Number of user corrections |
| --- | --- | --- | --- | --- | --- |
| implement-issue (implicit), update-user-guide (implicit) | no | Final `./gradlew check`, `./gradlew test -PuiTests=true`, `git diff --check`, and independent review round 2 passed before this docs-only follow-up; follow-up checks recorded below | Updated `MemberBookingTimeGroupingTest` and `MemberBookingsNavigationTest` | `docs/DeveloperGuide.md`, `docs/UserGuide.md` | 1 |

## Acceptance criteria

| Criterion | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation) |
| --- | --- | --- |
| Show active future bookings under Upcoming, soonest first; show active started bookings under Past, most recent first; classify a session starting exactly now as past. | completed | `MemberBookingTimeGroupingTest.classifiesBookingsBeforeAtAndAfterTheCurrentTime`; `MemberBookingsNavigationTest.dashboardBookingsSeparateUpcomingPastAndCancelledAndShowCancellationReasons`. |
| Show all cancelled bookings under Cancelled, newest session first, with cancellation and available Trainer reasons. | completed | `MemberBookingTimeGroupingTest` verifies cancelled sorting; the navigation test verifies all reason labels and the Trainer reason. |
| Show Cancel only for Upcoming. | completed | The navigation test verifies the Upcoming Cancel button and no buttons in Past or Cancelled. |
| Update tests and the User Guide. | completed | Both requested test classes updated; the Member booking section in `docs/UserGuide.md` now documents the three sections and cutoff. |

## Workflow checklist

- [x] Inspected relevant instructions, source, tests, documentation, conventions, and repository skills before editing.
- [x] Mapped criteria to intended changes and checks.
- [x] Kept changes in scope. Unexpected files and why: none.
- [x] Ran verification on the final code state; reran checks after relevant later edits.
- [x] Other tools used besides skills above: `gh issue view`, Git, Gradle, repository review script.
- [x] No commit, push, or external action occurred without authorization.

## Verification runs

| # | Command | Result | What was fixed next |
| --- | --- | --- | --- |
| 1 | `./gradlew check` | Failed Checkstyle due to one import-order violation in `MemberBookingTimeGroupingTest`. | Reordered the model imports. |
| 2 | `./gradlew check` | Passed. | none |
| 3 | `./gradlew test -PuiTests=true` | Passed. | none |
| 4 | `git diff --check` | Passed. | none |
| 5 | Independent review round 1 | Failed because the Upcoming sort order lacked a test with multiple future bookings. | Added three future bookings to the grouping test in deliberately unsorted input order and asserted ascending order. |
| 6 | `./gradlew check` after review fix | Passed. | none |
| 7 | `./gradlew test -PuiTests=true` after review fix | Passed; 354 tests, zero skipped, failed, or errored. | none |
| 8 | `git diff --check` after review fix | Passed. | none |
| 9 | Independent review round 2 | Passed with no blocking or non-blocking findings. | none |
| 10 | `./gradlew check` after human documentation review | Passed. | none |
| 11 | `./gradlew test -PuiTests=true` after human documentation review | Passed. | none |

## Self-assessment (self-assessed)

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Acceptance criteria met | 5 | Separate status-aware lists implement the requested classification and ordering. |
| Test adequacy | 5 | Unit and JavaFX coverage exercise exact-now behavior, all cancellation reasons, ordering, and action availability. |
| Scope and design fit | 5 | Reuses existing booking history data and cell rendering with a dedicated Cancelled list. |
| Consistency with repository conventions | 5 | `./gradlew check`, UI-enabled tests, and whitespace validation passed. |

## Independent review



### Round 1

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Active future bookings under Upcoming, soonest first; active started bookings under Past, newest first; exactly-now sessions are past | 4 | Controller implements all rules; unit tests cover the cutoff and Past ordering, but Upcoming ordering lacks coverage. |
| All cancelled bookings under Cancelled, newest session first, with cancellation and available Trainer reasons | 5 | Unit and navigation tests verify ordering, past/future cancellations, all four cancellation reasons, and Trainer text. |
| Cancel only for Upcoming | 5 | Cell factories enable cancellation only for Upcoming; navigation tests verify button presence and absence. |
| Update tests and the User Guide | 5 | Both booking test classes and the guide document and exercise the three sections. |
| Acceptance criteria met | 4 | Implementation matches the criteria; one explicit ordering requirement remains untested. |
| Test adequacy | 3 | Good boundary, cancellation, and interaction coverage, but both fixtures contain only one active future booking. |
| Scope and design fit | 5 | Focused controller/FXML changes reuse the existing history service and cell renderer without persistence changes. |
| Consistency with repository conventions | 5 | Shared UI conventions are preserved. Existing Checkstyle reports contain no errors; both relevant test reports show passes without skips. Whitespace validation passed; Gradle was not rerun during this read-only review. |

**Overall:** fail — Upcoming’s explicit soonest-first requirement lacks a test capable of detecting incorrect ordering.

**Blocking:**

- Add multiple active future bookings in deliberately unsorted order and assert their complete ascending order in [MemberBookingTimeGroupingTest.java](/Users/shawnnygoh/github-repos/CS3227-MP2/CS3227-2610-MP2/src/test/java/gymmie/member/MemberBookingTimeGroupingTest.java:25). Its sole Upcoming expectation contains one booking, and the navigation fixture also contains only one; removing or reversing the Upcoming comparator would leave both tests passing.

**Non-blocking:** None.

Reviewed: 2026-09-28; base: master; model: gpt-6-astra; HEAD: 52a8878; Uncommitted changes: yes.


### Round 2

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Active future bookings under Upcoming, soonest first; active started bookings under Past, newest first; exactly-now sessions are past | 5 | Status filters and comparators implement all rules. `MemberBookingTimeGroupingTest` verifies the cutoff and complete ordering, including three deliberately unsorted future bookings. |
| All cancelled bookings under Cancelled, newest session first, with cancellation and available Trainer reasons | 5 | Dedicated list includes cancellations regardless of session time. Tests verify descending order, all four cancellation reasons, and Trainer text. |
| Cancel only for Upcoming | 5 | Only Upcoming cells enable cancellation. Navigation coverage checks button availability, confirmation dismissal, and successful movement into Cancelled. |
| Update tests and the User Guide | 5 | Both booking test classes were updated; the guide describes all three sections, ordering, cutoff, reasons, and cancellation availability. |
| Acceptance criteria met | 5 | Current implementation and tests address every reported criterion. |
| Test adequacy | 5 | Deterministic grouping tests cover boundaries and ordering; JavaFX integration covers rendering and cancellation interactions. |
| Scope and design fit | 5 | Focused controller and FXML changes reuse existing history services and cell rendering. |
| Consistency with repository conventions | 5 | Shared UI conventions are preserved. Saved results show 354 tests passing without skips and no Checkstyle errors; whitespace validation passed. Gradle was not rerun during this read-only review. |

**Overall:** pass — The current change satisfies the acceptance criteria, with no blocking findings.

**Blocking:** None.

**Non-blocking:** None.

Reviewed: 2026-09-28; base: master; model: gpt-6-astra; HEAD: 52a8878; Uncommitted changes: yes.

## Review responses

| Round | Finding | Fixed or rejected | Reason or change |
| --- | --- | --- | --- |
| 1 | Upcoming soonest-first ordering lacked coverage capable of detecting an incorrect comparator. | Fixed | Added three future bookings in unsorted order and asserted the complete ascending sequence. |
| 2 | None. | — | Independent review passed. |

## User corrections

| User correction | Result |
| --- | --- |
| Human review found the stale Developer Guide description and the undocumented rebooking move, which the independent review missed. | Updated the Developer Guide to describe the three controller-grouped sections and full-history service contract; documented how rebooking moves a cancelled session's booking back to Upcoming and replaces its prior cancellation reason. |

## Notes for reflection

- **Where the skill helped:** Mapped each list's filtering, sorting, cancellation visibility, tests, and guide text to explicit acceptance criteria.
- **Where it needed guidance:** The independent review missed the stale Developer Guide description and the undocumented rebooking move; both were found in human review.
- **What to change:** The skill's documentation step should search both guides for the changed screen.
