# Implementation Report

- **Issue reference:** [#112](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/issues/112)
- **Title:** Show Trainer details to Members.
- **Date:** 2026-09-29.
- **Branch:** implement-issue-112-trainer-details.
- **Base commit:** 8d383cc280dd9d0e0d266b69f5df115b665451a1.
- **Skill invocation:** Explicit.

## Summary

| Skills used | Verification passed on first run | Final verification result | Tests added or changed | Docs updated | Number of user corrections |
| --- | --- | --- | --- | --- | --- |
| implement-issue (explicit), gymmie-ui-design (implicit), update-user-guide (implicit) | Yes | Passed: `./gradlew check` and `./gradlew test -PuiTests=true` | MemberSessionBrowseServiceTest; MemberSessionBrowseNavigationTest | docs/UserGuide.md; docs/DeveloperGuide.md | 1 |

## Acceptance criteria

| Criterion | Status (completed, blocked, awaiting-decision, or incomplete) | Evidence (test name, check, or observation) |
| --- | --- | --- |
| Members can see Trainer synopsis and specialisations while browsing sessions and filtering by Trainer. | completed | MemberSessionBrowseNavigationTest.filtersSessionCardsByTrainer checks both details on the selected Trainer's card. |
| Profile data is read through a Member-authorized service, without Member access to Trainer services. | completed | MemberSessionBrowseService.upcomingSessions requires the Member role before loading sessions and profile data; MemberSessionBrowseServiceTest.rejectsUnauthenticatedNonMemberAndDeactivatedMemberCallers covers authorization. The Member controller calls only the Member session browse service. |
| Trainers without profile details show “No profile details.” | completed | MemberSessionBrowseNavigationTest.refreshReloadsChangedBookingCountsAndSessionAvailability checks the exact empty-profile fallback; the service test checks an empty profile result. |
| Service and UI tests are present and the User Guide is updated. | completed | MemberSessionBrowseServiceTest and MemberSessionBrowseNavigationTest; updated Browse sessions by Trainer in docs/UserGuide.md. |

## Workflow checklist

- [x] Inspected relevant instructions, source, tests, documentation, conventions, and repository skills before editing.
- [x] Mapped criteria to intended changes and checks.
- [x] Kept changes in scope. Unexpected files and why: none.
- [x] Ran verification on the final code state; reran checks after relevant later edits.
- [x] Other tools used besides skills above: GitHub CLI, Git, shell, Gradle.
- [x] No commit, push, or external action occurred without authorization.

## Verification runs

| # | Command | Result | What was fixed next |
| --- | --- | --- | --- |
| 1 | `./gradlew check` | Passed. | None. |
| 2 | `./gradlew test -PuiTests=true` | Passed. | None. |
| 3 | `git diff --check` | Passed. | None. |
| 4 | `./gradlew check` | Passed after the human-review follow-up changes. | None. |
| 5 | `./gradlew test -PuiTests=true` | Passed after the human-review follow-up changes. | None. |

## Self-assessment (self-assessed)

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Acceptance criteria met | 5 | Member cards show profile details, empty profiles have the requested message, and access stays in the Member service. |
| Test adequacy | 5 | Service tests verify data and authorization; the opt-in JavaFX test checks populated and empty UI states. |
| Scope and design fit | 5 | Reuses the existing browse flow and Trainer profile repository; no Trainer service is exposed to Members. |
| Consistency with repository conventions | 5 | Gradle checks and whitespace validation passed; the Member User Guide section was updated. |

## Independent review



### Round 1

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Members can see Trainer synopsis and specialisations while browsing sessions and filtering by Trainer. | 5 | Session cards render both fields; `filtersSessionCardsByTrainer` verifies their contents after filtering. |
| Profile data is read through a Member-authorized service, without Member access to Trainer services. | 5 | `upcomingSessions()` checks `Role.MEMBER` before reading profiles through the repository. Authorization tests cover unauthenticated, Trainer, Manager, and deactivated Member callers. |
| Trainers without profile details show “No profile details.” | 5 | Repository defaults provide an empty profile; service and UI tests verify the empty data and exact fallback message. |
| Service and UI tests are present and the User Guide is updated. | 5 | Both test classes were updated, alongside the User Guide’s “Browse sessions by Trainer” section. |
| Acceptance criteria met | 5 | Current implementation and assertions cover all four criteria. |
| Test adequacy | 5 | Tests exercise populated profiles, absent profiles, filtering, authorization, and existing booking behavior. Saved results show all nine relevant tests passed without skips. |
| Scope and design fit | 5 | Reuses the existing Member browsing flow, transaction boundary, and profile repository; specialization lists are defensively copied. |
| Consistency with repository conventions | 5 | Javadoc and role organization follow repository guidance. Saved results show 354 passing tests and zero Checkstyle errors; diff whitespace checks pass. Gradle was not rerun during this read-only review. |

**Overall:** pass. No blocking defects, unmet criteria, or documented convention violations were found in the current working-tree changes.

**Blocking:** None.

**Non-blocking:** None.

Reviewed: 2026-09-29; base: master; model: gpt-6-astra; HEAD: 8d383cc; Uncommitted changes: yes.


### Round 2

| Area | Score (1–5) | Evidence |
| --- | --- | --- |
| Members can see Trainer synopsis and specialisations while browsing sessions and filtering by Trainer. | 5 | Session cards render both fields; `filtersSessionCardsByTrainer` verifies their contents after filtering. |
| Profile data is read through a Member-authorized service, without Member access to Trainer services. | 5 | `upcomingSessions()` checks `Role.MEMBER` before repository reads. Authorization tests cover unauthenticated, Trainer, Manager, and deactivated Member callers. |
| Trainers without profile details show “No profile details.” | 5 | Repository defaults supply an empty profile; service and UI assertions verify empty data and the exact fallback text. |
| Service and UI tests are present and the User Guide is updated. | 5 | Both test classes contain relevant assertions; the User Guide describes profile details and the fallback. |
| Acceptance criteria met | 5 | Implementation and tests cover all four reported criteria. |
| Test adequacy | 4 | Populated profiles, absent profiles, filtering, authorization, and booking regressions are covered; profile-read caching lacks a direct regression assertion. |
| Scope and design fit | 5 | Reuses the existing transaction and repository, caches profiles per Trainer per request, and documents the Member-to-Trainer dependency. |
| Consistency with repository conventions | 5 | Changed Javadoc follows conventions; whitespace checks pass. Saved results show 354 passing tests, no skips, and zero Checkstyle errors. Gradle was not rerun during this read-only review. |

**Overall:** pass. No blocking defects, unmet acceptance criteria, or documented convention violations were found.

**Blocking:** None.

**Non-blocking:**

- `MemberSessionBrowseServiceTest` verifies details across two sessions belonging to one Trainer, but would still pass if per-session profile reads returned; consider asserting one repository lookup per Trainer per request.

Reviewed: 2026-09-29; base: master; model: gpt-6-astra; HEAD: 8d383cc; Uncommitted changes: yes.

## Review responses

| Round | Finding | Fixed or rejected | Reason or change |
| --- | --- | --- | --- |
| 2 | The service test would still pass if profile reads went back to once per session. | Rejected | The cache only affects performance; the results are the same either way. The planned opt-in scale test covers browse timing, and a counting fake repository is outside the scope of #112. |

## User corrections

Human review found the stale Developer Guide profile-read contract and UC1 step, the per-session profile reads, and the undocumented cross-role dependency, none of which the independent review raised. Updated the Developer Guide, cached profile reads once per Trainer, and corrected the fallback message punctuation.

## Notes for reflection

- **Where the skill helped:** Kept issue criteria, authorization, UI, tests, and both user-facing and developer documentation together.
- **Where it needed guidance:** The independent review missed the stale Developer Guide contract, the missing UC1 detail, repeated profile reads, and the undocumented cross-role dependency. Human review identified these gaps. The User Guide skill's interaction-log instruction conflicts with the user's request not to create session logs; no interaction or session log was created.
- **What to change:** Check Developer Guide architecture contracts and use cases, as well as the User Guide, when a user-facing feature crosses role-package boundaries.
