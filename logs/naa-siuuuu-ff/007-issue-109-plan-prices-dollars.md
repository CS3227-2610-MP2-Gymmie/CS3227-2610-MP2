# Issue 109 Plan Prices in Dollars

## Goal

Implement issue #109 so Managers enter and edit membership plan prices in SGD dollars rather than cents while storing prices internally as cents, update UI prompt and label text, document the updated behavior in the User Guide, and add tests covering required price inputs (50, 49.90, 0, -1, 49.999).

## Scope

One focused implementation on branch `implement-issue-109-plan-prices-dollars` covering controller parsing/editing logic, FXML label/prompt updates, DisplayFormatters helper, User Guide documentation, unit tests, and UI integration tests.

## Key prompts

- "Lets do #109 (dollars not cents), and #113 (Show password toggle). first"
- "Afterwards, for each of them, checkout to a new branch, and follow the relevant skill workflow to solve them, test them, then push and make the PR."

## Decisions and corrections

- Updated `ManagerPlansController.parsePriceCents` to parse dollar amounts to integer cents with at most 2 decimal places, rejecting negative numbers, excessive decimals, and non-numeric inputs.
- Added `DisplayFormatters.dollars(cents)` to format stored integer cents into two-decimal dollar strings (e.g. 4990 -> "49.90"), and updated `ManagerPlansController.startEdit` to show dollars.
- Updated `Plans.fxml` label to "Price in SGD dollars (e.g. 49.90)" and prompt text to "e.g. 50 or 49.90".
- Updated `docs/UserGuide.md` under Manager membership plan management to describe entering and editing prices in SGD dollars.
- Created `ManagerPlansControllerTest` covering tests for 50, 49.90, 0, -1, 49.999, blank strings, currency prefixes, and non-numeric inputs.
- Updated `DisplayFormattersTest` and `ManagerPlansNavigationTest` to verify dollar formatting, editing behavior, and prompt text.
- Verified `./gradlew check` and `./gradlew test -PuiTests=true` pass with 0 errors.

## Files created or modified

- `docs/UserGuide.md`
- `evals/implement-issue/reports/issue-109-plan-prices-dollars.md`
- `logs/naa-siuuuu-ff/007-issue-109-plan-prices-dollars.md`
- `src/main/java/gymmie/manager/ManagerPlansController.java`
- `src/main/java/gymmie/ui/DisplayFormatters.java`
- `src/main/resources/gymmie/manager/view/Plans.fxml`
- `src/test/java/gymmie/manager/ManagerPlansControllerTest.java`
- `src/test/java/gymmie/manager/ManagerPlansNavigationTest.java`
- `src/test/java/gymmie/ui/DisplayFormattersTest.java`
