#!/usr/bin/env bash
set -euo pipefail

# Source: docs/DeveloperGuide.md, Trainer stories, UC4, repository contracts,
# non-functional requirements, and glossary. One issue per Trainer story.
# Repository labels and sole open milestone (v1.0) verified on 2026-09-25.
# Review and run manually. No issues are created while generating this script.
REPO="${REPO:-CS3227-2610-MP2-Gymmie/CS3227-2610-MP2}"
EPIC_TITLE="E3: Build trainer features"
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

create_issue "Create a future training session" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Let a Trainer create a session that Members can book.

### Acceptance Criteria
- [ ] Capture a future start time, duration in minutes, capacity, and optional description.
- [ ] Enforce Trainer authorization in the service layer and associate the session with its Trainer.
- [ ] Compare times using the local system time.
- [ ] Make the workflow reachable by mouse and keyboard through the GUI.
- [ ] Persist successful creation across application restarts.
EOF

create_issue "View own upcoming training sessions" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Let a Trainer see their own upcoming sessions to prepare for them.

### Acceptance Criteria
- [ ] Show the authenticated Trainer's own upcoming sessions.
- [ ] Exclude cancelled sessions and use local system time for the upcoming cut-off.
- [ ] Enforce role and ownership access in the service layer.
- [ ] Make the view reachable by mouse and keyboard through the GUI.

Notes: The guide does not specify the display order or exact fields for this view.
EOF

create_issue "View Members booked into own sessions" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Let a Trainer view the Members booked into each of their sessions to prepare a roster.

### Acceptance Criteria
- [ ] Show Members booked into the Trainer's selected session.
- [ ] Use Members' display names in rosters; do not expose their login usernames.
- [ ] Enforce role and session ownership access in the service layer.
- [ ] Make the roster reachable by mouse and keyboard through the GUI.

Notes: The guide does not specify roster sorting or how cancelled bookings appear in the Trainer's roster.
EOF

create_issue "Edit own training session details" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Let a Trainer correct their own session details while preserving valid bookings.

### Acceptance Criteria
- [ ] Enforce Trainer authorization and session ownership in the service layer.
- [ ] Reject a start time in the past; creation and rescheduling require a future time.
- [ ] Reject capacity below the current booking count, counting only BOOKED reservations.
- [ ] Preserve existing valid bookings when editing session details.
- [ ] Make editing reachable by mouse and keyboard through the GUI.
- [ ] Persist successful edits across application restarts.
EOF

create_issue "Reschedule own session and preserve bookings" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Let a Trainer move their own session to another future time without losing its roster.

### Acceptance Criteria
- [ ] Enforce Trainer authorization and session ownership in the service layer.
- [ ] Require the replacement start time to be in the future using local system time.
- [ ] Retain existing bookings when the session is rescheduled.
- [ ] Make rescheduling reachable by mouse and keyboard through the GUI.
- [ ] Persist the new start time across application restarts.

Notes: The guide does not specify how rescheduling beyond an existing booker's membership expiry interacts with retaining that booking.
EOF

create_issue "Delete own session with no booking history" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Let a Trainer remove an unused session without destroying booking history.

### Acceptance Criteria
- [ ] Enforce Trainer authorization and session ownership in the service layer.
- [ ] Allow deletion only when the session has never had any booking, including cancelled bookings.
- [ ] Atomically refuse deletion when booking history exists; retain the session for the cancellation workflow.
- [ ] Require confirmation before deletion and make no changes if confirmation is declined.
- [ ] Make deletion reachable by mouse and keyboard through the GUI.
- [ ] Persist successful deletion across application restarts.
EOF

create_issue "Cancel own session and its bookings atomically" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Let a Trainer cancel their own session before it starts, including sessions with bookings, as described by UC4.

### Acceptance Criteria
- [ ] Require Trainer to state reason for cancellation of session.
- [ ] Show the session and current bookings and request confirmation before cancellation.
- [ ] Enforce Trainer authorization and reject another Trainer's session in the service layer.
- [ ] Reject cancellation once the session has started, including the exact start time, using local system time.
- [ ] Make no changes when the Trainer declines confirmation.
- [ ] Mark the session cancelled and cancel all its bookings in one transaction.
- [ ] Keep affected bookings visible to Members with a cancelled status and a reason identifying Trainer cancellation.
- [ ] Roll back session and booking changes together and report a persistence failure.
- [ ] Make cancellation reachable by mouse and keyboard through the GUI, and persist successful changes across restarts.
EOF

create_issue "View and edit own Trainer profile" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Let a Trainer view and edit their own profile to keep their details current.

### Acceptance Criteria
- [ ] Let the authenticated Trainer view their own profile and update their display name, synopsis and training specialization as tags.
- [ ] Keep the username fixed, as required by the glossary and account repository contract.
- [ ] Enforce authorization in the service layer.
- [ ] Make the workflow reachable by mouse and keyboard through the GUI.
- [ ] Persist successful changes across application restarts.

Notes: The guide does not enumerate additional editable Trainer profile fields. Reuse the shared display-name capability rather than introducing a separate account identity rule.
EOF

create_issue "Change own Trainer account password" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Let a Trainer change their own password to keep their account safe.

### Acceptance Criteria
- [ ] Let the authenticated Trainer change their password.
- [ ] Keep the username fixed, as required by the glossary and account repository contract.
- [ ] Enforce authorization in the service layer.
- [ ] Make the workflow reachable by mouse and keyboard through the GUI.
- [ ] Persist successful changes across application restarts.

EOF

create_issue "Mark attendance for own classes" "priority.Medium" <<EOF
Parent epic: #${EPIC_NUMBER}

### Summary
Let a Trainer mark attendance for their classes so they know who is present.

### Acceptance Criteria
- [ ] Let a Trainer mark attendance for their own classes.
- [ ] Enforce Trainer authorization in the service layer.
- [ ] Make attendance marking reachable by mouse and keyboard through the GUI.

Notes: Beyond this story and general application constraints, the guide does not define attendance states, marking windows, corrections, or an attendance persistence model. These details remain to be specified.
EOF
