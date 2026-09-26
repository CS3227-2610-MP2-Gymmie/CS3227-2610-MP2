# Gymmie Issue 35 Correction

## Goal
Integrate and correct the existing Member membership-status view in the target
workspace, preserving the original implementation evidence and Trainer work.

## Scope
One user-assistant exchange; one focused correction and integration task.

## Key prompts
- “This is post-experiment correction, not a new implementation experiment.”
  This kept the work as an intervention on the target state, with the original
  evaluation preserved as historical evidence.
- “Keep the target's upstream NavigationTest.java unchanged.” This required
  moving the Member UI assertions into a separate Member-specific test class.
- “Before editing, preserve a separate evidence snapshot outside both
  worktrees” and “Do not copy credentials or the development checkout's
  database.” This defined the evidence archive boundary before any target edits.
- “Do not stage, commit, push, create a PR, or modify GitHub issues.” This
  limited the session to local integration and verification.

## Decisions and corrections
The target baseline was clean at `8255115` (`8255115bdc4c17383135c3d5a149af040796d05c`);
the original implementation worktree was based on `c94d030`. Archived the
original run, exported record, tracked diff, untracked service and service test,
guide and obsolete note, and supplemental manual scripts/results outside both
worktrees. Excluded captured application data and credentials.

Integrated the existing service through `AppContext` and added its Member-only
dashboard card without replacing the merged Trainer controller or FXML. Added
separate Member UI tests that reuse `JavaFxTestSupport`, expanded service tests
for authenticated ownership, and preserved normal model coverage. Merged the
Member instructions into the target User Guide and left upstream Trainer and
other sections intact. The upstream `NavigationTest.java` remained unchanged.

The first Gradle attempt was blocked by the sandbox's external wrapper-cache
lock. After command approval, Checkstyle reported two `SeparatorWrap` issues in
the new UI test; those were fixed. The final `./gradlew check` passed: 175 tests
across 26 suites, 0 failures/errors, 7 opt-in GUI tests skipped by default.
The full opt-in JavaFX command passed all 177 tests across 26 suites, including
2 unchanged navigation tests, 4 Member UI tests, and 3 Trainer navigation/profile
tests. GUI cases covered active through expiry today, archived plan display,
expiry text, refresh to/from cancelled status, expired and empty histories, role
visibility, button action, and keyboard Space.

## Files created or modified
- `docs/UserGuide.md`
- `src/main/java/gymmie/AppContext.java`
- `src/main/java/gymmie/DashboardController.java`
- `src/main/java/gymmie/service/MembershipStatusService.java`
- `src/main/resources/gymmie/view/Dashboard.fxml`
- `src/test/java/gymmie/member/MemberMembershipNavigationTest.java`
- `src/test/java/gymmie/service/MembershipStatusServiceTest.java`
