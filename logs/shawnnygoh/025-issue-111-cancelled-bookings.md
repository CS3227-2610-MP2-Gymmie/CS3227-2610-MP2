# Issue 111 Cancelled Bookings

## Goal
Implement issue #111 so cancelled bookings have a dedicated section in My
bookings, then address the human review's documentation corrections and commit
the changes.

## Scope
Three user-assistant exchanges covering one focused issue implementation, a
documentation follow-up, and the requested commits.

## Key prompts
- “Implement issue #111 on branch `implement-issue-111-cancelled-bookings`.”
  This set the issue scope and target branch.
- “Upcoming (active bookings for sessions not yet started, soonest first),
  Past (active bookings for sessions that have started, most recent first),
  and Cancelled (all cancelled bookings, most recent session first, with the
  cancellation reason and any Trainer reason).” This defined the
  classification, order, and reason display.
- “Docs and one test comment only; no behaviour changes.” This constrained
  the human-review follow-up to documentation and a Javadoc summary.
- “Commit the change with an appropriate commit message, then run
  $session-log, and commit the log.” This requested separate implementation
  and session log commits.

## Decisions and corrections
- Added the dedicated Cancelled section, status-aware sorting and filtering, and
  kept cancellation available only in Upcoming. A session starting exactly now
  is Past.
- Independent review found that Upcoming sort order lacked a multi-item test.
  Added deliberately unsorted future bookings and verified ascending order,
  then reran both requested Gradle checks and review.
- Human review found the Developer Guide still described only session-start
  grouping and that the User Guide omitted the rebooking move from Cancelled to
  Upcoming. Updated both guides and the test Javadoc, reran `./gradlew check`
  and `./gradlew test -PuiTests=true`, and recorded the correction in the issue
  report. No behavior changed; no further independent review was requested.
- The implementation and report were committed first as `685b34c`; this log is
  committed separately.

## Files created or modified
- `docs/DeveloperGuide.md`
- `docs/UserGuide.md`
- `src/main/java/gymmie/member/MemberBookingsController.java`
- `src/main/resources/gymmie/member/view/Bookings.fxml`
- `src/test/java/gymmie/member/MemberBookingTimeGroupingTest.java`
- `src/test/java/gymmie/member/MemberBookingsNavigationTest.java`
- `evals/implement-issue/reports/issue-111-separate-cancelled-bookings.md`
