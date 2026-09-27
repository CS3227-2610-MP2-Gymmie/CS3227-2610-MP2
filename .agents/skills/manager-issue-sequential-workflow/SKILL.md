````markdown
---
name: sequential-issue-workflow
description: Create, stage, commit, and open gated pull requests for Manager-role issues in an ordered, non-destructive sequence with human checkpoints.
---

## When to use this

- When initializing and executing a batch of dependent Manager-role issues.
- When working on Git operations that require deterministic step-by-step verification, strict gating against race conditions, and surgical file staging.
- Whenever tasks have downstream branch dependencies that require PR merging before the next branch is created.

## What to build

A gated orchestration workflow ensuring:

1. Manager issue IDs are generated deterministically and captured via `gh`.
2. Feature branches stage only relevant files without dirtying or carrying over working-tree artifacts.
3. Every commit message dynamically references real GitHub issue numbers (`Fixes #<id>`).
4. Pull requests are opened via `gh pr create` and gated behind human review and CI verification before proceeding to downstream branches.

## Prerequisites

- GitHub CLI (`gh`) authenticated with repository default set to `upstream` (`CS3227-2610-MP2-Gymmie/CS3227-2610-MP2`).
- Working tree clean or containing only intended task artifacts.
- Local repository synchronized with latest `upstream/master`.
- `scripts/create-manager-issues.sh` available locally.

## Steps

### Step A: Issue Creation & ID Discovery

1. Run `bash scripts/create-manager-issues.sh`.
2. Parse the script output and list all created or matched issue numbers and their titles.
3. **STOP & WAIT**: Report the extracted issue numbers to the user and request confirmation for Issue 1 before executing any Git branch or stage operations.

### Step B: Issue 1 Branch, Targeted Staging, and PR

1. Create and switch to the first feature branch:
   ```bash
   git checkout -b issue-1-nextid
   ```
````

2. Update the designated log file (`logs/naa-siuuuu-ff/001-add-nextid-account-plan.md`), replacing any placeholder with the real Issue 1 ID confirmed in Step A.
3. Surgically stage **only** the target repository and log files:

- `src/main/java/gymmie/persistence/repository/AccountRepository.java`
- `src/main/java/gymmie/persistence/repository/MembershipPlanRepository.java`
- `src/main/java/gymmie/persistence/sqlite/SqliteAccountRepository.java`
- `src/main/java/gymmie/persistence/sqlite/SqliteMembershipPlanRepository.java`
- `src/test/java/gymmie/persistence/sqlite/SqliteRepositoriesTest.java`
- `logs/naa-siuuuu-ff/001-add-nextid-account-plan.md`

4. Verify `git status` to ensure untracked scripts or auxiliary files are **not** staged.
5. Commit using the verified issue number:

```bash
git commit -m "Add nextId to AccountRepository and MembershipPlanRepository

Fixes #<real-issue-1-id>"

```

6. Push the branch to `origin`:

```bash
git push -u origin issue-1-nextid

```

7. Open the pull request via GitHub CLI:

```bash
gh pr create --base master --title "feat(persistence): add nextId to account and plan repositories" --body "Fixes #<real-issue-1-id>"

```

8. **STOP & WAIT**: Output the generated PR URL and pause. Do not proceed until the user explicitly confirms the PR has passed CI and is merged into `upstream/master`.

### Step C: Tooling Branch & PR (Independent)

1. Once user grants permission, return to `master`:

```bash
git checkout master

```

2. Create and switch to the tooling branch:

```bash
git checkout -b add-manager-tooling

```

3. Stage only the tooling scripts and pointer documentation:

- `scripts/create-manager-issues.sh`
- `GEMINI.md`

4. Commit:

```bash
git commit -m "Add manager issue-creation script and Gemini pointer file"

```

5. Push to `origin` and open a PR:

```bash
git push -u origin add-manager-tooling
gh pr create --base master --title "chore(tools): add manager issue creation script and agent guide" --body "Adds tooling scripts for manager issue generation."

```

6. Output PR URL and await user instruction.

### Step D: Issue 2 Branch Initialization (Gated)

1. Verify human confirmation that Issue 1 PR has been merged into `upstream/master`.
2. Sync the local `master` branch:

```bash
git checkout master
git pull upstream master

```

3. Create the clean branch for Issue 2:

```bash
git checkout -b issue-2-plan-service

```

4. **STOP & WAIT**: Halt execution immediately. Await user prompt containing specific implementation instructions for Issue 2.

## Verification environments

- `git status` shows clean branch boundaries and no unintended staged files during targeted commits.
- `gh pr status` confirms PR creation targeted at `upstream/master`.
- `./gradlew check` passes before pushing any code-bearing branch.

## Definition of done

- [ ] All six issue numbers confirmed and mapped.
- [ ] No blanket `git add .` executed; only explicitly listed files staged per step.
- [ ] No unconfirmed placeholders (e.g. `<issue-1-number>`) committed to Git history or logs.
- [ ] Pull requests created via `gh pr create` with valid closing keywords (`Fixes #<id>`).
- [ ] Explicit stop-and-wait pause honored between every lettered milestone.
- [ ] Next feature branch (`issue-2-plan-service`) branches off a freshly synchronized `master` containing merged Issue 1 commits.

```

```
