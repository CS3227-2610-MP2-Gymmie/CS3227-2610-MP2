# Implementation Report

- **Issue reference:** [#41](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/issues/41)
- **Title:** View upcoming and past bookings.
- **Date:** 2026-09-26
- **Branch:** implement-issue-41-43-view-bookings
- **Base commit:** 440c353c0483537e8e8c0b96dffa8373a16fbd14
- **Skill invocation:** implicit

## Summary

| Skills used | Verification passed on first run | Final verification result | Tests added or changed | Docs updated | Number of user corrections |
| --- | --- | --- | --- | --- | --- |
| implement-issue (implicit), update-user-guide (implicit) | no | Final `./gradlew check`, `./gradlew test -PuiTests=true`, and `git diff --check` passed; independent review round 2 passed | Added `MemberBookingsNavigationTest` and `MemberBookingTimeGroupingTest`; updated membership and booking navigation tests | `docs/UserGuide.md`, `docs/DeveloperGuide.md` | 0 |

## Acceptance criteria

| Criterion | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation) |
| --- | --- | --- |
| The Member's booking view distinguishes upcoming activity from past activity. | completed | `MemberBookingsNavigationTest.dashboardBookingsSeparateUpcomingAndPastAndKeepCancellationReasons` checks each list contains only the matching session times. |
| Cancelled bookings remain visible in the booking list with their cancelled status. | completed | The same UI test loads cancelled rows into both lists and checks the status and reason text for all four supported reasons. |

## Workflow checklist

- [x] Inspected relevant instructions, source, tests, documentation, conventions, and repository skills before editing.
- [x] Mapped criteria to intended changes and checks.
- [x] Kept changes in scope. Unexpected files and why: none.
- [x] Ran verification on the final code state; reran checks after relevant later edits.
- [x] Other tools used besides skills above: `gh issue view`, Git, Gradle.
- [x] No commit, push, or external action occurred without authorization.

## Verification runs

| # | Command | Result | What was fixed next |
| --- | --- | --- | --- |
| 1 | `./gradlew check` | Failed on first compile due to removed membership plan imports; later failed Checkstyle on import order. | Restored required imports and reordered imports. |
| 2 | `./gradlew check` | Passed. | none |
| 3 | `./gradlew test -PuiTests=true` | Failed two UI tests: navigation lookup timing and a stale booking success message assertion. | Applied CSS after navigation and updated the expected message. |
| 4 | `./gradlew test -PuiTests=true` | Passed. | none |
| 5 | `./gradlew check` after review fixes | Passed. | none |
| 6 | `./gradlew test -PuiTests=true` after review fixes | Passed; 262 tests, zero skipped, failed, or errored. | none |
| 7 | `git diff --check` | Passed. | none |

## Self-assessment (self-assessed)

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Acceptance criteria met | 5 | Upcoming and past booking lists are date-classified and retain cancelled rows. |
| Test adequacy | 5 | UI integration coverage checks both groups, statuses, reasons, and dashboard navigation. |
| Scope and design fit | 5 | Reuses the existing booking history service and removes the duplicate membership-screen list. |
| Consistency with repository conventions | 5 | Checkstyle, standard checks, and opt-in UI tests passed. |

## Independent review



### Round 1

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| The Member's booking view distinguishes upcoming activity from past activity. | 4 | Separate lists and chronological sorting are implemented and tested, but exact-start classification contradicts the documented boundary. |
| Cancelled bookings remain visible in the booking list with their cancelled status. | 5 | No status filtering removes cancellations; the new UI test checks retained rows and all four cancellation reasons. |
| Acceptance criteria met | 4 | Both workflows are implemented; the start-time boundary needs correction. |
| Test adequacy | 3 | Covers dashboard navigation and both lists using cancelled bookings. Existing test XML marks this UI test skipped; reported UI execution was not independently verified. Gradle was not rerun because this review is read-only. |
| Scope and design fit | 5 | Reuses the authorized booking-history service and moves the existing presentation into a dedicated screen. |
| Consistency with repository conventions | 3 | Uses shared formatting, feedback, and background tasks, but the new router method omits required exception documentation. |

**Overall:** fail. The exact-start classification bug and explicit Javadoc convention violation require correction.

**Blocking:**

- [MemberBookingsController.java:72](/Users/shawnnygoh/github-repos/CS3227-MP2/CS3227-2610-MP2/src/main/java/gymmie/member/MemberBookingsController.java:72): A booking whose start equals `now` is classified as upcoming. `TrainingSession.hasStartedAt` and the Developer Guide explicitly include the exact start instant as already started. Use `startsAt.isAfter(now)` for upcoming and its complement for past; add deterministic before/equal/after coverage.
- [Router.java:106](/Users/shawnnygoh/github-repos/CS3227-MP2/CS3227-2610-MP2/src/main/java/gymmie/Router.java:106): `showMemberBookings()` declares `IOException` without documenting it. AGENTS.md requires an `@throws` entry explaining when each exception occurs. Document view-loading failure.

**Non-blocking:**

- [DeveloperGuide.md:257](/Users/shawnnygoh/github-repos/CS3227-MP2/CS3227-2610-MP2/docs/DeveloperGuide.md:257) still says booking history appears on the membership screen. Update it to describe the new **My bookings** destination.

Reviewed: 2026-09-26; base: upstream/master; model: gpt-6-astra; HEAD: 440c353; Uncommitted changes: yes.


### Round 2

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| The Member's booking view distinguishes upcoming activity from past activity. | 5 | Separate lists use complementary start-time predicates. `MemberBookingTimeGroupingTest` covers before, exactly at, and after the cutoff; the navigation test verifies both lists. |
| Cancelled bookings remain visible in the booking list with their cancelled status. | 5 | Grouping retains cancelled bookings. The navigation test verifies cancelled entries in both lists and all four cancellation reasons. |
| Acceptance criteria met | 5 | Both criteria are implemented and tested; exact-start classification matches the documented rule. |
| Test adequacy | 4 | Boundary and UI tests cover both criteria. Existing XML records successful execution without skips; refresh and failure recovery lack dedicated coverage. |
| Scope and design fit | 5 | Reuses the existing Member-authorized history service, shared UI components, and navigation patterns; removes the duplicate membership-screen list. |
| Consistency with repository conventions | 5 | Documentation and exception Javadoc are updated. Existing Checkstyle and test artifacts postdate the changes and show success. Gradle was not rerun during this read-only review. |

**Overall:** pass. The current change satisfies both acceptance criteria, with no blocking findings.

**Blocking:** None.

**Non-blocking:**

- Consider adding UI tests for refreshing existing lists and recovering from a loading failure; current UI coverage exercises initial loading only.

Reviewed: 2026-09-26; base: upstream/master; model: gpt-6-astra; HEAD: 440c353; Uncommitted changes: yes.

## Review responses

| Round | Finding | Fixed or rejected | Reason or change |
| --- | --- | --- | --- |
| 1 | Exact-start bookings were put in upcoming; Router `IOException` lacked Javadoc; Developer Guide referenced the membership screen. | Fixed | Start-time grouping now treats equality as past, deterministic boundary coverage was added, the exception is documented, and the Developer Guide describes **My bookings**. |
| 2 | Optional refresh and loading-failure UI tests. | Not added | The issue criteria are covered by initial-load and grouping tests; refresh and failure recovery are outside this issue's acceptance criteria. |

## User corrections

None.

## Notes for reflection

- **Where the skill helped:** Kept issue criteria, verification, reporting, and final review in one workflow.
- **Where it needed guidance:** The report template was present in the repository skill copy rather than at the initially referenced path.
- **What to change:** Keep the report template path consistent with the installed skill location.
