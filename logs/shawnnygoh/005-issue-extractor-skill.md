# Issue Extractor Skill Session

## Goal
Create and refine a reusable `issue-extractor` skill and a reviewable Member issue-generation script for the Gymmie repository, without creating GitHub issues or committing changes.

## Scope
Five user-assistant exchanges covering one focused task that evolved through targeted corrections, portability fixes, and validation.

## Key prompts
- “Use the skill-creator skill to build a new skill called `issue-extractor`.” This established the skill's purpose, safety invariants, repository discovery workflow, and Member script deliverable.
- “Did you change both locally and for the repo-wide skill?” This required checking and reporting the status of the requested `.codex` and `.agents` copies.
- “Fix two issues in scripts/create-member-issues.sh and one in the issue-extractor skill.” This directed targeted fixes for Bash 3.2 portability, issue-body wording, and versioned requirements handling.
- “Fix one bug in scripts/create-member-issues.sh and reformat the issue-extractor skill file.” This corrected the milestone regression and required line reflow without changing wording.

## Decisions and corrections
- GitHub discovery verified the E4 Member epic, existing `type.Story`, `priority.High`, and `priority.Medium` labels, and the open `v1.0` milestone. The generated script covers exactly 10 Member stories, resolves the epic by title at runtime, and remains review-only.
- The repo-wide skill was created at `.agents/skills/issue-extractor/SKILL.md`. The requested local `.codex` copy could not be created because the directory was permission-protected and the escalation request was rejected.
- The trigger example was clarified from “E3” to “Epic 3”. A versioned post-MVP story was corrected from a false contradiction to an explicit v1.0 limitation with acceptance criteria deferred.
- Bash 3.2 empty-array handling was made safe, then a regression was corrected by returning the sole milestone directly with `${open_milestones[0]}`. The skill was reformatted without wording changes.
- `bash -n`, Bash 3.2 empty-milestone validation, and skill validation passed. The generated script was never run, no `gh issue create` command was executed, and no commit was made.

## Files created or modified
- `.agents/skills/issue-extractor/SKILL.md`
- `scripts/create-member-issues.sh`
