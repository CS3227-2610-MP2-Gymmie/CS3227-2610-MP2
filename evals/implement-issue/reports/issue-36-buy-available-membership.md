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
| implement-issue (explicit), gymmie-ui-design (implicit), test-scaffold (implicit), update-user-guide (implicit) | no | `./gradlew check` and all opt-in JavaFX tests passed after the package move | 4 service tests relocated with coverage retained; 5 Member UI tests passed, including the issue #36 purchase case | `docs/UserGuide.md`, `docs/DeveloperGuide.md` | 2 |

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

## Self-assessment (self-assessed)

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Acceptance criteria met | 5 | Purchase listing, archived-plan recheck, term snapshots, and single-active-membership rule have service and UI evidence. |
| Test adequacy | 5 | Tests cover success, archive changes, authorization, duplicate active purchase, and the Member dashboard action. |
| Scope and design fit | 5 | Changes follow existing transaction, repository, Member aggregate, and JavaFX dashboard patterns. |
| Consistency with repository conventions | 5 | Member services, controller, FXML, CSS, and service tests now follow the role-specific paths in `docs/DeveloperGuide.md`; shared models and repositories remain common. |

## Independent review

A separate reviewer reviewed the second-commit refactor and reported no
findings. The reviewer confirmed role-specific service,
controller, FXML, CSS, and test placement; Router navigation; continued service
authorization; and guide accuracy. The reviewer also confirmed
`git diff --check HEAD` is clean.



## User corrections

The user corrected the proposed branch name to follow repository conventions. Existing branches use descriptive lowercase kebab-case names; the implementation branch is `implement-issue-36-buy-membership`.

The user corrected the Member code organization to match `docs/DeveloperGuide.md`. The #35/#36 membership services and service tests now live in the Member role package, and the membership workflow moved from the dashboard card into a routed Member screen with its own FXML and CSS. The dashboard retains a Member-only entry point. The user also requested no session logs for this correction; none were created.

## Notes for reflection

- **Where the skill helped:** The acceptance criteria led to matching service, UI, persistence, test, and report evidence instead of treating the domain model alone as a completed purchase flow.
- **Where it needed guidance:** The branch needed to match the repository's existing lowercase kebab-case names.
- **What to change:** None identified for this run.
