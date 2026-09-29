# Implementation Report

- **Issue reference:** User-provided acceptance criteria (no issue number)
- **Title:** Member booking visibility on Trainer deactivation and reactivation
- **Date:** 2026-09-29
- **Branch:** trainerSessions
- **Base commit:** d45a2b6553f9ff90b675608e633e64539feefc61
- **Skill invocation:** implicit

## Summary

| Skills used | Verification passed on first run | Final verification result | Tests added or changed | Docs updated | Number of user corrections |
| --- | --- | --- | --- | --- | --- |
| implement-issue, test-scaffold, update-user-guide (implicit) | No: sandbox cache access and test formatting | Passed: check with all UI tests enabled | TrainerBookingVisibilityTest; MemberBookingsNavigationTest | docs/UserGuide.md; docs/DeveloperGuide.md; guide log | 0 |

## Acceptance criteria

| Criterion | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation) |
| --- | --- | --- |
| Deactivation moves upcoming reservations to Cancelled | completed | trainerToggleMovesOnlyUpcomingBookingsAndPreservesStoredHistory |
| Reactivation restores eligible upcoming reservations | completed | Repeated toggles; reactivationAfterStartDoesNotRestoreExpiredBookingToUpcoming |
| Historical bookings and explicit cancellations unchanged | completed | Stored record equality; explicitCancellationWhileTrainerInactiveIsNotRestored |
| Unit/integration checks pass | completed | ./gradlew check test -PuiTests=true passed, including refreshReflectsTrainerDeactivationAndReactivation |

## Workflow checklist

- [x] Inspected relevant instructions, source, tests, documentation, conventions, and repository skills before editing.
- [x] Mapped criteria to intended changes and checks.
- [x] Kept changes in scope. Existing untracked .DS_Store files untouched.
- [x] Ran verification on the final code state; reran checks after relevant later edits.
- [x] Other tools used besides skills above: shell, Git, Gradle, independent review script.
- [x] No commit, push, or external action occurred without authorization.

## Verification runs

| # | Command | Result | What was fixed next |
| --- | --- | --- | --- |
| 1 | Targeted TrainerBookingVisibilityTest | Passed after retry with Gradle cache access | None |
| 2 | ./gradlew check | Test formatting violations | Wrapped long lines and assigned Button before firing |
| 3 | ./gradlew test -PuiTests=true | Overlapping Gradle runs collided in test-result output (EOF / missing binary result file) | Rerun check and UI tests in one Gradle invocation |
| 4 | ./gradlew check test -PuiTests=true | Passed (style, full tests and UI tests) | None |
| 5 | Independent review round 1 | Blocking UI cancellation regression | Restored cancellation action and added UI test; corrected Developer Guide |
| 6 | ./gradlew check test -PuiTests=true | Passed after review fixes: 370 tests, 1 skipped, 0 failures/errors; clean style | None |
| 7 | Independent review round 2 | Overall pass; no blocking findings | Clarified non-blocking glossary omission; no code changes after verification |

## Self-assessment (self-assessed)

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Acceptance criteria met | 4 | Dynamic read projection preserves stored reservations and history |
| Test adequacy | 4 | Real SQLite/Manager lifecycle integration, cutoff, repeat toggles, cancellation, RBAC, rendered UI |
| Scope and design fit | 5 | No schema changes or destructive booking mutations |
| Consistency with repository conventions | 4 | Uses existing service transaction, Clock and JavaFX testing patterns |

## Independent review



### Round 1

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Deactivation moves upcoming reservations to Cancelled | 5 | Service projects eligible reservations as cancelled; lifecycle and rendered UI tests cover the transition. |
| Reactivation restores eligible upcoming reservations | 5 | Tests cover repeated toggles, unaffected Trainers, and reactivation at the session-start cutoff. |
| Historical bookings and explicit cancellations unchanged | 4 | Stored-record equality and cancellation tests verify preservation, but Members lose access to explicit cancellation while the Trainer is inactive. |
| Unit/integration checks pass | 4 | Existing results show 369 tests, zero failures/errors, one skipped, and clean Checkstyle reports. Both affected test classes ran; checks were not rerun during this read-only review. |
| Acceptance criteria met | 4 | Reported transitions work, with a cancellation workflow regression. |
| Test adequacy | 3 | Good lifecycle and UI refresh coverage; the inactive-Trainer cancellation test calls the service directly and misses the inaccessible UI action. |
| Scope and design fit | 4 | Small transactional read projection preserves storage and capacity, but presentation must distinguish reversible unavailability from permanent cancellation. |
| Consistency with repository conventions | 4 | Follows service, Clock, Javadoc, and test patterns; Developer Guide descriptions remain outdated. |

**Overall:** fail — Projecting retained reservations as cancelled removes Members’ ability to explicitly cancel them before automatic restoration.

**Blocking:**

- [MemberBookingHistoryService.java:71](/Users/taiayovelapang/Desktop/NUS/CS3227/MP2/src/main/java/gymmie/member/service/MemberBookingHistoryService.java:71): After Trainer deactivation and refresh, the projected `CANCELLED` status reaches the controller’s cancelled-card branch, which always hides **Cancel booking**. The reservation remains stored as `BOOKED`, so Members cannot release it or prevent its restoration while the Trainer is inactive. Preserve a cancellation action for these future reservations and test cancellation through the UI; `explicitCancellationWhileTrainerInactiveIsNotRestored` currently bypasses this regression by invoking the service directly.

**Non-blocking:**

- [DeveloperGuide.md:1057](/Users/taiayovelapang/Desktop/NUS/CS3227/MP2/docs/DeveloperGuide.md:1057) still states that affected bookings remain **Booked** in **My bookings**, contradicting the new behavior and updated User Guide.

Reviewed: 2026-09-29; base: d45a2b6553f9ff90b675608e633e64539feefc61; model: gpt-6-astra; HEAD: d45a2b6; Uncommitted changes: yes.


### Round 2

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Deactivation moves upcoming reservations to Cancelled | 5 | Service projects future bookings for inactive Trainers as cancelled; lifecycle and UI refresh tests verify the transition and reason. |
| Reactivation restores eligible upcoming reservations | 5 | Tests cover repeated toggles, unaffected Trainers, and the exact session-start cutoff. |
| Historical bookings and explicit cancellations unchanged | 5 | Stored-record equality verifies preservation; service and UI tests confirm explicit cancellation prevents restoration. |
| Unit/integration checks pass | 4 | Saved results show 370 tests, one skipped, no failures/errors, and zero Checkstyle violations. Both affected classes executed; checks were not rerun during this read-only review. |
| Acceptance criteria met | 5 | Current implementation and tests cover all four criteria, including cancellation while the Trainer is inactive. |
| Test adequacy | 5 | SQLite lifecycle tests cover boundaries, cancellation reasons, storage preservation, and RBAC; JavaFX tests cover refresh and cancellation. |
| Scope and design fit | 5 | Small transactional read projection preserves reservations without schema changes or destructive mutations. |
| Consistency with repository conventions | 4 | Changes follow existing service, Clock, Javadoc, and UI testing patterns; documentation has one minor omission. |

**Overall:** pass — No blocking defects, unmet acceptance criteria, or missing criterion coverage were found.

**Blocking:** None.

**Non-blocking:**

- [DeveloperGuide.md:1034](/Users/taiayovelapang/Desktop/NUS/CS3227/MP2/docs/DeveloperGuide.md:1034): The **Cancelled booking** glossary definition omits temporary Trainer deactivation, although the updated known-limitations section explains it.

Reviewed: 2026-09-29; base: d45a2b6553f9ff90b675608e633e64539feefc61; model: gpt-6-astra; HEAD: d45a2b6; Uncommitted changes: yes.

## Review responses

| Round | Finding | Fixed or rejected | Reason or change |
| --- | --- | --- | --- |
| 1 | Inactive Trainer card hid explicit cancellation | Fixed | Preserve Cancel booking for display-only Trainer unavailability; added memberCanPermanentlyCancelWhileTrainerIsInactive UI test |
| 1 | Developer Guide stale behavior | Fixed | Updated known limitation to explain projection and restoration |
| 2 | Glossary omitted temporary Trainer deactivation | Fixed | Added temporary display-status explanation to Cancelled booking glossary |

## User corrections

None.

## Notes for reflection

- **Where the skill helped:** Explicit checks for boundaries, documentation and independent review.
- **Where it needed guidance:** No numbered issue; used a descriptive report filename.
- **What to change:** None.
- **Behavior boundary:** Display status is recalculated on page load or Refresh, matching existing UI refresh behavior. Once a session starts it is historical, and Trainer toggles do not alter it. Reservations retain capacity while the Trainer is unavailable; explicit cancellations still release it. No separate notification system exists for this screen, so its cancellation reason provides the status explanation.
