# Implementation Reports

The implement-issue skill writes one report per issue into `reports/` for later reflection.

## Independent review

After the implementing session, run the review script from a normal terminal
before committing the report:

```sh
evals/implement-issue/review.sh evals/implement-issue/reports/<report>.md
```

Pass a second argument to compare against a different base ref; it defaults to
`master`. The script asks a fresh read-only reviewer to assess the report and
the diff, then replaces the report's `## Independent review` section.
