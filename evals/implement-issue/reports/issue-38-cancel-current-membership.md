# Implementation Report

- **Issue reference:** [#38](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/issues/38)
- **Title:** Cancel current membership plan.
- **Date:** 2026-09-26
- **Branch:** `implement-issue-38-cancel-membership`
- **Base commit:** `4d0a3afff4aee9f3cc3e2ff2ef0d997073acd63a`
- **Skill invocation:** explicit.

## Summary

| Skills used | Verification passed on first run | Final verification result | Tests added or changed | Docs updated | Number of user corrections |
| --- | --- | --- | --- | --- | --- |
| implement-issue (explicit), update-user-guide (implicit) | no; compilation and Checkstyle issues were corrected | `./gradlew check`, Member JavaFX UI tests, and `git diff --check` passed after review fixes | 3 cancellation service tests and 1 Member booking-history UI test added | `docs/UserGuide.md`, `docs/DeveloperGuide.md` | 0 |

## Acceptance criteria

| Criterion | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation) |
| --- | --- | --- |
| Cancellation marks the membership cancelled immediately and provides no refund. | completed | `cancellationImmediatelyUpdatesMembershipAndOnlyOwnFutureBookings` verifies `CANCELLED` persisted while purchase snapshots remain intact; the Member confirmation and success message state there is no refund. |
| All of the Member's future bookings are cancelled in the same transaction. | completed | `cancellationImmediatelyUpdatesMembershipAndOnlyOwnFutureBookings` verifies both future bookings for the signed-in Member are cancelled while past and other-Member bookings remain unchanged. The service uses one `UnitOfWork` transaction. |
| Affected bookings remain visible with a cancelled status and a reason that identifies membership cancellation. | completed | `bookingHistoryShowsCancelledStatusAndMembershipReason` verifies the Member screen renders a cancelled booking with the “Membership cancelled” reason. |
| If persistence fails, both membership and booking cancellations are rolled back. | completed | `bookingPersistenceFailureRollsBackMembershipAndEarlierBookingUpdates` forces a later booking update to fail and verifies both the membership and earlier booking update are rolled back. |

## Workflow checklist

- [x] Inspected issue requirements, repository instructions, source, tests, and documentation before editing.
- [x] Mapped the acceptance criteria to the cancellation service, Member screen, persistence tests, and user guide.
- [x] Created a task branch using the repository's `implement-issue-<number>-<slug>` naming convention.
- [x] Kept changes in scope. The implementation report is required by the issue workflow.
- [x] Ran verification on the final code state.
- [x] Independent review findings were fixed and sent through a second review round.
- [x] Corrected a report wording issue identified as non-blocking in round 2.
- [x] Issue implementation and report are committed as requested; session log is committed separately.

## Verification runs

| # | Command | Result | What was fixed next |
| --- | --- | --- | --- |
| 1 | `./gradlew check` | Failed on two Checkstyle indentation violations in `MemberMembershipController`. | Corrected the indentation. |
| 2 | `./gradlew check` | Passed; compile, Checkstyle, and tests completed. | None. |
| 3 | `git diff --check` | Passed. | None. |
| 4 | Independent review, round 1 | Failed: booking history was not displayed to Members, and confirmation bypassed `UiFeedback.confirm`. | Added a Member booking-history list and UI test; switched to the shared confirmation helper. |
| 5 | `./gradlew check` | First attempt found a checked exception in a stream mapping, a test compilation error, and Checkstyle findings. | Replaced the stream mapping with a checked-exception-safe loop; corrected the test observable and formatting/imports. |
| 6 | `./gradlew check` | Passed on final changes. | None. |
| 7 | `./gradlew test -PuiTests=true --tests gymmie.member.MemberMembershipNavigationTest` | Passed, including the new cancelled-booking history display test. | None. |
| 8 | `git diff --check` | Passed after final review fixes. | None. |

## Self-assessment (self-assessed)

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Acceptance criteria met | 5 | Immediate status update, future booking updates, retained cancellation reason, no refund, and rollback are represented in the service, Member screen, and tests. |
| Test adequacy | 5 | Tests cover authorization, stale selection, past/future bookings, other-Member isolation, visible cancellation reason, and rollback after a partial update. |
| Scope and design fit | 5 | Services follow existing authorization, repository, and `UnitOfWork` patterns; the UI coordinates cancellation with purchase and renewal actions and uses the shared owned confirmation dialog. |
| Consistency with repository conventions | 5 | Role-specific package placement, Javadoc, FXML, User Guide, Checkstyle, and whitespace checks follow repository conventions. |

## Independent review



### Round 1

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Cancellation marks the membership cancelled immediately and provides no refund. | 5 | `Membership.cancel()` preserves purchase terms and changes status to `CANCELLED`; the service persists it immediately. Confirmation explains no refund. |
| All of the Member's future bookings are cancelled in the same transaction. | 5 | One transaction updates membership and owned future bookings. Tests cover multiple bookings, past sessions, other Members, and existing cancellations. |
| Affected bookings remain visible with a cancelled status and a reason that identifies membership cancellation. | 2 | Records retain `CANCELLED` and `MEMBERSHIP_CANCELLED`, but no Member booking-history interface displays them. |
| If persistence fails, both membership and booking cancellations are rolled back. | 5 | A trigger-induced failure on a later booking tests rollback of the membership and an earlier booking update. |
| Acceptance criteria met | 3 | Persistence requirements are implemented; Member-visible cancelled booking history is missing. |
| Test adequacy | 3 | Three service tests cover cancellation, authorization, and rollback; none verifies booking visibility. Tests were inspected, not rerun under read-only constraints. |
| Scope and design fit | 4 | Service follows existing authorization, aggregate validation, repository, and transaction patterns. |
| Consistency with repository conventions | 3 | Whitespace checks pass, but the confirmation bypasses the documented owned, themed dialog helper. |

**Overall:** fail — Booking visibility is incomplete, and the confirmation dialog violates documented UI conventions.

**Blocking:**

- [MemberMembershipController.java:236](/Users/shawnnygoh/github-repos/CS3227-MP2/CS3227-2610-MP2/src/main/java/gymmie/member/MemberMembershipController.java:236): Cancellation displays only a count; there is no Member booking-history screen showing affected bookings and their reasons. Repository retention does not satisfy criterion 3 or Developer Guide UC2 step 7. Provide a reachable history display and a test verifying cancelled bookings remain visible with the membership-cancellation reason.
- [MemberMembershipController.java:209](/Users/shawnnygoh/github-repos/CS3227-MP2/CS3227-2610-MP2/src/main/java/gymmie/member/MemberMembershipController.java:209): The raw `Alert` has neither an owner nor shared styling. [DeveloperGuide.md:56](/Users/shawnnygoh/github-repos/CS3227-MP2/CS3227-2610-MP2/docs/DeveloperGuide.md:56) requires shared styling and `UiFeedback.confirm` for owned confirmation dialogs. Use that helper with the current window.

**Non-blocking:** None.

Reviewed: 2026-09-26; base: master; model: gpt-6-astra; HEAD: 4d0a3af; Uncommitted changes: yes.


### Round 2

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Cancellation marks the membership cancelled immediately and provides no refund. | 5 | `Membership.cancel()` preserves purchase snapshots and sets `CANCELLED`; the service persists it without refund operations. Confirmation explains no refund. |
| All of the Member's future bookings are cancelled in the same transaction. | 5 | One `UnitOfWork` transaction updates the membership and owned future bookings. Tests verify past, other-Member, and already-cancelled bookings remain unchanged. |
| Affected bookings remain visible with a cancelled status and a reason that identifies membership cancellation. | 5 | History queries retain cancellations; the Member list displays “Cancelled” and “Membership cancelled.” The UI test verifies both. |
| If persistence fails, both membership and booking cancellations are rolled back. | 5 | A trigger fails a later booking update; the test verifies rollback of the membership and an earlier booking update. |
| Acceptance criteria met | 5 | Current service, persistence, UI, and tests cover all four criteria. |
| Test adequacy | 4 | Service tests cover successful cancellation, isolation, authorization, stale selection, and rollback; UI coverage verifies history rendering. Tests inspected, not rerun under read-only constraints. |
| Scope and design fit | 5 | Changes follow existing service authorization, aggregate validation, transaction, dependency-injection, and asynchronous controller patterns. |
| Consistency with repository conventions | 5 | Uses shared confirmation, feedback, and formatting helpers; follows package and Javadoc conventions; updates the User Guide. `git diff --check` passes. |

**Overall:** pass — No blocking findings were identified in the current working tree, including untracked files.

**Blocking:** None.

**Non-blocking:**

- The report says `bookingHistoryShowsCancelledStatusAndMembershipReason` verifies the session time, but it asserts only status and reason. Add a formatted timestamp assertion or narrow the report’s claim.

Reviewed: 2026-09-26; base: master; model: gpt-6-astra; HEAD: 4d0a3af; Uncommitted changes: yes.


### Round 3

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Cancellation marks the membership cancelled immediately and provides no refund. | 5 | `Membership.cancel()` preserves purchase terms and sets `CANCELLED`; the service persists it without refund operations. Confirmation explains no refund. |
| All of the Member's future bookings are cancelled in the same transaction. | 5 | One `UnitOfWork` transaction updates the membership and owned future bookings. Tests verify multiple cancellations while preserving past, other-Member, and already-cancelled bookings. |
| Affected bookings remain visible with a cancelled status and a reason that identifies membership cancellation. | 5 | History queries retain cancelled bookings; the Member screen displays “Cancelled” and “Membership cancelled.” The UI test asserts both. |
| If persistence fails, both membership and booking cancellations are rolled back. | 5 | A trigger fails a later booking update; the test verifies rollback of the membership and an earlier booking update. |
| Acceptance criteria met | 5 | Service, persistence, UI wiring, and tests address all four criteria. |
| Test adequacy | 4 | Tests cover successful cancellation, ownership isolation, stale selection, authorization, rollback, and history rendering. Tests were inspected, not rerun under read-only constraints. |
| Scope and design fit | 5 | Uses existing authorization, aggregate validation, transaction, dependency injection, and asynchronous controller patterns. |
| Consistency with repository conventions | 5 | Uses shared confirmation, feedback, and formatting helpers; follows Javadoc conventions and updates the User Guide. `git diff --check` passes. |

**Overall:** pass — No blocking findings were identified in the current working-tree changes, including untracked files.

**Blocking:** None.

**Non-blocking:** None.

Reviewed: 2026-09-26; base: master; model: gpt-6-astra; HEAD: 4d0a3af; Uncommitted changes: yes.

## User corrections

None.
