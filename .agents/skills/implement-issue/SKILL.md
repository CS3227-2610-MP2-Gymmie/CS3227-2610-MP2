---
name: implement-issue
description: Implement a scoped issue from its requirements through review, verification, delivery, and a short implementation report. Use for direct issue-scoped implementation requests; not for suggestions, issue drafting, review-only, or test-only work.
---

# Implement an Issue

Follow the user's authorized scope and applicable repository instructions. Raise material conflicts before dependent work; do not assume this skill overrides them. Discover conventions, instructions such as `AGENTS.md`, relevant repository skills, and the repository's build and verification tools by inspecting the current repository. Do not assume a language or toolchain.

Use this loop proportionally: **Understand → Plan → Implement and Test → Review → Fix as needed → Verify → Deliver → Commit when authorized.** Treat issue requirements and user corrections as the source of acceptance criteria. Keep each stage lightweight when the task is small.

## Workflow

1. **Understand.** Inspect enough applicable instructions, source, tests, and documentation to establish scope from evidence. Resolve routine uncertainty from the repository; identify material ambiguity or conflict and its impact, and pause only work that depends on a decision.
   - Input: user request and repository state. Output: scoped outcome and acceptance criteria, with unresolved decisions identified.
2. **Plan.** Record a concise working map from each criterion to intended changes and observable checks; revise it when evidence changes scope, assumptions, dependencies, or expected behavior. Inspect the current branch, revision, and working-tree status before editing; preserve and distinguish unrelated work. A clean tree or baseline commit is not required.
   - Input: scope and baseline. Output: proportional criterion-to-change-to-check plan.
3. **Implement and Test.** Make focused changes within authorized scope. Add or update suitable tests and affected documentation according to repository conventions; when required documentation is missing, add the smallest suitable entry within scope. Preserve test intent; never weaken or bypass tests to obtain a pass.
   - Input: plan. Output: scoped changes and appropriate tests or documented reasons none apply.
4. **Review.** Compare the final diff with the request, plan, and relevant invariants. Fix findings and review affected changes again; revise the plan when evidence changes scope or approach. After two materially different failed approaches to the same obstacle, stop speculative retries and state what decision, input, or capability is needed.
   - Input: diff and plan. Output: reviewed changes, resolved findings, or a clearly statused stopping point.
5. **Verify.** Run the repository's own checks suited to the criteria against the final relevant code state. Add interface checks when affected behavior warrants them. If a necessary check cannot run, try a suitable in-scope alternative when available. Rerun checks when later edits could affect their result. Report unavailable evidence; never claim an unsupported pass.
   - Input: reviewed final code state. Output: actual check results associated with criteria and the state checked.
6. **Deliver.** Summarize outcomes, evidence, assumptions, limitations, and remaining work honestly. Use only these criterion statuses: **completed** when the outcome has evidence; **blocked** when a dependency, environment, or external state prevents progress; **awaiting-decision** when a material choice needs the user's input; and **incomplete** when work or evidence remains without such a blocker.
   - Input: verification results. Output: concise, evidence-based delivery summary.
7. **Commit when authorized.** Follow repository Git conventions and preserve existing work. Stage and commit only when the user authorized it; authorization in the request is sufficient. Prepare required deliverables, including the report, before committing. Push requires separate authorization. Perform external actions only within explicit user authorization.
   - Input: reviewed and verified changes plus authorization. Output: reported commit/action results, or an uncommitted delivery.
8. **Report.** At delivery, write a short implementation report from [assets/report-template.md](assets/report-template.md) to the location specified by the user or repository instructions. Otherwise use `evals/implement-issue/reports/issue-<NNN>-<slug>.md`. When a commit is authorized, include the report in it. Summarize the report in chat. If the user later corrects or redirects the work during the session, update its **User corrections** section.
   - Input: task evidence and template. Output: completed report at the selected location and a chat summary.

## Boundaries

- Use for direct implementation or repair of scoped issue work, including requests with acceptance criteria or an issue identifier.
- Do not use for implementation suggestions, issue drafting, review-only requests, or test-only tasks.
- Apply repository and environment instructions with the user's request. Surface material conflicts instead of silently choosing precedence.
