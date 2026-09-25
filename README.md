# Gymmie

Gymmie is a JavaFX desktop app for gym management.

On launch (`./gradlew run`), Gymmie creates an active Manager account if the
case-insensitive username `manager` is absent:

- Username: `manager`
- Password: `manager123`

Seeding runs after schema initialization in a transaction against `data/gymmie.db`.
Other accounts, including inactive accounts, do not prevent seeding. The seed uses
the first available positive account ID and preserves existing accounts.
An existing active Manager with this username is left unchanged, including its
password. If the username belongs to another role or an inactive Manager, startup
fails with a clear conflict error instead of overwriting or reactivating it.

The seeded Manager is identified by its immutable username (case-insensitive)
and Manager role and cannot be deactivated. Its password can be changed through
the normal password-change service without losing that protection. The initial
password uses the application's password-storage rules: PBKDF2-HMAC-SHA256 with
600,000 iterations and a fresh random 128-bit salt. Only the hash and salt are
stored in the database, never the plaintext password.

The application opens on the login screen. Use Tab / Shift+Tab to move between
controls and Enter to submit login (or click **Log in**). Verified credentials
for a deactivated account show a distinct message asking you to contact a Manager.
Managers, Trainers, and Gym Users land on their respective dashboard shells.
Each dashboard provides **Log out** and **Change password**, including password
confirmation; all controls support keyboard focus and mouse interaction.
Other gym workflows are not yet implemented.

`AppContext` constructs the database and initializes its schema before wiring
persistence, seeding the Manager, and exposing shared services. `Router` owns
navigation and `ViewLoader` loads bundled FXML with injected controllers.

Use JDK 25 on Windows, macOS, or Linux with a graphical desktop. On Windows, run
`.\gradlew.bat run`; on macOS/Linux, run `./gradlew run`. JavaFX dependencies
resolve for the build platform, so build the fat JAR on each target OS and
architecture instead of copying one OS's JAR to another.

Run `./gradlew check shadowJar` for the standard checks and package build. On a
graphical desktop, `./gradlew check -PuiTests=true` also exercises the actual FXML
login forms, asynchronous authentication, all role dashboards, deactivation
feedback, and logout with an isolated temporary database.
