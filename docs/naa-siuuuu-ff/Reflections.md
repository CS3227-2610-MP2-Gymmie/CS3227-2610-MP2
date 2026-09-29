# Reflections

## 1. Introduction: Guarded Agentic Engineering

In MP1, working with AI assistants primarily involved iterative prompting and reacting to what the model generated. In MP2, the goal shifted towards designing structured agents with predefined skills, operational boundaries, and verification loops. 

Modern AI coding agents are extraordinarily powerful: they can continuously churn out plausible, well-formatted, and correct-looking code at immense speeds. However, this velocity introduces a subtle and dangerous risk: **the silent accumulation of technical debt**. Because AI-generated code looks superficially sound, minor logical oversights, architectural inconsistencies, and leaky abstractions can easily slip through. When subsequent tasks are built on top of faulty foundations, finding and debugging issues becomes exponentially more difficult, expensive, and time-consuming.

My core philosophy throughout MP2 was therefore: **slow down at the start to go faster and safer later**. By investing upfront in planning, defining explicit domain rules, and enforcing human-in-the-loop checkpoints, we struck a balance—reaping the tremendous speed advantages of AI without paying the compounding penalties of technical debt.

---

## 2. My Tooling Setup: Splitting Planning from Implementation

My tooling changed partway through the project, and the setup I ended up with turned out to be one of the more useful lessons of MP2.

- **Codex was unavailable.** I was logged out of my school account due to technical issues (still being resolved at the time of writing), so I could not use Codex at all.
- **Antigravity, first with Gemini, then with Claude.** I began running the implementation agent inside Antigravity with Gemini, then switched to a Claude model within Antigravity for the rest of the work.
- **A separate chat assistant for planning and review.** Alongside the implementation agent, I used Claude's web chat interface to plan prompts, break issues into steps, and review the agent's output before accepting it. Antigravity did the actual implementation.

This split was deliberate and turned out to be an efficient use of tokens. Planning and review are conversational, and the Claude web interface generally consumes less of my quota for that kind of back-and-forth. Gemini inside Antigravity had effectively unlimited usage, so it could absorb the high-volume implementation work. Instead of spending an expensive agent's budget on thinking out loud, I did the thinking in the cheap place and sent the agent a precise, already-reviewed prompt.

The split also added a second pair of eyes. The model reviewing the output was not the model that wrote it, so it was less inclined to rationalise its own mistakes. It still did not replace my own review, but it caught things faster than I would have alone.

---

## 3. What I Customized the Agent to Do

I developed and maintained several skills tailored to the Manager epic and cross-cutting repository needs:

| Skill | Problem it Solved | Core Guardrails & Invariants |
| --- | --- | --- |
| `manager-issue-sequential-workflow` | Autonomous agents racing ahead, branching off unmerged commits, and leaking untracked files into PRs. | Mandatory human **stop-and-wait gates**, dynamic issue resolution via `gh`, surgical staging of intended files only. |
| `manage-membership-plans` | Generic agents inventing arbitrary CRUD operations without respecting gym domain invariants. | Validation rules (duration > 0, price >= 0, unique name), Manager role authorization, and delegation to domain repositories. |
| `soft-delete-archive-pattern` | Naive SQL/ORM implementations calling hard `DELETE`, destroying audit trails and historical purchase records. | Enforces `isArchived` flag, `archive()`/`restore()` contracts, and isolates administrative queries from active queries. |
| `update-user-guide` | Documentation drifting out of sync with rapid UI and service changes. | User-perspective documentation updates shipped in the exact same PR as the feature. |
| `checkstyle-compliance-check` | Checkstyle and Javadoc formatting violations failing CI builds. | Pre-commit automated style validation matching SE-EDU conventions. |

### Designing `manager-issue-sequential-workflow`
The Manager epic required sequential development across multiple architectural layers: persistence `nextId` helpers, domain models, services, and JavaFX controllers. Left unconstrained, an autonomous agent tasked with multiple stories might attempt to branch downstream features before upstream pull requests are merged, or use `git add .` and bundle scripts, temporary logs, or scratch files into unrelated pull requests.

I wrote this skill **before** the agent started on any Manager work, not as a reaction to something going wrong. The risks above were predictable from how agents behave by default, and I wanted the rules in place before the first branch was cut rather than retrofitted after a messy PR.

The `manager-issue-sequential-workflow` skill codified strict discipline:
1. **Dynamic Discovery**: It queried the GitHub CLI directly for issue IDs, ensuring commit messages and PR bodies linked real issue numbers (`Fixes #<id>`).
2. **Stop-and-Wait Gates**: The agent was required to halt and report issue IDs to the human engineer before creating branches, staging files, or executing subsequent tasks.
3. **Surgical Staging**: It explicitly specified target files for each stage, completely avoiding `git add .` to protect against working-tree contamination.

### Encoding Domain Invariants as Safeguards
Generic agents typically treat every entity as a simple CRUD resource, defaulting to `DELETE FROM table WHERE id = ?`. In a gym management system, deleting a membership plan that members have purchased breaks historical records and foreign key relationships.

Creating `soft-delete-archive-pattern` and `manage-membership-plans` guaranteed that regardless of which agent conversation implemented a feature, the business logic remained consistent: plans with purchase histories are safely archived rather than destroyed, active queries filter out archived entries by default, and only Managers possess authorization to perform lifecycle transitions. These skills acted as permanent guardrails across the codebase.

### How I Validated the Skills
A skill is only useful if the agent actually follows it, so I did not trust any new skill on important work straight away. For each one, I first let the agent use it on a **small, low-stakes task** that I fully understood and could easily fix by hand if something went wrong. I watched whether the agent followed the steps as written: whether it stopped at the gates, staged only the named files, and ran the checks. Only after that trial did I let the skill drive larger tasks and real PRs.

In practice I did not need to revise any of my skills after these trials. The models were capable enough to follow well-written instructions faithfully, and the skills worked as intended on the first real use. I think this says less about the models being clever and more about how much a clear, concrete, step-by-step skill reduces the room for misinterpretation. The vague skills are the ones that need rewriting, and the effort spent making each one precise upfront paid off here.

---

## 4. The Power of Small, Sequential Pull Requests

One of the most effective decisions in our development process was decomposing large feature requests into small, single-concern deliverables. For the Manager epic, we structured the work into six clean, PR-sized chunks:

1. **Issue 1 (PR #99)**: Add `nextId` generation to `AccountRepository` and `MembershipPlanRepository`.
2. **Issue 2 (PR #100)**: Implement `MembershipPlanService` with validation and role checks.
3. **Issue 3 (PR #101)**: Implement Membership Plan management JavaFX UI.
4. **Issue 4 (PR #102)**: Implement Account Provisioning service.
5. **Issue 5 (PR #103)**: Implement Account Provisioning UI.
6. **Subsequent Enhancements (PRs #109, #113, #118, #132, #139)**: Currency in dollars, password visibility toggles, landing page, User Guide expansion, and UI screenshots.

### Why Small PRs Succeeded
- **Reviewability**: Reviewing a 50-to-150-line diff focused strictly on service logic or repository queries takes minutes and allows the human to scrutinize every edge case. Reviewing a 1,000-line monolithic PR generated by an AI almost guarantees missed bugs.
- **Single Responsibility & Blast Radius**: Each PR had one explicit purpose. When an issue occurred (such as a Checkstyle indentation error in PR #99), fixing it was isolated and trivial.
- **Auditability and Bisecting**: If a regression emerged, pinpointing the exact commit that introduced it was immediate.
- **Workflow Planning Before Code**: Because the entire sequence was mapped out before writing code, tasks progressed smoothly without architectural rework or conflicting abstractions.

### Redo Rather Than Repair
Small PRs also changed how I would handle a bad agent output. When an agent produces a large, tangled change, the tempting move is to patch it until it works. My view is that if an agent makes a real mess, especially one spanning a lot of code, it is usually cheaper and safer to **discard the attempt and redo the task** with a better prompt than to untangle it. Patching AI output you do not fully understand is exactly how silent technical debt gets in. Small PRs make this cheap, because throwing away a small single-concern change costs very little. This never became necessary in my work, but having the option kept the stakes of each agent run low.

---

## 5. Verification: Balancing Automated Testing with Manual Intuition

Our verification strategy relied on two complementary tiers.

### Automated Verification, and the Lesson of PR #99
My very first Manager PR, PR #99, failed CI on a Checkstyle indentation error. The cause was simple: up to that point, `./gradlew check` was not being run **explicitly** before pushing. The agent's own tests had passed, but the full style and Javadoc checks that CI enforces had never run locally.

After that, I made it a hard rule that `./gradlew check` must run and pass before anything is committed or pushed, and the `checkstyle-compliance-check` skill made that part of the agent's routine. **No PR failed CI after PR #99.** One failure and one explicit rule were enough to eliminate the whole category of problem. The rule gave a reliable floor of confidence:
- Domain invariants, validation logic, and authorization rules were verified across happy paths and error states by focused JUnit 5 tests.
- Style checks and Javadoc rules were verified before pushing, preventing CI failures on the shared upstream repository.

The broader lesson is that an agent reporting "tests pass" is not the same as the project's definition of done passing. The verification command has to be named explicitly, or the agent will quite reasonably run a narrower one.

### Manual Verification and Exploratory Testing
Automated unit tests ensure the code does what the developer asked; manual testing ensures the application behaves the way a human user expects.

After automated tests passed, I conducted manual walkthroughs on the running desktop app (`./gradlew run`). This caught subtle usability details that unit tests easily overlook:
- A **button that was missing the correct box and colour scheme** around it, so it did not match the styling of the rest of the screen. No unit test would ever flag this; it was only obvious by looking at the running app, and it was quick to fix once seen.
- In Issue #113, verifying that the password-toggle icon visually synchronized with the text field state when provisioning accounts.
- In Issue #109, confirming that entering plan prices in dollars properly translated to cents internally while displaying formatted currency cleanly on UI cards.
- In Issues #115 and #138, executing manual user flows across all three roles (Manager, Trainer, Member) to capture all 12 application screenshots and confirm that documented error messages matched runtime behavior word-for-word.

---

## 6. Team Collaboration & Skill Ecosystem

Across the team, members contributed specialized skills:
- **Shawn** built foundational scaffolding (`create-javafx-app`) and issue extraction (`issue-extractor`).
- **Taia** built UI styling guidance (`gymmie-ui-design`) and test scaffolding (`test-scaffold`).
- **I** contributed sequential workflow orchestration (`manager-issue-sequential-workflow`), domain patterns (`manage-membership-plans`, `soft-delete-archive-pattern`), style verification (`checkstyle-compliance-check`), and documentation maintenance (`update-user-guide`).

### Managing Overlap and Context
As the project evolved, the number of skills in `.agents/skills/` grew. We were deliberate about keeping skill descriptions tightly scoped to avoid triggering multiple overlapping skills on simple requests. Even when multiple skills touched related areas (such as `implement-issue` calling `update-user-guide` or `checkstyle-compliance-check`), the explicit definitions of done prevented conflicting instructions or redundant work.

---

## 7. What I Would Do Differently & Key Takeaways

### 1. Focus Even More on Upfront In-Person Collaboration
If I were to start MP2 over, I would prioritize sitting down in person with my teammates even earlier to establish unanimous alignment on the system requirements, entity relationships, and shared contracts before any agent writes code. While agents can rapidly adapt code, human misalignment on requirements cascades into rework across multiple issues and PRs.

### 2. Make the Definition of Done Explicit from Day One
PR #99 would not have failed if `./gradlew check` had been an explicit, mandatory step from the start. Next time, the exact verification command goes into the agent's instructions before the first task, not after the first failure.

### 3. Plan Cheaply, Implement Deliberately
Separating planning and review (in a chat assistant) from implementation (in the agent) made better use of limited token budgets and added an independent review step. I would set this up deliberately from the beginning instead of arriving at it because a tool was unavailable.

### 4. Clarity Before Autonomy
Before granting autonomy to an agent, **absolute clarity is paramount**. The human engineer remains ultimately responsible for every line of code committed to the repository. Handing the reins to an AI without a clear mental picture of the architecture, edge cases, and design intricacies leads to fragile software. Slowing down to understand the codebase and establish explicit boundaries is what enables high velocity later.

### 5. Deliberate Guardrails Over Unchecked Freedom
Modern foundation models possess impressive reasoning capabilities, but their primary goal is satisfying the immediate prompt. Without guardrails, they optimize for short-term completion over long-term maintainability. Explicit guardrails—stop-and-wait gates, non-destructive archiving contracts, single-concern PRs, and strict verification commands—keep the agent focused, predictable, and aligned with sound software engineering principles.
