---
name: update-user-guide
description: Use whenever a new user-facing feature is added, or an existing feature's behavior/UI/workflow changes, to add or update the corresponding section in docs/UserGuide.md.
---

## When to use this

- A new feature is implemented for any role (Manager, Trainer, Member) that a user can see or interact with.
- An existing feature's behavior changes in a way a user would notice: new fields, changed validation rules, changed UI flow, renamed buttons/menus, changed permissions, new error/status messages.
- A feature is removed or deprecated and its instructions need to be removed or marked as such.
- Reviewing a PR that touches UI controllers, FXML, or Application Services and confirming the User Guide was updated to match.

## What to build

- Not new product code — a documentation update to `docs/UserGuide.md` only.
- A new or revised section describing the feature from the end user's perspective: what it does, how to access it (menu/screen path), step-by-step usage, and any constraints or error conditions a user might hit.
- Section should match the structure/voice of existing sections in the guide (heading level, numbering style, screenshots/format if used elsewhere) rather than introducing a new format.

## Prerequisites

- `docs/UserGuide.md` exists (create it with a minimal skeleton — title, table of contents, one section per role — if this is the first entry).
- The feature/change being documented is implemented and behaves as intended (don't document planned-but-unbuilt behavior).

## Steps

1. Identify the role(s) the feature/change belongs to (Manager, Trainer, Member) and locate the matching section in `docs/UserGuide.md`. If no such section exists yet, create one following the existing heading structure.
2. Write from the user's point of view, not the implementation's: describe what the user sees and does, not class/method names or internal logic.
3. For a **new feature**: add a new subsection with a short description, the access path (e.g., "Manager Dashboard → Plans → New Plan"), numbered usage steps, and any validation/error messages the user may encounter.
4. For a **modified feature**: update the existing subsection in place — revise steps, screenshots/field names, or constraints that changed. Do not leave stale instructions describing the old behavior alongside the new.
5. For a **removed/deprecated feature**: remove the subsection, or clearly mark it as deprecated if it may still be referenced elsewhere in the guide.
6. Update the table of contents (if the guide has one) to reflect any added, renamed, or removed sections.
7. Cross-check the updated section against the actual running app (or FXML/controller behavior) to confirm accuracy — screen names, button labels, and steps must match what's implemented, not what was originally planned.
8. Include the User Guide update in the same PR as the feature/change it documents, not as a separate follow-up PR.
9. Log the session under `logs/<github-username>/<index>-update-user-guide.md`.

## Verification environments

- Manual read-through: open `docs/UserGuide.md` and confirm the new/updated section reads clearly to someone unfamiliar with the codebase.
- Manual walkthrough: follow the documented steps against the running app (`./gradlew run` or Launcher) and confirm every step and screen/button name matches exactly.
- Check the table of contents (if present) links/matches the current section headings.

## Definition of done

- [ ] `docs/UserGuide.md` has a section for every new user-facing feature introduced in this PR.
- [ ] Every modified feature's existing section is updated in place, with no stale/contradictory instructions left behind.
- [ ] Removed/deprecated features are removed or explicitly marked, not left describing dead functionality.
- [ ] Section steps and terminology (screen names, button labels) match the actual implemented UI.
- [ ] Table of contents (if present) is in sync with section headings.
- [ ] User Guide update is included in the same PR as the code change, not deferred.
- [ ] Interaction log entry written.
