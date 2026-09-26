# Implementation Reports

The implement-issue skill writes one report per issue into `reports/` for later reflection.

## Independent review

After verification passes, the implementing agent runs the review script and
responds to its blocking findings. The agent fixes each finding or records why
it was rejected, then reruns verification and review until the review reports
Overall: pass, for at most 3 rounds. The user reviews the final round and can
rerun the script if needed. Do not edit the script's review output.

The base ref must be current: update `master` first, or pass `upstream/master`
as the second argument. Run the script from a normal terminal:

```sh
evals/implement-issue/review.sh evals/implement-issue/reports/<report>.md
```

Pass a second argument to compare against a different base ref; it defaults to
`master`. The script asks a fresh read-only reviewer to assess the report and
the diff, then appends a numbered round to the report's `## Independent review`
section.
