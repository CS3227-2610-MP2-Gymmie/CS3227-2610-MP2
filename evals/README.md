# Implement-issue evaluation harness

The implement-issue skill in `.agents/skills/implement-issue/` guides implementation work and its workflow contract describes expected practice; YAML cases in `evals/implement-issues/cases/` provide prompts, requirements, prerequisites, applicability, and case-specific checks; `evals/run.py` dispatches one case and saves evidence for deterministic, trace, optional model-assisted, and human assessment.

The harness supplies the selected case's prompt to Codex; choose a case ID and do not paste the entire YAML case into the agent.

## Preparation and readiness

Use Python 3.10 or later with PyYAML (`python3 -m pip install -r evals/requirements.txt`), Java 25, the repository Gradle wrapper, and dependencies available in the configured `GRADLE_USER_HOME` or the normal Gradle cache.

Install and authenticate Codex with `codex login` under the intended `CODEX_HOME`, then check `codex login status`; the harness does not change global configuration or copy credentials into run artifacts.

Prepare an existing Gymmie checkout or Git worktree and set `WORKSPACE` to its absolute path; implementation cases require a user-prepared feature branch and a clean relevant application starting state, while explanation-only cases do not have that feature-branch requirement.

Implementation readiness requires disposable application data because Gymmie's default database is relative to the workspace as `data/gymmie.db`; ensure that file and its `-wal`, `-shm`, `-journal`, and `-mj*` SQLite sidecars are absent, and do not point `data` through a symlink.

The readiness check inspects those paths without opening, moving, deleting, or copying a database; an implementation may create `data/gymmie.db` under the workspace, so prepare a checkout whose application data you are willing to use for the evaluation.

Temporary `HOME` and `TMPDIR` isolate selected home and temporary files, not Gymmie's relative database, workspace files, or the machine; Java/Gradle settings and Windows compatibility variables are inherited as process configuration, not filesystem permissions; the workspace is not a security sandbox, and verification subprocesses run outside the Codex child sandbox with model authentication variables filtered.

Find case files and IDs with `ls evals/implement-issues/cases/` and `rg '^id:' evals/implement-issues/cases/`; inspect the chosen YAML's `prompt`, `preconditions`, `preflight`, `expected_behavior`, and `checks` sections to see its accepted readiness IDs and supported checks.

Readiness reports accepted IDs directly: `shared_foundation_ready` is available when a case lists shared foundation prerequisites but has no machine checks; case-declared `preflight.human_readiness` IDs are also listed, including optional checks.

When a case configures `preflight.feature_probe.human_completion_check`, readiness prints that ID and accepts `ID=complete: EVIDENCE` to mark the case not applicable or `ID=incomplete: EVIDENCE` to proceed; it also prints the supported `_incomplete` ID, so use the exact IDs and evidence forms shown by `ready`.

A prose applicability instruction does not by itself create a CLI readiness ID; for cases without a configured feature probe, including the current renewal and cancellation cases, readiness cannot record that decision, so honor the case instruction manually before dispatch.

Run output reports the workspace, case, settings, branch requirement, relevant changes, application-data check, and readiness blockers before dispatch; output locations must not be the workspace or its ancestors and must not overlap application or evaluation inputs.

For a custom output directory inside the workspace, choose a dedicated Git-ignored location; an existing location can contain only recognized harness run or earlier readiness artifacts, while unrelated files cause readiness to stop.

## Commands

Set command placeholders: `WORKSPACE='/absolute/path/to/prepared/Gymmie-checkout'`, `CASE_ID='<case-id>'`, and `RUN_ID='<run-id>'`; substitute a case ID shown in its YAML and the run ID printed after execution.

Validate all cases: `python3 evals/run.py validate`.

Check readiness without launching an implementation model or repository tests: `python3 evals/run.py ready "$CASE_ID" --workspace "$WORKSPACE"`; add `--human-check '<accepted-id>=<evidence>'` for each applicable human check reported by the command.

Run one selected case: `python3 evals/run.py run "$CASE_ID" --workspace "$WORKSPACE"`; this launches Codex with the case prompt, captures its execution, and automatically verifies a completed run.

Inspect saved results: `python3 evals/run.py inspect "$RUN_ID"`; run directories are listed by `find evals/implement-issues/runs -mindepth 1 -maxdepth 1 -type d -print`.

Rerun verification when needed: `python3 evals/run.py verify "$RUN_ID"`; it runs the applicable repository checks and case-specific deterministic checks, and returns a failing exit code for failed, blocked, stale, unavailable, or timed-out commands.

Optionally launch separate read-only semantic grading: `python3 evals/run.py grade "$RUN_ID"`; alternatively add `--model-grade` to `run` to grade immediately after automatic verification, and do not repeat grading to overwrite evidence because each assessment is retained separately.

Export a current verification record for curation: `python3 evals/run.py record "$RUN_ID"`; the record remains `human_review: pending` until a person reviews it outside the harness.

Run the harness tests without live agents, models, or GUI sessions: `python3 -m unittest discover -s evals -p 'test_*.py' -v`.

`validate`, `inspect`, and `record` do not launch models or repository checks; `ready` performs preflight and Codex login-status checks; `run` launches an implementation or answer-generation model and then repository verification; `verify` executes applicable repository checks; `grade` and `run --model-grade` launch a separate model grader.

## Evidence and interpretation

Implementation cases can modify application files; explanation-only or negative-trigger cases request an answer and check whether repository content changed, which does not establish the state of an external issue or service.

Deterministic results report only case checks with executable support, including required JUnit suite reports where configured; a general repository check is not proof that every acceptance criterion works, and unsupported criteria remain pending or require human review.

Trace results recognize only supported, unambiguous verification commands with recorded outcomes; a successful invocation signal does not establish adequate acceptance coverage, final-state verification, or complete workflow-contract compliance, and ambiguous or absent evidence remains not observable.

The optional model grader assesses only the case's semantic criteria using requirements and selected evidence; its judgments record the verification ID and fingerprints of the evidence supplied, remain separate from deterministic and human results, and cannot override deterministic failures.

Inspection marks saved model judgments current or stale against the present verification and assessed evidence; a stale judgment is retained as history but is not exported as a current assessment, and a fresh intentional grading invocation is saved separately.

Human review remains pending in harness output and exported records; passing commands, model judgments, and skipped or unavailable evidence do not constitute human approval or prove unsupported criteria.

Verification distinguishes successful, failed, blocked, skipped, unavailable, and timed-out commands; JUnit totals are checked for consistency, every configured acceptance suite needs a current report, and skipped required suites do not count as complete acceptance coverage.

Graphical readiness is case-declared rather than universal; an opt-in UI status of `skipped` means no UI tests ran, and the repository's JavaFX navigation test requires Gradle's `-PuiTests=true` opt-in.

Generated run artifacts go to Git-ignored `evals/implement-issues/runs/` by default, curated records go to `evals/implement-issues/records/`, and the ignore rules also exclude evaluation Python bytecode; a custom output directory within the workspace should already be Git-ignored.

Runs preserve case and prompt inputs, workflow archive, compact metadata, implementation JSONL trace, stderr, final response, diff, authoritative verification report and per-command evidence, plus versioned optional model-grader outputs and traces; readiness-only checks do not create a full run inventory.

The harness can remain uncommitted while the skill is committed if relevant application files are clean; keep harness, case, workflow, and assessed evidence unchanged during evaluation because changes to workspace code or assessed inputs make saved results stale.

Human corrections change the assessed code state and stale the original verification and model associations; verify a correction separately with the repository checks and keep its verification and human intervention records separate from the implementation experiment. The harness does not treat a corrected checkout as a rerun of that experiment: repeating the implementation case requires re-preparing its original starting state, and a case whose requested behavior is now implemented may be not applicable. Commit only when authorized by the implementation workflow.
