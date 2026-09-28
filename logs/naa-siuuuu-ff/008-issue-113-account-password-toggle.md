# Issue 113 Show Password Toggle When Manager Creates Accounts

## Goal

Implement issue #113 so Managers can toggle/reveal the password when provisioning accounts by reusing the existing `PasswordReveal` component, update the User Guide, and add UI navigation tests.

## Scope

One focused implementation on branch `implement-issue-113-account-password-toggle` covering FXML layout updates, controller initialization, User Guide updates, and JavaFX navigation test additions.

## Key prompts

- "Lets do #109 (dollars not cents), and #113 (Show password toggle). first"
- "It has been merged successfully, next time, I will let you know if the CI checks for the PR passes or fail, you dont have to keep running the sleep command. Lets move on to the next issue"

## Decisions and corrections

- In `Accounts.fxml`, wrapped `accountPassword` and `accountPasswordReveal` in an `HBox` (`passwordBox`) styled with `button, reveal-button` matching `Login.fxml` and `Dashboard.fxml`.
- In `ManagerAccountsController`, installed `PasswordReveal.install(accountPassword, accountPasswordReveal)` in `initialize()`.
- Updated `startEdit` and `resetForm` in `ManagerAccountsController` to manage `passwordBox` visibility and managed state so both the password field and reveal button hide during account edits and restore on reset.
- Updated `docs/UserGuide.md` under the Provision a new account section to mention holding the Show button or Space to temporarily reveal the password.
- Updated `ManagerAccountsNavigationTest` to assert the presence of `accountPasswordReveal`, verify that `passwordBox` hides during account edit and restores after update, and added `passwordRevealTogglesPreviewVisibilityWhenArmed` verifying reveal/disarm preview behavior.
- Verified `./gradlew check` and `./gradlew test -PuiTests=true` pass with 0 errors.

## Files created or modified

- `docs/UserGuide.md`
- `evals/implement-issue/reports/issue-113-account-password-toggle.md`
- `logs/naa-siuuuu-ff/008-issue-113-account-password-toggle.md`
- `src/main/java/gymmie/manager/ManagerAccountsController.java`
- `src/main/resources/gymmie/manager/view/Accounts.fxml`
- `src/test/java/gymmie/manager/ManagerAccountsNavigationTest.java`
