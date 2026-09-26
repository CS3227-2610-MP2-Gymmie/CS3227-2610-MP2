# Gymmie User Guide

## Quick start

## Verify the application

## Features

### Manager

### Trainer

#### View your upcoming sessions

Log in as a Trainer and choose **My upcoming sessions** on the **Trainer dashboard**.

1. Read your sessions in start-time order. Each card shows the local start date
   and time, session number, duration, capacity, and description.
2. Choose **Refresh** to reload the list and apply the current time cut-off.
3. Choose **Back to dashboard** to return.

Only your own uncancelled sessions starting strictly after your computer's
current local time appear. Sessions that have already started are excluded.
**No upcoming sessions.** means there are no matching sessions. The list updates
when opened or refreshed; use **Refresh** if you leave it open.

Click buttons with the mouse, or use **Tab** and **Shift+Tab** to focus them and
**Space** to activate them. The page scrolls for longer lists. Previous details
are cleared while refreshing. If loading fails, an error appears; retry with
**Refresh**. Access requires an active Trainer account; sign in again if your
session has ended.

#### View a session roster

On **Trainer dashboard → My upcoming sessions**, find the session you want and
choose its **View roster** button. The roster opens inside that session's card.
It lists currently booked Members by display name in alphabetical order, with
a count. Cancelled bookings are excluded. Members with the same display name
appear separately. Login usernames and passwords are never shown in rosters.

Use the mouse, or **Tab** / **Shift+Tab** to focus the session's **View roster**
button and **Space** to activate it. Choose **Refresh roster** to reload current
bookings and display names. **No Members booked.** means the roster is empty.
Previous names are cleared while loading; if an error appears, activate the
button again to retry. Only an active, signed-in Trainer can access rosters for
their own sessions. Refreshing the session list closes all open rosters.

#### Edit your training session

On **Trainer dashboard → My upcoming sessions**, choose **Edit session** on the
session's card. The form opens with its saved details.

1. Correct the start date, local start time (**HH:mm**), duration, capacity, or description.
2. Choose **Save changes**. **Success: Session changes saved.** confirms the edit.
3. Choose **Back to upcoming sessions** to see the updated card.

Use the mouse, or **Tab** / **Shift+Tab** to reach **Edit session** and press
**Space**. The form supports the same calendar and keyboard controls as creation;
**Tab** from the description reaches **Save changes**. Press **Space** to save,
or **Enter** in the time, duration, or capacity field.

The start must be in the future, duration must be **15–240 minutes**, and capacity
must be **1–50** and at least the number of current bookings. Cancelled bookings
do not count toward this minimum. Only an active Trainer can edit their own
sessions; cancelled sessions cannot be edited. Validation errors retain your
entries so you can correct and retry. Your session must not overlap another of
your uncancelled sessions, including one already in progress. Both the start time
and duration determine overlap. Back-to-back sessions are allowed: one may start
exactly when another ends. Other Trainers' sessions do not block your schedule.

Editing preserves existing bookings, and saved details remain after restarting
Gymmie. Going back without saving discards changes made since the last save.

#### Delete an unused session

On **Trainer dashboard → My upcoming sessions**, choose **Delete session** on
one of your session cards. Check the session number and start time in the
confirmation, then choose **OK** to permanently delete it. Choose **Cancel**,
press **Escape**, or close the dialog to leave everything unchanged.

Use the mouse, or **Tab** / **Shift+Tab** to focus **Delete session** and press
**Space**. The confirmation buttons also support Tab and Space.
**Success: Session deleted.** confirms removal; it remains deleted after restarting
Gymmie. Only an active Trainer can delete their own sessions.

A session can be deleted only if it has never had a booking. Even one cancelled
booking prevents deletion. An empty roster does not necessarily mean deletion is
allowed. If booking history exists, an error appears and the session and all
bookings remain unchanged, retaining them for the cancellation workflow.

#### Create a training session

Log in as a Trainer and choose **Create session** on the **Trainer dashboard**.

1. Open **Start date** using its calendar button and select a date. Use the
   calendar's month arrows to browse to another month.
2. Enter **Start time** as `HH:mm`, for example `14:30`. Use 24-hour time in your
   computer's local time zone. The selected date and time must still be strictly
   in the future when you submit the form.
3. Enter **Duration (minutes)** as a whole number from 15 to 240.
4. Enter **Capacity (Members)** as a whole number from 1 to 50.
5. Add a **Description**, or leave it empty. Multiple lines are supported.
6. Choose **Create session**. **Success: Session created for …** confirms it was
   saved under your Trainer account. The form clears so you can create another.
7. Choose **Back to dashboard** to return.

Use **Tab** and **Shift+Tab** to move through the controls and **Space** to
activate focused buttons. On **Start date**, press **F4** to open the calendar,
use the arrow keys to move between dates, and press **Enter** to select one.
You can also press **Enter** in a single-line field to submit. All actions are
also available with the mouse.

Missing dates, invalid times, past or current start times, and values outside
the numeric limits show an error. Correct the inputs and retry; failed submissions keep your inputs
and do not create a session. Creation requires an active, signed-in Trainer.
Creation also rejects a time range that overlaps another of your uncancelled
sessions, including a session already in progress. Back-to-back sessions are
allowed, and other Trainers' sessions do not block your schedule. An overlap
error identifies the conflicting session; change the start time or duration and retry.
Saved sessions remain after restarting Gymmie. Returning to the dashboard before
submitting discards unsaved inputs.

#### View and edit your profile

Log in as a Trainer and choose **My profile** on the **Trainer dashboard**.
The editor loads your current username, display name, synopsis, and training
specializations. Your username is fixed and cannot be edited.

1. Edit **Display name** (1–100 characters). This is your shared account display
   name, shown to other users; it does not need to be unique.
2. Edit **Synopsis**, or leave it empty.
3. Type a specialization in **Specialization tag** and click **Add tag** or press
   **Enter**. Each tag appears as a button; click it to remove it. Surrounding
   whitespace is removed and duplicate tags are combined without regard to case.
   Tags are optional, and may contain spaces (for example, `Strength training`).
4. Choose **Save profile**. A tag still in the entry field is added when you save.
   **Success: Profile saved.** confirms that all changes were saved together.
5. Choose **Back to dashboard** to return. The welcome message uses your saved
   display name. Your profile changes remain after restarting Gymmie.

Use **Tab** and **Shift+Tab** to move between controls, and **Space** to activate
focused buttons, including tag removal and **Save profile**. You can also press
**Enter** in **Display name** to save. The synopsis accepts multiple lines.

**Reload profile** discards unsaved edits and loads the saved values. Returning
to the dashboard also discards unsaved edits. If saving fails, your saved profile
is unchanged; your edits remain in the form so you can correct them and retry.
Only an active, signed-in Trainer can view or edit their own Trainer profile.
If your account has been deactivated, contact a Manager.

#### Change your password

Log in with your Trainer account. On the **Trainer dashboard**, use the
**Change password** form:

1. Enter your **Current password**.
2. Enter a **New password** of 8–128 characters.
3. Repeat it in **Confirm new password**.
4. Click **Save password**, or press **Enter** in **Confirm new password**.

You can use **Tab** and **Shift+Tab** to move between the controls. To check a
password, hold its **Show** button with the mouse, or focus the button and hold
**Space**. Release to hide it again.

The message **Success: Password changed.** confirms that your password was saved.
Use the new password the next time you log in, including after restarting Gymmie.
Your username stays fixed; this form changes only your own password.

If fields are empty, the confirmation does not match, the current password is
incorrect, or the new password is outside the allowed length, correct the error
and try again. Fields are cleared when a request is submitted, so you may need to
re-enter all three passwords. Failed changes leave your saved password unchanged.
If your account has been deactivated, you cannot change its password; contact a
Manager.

### Member

#### View current membership

Log in with your Member account to open the **Gym User dashboard**, then select
**My membership**. The screen shows whether a membership covers today, its plan
name, and its expiry date.

1. Read the status, plan, and expiry date in the **Current membership** card.
2. Select **Refresh** to load the latest membership history, including after
   leaving Gymmie open overnight. You can also use **Tab** or **Shift+Tab** to
   focus the button and press **Space** or **Enter**.

**Active** means the membership covers today; the expiry date is included. An
archived plan still appears by name while its membership covers today.

If your latest membership has expired, the screen shows its plan and expiry date
with **Expired** status. Select **Renew current plan** to extend that membership.
Renewal keeps the same plan, including when the plan is archived, and adds its
saved duration from the later of today or the current expiry date. You cannot
switch plans as part of a renewal.

If there is no started, non-cancelled membership, the card shows **Inactive — no
current membership.** Plan and expiry date show a dash. A cancelled or future
membership does not make you active or available to renew. An expired membership
does not make you active, but remains available to renew.

While loading, previous details are cleared. If loading fails, the card shows an
error; select **Refresh** to try again. If your session has ended, sign in again.

#### Cancel your current membership

On **My membership**, select **Cancel current membership** while your membership
is active. Review the confirmation and choose **OK** to cancel it immediately.
There is no refund. Gymmie also cancels your bookings for sessions that have
not started; those bookings remain in your history with a cancelled status and
the membership cancellation reason. Past sessions and bookings already
cancelled are unchanged.

The **Booking history** card lists each booking with its session date and time.
Cancelled bookings show **Cancelled** and the reason, including **Membership
cancelled** for bookings affected by this action. The screen confirms
cancellation and reports how many future bookings were cancelled. If saving
fails, the membership and bookings remain unchanged; try again. After
cancellation, you can purchase another available plan.

#### Buy a membership

On the **My membership** screen, use **Buy a membership**:

1. Choose an available plan. Each option shows its duration and price in SGD.
   Archived plans are not offered for new purchases.
2. Select **Purchase membership**. The membership starts today and becomes
   active immediately. Its purchase price and duration are saved with the
   membership, so later plan edits do not change this purchase.

A Member can have only one active membership at a time. The purchase button is
disabled while a current membership is active. If another session purchases a
membership first, or the selected plan is archived before the purchase finishes,
the dashboard shows an error. Select **Refresh plans** to reload the current
offerings, then choose an available plan and try again.

#### Browse sessions by Trainer

On the **Gym User dashboard**, choose **Browse sessions** to see upcoming
sessions from active Trainers. Each session card shows the Trainer's display
name, local start date and time, duration, description, capacity, and current
number of booked Members. Cancelled and past sessions are not shown.

1. Choose **All trainers** or a Trainer from the **Trainer** list to filter the
   session cards. Trainer names include an account number to distinguish
   Trainers who share a display name.
2. Choose **Refresh** to reload current sessions and booking counts.
3. Choose **Book session** on an eligible session card. A successful booking is
   saved and the card changes to **Already booked**. Choose **My membership →
   Booking history** to see the saved booking.
4. Choose **Back to dashboard** to return.

Booking counts include current bookings and exclude cancelled bookings. A
session list with **No upcoming sessions are available.** means there are no
sessions to browse. If loading fails, choose **Refresh** to try again. Use
**Tab** and **Shift+Tab** to move through the controls and press **Space** or
**Enter** to activate them. Booking requires an active membership. The session
must have available capacity, must not have started, and must start on or before
the membership expiry date. Gymmie explains when a booking is rejected because
membership is inactive, the session is full or has started, the session starts
after membership expiry, or the Member already has an active booking for it.
If you cancelled a booking, its session card shows **Book session** again. You
can rebook it while space remains and the other booking requirements are met.

### Saving the data
