# Issue Workflow Skill Revisions

## Goal
Create and refine a reusable issue-implementation workflow skill, bundled contract, and optional evidence template. Keep the package portable, concise, and aligned with user authorization and repository conventions.

## Scope
Five user-assistant exchanges, one focused task refined through successive requirements.

## Key prompts
- “Create version 1 of an issue-implementation workflow for this Gymmie repository.” This established the original contract and skill deliverables, including a staged implementation and verification process.
- “Remove Gymmie-specific content from both files, including Java, JavaFX, Gradle assumptions, named project skills, fixed guide paths, role/domain examples, and repository-facts snapshots.” This made the package reusable across repositories and kept the contract bundled with the skill.
- “Make a focused cleanup of the implement-issue skill and add one reusable evidence template.” This added optional persistent evidence recording while keeping session logs separate and leaving human review pending.
- “Remove requirements to manually record SHA-256 values or exact workflow revisions.” This simplified evidence identity while retaining Git-context checks, checked-code-state evidence, and evaluator responsibility for controlled comparisons.

## Decisions and corrections
The contract was moved into the skill's `references/` directory after checking consumers; no separate documentation pointer was needed. Requirements were consolidated and renumbered IW-01 through IW-08; review applies to every implementation task, while fixing findings is conditional. The evidence template is optional, uses starting context rather than mandatory revision/identity fields, and distinguishes agent observations from pending human judgment. Controlled evaluations preserve equivalent inputs and record the workflow used; ordinary issue work does not require evaluation metadata. No application changes, commits, or pushes were made.

## Files created or modified
- `.agents/skills/implement-issue/SKILL.md`
- `.agents/skills/implement-issue/references/workflow-contract.md`
- `.agents/skills/implement-issue/assets/issue-record-template.md`
