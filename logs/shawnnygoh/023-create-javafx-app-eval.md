# Create JavaFX Skill Evaluation

## Goal
Preserve the create-javafx-app trigger and output evaluation, document its
method, verify its recorded results, and commit the evaluation and this log
separately without pushing.

## Scope
Four user-assistant exchanges; one focused repository tooling and
documentation task.

## Key prompts
- “Create a branch `add-create-javafx-app-evals` from master.” This set the
  branch and source evaluation scope.
- “Focus only on the skill's trigger and output evaluation: traces, reports,
  the scripts that produce them, and deterministic and model-assisted
  grading.” This excluded generated repositories and unrelated experiments.
- “This evaluation was run on 2026-09-07 while developing the
  create-javafx-app skill” This corrected the timing and historical context.
- “Then commit the evaluation and the log separately, with no push.” This
  specified the delivery sequence.

## Decisions and corrections
- The user corrected the evaluated skill files to v1 and v2 versions. The
  evaluation ran on 2026-09-07 during skill development, before the skill was
  used to scaffold this repository on 2026-09-09.
- The v2 `assets/gradle.yml` omits `java-package: jdk+fx`, which appears in the
  reference. The other two asset files match; generated repositories and
  skill assets were not copied.
- Paths under grades and reports were normalized by replacing only the
  supplied absolute prefix. Scores, evidence, and notes were preserved.
- The trace report generated successfully, the model grade validated at 96
  with an overall pass, and deterministic grader help ran successfully. The
  credential-pattern scan found no matches.
- An earlier elevated `.git` write request was rejected. The user then renewed
  the request to commit the evaluation and log as separate commits.

## Files created or modified
- `.gitignore`
- `evals/create-javafx-app/` evaluation files and README
