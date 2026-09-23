#!/usr/bin/env bash

set -euo pipefail

REPO="CS3227-2610-MP2-Gymmie/CS3227-2610-MP2"

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/create-e1-issues.sh epics
  ./scripts/create-e1-issues.sh tasks <E1 issue number>
  ./scripts/create-e1-issues.sh all

The epics pass creates E1-E6. The tasks pass creates the ten E1 tasks and
adds the supplied E1 issue number to each task body.
USAGE
}

find_issue_number() {
  local title="$1"
  local issue_data
  local existing_number
  local existing_title

  issue_data=$(gh issue list \
    --repo "$REPO" \
    --state all \
    --limit 200 \
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

create_epics() {
  local e1_url
  local e1_title="E1: Establish foundation and shared features"
  local e1_number
  local issue_number

  if e1_number=$(find_issue_number "$e1_title"); then
    printf 'Skipping existing issue #%s: %s\n' "$e1_number" "$e1_title"
  else
    e1_url=$(gh issue create \
      --repo "$REPO" \
      --title "$e1_title" \
      --label "type.Epic" \
      --label "priority.High" \
      --body-file - <<'EOF'
Build the shared foundation that all role-specific work depends on: the
domain model, SQLite persistence, authentication, authorization, session
state, and application routing.

Shared stories 1-4 (login with the deactivated-account message, role routing,
logout, and persistence across restarts) are delivered by the foundation
tasks under this epic. Change my own password and change my own display name will be
separate issues under this epic.

The shared persistence schema is:

```
account(id, username UNIQUE COLLATE NOCASE, password_hash, salt, display_name, role, active)
membership_plan(id, name, duration_days, price_cents, archived)
membership(id, member_id→account, plan_id→membership_plan, start_date, expiry_date,
status, snapshot_price_cents, snapshot_duration_days)
training_session(id, trainer_id→account, starts_at, duration_minutes, capacity,
description, cancelled)
booking(id, session_id→training_session, member_id→account, booked_at, status,
cancellation_reason)
```

Notes: SQLite, PRAGMA foreign_keys = ON per connection, timestamps as ISO-8601 TEXT,
money as integer cents, no hard deletes of records another user has interacted with.
EOF
    )
    e1_number="${e1_url##*/}"
    if [[ ! "$e1_number" =~ ^[0-9]+$ ]]; then
      printf 'Could not determine the E1 issue number from: %s\n' "$e1_url" >&2
      exit 1
    fi
  fi

  E1_NUMBER="$e1_number"
  printf 'Using E1 as issue #%s.\n' "$E1_NUMBER"

  if issue_number=$(find_issue_number "E2: Build manager features"); then
    printf 'Skipping existing issue #%s: E2: Build manager features\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "E2: Build manager features" \
      --label "type.Epic" \
      --label "priority.High" \
      --body-file - <<'EOF'
Deliver Manager workflows: membership-plan administration, Trainer and Member
account administration, account lifecycle rules, and the Manager's payment and
revenue capabilities.
EOF
  fi

  if issue_number=$(find_issue_number "E3: Build trainer features"); then
    printf 'Skipping existing issue #%s: E3: Build trainer features\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "E3: Build trainer features" \
      --label "type.Epic" \
      --label "priority.High" \
      --body-file - <<'EOF'
Deliver Trainer workflows for creating, viewing, editing, rescheduling,
cancelling, and safely deleting the Trainer's own sessions and viewing rosters.
EOF
  fi

  if issue_number=$(find_issue_number "E4: Build member features"); then
    printf 'Skipping existing issue #%s: E4: Build member features\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "E4: Build member features" \
      --label "type.Epic" \
      --label "priority.High" \
      --body-file - <<'EOF'
Deliver Member workflows for membership status, purchases, renewals,
cancellations, session browsing, bookings, and booking history.
EOF
  fi

  if issue_number=$(find_issue_number "E5: Establish agentic SE workflow"); then
    printf 'Skipping existing issue #%s: E5: Establish agentic SE workflow\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "E5: Establish agentic SE workflow" \
      --label "type.Epic" \
      --label "priority.High" \
      --body-file - <<'EOF'
Establish the agentic software-engineering workflow around Gymmie, including
reusable skills, guardrails, and an evaluation harness for measuring the
quality and safety of agent-assisted development.
EOF
  fi

  if issue_number=$(find_issue_number "E6: Document and release v1.0"); then
    printf 'Skipping existing issue #%s: E6: Document and release v1.0\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "E6: Document and release v1.0" \
      --label "type.Epic" \
      --label "priority.High" \
      --body-file - <<'EOF'
Complete the User Guide, add the Developer Guide's design sections as the
implementation becomes available, publish the product website, and prepare
the v1.0 release.
EOF
  fi
}

create_tasks() {
  local e1_number="$1"
  local issue_number

  if [[ ! "$e1_number" =~ ^[0-9]+$ ]]; then
    printf 'E1 issue number must be numeric: %s\n' "$e1_number" >&2
    exit 1
  fi

  if issue_number=$(find_issue_number "Add SQLite build support and verification"); then
    printf 'Skipping existing issue #%s: Add SQLite build support and verification\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "Add SQLite build support and verification" \
      --label "type.Task" \
      --label "priority.High" \
      --body-file - <<EOF
Parent epic: #${e1_number}

Add the sqlite-jdbc dependency, native-access flags needed by the packaged fat
JAR, and a Gradle verify task.

Acceptance criteria:
- The project remains buildable with the Gradle Wrapper and passes the
  repository's check task before a change is ready for integration.
- shadowJar creates a runnable fat JAR in build/libs/, and the packaged
  application can be launched with the documented java -jar command.

Notes: The Developer Guide does not specify the sqlite-jdbc coordinates, the
exact native-access flags, or the verify task's scope.
EOF
  fi

  if issue_number=$(find_issue_number "Add database schema and transaction boundary"); then
    printf 'Skipping existing issue #%s: Add database schema and transaction boundary\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "Add database schema and transaction boundary" \
      --label "type.Task" \
      --label "priority.High" \
      --body-file - <<EOF
Parent epic: #${e1_number}

Add schema.sql, Database, SchemaInitializer, and UnitOfWork around the shared
SQLite persistence boundary.

Acceptance criteria:
- Successful account, plan, membership, session, and booking changes remain
  available after the application is closed and restarted.
- Application data is stored in a data/ folder relative to the working
  directory.
- Membership cancellation and session cancellation update related bookings
  atomically; a failed persistence operation does not expose only part of the
  change.
EOF
  fi

  if issue_number=$(find_issue_number "Add domain records and exceptions"); then
    printf 'Skipping existing issue #%s: Add domain records and exceptions\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "Add domain records and exceptions" \
      --label "type.Task" \
      --label "priority.High" \
      --body-file - <<EOF
Parent epic: #${e1_number}

Add domain model records and exception types for accounts, plans, memberships,
training sessions, and bookings.

Acceptance criteria:
- The model enforces the documented field constraints: plan duration 1-365
  days, price 0-1,000,000 cents, session duration 15-240 minutes, capacity
  1-50, display name 1-100 characters, username 3-30 allowed ASCII
  characters, and password 8-128 characters.
- A Member's plan and status are derived from membership records, and a Member
  holds at most one active membership.
- Memberships retain the purchase-time price and duration snapshot.
- Time comparisons use the local system time, and money is represented as
  integer cents.
EOF
  fi

  if issue_number=$(find_issue_number "Add repository interfaces"); then
    printf 'Skipping existing issue #%s: Add repository interfaces\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "Add repository interfaces" \
      --label "type.Task" \
      --label "priority.High" \
      --body-file - <<EOF
Parent epic: #${e1_number}

Define repository interfaces for accounts, membership plans, memberships,
training sessions, and bookings.

Acceptance criteria:
- The interfaces support persistence of all five domain areas so successful
  changes survive application restarts.
- Their design supports the documented history rules: accounts are
  deactivated rather than deleted, purchased plans are archived rather than
  deleted, and sessions with booking history are cancelled rather than
  deleted.
EOF
  fi

  if issue_number=$(find_issue_number "Implement SQLite repositories"); then
    printf 'Skipping existing issue #%s: Implement SQLite repositories\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "Implement SQLite repositories" \
      --label "type.Task" \
      --label "priority.High" \
      --body-file - <<EOF
Parent epic: #${e1_number}

Implement SQLite repository classes for all five repository interfaces and
connect them to the shared database boundary.

Acceptance criteria:
- Account, plan, membership, session, and booking changes persist in the
  local data/ folder and remain available after restart.
- The documented history-preservation rules are respected; records with
  relevant history are not hard-deleted.
- Membership and session cancellation can participate in one atomic update of
  the affected bookings, with rollback on persistence failure.

Notes: The target scale of 1,000 accounts, 200 plans, 5,000 sessions, and
20,000 bookings is an app-level performance requirement; verify it separately.
EOF
  fi

  if issue_number=$(find_issue_number "Add authentication and permissions services"); then
    printf 'Skipping existing issue #%s: Add authentication and permissions services\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "Add authentication and permissions services" \
      --label "type.Task" \
      --label "priority.High" \
      --body-file - <<EOF
Parent epic: #${e1_number}

Add PasswordHasher, AuthService, UserSession, and Permissions for login,
logout, password changes, session state, and service-layer RBAC.

Acceptance criteria:
- Passwords are at least eight characters and are never stored in plaintext.
- Usernames are globally unique across roles, matched case-insensitively, and
  stored as entered by the user.
- Login distinguishes a deactivated account from invalid credentials.
- Role permissions are enforced in the service layer, not only by hiding JavaFX
  controls.
- Session state can be established for an authenticated account and cleared on
  logout.
- The service layer exposes an operation for a user to change their own
  password.

Notes: The guide does not prescribe hashing parameters such as iteration count and salt length.
EOF
  fi

  if issue_number=$(find_issue_number "Seed the initial manager account"); then
    printf 'Skipping existing issue #%s: Seed the initial manager account\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "Seed the initial manager account" \
      --label "type.Task" \
      --label "priority.High" \
      --body-file - <<EOF
Parent epic: #${e1_number}

Add Seeder logic for the first-run Manager account with username manager and
password manager123.

Acceptance criteria:
- The seeded Manager is created on the first run with the documented fixed
  credentials.
- The seeded Manager cannot be deactivated.
- The seeded password is protected by the application's password-storage
  rules and is not stored in plaintext.

Notes: The guide does not specify the Seeder API, duplicate-prevention
behavior on later starts, or the exact first-run detection mechanism.
EOF
  fi

  if issue_number=$(find_issue_number "Add routing and login composition"); then
    printf 'Skipping existing issue #%s: Add routing and login composition\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "Add routing and login composition" \
      --label "type.Task" \
      --label "priority.High" \
      --body-file - <<EOF
Parent epic: #${e1_number}

Add Router, ViewLoader, the login view, and AppContext as the composition root
for the JavaFX application.

Acceptance criteria:
- A user can log in and is routed to the dashboard for their role.
- Login communicates the distinct deactivated-account case described by the
  authentication requirements.
- Every delivered feature is reachable by mouse and keyboard through the GUI.
- The JavaFX application remains runnable on Java 25 on Windows, macOS, and
  Linux.
EOF
  fi

  if issue_number=$(find_issue_number "Add test utilities"); then
    printf 'Skipping existing issue #%s: Add test utilities\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "Add test utilities" \
      --label "type.Task" \
      --label "priority.High" \
      --body-file - <<EOF
Parent epic: #${e1_number}

Add test utilities for an in-memory SQLite database, a fixed Clock, and entity
builders.

Acceptance criteria:
- The utilities support JUnit tests for persistence, local-time rules,
  authentication, authorization, and the atomic cancellation use cases.
- Tests remain runnable through the Gradle Wrapper, and the repository's
  check task passes before integration.
- The fixed clock makes the guide's local-system-time comparisons deterministic
  in tests without changing production time handling.
EOF
  fi

  if issue_number=$(find_issue_number "Add shared UI foundation: stylesheet, common controls, and formatters"); then
    printf 'Skipping existing issue #%s: Add shared UI foundation: stylesheet, common controls, and formatters\n' "$issue_number"
  else
    gh issue create \
      --repo "$REPO" \
      --title "Add shared UI foundation and formatters" \
      --label "type.Task" \
      --label "priority.High" \
      --body-file - <<EOF
Parent epic: #${e1_number}

Add the shared stylesheet, common UI controls, and display formatters that
every role's views build on.

Acceptance criteria:
- A single stylesheet all role views load, so the three dashboards share one visual language.
- Shared helpers for alerts and error display, so services' error messages surface
  consistently across roles.
- Shared formatters for SGD prices, dates, and times, matching the Developer Guide's
  display conventions.
EOF
  fi
}

if [[ $# -eq 0 ]]; then
  usage >&2
  exit 2
fi

case "$1" in
  epics)
    [[ $# -eq 1 ]] || { usage >&2; exit 2; }
    create_epics
    printf 'Run the task pass with: %s tasks %s\n' "$0" "$E1_NUMBER"
    ;;
  tasks)
    [[ $# -eq 2 ]] || { usage >&2; exit 2; }
    create_tasks "$2"
    ;;
  all)
    [[ $# -eq 1 ]] || { usage >&2; exit 2; }
    create_epics
    create_tasks "$E1_NUMBER"
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
