# Refactor Implement-Issue Evaluation Cases

## Goal
Select a small Epic 4 dataset for evaluating the `implement-issue` workflow, then convert it to concise YAML case definitions with explicit prerequisites and checks.

## Scope
3 user-assistant exchanges; one continuous evaluation-dataset task followed by its session-log request.

## Key prompts
- “This session is limited to case selection and definition. Do not modify the bundled skill, implement features, build a harness, or run agent experiments.” This bounded the initial work to reading the backlog and defining cases.
- “Inspect the Epic 4 backlog, then select a small representative subset” spanning implementation complexity, coordinated persistence, a missing-dependency case, and an explanation-only case. This guided the initial selection from real linked issues.
- “Convert the existing Markdown cases to one YAML file per case” and remove the README after preserving the necessary details. This requested a structured conversion without adding a schema or replacement guide.
- “Treat the explanation-only case as a negative skill-trigger case.” This corrected the semantics of issue #44 and ruled out claiming it tested blocker discovery during an implementation request.
- “Do not build a harness, add hooks, run agent experiments, implement features, or modify the skill package. Do not commit or push.” This kept the conversion documentation-only.

## Decisions and corrections
- Identified the canonical upstream repository and inspected Epic #14 plus issues #35, #37, #38, and #44. `gh auth status` reported an invalid saved token; read-only issue listing and views succeeded after network access was approved.
- Selected #35 for the status view, #37 for renewal boundaries, #38 for transactional cancellation, and #44 for a negative skill-trigger case. The three implementation cases now require the shared Member authentication/routing foundation and applicable fixtures before dispatch. If the target feature already exists, the case is not applicable to that checkout.
- Reclassified #44 as a direct documentation question that must not invoke `implement-issue`. No separate blocker-discovery case was retained; the dataset records that coverage gap.
- Removed manually maintained checkout, skill-hash, authentication-history, and untracked-file details. YAML checks are marked planned; YAML parsing and consistency validation succeeded. No product tests or agent experiments were run.

## Files created or modified
- Created `evals/cases/member-status-view.yaml`.
- Created `evals/cases/member-renewal-boundaries.yaml`.
- Created `evals/cases/member-cancellation-atomicity.yaml`.
- Created `evals/cases/membership-switch-scope-decision.yaml`.
- Removed `evals/README.md` and the four superseded Markdown case files with matching names.
