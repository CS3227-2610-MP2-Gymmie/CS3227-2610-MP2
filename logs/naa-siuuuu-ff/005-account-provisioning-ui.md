# Implement Account Provisioning UI

## Goal

Build the Manager FXML screen and controller for account provisioning and lifecycle management, delegating all operations to `AccountProvisioningService`, updating the User Guide, and adding comprehensive UI navigation tests, addressing issue #97 (and folding in #98 documentation criteria).

## Scope

One focused implementation task on the `issue-5-account-ui` branch covering FXML/CSS view layout, controller interaction logic, dashboard routing, documentation, and JavaFX navigation tests.

## Key prompts

- "Excellent, it has been successfuly merged. Now, onto the next issue" — Directed pulling upstream master containing the merged Issue 4 (#96), creating branch `issue-5-account-ui`, and implementing Issue #97.

## Decisions and corrections

- Created `Accounts.fxml` and `accounts.css` under `gymmie.manager` supporting listing all accounts, provisioning new Trainer/Member accounts, editing display names, and deactivating/reactivating accounts.
- Applied visual distinction for deactivated accounts: cards receive `account-card-deactivated` style class with a tinted background and a red `DEACTIVATED` badge.
- In `ManagerAccountsController`, delegated all operations to `AccountProvisioningService` on background threads with responsive status feedback, clean form state transitions, and keyboard accessibility (mnemonic parsing, Enter key submissions, focus restoration).
- Added `openManageAccounts()` to `DashboardController` and `manageAccountsButton` to `Dashboard.fxml`, visible and managed exclusively for the Manager role.
- Added `showManagerAccounts()` to `Router`.
- Updated `docs/UserGuide.md` under `### Manager` documenting the account provisioning and management workflow (viewing, provisioning, editing, and deactivating/reactivating).
- Created `ManagerAccountsNavigationTest` covering dashboard navigation, active/deactivated visual distinction, account provisioning, display name editing, and deactivate/reactivate lifecycle.
- Verified both `./gradlew check` and `./gradlew test -PuiTests=true` pass with 0 errors.

## Files created or modified

- docs/UserGuide.md
- logs/naa-siuuuu-ff/005-account-provisioning-ui.md
- src/main/java/gymmie/DashboardController.java
- src/main/java/gymmie/Router.java
- src/main/java/gymmie/manager/ManagerAccountsController.java
- src/main/resources/gymmie/manager/css/accounts.css
- src/main/resources/gymmie/manager/view/Accounts.fxml
- src/main/resources/gymmie/view/Dashboard.fxml
- src/test/java/gymmie/manager/ManagerAccountsNavigationTest.java
