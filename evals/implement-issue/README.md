# Implementation Reports

The implement-issue skill writes one report per issue into `reports/` for later reflection.

## Optional independent review

Run this command to review the current branch diff against a report:

```sh
codex exec --sandbox read-only "Grade the current branch diff against the acceptance criteria in evals/implement-issue/reports/<report>.md using that report's self-assessment rubric. Give a 1-5 score and one line of evidence per criterion. Do not edit files."
```
