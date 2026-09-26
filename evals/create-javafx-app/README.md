# Create JavaFX app skill evaluation

This evaluation was run on 2026-09-07 while developing the create-javafx-app skill, before the skill was used to scaffold this repository (commit 'Add Gymmie JavaFX application scaffold', 2026-09-09).

File paths in grades and reports were normalized to repository-relative form when the evaluation was added; the results are unchanged.

## Test prompts

[`test-prompts.csv`](test-prompts.csv) records four trigger cases:

- **Explicit:** directly invokes `$create-javafx-app`.
- **Implicit:** requests a new JavaFX desktop app without naming the skill.
- **Contextual:** requests a desktop app with Gradle, FXML, and CSS, implying
  JavaFX through the context.
- **Negative:** asks to add a screen to an existing JavaFX app, which should not
  trigger a new-app scaffold skill.

The `v1` and `v2` traces and grades correspond to the evaluated skill versions
in [`skill-versions/v1.SKILL.md`](skill-versions/v1.SKILL.md) and
[`skill-versions/v2.SKILL.md`](skill-versions/v2.SKILL.md). The current skill
is [`.agents/skills/create-javafx-app/SKILL.md`](../../../.agents/skills/create-javafx-app/SKILL.md).
The v2 `assets/` files were compared with `reference/`: `checkstyle.xml` and
`suppressions.xml` match. `gradle.yml` differs because v2's asset omits
`java-package: jdk+fx` from the Java setup step; the reference file includes
that setting.

Generated repositories were kept locally and are not committed here. The
recorded grades in `grades/` are the evidence; the grading commands below
document the method. The saved reports and grades include both v1 and v2 runs.

## Capture traces

Each prompt was run with `codex exec --json`, saving its JSONL event stream as
the corresponding file under `traces/<version>/`. For example, from the
repository root:

```bash
eval_root="$PWD/evals/create-javafx-app"
workdir="/path/to/the-generated-repository"
prompt='Use the $create-javafx-app skill to create a JavaFX app named Gymmie'
codex exec --json \
  --model gpt-5.6-luna \
  --sandbox workspace-write \
  --skip-git-repo-check \
  -C "$workdir" \
  "$prompt" > "$eval_root/traces/v2/explicit-trace.jsonl"
```

Repeat with each prompt and the relevant version and filename in
`test-prompts.csv`. The model shown is the one used for this evaluation.

## Generate trace reports

`trace_report.py` reads a captured JSONL trace and writes a normalized JSON
summary plus an HTML report. The summary and report preserve the event details
needed for review, including commands and output, messages, file changes,
trigger evidence, and token usage.

```bash
python3 evals/create-javafx-app/scripts/trace_report.py \
  evals/create-javafx-app/traces/v2/explicit-trace.jsonl \
  --summary-out evals/create-javafx-app/reports/v2/explicit.trace-summary.json \
  --html-out evals/create-javafx-app/reports/v2/explicit.trace-report.html
```

Trigger detection uses a successful read of a path ending in
`create-javafx-app/SKILL.md`; it is an inference from trace events rather than
a dedicated skill-invocation event.

## Run deterministic grading

The deterministic grader checks trace behavior and the generated repository
against the reference files. Supply the local generated repository as the
artifact path; repositories are intentionally not stored in this evaluation
directory.

```bash
python3 evals/create-javafx-app/scripts/grade_deterministic.py \
  --trace evals/create-javafx-app/traces/v2/explicit-trace.jsonl \
  --artifact "$ARTIFACT_ROOT/v2/explicit" \
  --reference evals/create-javafx-app/reference \
  --expected-trigger true \
  --package gymmie \
  --app-name Gymmie \
  --out evals/create-javafx-app/grades/v2/explicit.deterministic.json
```

The negative control uses `--expected-trigger false`; the grader selects its
trigger-only profile for that case by default. Pass
`--artifact-profile scaffold` or `--artifact-profile trigger-only` to override
the default profile.

## Run and validate model-assisted grading

The qualitative pass evaluates scope, UI coherence, documentation accuracy,
and maintainability using the prompt and output schema in `model-assisted/`.
Run it from the repository root, replacing the artifact path and output name
for another case:

```bash
eval_root="$PWD/evals/create-javafx-app"
codex exec \
  --model gpt-5.6-luna \
  -c 'model_reasoning_effort="high"' \
  --sandbox read-only \
  --skip-git-repo-check \
  -C "$ARTIFACT_ROOT/v2/explicit" \
  --output-schema "$eval_root/model-assisted/repository-rubric.schema.json" \
  -o "$eval_root/grades/v2/explicit.model.json" \
  - < "$eval_root/model-assisted/repository-rubric.prompt.md"

python3 evals/create-javafx-app/scripts/validate_model_grade.py \
  evals/create-javafx-app/grades/v2/explicit.model.json
```

The validator checks the four rubric IDs, score/pass consistency, total score,
and overall pass rule in addition to the JSON schema.
