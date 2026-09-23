---
name: issue-extractor
description: Convert one section of a reference document into a reviewable GitHub issue-creation script for one epic; do not use for a single ad-hoc issue or bug report.
---

# Issue Extractor

Use this skill when the user asks to turn one named section of a reference document into GitHub issues for one epic, such as the stories for a particular role in a Developer Guide or the requirements for an Epic. Do not use it for a single ad-hoc issue, a bug report, or work spanning several epics or sections.

The output is a script for the developer to review and run. Never run `gh issue create` yourself, and never create issues as part of this skill.

## Scope and source handling

- Require one document and one section or epic per run. If the requested section is missing, say so clearly and stop.
- Read the whole relevant section and identify every discrete work item, whether it is expressed as a table row, bullet, numbered item, or prose. If the section contains no discrete work items, say so and stop instead of producing an empty or speculative script.
- Use supporting detail from elsewhere in the same document, including use cases, extensions, constraints, non-functional requirements, glossary entries, and known limitations. Never invent acceptance criteria.
- Keep the generated set limited to the requested section. Do not pull in related items from another role, epic, or section.

## Repository discovery

Before writing the script, inspect the target repository with `gh`:

1. Run `gh label list` and choose only labels that actually exist. Map document priority to the repository's labels when the document expresses priority. If no existing label fits an item, leave a review comment in the generated script rather than inventing a label name.
2. Run `gh issue list` and resolve the parent epic by exact title. Fail with a clear message if it is missing or if more than one issue has that title. Resolve the number at script runtime; never hardcode an issue number.
3. Run `gh api repos/{owner}/{repo}/milestones` and inspect open milestones. If exactly one is open, apply it. If several are open, ask the developer to choose one and make the generated script fail clearly until one is chosen.
4. Run `gh issue view` on two or three existing hand-authored issues and match their body conventions.

## Issue content

For each source item:

- Write an imperative title under 60 characters, derived from the item rather than copied verbatim.
- Make the body state `Parent epic: #<resolved number>`, describe the work in one or two sentences, and include acceptance criteria supported by the same document.
- If the item has no supporting detail beyond its own line, add a short `Notes:` line that says the gap remains instead of filling it with guesses.
- If the document contains genuinely unresolved conflicting requirements, preserve the conflict in `Notes:` and do not manufacture a resolution. Any documented limitations are not conflicts but can be noted here as well.

## Generated script

Write `scripts/create-<section>-issues.sh`, using a lowercase hyphenated section slug. The script must:

- begin with `set -euo pipefail`;
- resolve the parent epic by exact title at runtime and fail on missing or ambiguous matches;
- discover the open milestone as described above;
- contain a helper that lists all issues and compares titles exactly for idempotency; do not use `gh issue list --search` because colons in titles can be mishandled;
- use heredoc bodies and print a skip message when an exact title already exists, including closed issues;
- use only labels discovered in the target repository;
- put the resolved epic number in every issue body; and
- create nothing on a rerun, reporting each skipped title instead.

The script may contain `gh issue create` commands for the developer to review, but this skill must not execute the generated script. Validate it with `bash -n` only unless the developer explicitly asks to run it later. Do not commit the skill or generated script unless explicitly asked.
