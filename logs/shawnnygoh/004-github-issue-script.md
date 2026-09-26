# GitHub Issue Script Session

## Goal
Create and refine a reviewable shell script for opening Gymmie’s upstream GitHub issues. The script groups six epics and ten E1 tasks, while remaining safe for the user to inspect and run manually.

## Scope
Five user-assistant exchanges covering one focused task: drafting and revising the GitHub issue-creation script, followed by recording this session.

## Key prompts

- “Do not run `gh issue create`. Write a reviewable shell script” — required a non-mutating, user-controlled issue workflow.
- “Acceptance criteria must come from the Developer Guide” — constrained task criteria to documented requirements and required unspecified details to be identified.
- “SCHEMA RENDERING (critical)” — corrected the E1 schema formatting so GitHub renders it as a fenced code block without literal escape characters.
- “Can you add this as the tenth E1 task?” — extended the E1 task set with shared UI foundation work.

## Decisions and corrections

- Targeted the upstream repository `CS3227-2610-MP2-Gymmie/CS3227-2610-MP2` while keeping issue creation in a reviewable local script.
- Structured the script with `epics`, `tasks <E1-number>`, and `all` modes, capturing E1’s number so task bodies can reference it.
- Added exact-title duplicate checks across open and closed issues, then incorporated the user’s removal of `--search` by comparing titles from the full issue list.
- Revised E1 to own the shared stories, fenced and unescaped its schema, removed boilerplate acceptance bullets, moved the scale requirement to Notes, and adjusted Task 6 to provide service-layer session and password-change support.
- Added the tenth high-priority task for the shared UI foundation. The script was syntax-checked, but no GitHub issue-creation command was run.

## Files created or modified

- `scripts/create-e1-issues.sh`
