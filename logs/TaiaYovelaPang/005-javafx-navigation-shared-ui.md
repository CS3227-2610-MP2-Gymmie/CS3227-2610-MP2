# JavaFX Navigation and Shared UI

## Goal
Build Gymmie's JavaFX composition root, login and role navigation, then establish reusable styling, feedback controls, and display formatters.

## Scope
Six user-assistant exchanges covering one related UI implementation sequence, explanations, refinements, and this logging request.

## Key prompts
- “Add Router, ViewLoader, the login view, and AppContext as the composition root” requested startup wiring, authenticated role routing, distinct deactivation feedback, and GUI accessibility across supported platforms.
- “Increase the font size of GYMMIE as well as include a button for user to view their password when pressed” requested more prominent branding and password visibility controls.
- “Help minimize these errors and warning if possible” supplied IntelliJ screenshots to guide cleanup of FXML linkage, redundant attributes, unused parameters, and test polling.
- “Add the shared stylesheet, common UI controls, and display formatters” requested a reusable foundation with consistent service errors and Developer Guide display conventions.

## Decisions and corrections
- AppContext constructs Database and initializes the schema before persistence services and Manager seeding. Router reuses one scene and selects role-specific dashboard shells.
- Login, logout, and password change are GUI-accessible. Authentication and password changes use background tasks because hashing and database work would otherwise block JavaFX; callbacks update controls on the UI thread.
- Increased GYMMIE from 16px to 32px. Added mouse/Space press-and-hold password reveal controls that hide on release.
- Declared FXML controllers and supplied existing instances through a controller factory so IntelliJ can resolve injected fields and handlers. Removed redundant markup, used Java 25 unnamed callback parameters, replaced sleep polling with property listeners, and narrowly suppressed the intentionally unused permissions accessor.
- Corrected Checkstyle failures and an integration-test lookup that ran before the replacement view had applied CSS.
- Added shared status controls, themed alerts and confirmations, and safe error mapping. Expected service messages remain visible; unexpected infrastructure details use fallback messages.
- The guide specified decimal SGD amounts and local time but no exact display patterns. Documented SGD 49.90, 25 Sep 2026, and 15:04, independent of OS locale.
- Final Checkstyle, tests including JavaFX integration, and packaging passed on macOS. Manual visual verification and Windows/Linux runtime checks remain outstanding. No commits were made by the assistant.

## Files created or modified
- `src/main/java/gymmie/`: App, AppContext, Router, ViewLoader, LoginController, DashboardController, PasswordReveal; removed MainController.
- `src/main/java/gymmie/ui/`: DisplayFormatters, SharedStyles, StatusLabel, UiFeedback.
- `src/main/resources/gymmie/view/`: Login.fxml, Dashboard.fxml; removed MainWindow.fxml.
- `src/main/resources/gymmie/css/gymmie.css`.
- `src/test/java/gymmie/`: AppContextTest, NavigationTest.
- `src/test/java/gymmie/ui/`: DisplayFormattersTest, UiFeedbackTest.
- `build.gradle`, `README.md`, `docs/DeveloperGuide.md`.
