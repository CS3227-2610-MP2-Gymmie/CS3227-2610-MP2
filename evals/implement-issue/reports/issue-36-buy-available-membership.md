# Implementation Report

- **Issue reference:** [#36](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/issues/36)
- **Title:** Buy an available membership plan.
- **Date:** 2026-09-26
- **Branch:** `implement-issue-36-buy-membership`
- **Base commit:** `c4f9175`
- **Skill invocation:** explicit.

## Summary

| Skills used | Verification passed on first run | Final verification result | Tests added or changed | Docs updated | Number of user corrections |
| --- | --- | --- | --- | --- | --- |
| implement-issue (explicit), gymmie-ui-design (implicit), test-scaffold (implicit), update-user-guide (implicit) | no | `./gradlew check` and opt-in Member JavaFX tests passed | 4 service tests; 1 Member dashboard integration test | `docs/UserGuide.md` | 1 |

## Acceptance criteria

| Criterion | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation) |
| --- | --- | --- |
| Archived plans are removed from new-purchase choices. | completed | `availablePlansExcludesArchivedPlansAndRequiresMemberRole`, `rejectsPlanArchivedAfterItWasListed`, and `memberCanPurchaseVisiblePlanAndArchivedPlansAreHidden`. The service uses `findAllAvailable` and rechecks the chosen plan in the purchase transaction. |
| The membership keeps the plan's SGD price and duration copied at purchase time. | completed | `purchaseActivatesMemberAndKeepsPurchaseTimeTerms` verifies snapshots remain after catalogue edits; dashboard integration verifies price and duration snapshots on a purchase. |
| A Member holds at most one active membership at a time. | completed | `rejectsArchivedPlansAndSecondActivePurchaseWithoutChangingHistory`; the service checks current coverage and validates the new record through `Member.withMembership` before inserting. |

## Workflow checklist

- [x] Inspected relevant instructions, source, tests, documentation, conventions, and repository skills before editing.
- [x] Mapped criteria to intended changes and checks.
- [x] Kept changes in scope. Unexpected files and why: implementation report and required User Guide interaction log.
- [x] Ran verification on the final code state; reran checks after relevant later edits.
- [x] Other tools used besides skills above: `gh issue view` to read issue #36.
- [x] No commit, push, or external write action occurred.

## Verification runs

| # | Command | Result | What was fixed next |
| --- | --- | --- | --- |
| 1 | `./gradlew test --tests 'gymmie.service.MembershipPurchaseServiceTest'` | Initial run failed to compile two test assertions because a checked transaction lambda was ambiguous with JUnit's `BooleanSupplier` overload. | Stored transaction results in local variables before asserting. |
| 2 | `./gradlew test --tests 'gymmie.service.MembershipPurchaseServiceTest'` | Passed. | None. |
| 3 | `./gradlew check` | Initial run found one Checkstyle import-order violation. | Reordered the imports. |
| 4 | `./gradlew check` | Passed on the final implementation. | None. |
| 5 | `./gradlew test -PuiTests=true --tests 'gymmie.member.MemberMembershipNavigationTest'` | An early UI run exposed a timing issue while membership status was still loading; after the dashboard fix, all 5 tests passed on the final implementation. | Keep purchase disabled until current membership status is loaded. |
| 6 | `git diff --check` | Passed. | None. |

## Self-assessment (self-assessed)

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Acceptance criteria met | 5 | Purchase listing, archived-plan recheck, term snapshots, and single-active-membership rule have service and UI evidence. |
| Test adequacy | 5 | Tests cover success, archive changes, authorization, duplicate active purchase, and the Member dashboard action. |
| Scope and design fit | 5 | Changes follow existing transaction, repository, Member aggregate, and JavaFX dashboard patterns. |
| Consistency with repository conventions | 5 | `./gradlew check` passes; User Guide and interaction log follow local structure. |

## Independent review

Reviewed the final diff against all three issue criteria. Purchase authorization and state checks happen within one transaction; the plan is reloaded to reject an archived offering, and membership history is validated before insertion. Existing archived-plan history remains readable. The Member-only dashboard shows price and duration, refreshes available plans, and reports purchase success. Full visual inspection outside the JavaFX integration test was not performed.

## User corrections

The user corrected the proposed branch name to follow repository conventions. Existing branches use descriptive lowercase kebab-case names; the implementation branch is `implement-issue-36-buy-membership`.

## Notes for reflection

- **Where the skill helped:** The acceptance criteria led to matching service, UI, persistence, test, and report evidence instead of treating the domain model alone as a completed purchase flow.
- **Where it needed guidance:** The branch needed to match the repository's existing lowercase kebab-case names.
- **What to change:** None identified for this run.
