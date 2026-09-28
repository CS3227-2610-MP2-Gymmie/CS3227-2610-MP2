# Reflections

In MP1 I learned to write prompts as contracts, with scope, tests, verification
commands and a handoff. In MP2 I tried to stop retyping those contracts by
moving them into the agent itself. Two skills I built for Codex include:
`create-javafx-app`, which scaffolds a new JavaFX project, and `implement-issue`,
which takes one GitHub issue from requirements to a reviewed, verified change.
I used the first once, to create Gymmie, and the second for every Member feature
I shipped. This reflection is about how I designed those two skills, how I
checked that they worked, and what they got wrong.

The evidence is in the repository: the scaffold evaluation in
[`evals/create-javafx-app/`](../../evals/create-javafx-app/), one report per issue
in [`evals/implement-issue/reports/`](../../evals/implement-issue/reports/), and my
session summaries in [`logs/shawnnygoh/`](../../logs/shawnnygoh/).

## 1. What I customized the agent to do

I only built a skill when a task was repeatable and a general-purpose agent kept
making the same kind of mistake on it. I used three questions from the skills
tutorial I presented in Week 5:

1. Are the project's conventions stable enough to write down?
2. Does a generic agent miss constraints that matter here?
3. Does domain knowledge change how the work should be implemented or tested?

| Task | Why it became a skill | What the skill carries |
| --- | --- | --- |
| Scaffolding a JavaFX app | The output should be identical every time: same package layout, same Checkstyle rules, same CI file. Generic agents invent their own. | Exact steps, copied asset files, the expected project tree and a definition of done. |
| Implementing an issue | I did it more than a dozen times. Without a fixed process, the agent skipped tests, skipped docs or claimed success without evidence. | A workflow loop, a report template and an independent review step. |

Deciding what *not* to put in a skill mattered as much. I kept `implement-issue`
repository-neutral so I could reuse it in other projects. Gymmie-specific facts
went into [`AGENTS.md`](../../AGENTS.md) instead: the role package layout, branch
naming, Javadoc rules and the rule that every change includes tests. Details of
a single issue, such as its branch name, scope and non-goals, stayed in the
prompt. The skill also works alongside the team's other skills: in the reports
it pulled in `update-user-guide`, `test-scaffold` and `gymmie-ui-design` on its
own when a change touched those areas.

## 2. `create-javafx-app`: defining it and making sure it works

### Defining it

The skill is one `SKILL.md` with a short frontmatter description, numbered steps
with explicit files and versions, the expected project tree and a definition of
done. I followed the lecture advice to write imperative steps with explicit
inputs and outputs, and to make "done" checkable.

### Testing it

I tested it the way Lecture 4 describes, before using it on Gymmie:

1. **Black-box prompts.** Four prompts in
   [`test-prompts.csv`](../../evals/create-javafx-app/test-prompts.csv): explicit
   (`$create-javafx-app`), implicit ("Set up a new JavaFX desktop app"),
   contextual ("a desktop app … using Gradle … FXML … CSS") and a negative
   control ("Add a settings screen to my existing JavaFX app"), which should not
   trigger the skill.
2. **Traces.** Each prompt ran through `codex exec --json`. A script turns the
   JSONL events into a readable HTML report, so I could see what the agent did,
   not just what it produced.
3. **A deterministic grader** with 20 checks. It verifies:
   - that the skill was triggered, and before any file changed;
   - that `./gradlew check` succeeded;
   - the required files and package paths, with no `com.` prefix;
   - the app name in the FXML;
   - byte-for-byte matches with my reference Checkstyle and CI files.
4. **A model-assisted grader** for what a script cannot judge: minimal scope, UI
   coherence, documentation accuracy and maintainability. It scores each out of
   25 against a JSON schema, and a validator rejects malformed or inconsistent
   grades.

| Version | Explicit | Implicit | Contextual | Negative |
| --- | --- | --- | --- | --- |
| v1 (deterministic / model) | 13/20 / 91 | 13/20 / 87 | 17/20 / 96 | not triggered (correct) |
| v2 (deterministic / model) | 20/20 / 96 | 20/20 / 95 | 16/20 / 94 | not triggered (correct) |

v1 failed in three ways:
- Two runs created `src/main/java/com/gymmie/` instead of `gymmie/`.
- The Checkstyle and CI files did not match the standard I wanted, because
  "follow the se-education.org standard" left the agent to write its own.
- `./gradlew run` was reported as a failure in a sandbox with no display, even
  though a JavaFX app is supposed to keep running.

For v2 I made three changes:
- Replaced the descriptions of the Checkstyle and CI files with **asset files the
  agent copies**.
- Spelled out `<app-name>` in every path.
- Added a "Verification environments" section: `run` is interactive, and without
  a display it is "not verified", not "failed".

The explicit and implicit prompts went to 20/20.

### What the evaluation taught me

- **The two graders disagreed, and both were needed.** The model grader passed
  every v1 run (87–96) while the deterministic grader failed three of them. A
  scaffold in the wrong package still *reads* as a coherent app. Hard
  requirements need hard checks; the model grader is for judgement.
- **Skill selection and execution quality are separate problems.** In a side
  experiment for the tutorial, a skill with a strong description was selected
  correctly 4/4 times with either a detailed or a minimal body. With a weak
  description it was selected only 1/4 and 2/4 times. A detailed body cannot
  rescue a skill that is never selected.
- **One run proves little.** The contextual prompt still produced
  `com.gymmie` in v2 (16/20). The skill became more reliable, but not
  deterministic, and I only ran each prompt once per version.
- **Assets need checks too.** When I committed the evaluation, I found that v2's
  `gradle.yml` asset had drifted from my reference: it omits
  `java-package: jdk+fx`. No grader checked the asset against the reference, so
  nothing noticed.

When I finally used the skill to create Gymmie (log `001`), the new
verification rule did its job. The agent reported `./gradlew run` as unverified
because it had no display, rather than as failed. `check` and `shadowJar`
passed, and the only fixes were an import order and a missing JUnit launcher
dependency.

## 3. `implement-issue`: defining it and making sure it works

### The version I threw away

My first attempt followed Lecture 5's workflow evaluation closely (logs
`008`–`012`):
- a formal workflow contract with eight rules;
- four YAML task cases drawn from real Member issues;
- a Python runner of 1,835 lines plus 850 lines of its own tests;
- deterministic, trace, model and human graders.

I ran it once, on #35. The run hit sandbox launch failures, then got
blocked by the harness's own rule against editing a shared test file, and #35
needed a separate correction session before it could ship.

When I audited it, 31 of its 40 checks either could not observe anything or had
never been implemented. Most of the code protected the harness rather than
testing the agent. Each task case could also only run once, because
implementing the issue made the case obsolete. I deleted it (log `013`). It was
my clearest example of the agent creating more work than it saved: every time I
asked it to harden the harness, it added more defensive code.

### The version I kept

The replacement is one generic [`SKILL.md`](../../.agents/skills/implement-issue/SKILL.md)
with a loop: **Understand → Plan → Implement and Test → Review → Verify →
Deliver → Report → Commit when authorized.** Two pieces replace the harness:

- **A per-issue report** ([template](../../.agents/skills/implement-issue/assets/report-template.md)).
  It records:
  - which skills ran and whether they were triggered explicitly or implicitly;
  - whether the first verification passed, and every verification run;
  - a self-assessment and the user's corrections;
  - notes for reflection.

  It is a small, manual version of the lecture's workflow metrics.
- **An independent reviewer**, [`review.sh`](../../evals/implement-issue/review.sh).
  After verification passes, the script starts a fresh, read-only Codex session
  to grade the diff against the acceptance criteria. The implementing agent
  fixes blocking findings or records why it rejected them, for at most three
  rounds. Rounds are appended and never edited, and I have the final say.

I also checked triggering. The skill once triggered on its own rewrite and
produced an `issue-000` report. So I narrowed the description to exclude changes
to agent skills and workflow tooling.

### Results on my Member issues

| Issue | Trigger | First verification passed | Review rounds (verdicts) | My corrections |
| --- | --- | --- | --- | --- |
| #36 buy membership | explicit | no | run manually; found 2 bugs | 3 |
| #37 renew | explicit | no | agent-run, then 1 by me | 2 |
| #38 cancel membership | explicit | no | 3 (fail, pass, pass) | 0 |
| #39 browse sessions | implicit | no | 5 (fail ×4, pass) | 1 |
| #40 book session | implicit | no | 3 (pass ×3) | 2 |
| #41 upcoming/past bookings | implicit | no | 2 (fail, pass) | 0 |
| #42 cancel booking | implicit | no | 3 (fail, pass, pass) | 2 |
| #43 cancellation reasons | implicit | no | 1 (pass) | 0 |
| #110 full sessions | implicit | no | 2 (fail, pass) | 1 |
| #111 cancelled bookings | implicit | no | 2 (fail, pass) | 1 |
| #112 Trainer details | explicit | yes | 2 (pass, pass) | 1 |

Across these eleven issues:
- **Triggering:** the skill ran every time. From #39 onwards it was usually
  triggered implicitly by "Implement issue #N", without naming the skill.
- **First-run verification** passed only once. Most first attempts failed
  Checkstyle or a test, and the agent fixed them before handing over.
- **The reviewer found real defects** that the first attempt missed in 8 of the
  11 issues.
- **I still made 13 corrections** in total. Only #38, #41 and #43 needed none.

## 4. What the agent handled well

- **Tests that catch real bugs.** The reviewer loop repeatedly found problems a
  green local run would hide:
  - a booking starting *exactly now* shown as upcoming (#41);
  - UI tests that waited for a "Loading" status after it could already have
    passed, so they would hang on a fast machine (#42, #110);
  - a test fixture with only one upcoming booking, so deleting the sort
    comparator would still pass (#111).

  The last is a mutation-testing argument, and I would not have spotted it
  reading the diff.
- **Keeping code, tests and docs together.** Because the skill brings in
  `update-user-guide`, the User Guide was updated in the same change as each
  feature. Every issue shipped with service tests and JavaFX UI tests, and the
  suite grew to 354 tests.
- **Pushing back on scope, once the rules were clear.** In #38 the agent
  accepted a reviewer suggestion that added an unrequested booking-history
  screen. By #41 it was rejecting non-blocking findings with a reason: "outside
  this issue's acceptance criteria".
- **Speed with a fixed process.** On the last evening I implemented, reviewed
  and merged #110, #111 and #112, each a full feature with tests and
  documentation.

## 5. Where it needed guidance, and where it created work

1. **It does not discover conventions nobody wrote down.**
   - Member code went into shared packages in #35 and #36, even though the
     Developer Guide described the role layout.
   - Codex's default `codex/` branch prefix appeared in #36, #37 and #40.

   Writing these rules into `AGENTS.md`, and naming the branch in the prompt,
   fixed both. This is the cost of keeping the skill generic: the repository
   has to state its facts explicitly.
2. **Skills sit below the platform's own instructions.** Codex's built-in
   instructions say not to add or run tests unless the user asks. So a plain
   "Implement issue #39" produced no tests, even though the skill requires them.
   The reviewer failed three rounds on missing tests, the agent declined three
   times, and the loop stopped at the cap as designed. The same thing explains
   #37. The fix was a standing rule in `AGENTS.md`, which carries the user's
   authority: every code change includes tests, and this counts as a request to
   test and verify. After that, the agent added tests and ran both Gradle
   commands without being told.
3. **An evaluator the agent controls is not independent.** In #37 the agent ran
   the reviewer in a loop and then rewrote the reviewer's verdict. Three
   structural fixes followed: rounds are append-only, "blocking" is defined
   precisely enough for the loop to converge, and there is a hard three-round
   cap.
4. **The reviewer only checks what it is shown.**
   - It never checked the Developer Guide. That gap appeared as a correction in
     #42 and then in three issues in a row (#110, #111, #112), where a use case
     or service contract went stale.
   - In #112 it gave design fit 5/5 without noticing two problems. The Member
     service now imported Trainer packages, and it read each Trainer's profile
     once per session card rather than once per Trainer.
   - Several rounds quoted old build results ("354 passing tests") instead of
     running Gradle, so I still ran `./gradlew check` myself every time.

   The self-assessment was even less reliable. It gave itself 5/5 while
   breaking a convention in #36, and 5/5 test adequacy for a change that had no
   tests.
5. **Product meaning stays with a human.** In #40, neither review round noticed
   that the database's one-booking-per-session rule meant a Member who
   cancelled could never rebook, and the button said "Already booked". I found
   it while reviewing. The fix of reactivating the existing row is now a
   documented trade-off.
6. **Where it created extra work:**
   - **Copied patterns.** #35 added Refresh buttons nobody asked for. Later
     issues copied them, and they became the main source of UI race bugs in
     review.
   - **Commit messages.** In #110 and #111 the agent ignored the message I had
     drafted. It wrote one as a single 230-character line, and another with
     literal `\n` characters where the line breaks should be. I now give the
     exact message and require `git commit -F`.
   - **"Verified" without running anything.** One tooling session "verified"
     `review.sh` with `git diff --check`, which only finds whitespace errors.
     The script did not even parse on macOS's default bash. A check that does
     not run the artifact is not verification.

## 6. What I would change next time

- **Make the documentation step search both guides.** Three issues in a row
  would have been caught by one instruction: search the User Guide *and* the
  Developer Guide for the screen or service being changed.
- **Give the reviewer a fixed checklist and fresh evidence.** Reuse my MP1
  review checklist: dependency direction, query counts, "could this test pass
  for the wrong reason?" and whether the docs still match. Make the reviewer run
  the build itself or receive its output, instead of trusting old reports.
- **Take commit messages from the user verbatim,** or add a small `commit` skill
  that writes the message to a file and checks subject length and wrapping.
- **Enforce rules with hooks, not reminders.** Lecture 5's `PreToolUse` hooks
  could block `git commit -m` with escaped newlines, or refuse to finish before
  `./gradlew check` has run. Right now these depend on me noticing.
- **Rerun the scaffold evaluation as a regression suite.** Run each prompt
  several times to measure how often it fails rather than whether it can pass.
  Also add a check that assets match their reference. The contextual prompt
  suggests the package rule belongs in the description as well as the body.
- **Keep some trace metrics for `implement-issue`.** When I removed the harness
  I also lost command counts and token use. A single script that summarizes the
  JSONL trace next to each report would restore them without the overhead.

## 7. What I learned about designing a single agent

- **Put each rule where it has authority and the right lifetime.** Platform
  instructions win over repository instructions, which win over skills.
  - Portable *process* goes in a skill.
  - Stable *repository facts* and standing permissions go in `AGENTS.md`.
  - *This issue's* scope goes in the prompt.

  Most of my corrections came from a rule sitting in the wrong layer.
- **The description decides whether a skill runs; the body decides how well.**
  Test the two separately.
- **Independence has to be built into the structure.** A fresh, read-only,
  append-only, capped reviewer found real bugs in most issues. It was still
  only as good as the checklist it was given. Architecture, product meaning and
  "did we actually run it" stayed my job.
- **Start with checks you can observe, and grow them from real failures.** My
  1,835-line harness measured less than a 64-line report template and a
  56-line review script. Every useful rule in the final setup started as a specific
  failure I had seen.
