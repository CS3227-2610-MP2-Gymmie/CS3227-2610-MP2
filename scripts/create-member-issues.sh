#!/usr/bin/env bash

set -euo pipefail

# Discovered from the target repository on 2026-09-23:
#   type.Story, priority.High, priority.Medium, and milestone v1.0.
# Override REPO when reviewing this script against another checkout.
REPO="${REPO:-CS3227-2610-MP2-Gymmie/CS3227-2610-MP2}"
EPIC_TITLE="E4: Build member features"
MILESTONE_TITLE="${MILESTONE_TITLE:-}"

find_issue_number() {
  local title="$1"
  local issue_data
  local existing_number
  local existing_title

  # Do not replace this with --search: exact comparison is required because
  # GitHub search can mishandle titles containing colons.
  issue_data=$(gh issue list \
    --repo "$REPO" \
    --state all \
    --limit 1000 \
    --json number,title \
    --jq '.[] | [.number, .title] | @tsv')

  while IFS=$'\t' read -r existing_number existing_title; do
    if [[ "$existing_title" == "$title" ]]; then
      printf '%s\n' "$existing_number"
      return 0
    fi
  done <<< "$issue_data"

  return 1
}

resolve_epic_number() {
  local issue_data
  local existing_number
  local existing_title
  local resolved_number=""

  issue_data=$(gh issue list \
    --repo "$REPO" \
    --state all \
    --limit 1000 \
    --json number,title \
    --jq '.[] | [.number, .title] | @tsv')

  while IFS=$'\t' read -r existing_number existing_title; do
    if [[ "$existing_title" == "$EPIC_TITLE" ]]; then
      if [[ -n "$resolved_number" ]]; then
        printf 'Epic title is ambiguous: %s\n' "$EPIC_TITLE" >&2
        return 1
      fi
      resolved_number="$existing_number"
    fi
  done <<< "$issue_data"

  if [[ -z "$resolved_number" ]]; then
    printf 'Could not find parent epic by exact title: %s\n' "$EPIC_TITLE" >&2
    return 1
  fi

  printf '%s\n' "$resolved_number"
}

resolve_milestone_title() {
  local milestone_title
  local selected_count=0
  local open_milestones=()

  while IFS= read -r milestone_title; do
    [[ -n "$milestone_title" ]] || continue
    open_milestones+=("$milestone_title")
  done < <(gh api "repos/${REPO}/milestones?state=open&per_page=100" --jq '.[].title')

  if [[ -n "$MILESTONE_TITLE" ]]; then
    for milestone_title in ${open_milestones[@]+"${open_milestones[@]}"}; do
      if [[ "$milestone_title" == "$MILESTONE_TITLE" ]]; then
        printf '%s\n' "$MILESTONE_TITLE"
        return 0
      fi
    done
    printf 'MILESTONE_TITLE is not an open milestone: %s\n' "$MILESTONE_TITLE" >&2
    return 1
  fi

  selected_count=${#open_milestones[@]}

  case "$selected_count" in
    0)
      printf '%s\n' ''
      ;;
    1)
      printf '%s\n' "${open_milestones[0]}"
      ;;
    *)
      printf 'More than one open milestone exists. Set MILESTONE_TITLE to one of:\n' >&2
      printf '  %s\n' ${open_milestones[@]+"${open_milestones[@]}"} >&2
      return 1
      ;;
  esac
}

create_issue() {
  local title="$1"
  local priority_label="$2"
  local issue_number
  local create_args=(
    gh issue create
    --repo "$REPO"
    --title "$title"
    --label "type.Story"
    --label "$priority_label"
  )

  if issue_number=$(find_issue_number "$title"); then
    printf 'Skipping existing issue #%s: %s\n' "$issue_number" "$title"
    return 0
  fi

  if [[ -n "$MILESTONE_TITLE" ]]; then
    create_args+=(--milestone "$MILESTONE_TITLE")
  fi

  "${create_args[@]}" --body-file -
}

EPIC_NUMBER=$(resolve_epic_number)
MILESTONE_TITLE=$(resolve_milestone_title)

printf 'Using parent epic #%s: %s\n' "$EPIC_NUMBER" "$EPIC_TITLE"
if [[ -n "$MILESTONE_TITLE" ]]; then
  printf 'Using milestone: %s\n' "$MILESTONE_TITLE"
else
  printf 'No open milestone found; issues will not be assigned one.\n'
fi

create_issue "View membership status" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

Let a Member view the current membership status and expiry date so they know
whether they can book a session.

Acceptance criteria:
- The Member can see the current membership and its expiry date.
- The screen shows the current plan name.
- The screen shows whether the membership is currently active.
EOF

create_issue "Buy an available membership plan" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

Let a Member buy an available membership plan and become an active Member.

Acceptance criteria:
- Plans that are archived are removed from the list available for a new
  purchase.
- The membership keeps the plan's SGD price and duration copied at purchase
  time as a membership snapshot.
- A Member holds at most one active membership at a time.
EOF

create_issue "Renew current membership plan" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

Let a Member renew their current membership plan without losing remaining days
when renewing early.

Acceptance criteria:
- Renewal extends from the later of today and the current expiry date.
- Renewal cannot switch to a different plan.
- An archived plan remains available for existing holders to renew.
EOF

create_issue "Cancel current membership plan" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

Let a Member cancel their current membership plan immediately.

Acceptance criteria:
- Cancellation marks the membership cancelled immediately and provides no
  refund.
- All of the Member's future bookings are cancelled in the same transaction.
- Affected bookings remain visible with a cancelled status and a reason that
  identifies membership cancellation.
- If persistence fails, both the membership cancellation and booking
  cancellations are rolled back.
EOF

create_issue "Browse sessions by Trainer" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

Let a Member browse sessions by Trainer and view the details needed to choose
a suitable session.

Acceptance criteria:
- Session details include the Trainer, start time, duration, description,
  capacity, and current booking count.
EOF

create_issue "Book an eligible session" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

Let a Member book a session when the documented membership, timing, capacity,
and duplicate-booking conditions are satisfied.

Acceptance criteria:
- Gymmie requires an active membership, available capacity, a session that
  has not started, and no existing booking for the Member.
- The session starts on or before the Member's membership expiry date.
- The booking is persisted and shown in the Member's booked sessions.
- A rejected booking explains the applicable failure for no membership, a full
  or started session, a duplicate booking, or a session after expiry.
- A persistence failure does not publish a partial booking.
EOF

create_issue "View upcoming and past bookings" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

Let a Member view upcoming and past bookings to track planned and completed
activity.

Acceptance criteria:
- The Member's booking view distinguishes upcoming activity from past
  activity.
- Bookings that were cancelled remain visible in the booking list with their
  cancelled status.

Notes: The Developer Guide does not specify the exact sort order or display
fields for upcoming and past bookings.
EOF

create_issue "Cancel a booking before session starts" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

Let a Member cancel their booking before the session starts so another Member
can use the released capacity.

Acceptance criteria:
- A Member can cancel their booking before the session starts.
- The released capacity is available to another Member.
EOF

create_issue "Show booking cancellation reasons" "priority.High" <<EOF
Parent epic: #${EPIC_NUMBER}

Show a Member when a booked session was cancelled and why it was cancelled.

Acceptance criteria:
- A cancelled booking remains visible with a cancelled status.
- The reason identifies whether the Trainer cancelled the session, the Member
  cancelled the booking, the Member cancelled their membership, or a Manager
  deactivated the account.
EOF

create_issue "Switch membership plans without losing days" "priority.Medium" <<EOF
Parent epic: #${EPIC_NUMBER}

Let a Member switch from one membership plan to another without forfeiting
remaining days.

Notes: Post-MVP. In v1.0 a Member must cancel first, forfeiting remaining days,
then buy the new plan — see Known limitations. Acceptance criteria to be
defined if scheduled.
EOF
