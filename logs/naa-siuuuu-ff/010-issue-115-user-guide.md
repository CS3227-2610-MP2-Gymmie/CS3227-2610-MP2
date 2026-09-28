# Issue 115 User Guide Polish and Completion

## Goal

Implement issue #115 by refining and expanding `docs/UserGuide.md` with complete role feature tables, performance-ordered actions, accurate domain constraints, system verification instructions, data storage guidance, workspace reset steps, and documented limitations.

## Scope

Focused documentation update on branch `implement-issue-115-user-guide` targeting `docs/UserGuide.md` while preserving screenshot placeholders.

## Key prompts

- "Ok now do Issue #115. Feel free to edit this user guide accordingly. Leave the screenshots for now. Go ahead"

## Decisions and corrections

- **Accuracy:**
  - Specified username constraints: 3–30 characters, letters/digits/hyphens/underscores (`[A-Za-z0-9_-]`), case-insensitive global uniqueness across all roles, stored with original casing preserved.
  - Documented that membership purchases are recorded and activated immediately in Gymmie, while payment collection and fee handling take place in person at the gym reception desk without electronic payment processing.
  - Documented known limitations: no direct plan switching (cancel then purchase required), no payment/revenue tracking, no attendance marking, renewing expired memberships keeps original purchase date, and rebooking overwrites prior cancellation reason.
- **Readability:**
  - Added structured feature tables for Manager, Trainer, and Member roles outlining actions in order of performance (create -> view -> edit -> delete/cancel).
  - Maintained screenshot placeholders (`![...](images/...)`) with descriptive captions.
- **Completeness:**
  - Added Section 1 Quick start with launch options (Gradle and ShadowJar) and exact login error messages.
  - Added Section 3 Verifying the application with automated verification commands (`./gradlew check`, `./gradlew test -PuiTests=true`) and an end-to-end smoke verification walkthrough.
  - Added Section 8.4–8.6 detailing SQLite database location (`data/gymmie.db`), transaction guarantees, safe inspection warnings, and workspace reset instructions (`rm data/gymmie.db` followed by `./gradlew run`).
  - Synced Table of Contents anchors across all 41 sections and subsections.
- Verified `./gradlew check` passes without style or test regressions.

## Files created or modified

- `docs/UserGuide.md`
- `evals/implement-issue/reports/issue-115-user-guide.md`
- `logs/naa-siuuuu-ff/010-issue-115-user-guide.md`
