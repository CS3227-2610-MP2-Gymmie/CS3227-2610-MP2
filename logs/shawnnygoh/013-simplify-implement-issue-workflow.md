# Simplify Implement Issue Workflow

## Goal
Remove the implement-issue evaluation harness, consolidate its skill and report guidance, and add reports documentation in two authorized commits. Review the user's later edits for commit readiness and record this session.

## Scope
Two user-assistant exchanges covering one focused repository-maintenance task and its follow-up readiness review.

## Key prompts
- “Create a branch `simplify-implement-issue` from master. Do not push.” This set the branch base and prohibited publishing.
- “Delete everything under evals/ (run.py, test_run.py, requirements.txt, README.md, implement-issues/).” This scoped the first commit to removing the harness and its ignore rules.
- “Rewrite .agents/skills/implement-issue/ as one generic SKILL.md plus one asset, assets/report-template.md.” This set the skill's reusable structure and repository-neutral scope.
- “Add evals/implement-issue/README.md (at most 15 lines).” This requested the reports-folder explanation and optional independent-review command in the second commit.
- “I made some changes. Can you let me know if it's ready for commit? Then, use the [$session-log] skill to create a log for this session.” This asked for a review without committing and for this session record.

## Decisions and corrections
- Created the requested branch from `master`; committed harness removal as `89c148b` and skill/report consolidation as `915e84b`. Nothing was pushed.
- `git grep` found old harness paths only in historical session logs; those logs were preserved.
- The follow-up working tree changes clarify skill boundaries and branch creation, revise report naming and template fields, and delete the generated report for this maintenance task. They remain uncommitted.
- Readiness review: skill validation and `git diff HEAD --check` pass. The report-name example was made repository-neutral and the template's skill fields were consolidated before commit.
- `implement-issue` triggered implicitly on this maintenance task and generated `issue-000-simplify-implement-issue.md`. The user deleted it and narrowed the skill description to exclude changes to agent skills and workflow tooling.

## Files created or modified
- Modified `.gitignore`.
- Deleted `evals/README.md`, `evals/requirements.txt`, `evals/run.py`, and `evals/test_run.py`.
- Deleted `evals/implement-issues/cases/member-cancellation-atomicity.yaml`, `member-renewal-boundaries.yaml`, `member-status-view.yaml`, and `membership-switch-scope-decision.yaml`.
- Modified `.agents/skills/implement-issue/SKILL.md`.
- Deleted `.agents/skills/implement-issue/assets/issue-record-template.md` and `.agents/skills/implement-issue/references/workflow-contract.md`.
- Created and then modified `.agents/skills/implement-issue/assets/report-template.md`.
- Created `evals/implement-issue/README.md`.
- Created and then deleted `evals/implement-issue/reports/issue-000-simplify-implement-issue.md`.
