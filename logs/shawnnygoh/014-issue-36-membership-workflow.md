# Issue 36 Membership Workflow

## Goal
Implement issue #36 so Members can buy available membership plans, then align the #35/#36 Member services and UI with the role-specific package layout in `docs/DeveloperGuide.md`.

## Scope
Six user-assistant exchanges for one focused issue implementation, architecture correction, review fixes, and report updates.

## Key prompts
- “Use $implement-issue to implement issue #36.” This set the acceptance criteria and required implementation evidence and a report.
- “The branch name should follow repo conventions” This corrected the proposed slash-prefixed branch; local branches used lowercase descriptive kebab-case names.
- “In a second commit, move the Member features from #35 and #36 into gymmie.member following the Trainer layout.” This required keeping the initial feature commit intact and moving services, UI, tests, and resources separately.
- “Can you add this to the report's independent review section instead:” This replaced the earlier review summary with the supplied scored criteria table; tests were not rerun for that report-only change.
- “The independent review found two real issues. Fix them in a third commit:” This required an in-progress purchase guard, a readiness wait in the UI test, a UI assertion where practical, and both full verification runs.
- “Then, in a fourth commit, update the report and the log:” This kept the review fixes and documentation updates in separate commits and specified the report and log corrections.

## Decisions and corrections
The initial request to create a `codex/` branch was rejected. After checking repository branch names, created `implement-issue-36-buy-membership` and proceeded there.

Created two focused commits: `9ba415a` added the issue #36 purchase flow and report; `0963f69` moved the Member status and purchase services and tests into `gymmie.member`, added a routed Member membership screen, and updated the User and Developer Guides. Full `./gradlew check` and opt-in JavaFX tests passed after the move. An initial separate reviewer reported no findings; the subsequent scored independent review supplied by the user identified two real issues missed by the self-assessment: a plan change could re-enable purchase during an in-flight request, and the UI test did not wait for membership status to load.

The review table was initially added as a report-only edit. On follow-up, the user clarified that its two findings required code and test fixes. Committed those as `d280686`: the controller now tracks purchase progress so plan selection and refresh cannot re-enable the button, and the UI test waits for readiness and checks the button after changing plans during purchase. `./gradlew check` and `./gradlew test -PuiTests=true` both passed. Updated the report to record the fixes and verification, use repository-relative review links, reassess convention consistency, and note the role-layout guidance and missed findings. This log is updated in the requested fourth commit.

## Files created or modified
- `src/main/java/gymmie/AppContext.java`, `DashboardController.java`, `Router.java`, and `member/MemberMembershipController.java`.
- `src/main/java/gymmie/member/service/MembershipStatusService.java` and `MembershipPurchaseService.java` (moved from `gymmie.service`).
- `src/main/java/gymmie/model/Membership.java`, `src/main/java/gymmie/persistence/repository/MembershipRepository.java`, and `src/main/java/gymmie/persistence/sqlite/SqliteMembershipRepository.java`.
- `src/main/resources/gymmie/view/Dashboard.fxml`; new `src/main/resources/gymmie/member/view/Membership.fxml` and `member/css/membership.css`.
- `src/test/java/gymmie/member/MemberMembershipNavigationTest.java`; service tests moved to `src/test/java/gymmie/member/service/`.
- `docs/UserGuide.md`, `docs/DeveloperGuide.md`, and `evals/implement-issue/reports/issue-36-buy-available-membership.md`.
