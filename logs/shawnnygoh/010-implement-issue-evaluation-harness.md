# Implement-Issue Evaluation Harness

## Goal
Build and harden a small evaluation harness for the existing implement-issue dataset, then document its generic workflow and preserve a session record. The harness was tested synthetically without implementing issue 35 or launching live model or GUI runs.

## Scope
Eight user-assistant exchanges; one continuous harness task with implementation, regression fixes, documentation, and validation.

## Key prompts
- “Do not implement issue 35, run live model calls or GUI sessions, delete existing runs or application data, commit, push, switch branches, or change global configuration.” This bounded the work to harness changes and synthetic verification.
- “Preserve the existing design and commands. Do not rewrite the harness, expand its scope, or change the reusable implementation skill.” This kept follow-up fixes focused on confirmed defects.
- “Please revise evals/README.md so it documents the evaluation harness generically across all supported cases, rather than centering on issue 35.” This required documentation to reflect case-specific behavior rather than a single pilot.
- “distinguish curated evaluation records from application inputs, without broadly excluding arbitrary files.” This guided the record-artifact freshness fix and its regression coverage.

## Decisions and corrections
Kept the existing CLI and harness structure. Added conservative checks for output paths, application data, verification reports, trace evidence, protected inputs, model-evidence freshness, and curated record recognition, with synthetic regression tests. Updated readiness guidance and clarified that human corrections need separate verification and intervention records; rerunning an implementation experiment requires its original starting state. Preserved human review as pending and avoided claims that synthetic tests prove live execution or complete security.

## Files created or modified
- `.gitignore`
- `evals/README.md`
- `evals/run.py`
- `evals/test_run.py`
- `evals/requirements.txt`
- `evals/implement-issues/cases/member-cancellation-atomicity.yaml`
- `evals/implement-issues/cases/member-renewal-boundaries.yaml`
- `evals/implement-issues/cases/member-status-view.yaml`
- `evals/implement-issues/cases/membership-switch-scope-decision.yaml`
