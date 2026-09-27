# Implement Membership Plan Management UI

## Goal

Build the Manager FXML screen and controller for membership plan management, delegating all operations to `MembershipPlanService`, updating the User Guide, and adding comprehensive UI navigation tests, addressing issue #95.

## Scope

One focused implementation task on the `issue-3-plan-ui` branch covering FXML/CSS view layout, controller interaction logic, dashboard routing, documentation, and JavaFX navigation tests.

## Key prompts

- "Excellent, PR has been merged to master of upstream. Move on to the third issue, do it exactly like you did for the previous 2." — Directed fast-forwarding local master to upstream, creating branch `issue-3-plan-ui`, and implementing Issue #95.

## Decisions and corrections

- Created `Plans.fxml` and `plans.css` in `gymmie.manager` supporting listing all plans, creating new plans, editing unarchived plans, and toggling archive/restore/delete.
- Applied visual distinction for archived plans: cards receive `plan-card-archived` style class with a tinted background, alongside clear `ACTIVE` and `ARCHIVED` status badges.
- In `ManagerPlansController`, delegated all operations to `MembershipPlanService` on background threads with responsive status feedback and keyboard accessibility (mnemonic keys, form submission via Enter, focus handling).
- Added `openManagePlans()` to `DashboardController` and `managePlansButton` to `Dashboard.fxml`, displayed only for the Manager role.
- Updated `docs/UserGuide.md` under `### Manager` documenting the plan management workflow.
- Created `ManagerPlansNavigationTest` covering dashboard navigation, active/archived distinction, plan creation, editing, archive/restore, and unpurchased plan deletion. Fixed initial Seeder duplication in test context setup and verified both `./gradlew check` and `./gradlew test -PuiTests=true` pass.

## Files created or modified

- docs/UserGuide.md
- src/main/java/gymmie/DashboardController.java
- src/main/java/gymmie/Router.java
- src/main/java/gymmie/manager/ManagerPlansController.java
- src/main/resources/gymmie/manager/css/plans.css
- src/main/resources/gymmie/manager/view/Plans.fxml
- src/main/resources/gymmie/view/Dashboard.fxml
- src/test/java/gymmie/manager/ManagerPlansNavigationTest.java
