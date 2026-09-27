# Manager Issue Creation and Completion Workflow

## Goal

Prepare the development infrastructure for executing the Manager role features (Epic 2: `E2: Build manager features`), including formalizing the issue creation script, establishing `GEMINI.md` as an AI workflow guidance document, and creating an agent skill to orchestrate dependent Git feature branches safely.

## Scope

Preparation of automation tooling, agent instructions, and branching safeguards across issue registration, Git hygiene, and workflow validation.

## Key prompts

* "Discussed the architectural flow of Manager features and sequenced them into six distinct PR-scoped deliverables." — Evaluated feature dependencies and broke down the Manager role implementation into sequential, PR-sized chunks.
* "Analyzed race conditions and git-tangle risks when directing autonomous agents to execute multi-step scripts." — Investigated edge cases around premature downstream branch execution, uncommitted working tree leaks, and placeholder IDs.
* "Formulated sequential-issue-workflow/SKILL.md to introduce explicit stop-and-wait gates, surgical file staging, and dynamic issue ID resolution via gh." — Created the orchestration skill to enforce human verification checkpoints and clean branch cutoffs.
* "Drafted the GitHub issue creation script (scripts/create-manager-issues.sh) to idempotently register the 6 Manager user stories under the existing E2 epic and milestone." — Automated issue registration directly linked to parent epic #12.
* "Documented prompt instructions and workflow context inside GEMINI.md." — Established persistent workflow rules and operational guidelines for agent sessions.

## Decisions and corrections

* **Gated Checkpoints vs. Autonomous Execution**: Enforced human verification stops between issue discovery, branch switching, and downstream branch initialization to prevent branching off unmerged commits.
* **Surgical Staging**: Mandated targeted file staging rather than `git add .` to avoid leaking scripts and context docs into unrelated domain PRs.
* **Deterministic Issue Linking**: Configured the script and workflow to query the GitHub CLI directly for issue IDs, ensuring commit messages and PR bodies use real issue numbers for traceability and automatic issue closing.
* Verified `scripts/create-manager-issues.sh` syntax and flag compatibility with `gh`.
* Validated YAML frontmatter formatting and execution steps inside `sequential-issue-workflow/SKILL.md`.
* Ran `./gradlew check` to ensure no workspace or Checkstyle regressions.

## Files created or modified

* scripts/create-manager-issues.sh
* .agents/skills/sequential-issue-workflow/SKILL.md
* GEMINI.md