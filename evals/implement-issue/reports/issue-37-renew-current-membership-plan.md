# Implementation Report

- **Issue reference:** [#37](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/issues/37)
- **Title:** Renew current membership plan.
- **Date:** 2026-09-26
- **Branch:** `implement-issue-37-renew-current-membership-plan`
- **Base commit:** `731cbb7ee5f670a81f8f6e63895a7b522093bcf4`
- **Skill invocation:** explicit.

## Summary

| Skills used | Verification passed on first run | Final verification result | Tests added or changed | Docs updated | Number of user corrections |
| --- | --- | --- | --- | --- | --- |
| implement-issue (explicit), update-user-guide (implicit), test-scaffold (implicit) | no; initial compile, Checkstyle, UI expectations, and review findings were fixed | `./gradlew check` and `./gradlew test -PuiTests=true` passed on the final changes | 13 renewal service tests added; one status service test and four renewal-related UI tests added; expired-membership UI test updated | `docs/UserGuide.md`, `docs/DeveloperGuide.md` | 2 |

## Acceptance criteria

| Criterion | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation) |
| --- | --- | --- |
| Renewal extends from the later of today and the current expiry date. | completed | `renewalWithRemainingTimeExtendsFromExistingExpiry`, `expiredMembershipRenewalExtendsFromToday`, and `membershipExpiringTodayRenewsFromToday`. |
| Renewal cannot switch to a different plan. | completed | `holderCanRenewArchivedPlanWithoutChangingPlanOrPurchaseSnapshots` verifies the persisted plan ID and snapshots remain unchanged. `MembershipRenewalService.renew(membershipId)` validates the membership shown to the Member and accepts no plan choice. |
| An archived plan remains available for existing holders to renew. | completed | `holderCanRenewArchivedPlanWithoutChangingPlanOrPurchaseSnapshots`, `expiredArchivedMembershipRemainsRenewableWhenLaterMembershipWasCancelled`, `renewableMembershipShowsExpiredArchivedPlanAndSkipsFutureMembership`, and `memberCanRenewCurrentMembershipFromItsArchivedPlan` cover service and Member UI behavior. |

## Workflow checklist

- [x] Inspected relevant instructions, source, tests, documentation, conventions, and repository skills before editing.
- [x] Mapped criteria to intended changes and checks.
- [x] Kept changes in scope. Unexpected files and why: this implementation report is required by the issue workflow.
- [x] Ran verification on the final code state; reran checks after relevant later edits.
- [x] Other tools used besides skills above: `gh issue view`, Gradle compilation and Checkstyle, `git diff --check`.
- [x] Commit is authorized by the user; no push is authorized or planned.

## Verification runs

| # | Command | Result | What was fixed next |
| --- | --- | --- | --- |
| 1 | `./gradlew check` | Test compilation failed because the test called `AccountRepository.findByUsername` without its connection argument. | Added the transaction connection to the repository lookup. |
| 2 | `./gradlew check` | Checkstyle found two separator-wrap violations in the new UI test. | Reformatted the JavaFX lookup and action. |
| 3 | `./gradlew test -PuiTests=true` | 197 tests completed; an existing expired-history assertion failed because the membership is now shown as expired and renewable. | Updated the UI test to check expired-plan details and renewal availability. |
| 4 | `./gradlew check` | Checkstyle found one separator-wrap violation in the revised assertion. | Stored the looked-up labels before asserting. |
| 5 | `./gradlew check` | Passed after updating the expired-membership UI test. | None. |
| 6 | `./gradlew test -PuiTests=true` | Passed with 197 tests. | Independent review identified UI concurrency and failed-refresh state issues plus missing selection edge cases. |
| 7 | `./gradlew check` | Checkstyle found two separator-wrap violations in added JavaFX assertions. | Reworked the property lookups as block lambdas. |
| 8 | `./gradlew test -PuiTests=true` | 204 tests completed; the renewal UI test timed out waiting for purchase availability because its fixture was still active. | Changed its membership fixture to be expired so both actions are available before renewal begins. |
| 9 | `./gradlew check` | Passed with the expanded service and UI coverage. | None. |
| 10 | `./gradlew test -PuiTests=true` | Passed with 204 tests, including date boundaries, selection, ownership, rollback, archived renewal, and action coordination. | Independent review identified a refresh race that could re-enable renewal for stale membership details. |
| 11 | `./gradlew check` | Passed after renewal was tied to the displayed membership ID and refresh was disabled during mutations. | None. |
| 12 | `./gradlew test -PuiTests=true` | Passed with 204 tests, including the stale-action and refresh guards. | None. |
| 13 | Independent review | Found inconsistent cancelled-history selection and requested an archived-plan regression case. | Centralized target selection and filtered cancelled records before choosing the latest started membership. |
| 14 | `./gradlew check` | Passed after the selection fix and regression test. | None. |
| 15 | `./gradlew test -PuiTests=true` | Passed with 205 tests on the final implementation. | None. |
| 16 | `git diff --check` | Passed on the final changes. | None. |
| 17 | `./gradlew test -PuiTests=true` | The two new UI failure tests used the plural table name, but the schema table is `membership`; both failed during fixture setup. | Corrected the trigger and drop-table fixtures to use the schema's singular table name. |
| 18 | `./gradlew check` | Passed after adding failure-recovery UI coverage. | None. |
| 19 | `./gradlew test -PuiTests=true` | Passed with 207 tests, including renewal failure recovery, failed-refresh stale-action prevention, and successful renewal feedback. | None. |
| 20 | `git diff --check` | Passed on the final changes. | None. |
| 21 | `./gradlew check` | Passed after the UI failure fixtures were corrected. | None. |

## Self-assessment (self-assessed)

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Acceptance criteria met | 5 | Tests cover date boundaries, archived renewal, immutable plan snapshots, plan identity, and the Member renewal action using its displayed membership ID. |
| Test adequacy | 5 | Service tests cover authorization, Member ownership, cancellation, future/empty/mixed history, future overlap rejection, stale IDs, later cancelled history, and transaction failure rollback; JavaFX tests cover renewal, failure recovery, failed refresh, stale-action prevention, and competing-action guards. |
| Scope and design fit | 4 | Renewal uses the existing membership update boundary and keeps purchase separate. |
| Consistency with repository conventions | 5 | Member services remain in `gymmie.member.service`; Member UI and both guides use existing structure. |

## Independent review

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Renewal extends from the later of today and the current expiry date. | 5 | `Membership.renew()` implements this calculation; service tests cover future expiry, expired membership, and expiry today. |
| Renewal cannot switch to a different plan. | 5 | Renewal accepts only a membership ID; model and repository preserve plan identity and purchase snapshots. |
| An archived plan remains available for existing holders to renew. | 5 | Renewal does not require catalogue availability; service and UI tests cover archived plans. |
| Acceptance criteria met | 5 | All three criteria are implemented through the Member UI, authorized service, and persistence boundary. |
| Test adequacy | 4 | Covers ownership, authorization, overlap rejection, rollback, and UI failure recovery; missing the specific stale-selection regression below. Existing results show 13 renewal tests passing; UI tests are skipped in those artifacts. Tests were not rerun during this read-only review. |
| Scope and design fit | 5 | Reuses transactions, permissions, repository updates, and aggregate validation; shares target selection between display and renewal. |
| Consistency with repository conventions | 5 | Package placement, Javadoc, shared UI components, and guide updates follow conventions; existing Checkstyle reports contain zero violations and diff whitespace checks pass. |

**Overall:** pass — The acceptance criteria are satisfied, with one nonblocking test-coverage gap.

**Findings:** Missing regression test in `MembershipRenewalServiceTest`: display expired membership A, purchase membership B for the same Member, then attempt `renew(A.id())` and assert rejection with both records unchanged; the existing foreign-owner test does not cover this stale-selection scenario.

Reviewed: 2026-09-26 (base: upstream/master, model: gpt-6-astra)

## User corrections

The user asked that the task branch follow repository conventions. The branch
uses the existing `implement-issue-<number>-<slug>` naming pattern.

The user clarified that tests are part of implementation and authorized the
service and Member UI tests, both Gradle verification commands, and a commit
including this report. The user also reiterated that no session log should be
created yet; none was created.

## Notes for reflection

- **Where the skill helped:** The issue criteria were traced through model, service, Member UI, and user documentation, then checked with service and UI tests.
- **Where it needed guidance:** The user clarified branch naming conventions and that implementation work includes tests and verification.
- **What to change:** Keep existing UI expectations aligned with any intentional change in expired-membership behavior.
