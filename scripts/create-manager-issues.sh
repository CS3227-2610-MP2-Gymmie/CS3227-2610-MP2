#!/usr/bin/env bash
set -euo pipefail

# Source: Manager feature requirements, manage-membership-plans skill,
# soft-delete-archive-pattern skill, update-user-guide skill, and repository
# contracts. One issue per PR-scoped unit of work.
# Repository labels and sole open milestone (v1.0) verified on 2026-09-27.
# Review and run manually. No issues are created while generating this script.
REPO="${REPO:-CS3227-2610-MP2-Gymmie/CS3227-2610-MP2}"
EPIC_TITLE="E2: Build manager features"
MILESTONE_TITLE="${MILESTONE_TITLE:-}"

list_all_issues() {
  # Paginate without a fixed limit, include closed issues, and exclude PRs.
  gh api --paginate "repos/${REPO}/issues?state=all&per_page=100" \
    --jq '.[] | select(has("pull_request") | not) | [.number, .title] | @tsv'
}

find_issue_number() {
  local title="$1" number existing_title
  while IFS=$'\t' read -r number existing_title; do
    if [[ "$existing_title" == "$title" ]]; then
      printf '%s\n' "$number"
      return 0
    fi
  done <<< "$ISSUES"
  return 1
}

resolve_epic_number() {
  local number title resolved=""
  while IFS=$'\t' read -r number title; do
    if [[ "$title" == "$EPIC_TITLE" ]]; then
      if [[ -n "$resolved" ]]; then
        printf 'Ambiguous parent epic title: %s\n' "$EPIC_TITLE" >&2
        return 1
      fi
      resolved="$number"
    fi
  done <<< "$ISSUES"
  if [[ -z "$resolved" ]]; then
    printf 'Missing parent epic with exact title: %s\n' "$EPIC_TITLE" >&2
    return 1
  fi
  printf '%s\n' "$resolved"
}

resolve_milestone_title() {
  local milestones title only="" count=0 found=0
  milestones=$(gh api --paginate \
    "repos/${REPO}/milestones?state=open&per_page=100" --jq '.[].title') || return 1
  while IFS= read -r title; do
    [[ -n "$title" ]] || continue
    count=$((count + 1))
    only="$title"
    if [[ "$title" == "$MILESTONE_TITLE" ]]; then
      found=1
    fi
  done <<< "$milestones"
  if [[ -n "$MILESTONE_TITLE" ]]; then
    if [[ "$found" -ne 1 ]]; then
      printf 'Selected milestone is not open: %s\n' "$MILESTONE_TITLE" >&2
      return 1
    fi
    printf '%s\n' "$MILESTONE_TITLE"
  elif [[ "$count" -gt 1 ]]; then
    printf 'Multiple open milestones; set MILESTONE_TITLE to one of:\n%s\n' \
      "$milestones" >&2
    return 1
  else
    printf '%s\n' "$only"
  fi
}

create_issue() {
  local title="$1" priority="$2" number
  local args=(gh issue create --repo "$REPO" --title "$title"
    --label type.Story --label "$priority")
  # Refresh before every creation; an API failure must stop the script.
  ISSUES=$(list_all_issues)
  if number=$(find_issue_number "$title"); then
    printf 'Skipping existing issue #%s: %s\n' "$number" "$title"
    return 0
  fi
  if [[ -n "$MILESTONE_TITLE" ]]; then
    args+=(--milestone "$MILESTONE_TITLE")
  fi
  "${args[@]}" --body-file -
}

ISSUES=$(list_all_issues)
EPIC_NUMBER=$(resolve_epic_number)
MILESTONE_TITLE=$(resolve_milestone_title)
printf 'Parent epic #%s: %s\n' "$EPIC_NUMBER" "$EPIC_TITLE"
printf 'Milestone: %s\n' "${MILESTONE_TITLE:-none}"

# ---------------------------------------------------------------------------
# Issue 1: Repository nextId — no dependencies
# ---------------------------------------------------------------------------
create_issue "Add nextId to AccountRepository and MembershipPlanRepository" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Add \`nextId(Connection)\` to \`AccountRepository\` and
\`MembershipPlanRepository\`, matching the existing convention in
\`BookingRepository\` and \`MembershipRepository\` (\`MAX(id) + 1\`).
This closes the gap where neither repository exposed a service-level ID
generator despite \`insert()\` requiring a caller-assigned positive id.

### Acceptance Criteria
- [ ] \`AccountRepository\` exposes \`long nextId(Connection)\` with the same Javadoc style as \`BookingRepository.nextId\`.
- [ ] \`MembershipPlanRepository\` exposes \`long nextId(Connection)\` with the same Javadoc style as \`MembershipRepository.nextId\`.
- [ ] SQLite implementations use \`SELECT COALESCE(MAX(id), 0) + 1\` against the respective table.
- [ ] Tests in \`SqliteRepositoriesTest\` verify \`nextId\` returns 1 on an empty table and increments after inserts.
- [ ] No other methods on these interfaces are changed.
- [ ] \`./gradlew check\` passes.
EOF

# ---------------------------------------------------------------------------
# Issue 2: Plan management service — depends on issue 1
# ---------------------------------------------------------------------------
create_issue "Implement membership plan management service" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Create \`MembershipPlanService\` for Manager-role CRUD on membership plans
(name, duration, price in cents), using \`MembershipPlanRepository\` and the
new \`nextId\` method.

### Dependencies
- Depends on: "Add nextId to AccountRepository and MembershipPlanRepository"

### Acceptance Criteria
_(Referenced from manage-membership-plans and soft-delete-archive-pattern
skill Definitions of Done.)_
- [ ] Service enforces Manager authorization before every operation.
- [ ] Create: validates non-blank name, duration > 0, priceCents >= 0; assigns ID via \`MembershipPlanRepository.nextId\`.
- [ ] Edit: updates name, duration, and price of an existing unarchived plan.
- [ ] Delete-or-archive: uses \`deleteIfUnpurchased\` for never-purchased plans; archives purchased plans via the active/archived flag (soft-delete-archive-pattern). No hard-delete method exposed to services for purchased plans.
- [ ] Default read operations exclude archived plans; admin queries return all plans.
- [ ] Historical membership snapshots referencing archived plans remain intact.
- [ ] Unit tests cover create, edit, archive, delete-if-unpurchased, and unauthorized-caller paths.
- [ ] \`./gradlew check\` passes.
- [ ] Interaction log recorded.
EOF

# ---------------------------------------------------------------------------
# Issue 3: Plan management UI — depends on issue 2
# ---------------------------------------------------------------------------
create_issue "Implement membership plan management UI" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Build the Manager FXML screen and controller for membership plan management,
delegating all actions to \`MembershipPlanService\`.

### Dependencies
- Depends on: "Implement membership plan management service"

### Acceptance Criteria
- [ ] FXML screen lists plans with name, duration, and price.
- [ ] Create, edit, and archive/delete actions are reachable by mouse and keyboard through the GUI.
- [ ] Controller delegates to \`MembershipPlanService\`; no business logic in the controller.
- [ ] Archived plans are visually distinguishable from active plans in the admin view.
- [ ] User Guide section documents the Manager plan management workflow (see update-user-guide skill Definition of Done), or a separate User Guide issue tracks this.
- [ ] \`./gradlew check\` passes.
- [ ] Interaction log recorded.
EOF

# ---------------------------------------------------------------------------
# Issue 4: Account provisioning service — depends on issue 1
# ---------------------------------------------------------------------------
create_issue "Implement account provisioning service" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Create a service for Manager-role provisioning and management of Trainer and
Member accounts (create, edit display name, deactivate/reactivate), using
\`AccountRepository\` and the new \`nextId\` method.

### Dependencies
- Depends on: "Add nextId to AccountRepository and MembershipPlanRepository"

### Acceptance Criteria
_(Referenced from soft-delete-archive-pattern skill Definition of Done.)_
- [ ] Service enforces Manager authorization before every operation.
- [ ] Create: validates unique username (case-insensitive), hashes password, assigns ID via \`AccountRepository.nextId\`, and sets role to Trainer or Member.
- [ ] Edit: allows updating display name; username and role are immutable per the repository contract.
- [ ] Deactivate: sets the active flag to false and cancels the account's future bookings in the same transaction, with \`ACCOUNT_DEACTIVATED\` as the cancellation reason.
- [ ] The seeded Manager account cannot be deactivated (enforced by the Account model).
- [ ] Reactivate: restores an inactive account without re-creating cancelled bookings.
- [ ] Default read operations exclude deactivated accounts; admin queries return all accounts.
- [ ] Unit tests cover create, edit, deactivate, reactivate, seeded-manager guard, and unauthorized-caller paths.
- [ ] \`./gradlew check\` passes.
- [ ] Interaction log recorded.
EOF

# ---------------------------------------------------------------------------
# Issue 5: Account provisioning UI — depends on issue 4
# ---------------------------------------------------------------------------
create_issue "Implement account provisioning UI" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Build the Manager FXML screen and controller for account provisioning and
management, delegating all actions to the account provisioning service.

### Dependencies
- Depends on: "Implement account provisioning service"

### Acceptance Criteria
- [ ] FXML screen lists accounts with username, display name, role, and active status.
- [ ] Create, edit, and deactivate/reactivate actions are reachable by mouse and keyboard through the GUI.
- [ ] Controller delegates to the account service; no business logic in the controller.
- [ ] Deactivated accounts are visually distinguishable from active accounts.
- [ ] User Guide section documents the Manager account management workflow (see update-user-guide skill Definition of Done), or a separate User Guide issue tracks this.
- [ ] \`./gradlew check\` passes.
- [ ] Interaction log recorded.
EOF

# ---------------------------------------------------------------------------
# Issue 6: User Guide — depends on issues 3 and 5
# ---------------------------------------------------------------------------
create_issue "Update User Guide for Manager plan and account management" "priority.Medium" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Add or update \`docs/UserGuide.md\` sections covering Manager plan management
and account provisioning workflows.

### Dependencies
- Depends on: "Implement membership plan management UI"
- Depends on: "Implement account provisioning UI"

### Acceptance Criteria
_(Referenced from update-user-guide skill Definition of Done.)_
- [ ] \`docs/UserGuide.md\` has a section for Manager plan management describing create, edit, and archive/delete workflows.
- [ ] \`docs/UserGuide.md\` has a section for Manager account management describing create, edit, and deactivate/reactivate workflows.
- [ ] Steps and terminology (screen names, button labels) match the actual implemented UI.
- [ ] Table of contents (if present) is in sync with section headings.
- [ ] User Guide update is included in the same PR as the code change, not deferred.
- [ ] Interaction log entry written.

Notes: This issue may be folded into the plan management UI and account
provisioning UI issues rather than kept standalone, depending on team
convention. If folded, mark this issue as a duplicate and close it.
EOF
