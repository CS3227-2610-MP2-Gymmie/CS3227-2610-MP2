# v1.0 Docs Consistency Follow-up

## Goal
Bring Gymmie's user and developer documentation and landing page in line with the v1.0 implementation, then record the work after the user requested a session log.

## Scope
Three user-assistant exchanges around one focused documentation task and its follow-up corrections.

## Key prompts
- “Docs consistency pass before the v1.0 release.” This set the release-readiness goal for the guide and website pass.
- “Verify every statement you add or keep against the code.” This required checking documentation claims against controllers, services, models, build configuration, and CI.
- “Leave the screenshot image links and ‘Screenshot placeholder’ lines in place; a teammate is adding the images.” This preserved the pending User Guide screenshot slots.
- “Remove every mention of reflections from docs/DeveloperGuide.md, docs/UserGuide.md, docs/index.md and README.md.” This kept reflections separate from product documentation.
- “Run ./gradlew check. Do not commit yet. Do not create session logs until I ask.” The follow-up required verification and deferred both the log and commit until the later explicit request.

## Decisions and corrections
- Fetched `upstream` and created `fix-docs-consistency` from `upstream/master`.
- Updated the User Guide for JDK and release JAR launch, storage location, plan limits, password reveal, Trainer deactivation, booking states and sorting, cancellations, and platform limitations. Preserved its screenshot links and placeholder lines.
- Added the concrete Manager, Trainer, and Member walkthrough back to §3.2, with both launch methods and the same-folder data note.
- Updated the Developer Guide's development process, future enhancements, acknowledgements, setup instructions, known limitations, and tradeoffs. Removed the forthcoming-reflections note per user decision; did not create a reflections file.
- Replaced the landing page hero with `images/Ui.png`, corrected quick start and role descriptions, removed TODO comments, and added the requested Cayman Jekyll configuration.
- The Downloads references were outside sandbox access; the user waived that criterion after a human reviewer compared against them. The report records this and the two user corrections. The user explicitly said not to rerun `review.sh` for the docs-only follow-up.
- `./gradlew check` passed after the follow-up. No commit was made before the user asked to commit this log.

## Files created or modified
- `docs/UserGuide.md`
- `docs/DeveloperGuide.md`
- `docs/index.md`
- `docs/_config.yml`
- `evals/implement-issue/reports/docs-consistency-v1.0.md`
