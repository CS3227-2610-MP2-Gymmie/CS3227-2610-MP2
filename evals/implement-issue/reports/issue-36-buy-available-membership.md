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
| implement-issue (explicit), gymmie-ui-design (implicit), test-scaffold (implicit), update-user-guide (implicit) | no | `./gradlew check` and all opt-in JavaFX tests passed after the purchase guard and readiness wait | 4 service tests relocated with coverage retained; 5 Member UI tests passed, including the issue #36 purchase case and plan-change guard | `docs/UserGuide.md`, `docs/DeveloperGuide.md` | 3 |

## Acceptance criteria

| Criterion | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation) |
| --- | --- | --- |
| Archived plans are removed from new-purchase choices. | completed | `availablePlansExcludesArchivedPlansAndRequiresMemberRole`, `rejectsPlanArchivedAfterItWasListed`, and `memberCanPurchaseVisiblePlanAndArchivedPlansAreHidden`. The service uses `findAllAvailable` and rechecks the chosen plan in the purchase transaction. |
| The membership keeps the plan's SGD price and duration copied at purchase time. | completed | `purchaseActivatesMemberAndKeepsPurchaseTimeTerms` verifies snapshots remain after catalogue edits; dashboard integration verifies price and duration snapshots on a purchase. |
| A Member holds at most one active membership at a time. | completed | `rejectsArchivedPlansAndSecondActivePurchaseWithoutChangingHistory`; the service checks current coverage and validates the new record through `Member.withMembership` before inserting. |

## Workflow checklist

- [x] Inspected relevant instructions, source, tests, documentation, conventions, and repository skills before editing.
- [x] Mapped criteria to intended changes and checks.
- [x] Kept changes in scope. Unexpected files and why: implementation report required by the issue workflow.
- [x] Ran verification on the final code state; reran checks after relevant later edits.
- [x] Other tools used besides skills above: `gh issue view` to read issue #36.
- [x] Created the two requested focused commits; no push or GitHub write occurred.

## Verification runs

| # | Command | Result | What was fixed next |
| --- | --- | --- | --- |
| 1 | `./gradlew test --tests 'gymmie.service.MembershipPurchaseServiceTest'` | Initial run failed to compile two test assertions because a checked transaction lambda was ambiguous with JUnit's `BooleanSupplier` overload. | Stored transaction results in local variables before asserting. |
| 2 | `./gradlew test --tests 'gymmie.service.MembershipPurchaseServiceTest'` | Passed. | None. |
| 3 | `./gradlew check` | Initial run found one Checkstyle import-order violation. | Reordered the imports. |
| 4 | `./gradlew check` | Passed on the final implementation. | None. |
| 5 | `./gradlew test -PuiTests=true --tests 'gymmie.member.MemberMembershipNavigationTest'` | An early UI run exposed a timing issue while membership status was still loading; after the dashboard fix, all 5 tests passed on the final implementation. | Keep purchase disabled until current membership status is loaded. |
| 6 | `git diff --check` | Passed. | None. |
| 7 | `./gradlew check` after moving Member tests | The first run found tests relying on package-private `UserSession` methods and one Checkstyle separator-wrap violation. | Changed test setup to authenticate through `AuthService` and corrected the wrapping. |
| 8 | `./gradlew check` after the package move | Passed. | None. |
| 9 | `./gradlew test -PuiTests=true` | Passed all opt-in JavaFX integration tests after the screen move. | None. |
| 10 | `./gradlew check` after the independent-review fixes | The first run found a Checkstyle separator-wrap violation in the updated UI test. | Adjusted the lambda formatting. |
| 11 | `./gradlew check` | Passed after the formatting fix. | None. |
| 12 | `./gradlew test -PuiTests=true` | Passed all opt-in JavaFX integration tests, including readiness synchronization and plan selection during a purchase. | None. |

## Self-assessment (self-assessed)

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Acceptance criteria met | 5 | Purchase listing, archived-plan recheck, term snapshots, and single-active-membership rule have service and UI evidence. |
| Test adequacy | 5 | Tests cover success, archive changes, authorization, duplicate active purchase, and the Member dashboard action. |
| Scope and design fit | 5 | Changes follow existing transaction, repository, Member aggregate, and JavaFX dashboard patterns. |
| Consistency with repository conventions | 5 | Reassessed after the role-layout correction: Member services, controller, FXML, CSS, and tests follow the role-specific paths in `docs/DeveloperGuide.md`; shared models and repositories remain common, and `git diff --check` passes. |

## Independent review

| Criterion | Score | Evidence |
| --- | --- | --- |
| Acceptance criteria met | **5/5** | Archived plans are filtered and rechecked transactionally; purchase terms are persisted as snapshots; active-membership and overlap checks reject duplicate coverage, with corresponding tests. |
| Test adequacy | **4/5** | Service tests cover the criteria and authorization, but the UI test (`src/test/java/gymmie/member/MemberMembershipNavigationTest.java`) can assert purchase readiness before the independent membership-status task finishes. |
| Scope and design fit | **4/5** | Services follow existing transaction and aggregate patterns, but purchase availability (`src/main/java/gymmie/member/MemberMembershipController.java`) lacks an in-progress guard, so changing plans during a purchase can re-enable submission. |
| Consistency with repository conventions | **5/5** | Member services, tests, controller, FXML and CSS follow role-specific paths; shared persistence stays common, guides are updated, and `git diff --check` passes. |

Both findings were fixed in `d280686`; see User corrections.

## User corrections

The user corrected the proposed branch name to follow repository conventions. Existing branches use descriptive lowercase kebab-case names; the implementation branch is `implement-issue-36-buy-membership`.

The user corrected the Member code organization to match `docs/DeveloperGuide.md`. The #35/#36 membership services and service tests now live in the Member role package, and the membership workflow moved from the dashboard card into a routed Member screen with its own FXML and CSS. The dashboard retains a Member-only entry point. The user also requested no session logs for that correction; none were created then.

The independent review identified two defects missed by the self-assessment: changing the selected plan could re-enable purchase during the background request, and the purchase UI test did not wait for the separate membership-status task. Added an in-progress guard honored by plan refreshes and selection changes, made the UI test wait for readiness, and asserted the button stays disabled when the plan changes mid-purchase. Both requested verification commands passed.

## Notes for reflection

- **Where the skill helped:** The acceptance criteria led to matching service, UI, persistence, test, and report evidence instead of treating the domain model alone as a completed purchase flow.
- **Where it needed guidance:** The initial Member implementation needed guidance to follow the role package layout documented in `docs/DeveloperGuide.md`.
- **What to change:** Always run the independent review before committing the report.
