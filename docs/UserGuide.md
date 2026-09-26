# Gymmie User Guide

## Quick start

## Verify the application

## Features

### Manager

### Trainer

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

If you have no membership covering today, the card shows **Inactive — no current
membership.** Plan and expiry date show a dash. Expired, cancelled, and future
memberships do not make you active.

While loading, previous details are cleared. If loading fails, the card shows an
error; select **Refresh** to try again. If your session has ended, sign in again.
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

### Saving the data
