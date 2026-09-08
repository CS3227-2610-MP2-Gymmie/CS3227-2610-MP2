---
name: create-javafx-app
description: Scaffold a new JavaFX desktop app with a small, consistent project structure.
---

## When to use this

Use when you need to set up a new JavaFX desktop app.

## What to build

Create a JavaFX project and add a starter scene as an entry point. Keep it minimal.

## Prerequisites

- JDK 25
- Gradle 9.7.1
- A GitHub repository already initialized

## Steps

1. Scaffold the JavaFX app with Gradle by running `gradle :wrapper --gradle-version 9.7.1` and apply the necessary plugins (e.g. OpenJFX JavaFX Plugin 0.1.0, Gradle Shadow 9.6.1).

2. Add style checks following the se-education.org Java coding standard by copying:

   - `assets/checkstyle.xml` to `config/checkstyle/checkstyle.xml`
   - `assets/suppressions.xml` to `config/checkstyle/suppressions.xml`

3. Add Jacoco for measuring code coverage.

4. Add Shadow Gradle and verify that `./gradlew shadowJar` produces a runnable fat jar.

5. Add JUnit 5 and write one trivial test.

6. Create App.java, Launcher.java, MainController.java and MainWindow.fxml which contains a minimal UI with a title label that displays the app's name. Style the UI with CSS.

7. Add a GitHub Actions workflow by copying `assets/gradle.yml` to `.github/workflows/gradle.yml`.

8. Add AGENTS.md which includes the tech stack, build commands (`./gradlew run`, `./gradlew check`, `./gradlew shadowJar`), code style convention, and git convention.

9. Add a minimal README.md which includes the project name, a one-line description, and how to build and run the app.

10. Add a .gitignore which excludes any `build/`, `.gradle/` or IDE folders.

## Verification environments

`./gradlew run` is interactive: a successful JavaFX application normally keeps running until its window is closed. Do not treat that as a hung or failed build.

When a graphical environment is unavailable, report `./gradlew run` as not verified rather than failed. Continue verifying `./gradlew check` and `./gradlew shadowJar`.

## Project structure after setup

- src/
    - main/java/<app-name>/
        - App.java
        - MainController.java
        - Launcher.java (entry)
    - main/resources/<app-name>/
        - view/
            - MainWindow.fxml (root UI)
        - css/
            - <app-name>.css
    - test/java/<app-name>/
        - TrivialTest.java
- gradle/
    - wrapper/
        - gradle-wrapper.jar
        - gradle-wrapper.properties
- config/
    - checkstyle/
        - checkstyle.xml
        - suppressions.xml
- build.gradle
- settings.gradle
- gradlew.bat
- gradlew
- README.md
- AGENTS.md
- .gitignore
- .github/workflows/gradle.yml

## Definition of done

- `./gradlew run` launches the application window without errors when a graphical environment is available
- `./gradlew check` passes (Checkstyle and tests both pass)
- `./gradlew shadowJar` produces a runnable fat jar in `build/libs/`
- All files mentioned in the project structure above exist
