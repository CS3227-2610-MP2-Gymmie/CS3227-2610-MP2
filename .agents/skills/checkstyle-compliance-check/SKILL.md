---
name: checkstyle-compliance-check
description: Use before opening or updating any PR in the Gymmie repo, to run and resolve Checkstyle violations against the se-education.org rule set prior to submission.
---

## When to use this

- Immediately before opening a PR, on any branch with new or modified Java source.
- After a large refactor or AI-agent-generated code change, before running full `./gradlew check`.
- When CI reports a Checkstyle failure and it needs local reproduction and fixing.

## What to build

- Not a feature — a repeatable verification pass. No new production code should be introduced solely to satisfy this skill; only style/formatting corrections.

## Prerequisites

- Local clone with `config/checkstyle/checkstyle.xml` and `config/checkstyle/suppressions.xml` present (do not edit these unless the task explicitly calls for a rule change, which requires separate justification/approval).
- Gradle wrapper available (`./gradlew`).

## Steps

1. Run `./gradlew check` first, as a single deterministic pass — this covers Checkstyle, tests, and coverage in one go, so there's no need to inspect the codebase manually before knowing whether anything is even broken.
2. If `./gradlew check` passes with zero violations, stop here — do not read or reason over source files that have no reported issues; that only burns tokens for no benefit.
3. If it fails, pull the violation report (console output or `build/reports/checkstyle/*.xml`/`.html`) and group violations by rule ID (e.g., naming conventions, import order, javadoc, line length, whitespace) rather than reading files blind.
4. Only now open the specific files named in the report, scoped to the specific lines/rules flagged. Fix violations in the affected files only — do not reformat unrelated files in the same PR.
5. If a violation seems like a false positive or overly strict for a specific line, check `suppressions.xml` for an existing pattern before adding a new suppression; do not add a blanket suppression without noting the reason in the PR description.
6. Re-run `./gradlew check` once, after fixes are applied, to confirm both the Checkstyle violations are resolved and the fixes didn't break tests or coverage. Avoid re-running narrower targets (`checkstyleMain`/`checkstyleTest`) separately — the single full-suite run is the deterministic source of truth and keeps tool calls to a minimum.
7. Log the session under `logs/<github-username>/<index>-checkstyle-compliance-check.md`, noting which rule categories were violated and how they were resolved (useful for the Reflections on Agentic SE component).

## Verification environments

- `./gradlew check` — full suite (Checkstyle + tests + coverage) passes with zero violations. This is the only command needed to both detect and confirm-fix issues; no separate Checkstyle-only invocation is required.

## Definition of done

- [ ] Zero Checkstyle violations in `checkstyleMain` and `checkstyleTest` reports.
- [ ] No unrelated files reformatted.
- [ ] Any new suppression is justified in the PR description and matches existing suppression patterns where possible.
- [ ] `./gradlew check` passes end-to-end after fixes.
- [ ] Interaction log entry written, noting violation categories found and resolved.
