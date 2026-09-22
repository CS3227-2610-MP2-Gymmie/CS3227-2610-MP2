# JavaFX App Scaffold

## Goal
Use the `create-javafx-app` skill to create a JavaFX app called Gymmie, including the scaffold's required build, testing, style-checking, packaging, CI, and documentation setup. Verify the generated project as far as the environment allows.

## Scope
One user prompt and one focused scaffolding and verification task; the agent then generated and validated the project files.

## Key prompts
- “Use the $create-javafx-app skill to create a JavaFX app called Gymmie” — This directed the agent to apply the project scaffolding skill and establish Gymmie as the application name.

## Decisions and corrections
- Used Java 25 and Gradle 9.7.1, with a project-local Gradle wrapper.
- Worked around the system Gradle native-library failure by using an isolated Gradle user home.
- Corrected the `IOException` import order after Checkstyle reported it.
- Added the JUnit Platform launcher after the first test run could not load the test framework.
- Confirmed `./gradlew check` and `./gradlew shadowJar` passed, producing `build/libs/Gymmie-1.0.0-all.jar`.
- `./gradlew run` was not fully verified because the environment had no usable graphical display.

## Files created or modified
- `.github/workflows/gradle.yml`, `.gitignore`, `AGENTS.md`, `README.md`
- `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`
- `config/checkstyle/checkstyle.xml`, `config/checkstyle/suppressions.xml`
- `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
- `src/main/java/gymmie/App.java`, `Launcher.java`, `MainController.java`
- `src/main/resources/gymmie/css/gymmie.css`, `view/MainWindow.fxml`
- `src/test/java/gymmie/TrivialTest.java`
