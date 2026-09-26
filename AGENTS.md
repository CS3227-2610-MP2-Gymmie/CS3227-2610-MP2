## Project context

Gymmie is a Java 25 JavaFX desktop application for gym facility management and workout tracking, enforcing strict Role-Based Access Control (RBAC) across three roles:
- Manager
- Trainer
- Member

## Tech stack

- Java 25
- JavaFX 25
- Gradle 9.7.1
- JUnit 5, Checkstyle, JaCoCo, and Shadow

## Build commands

- `./gradlew run` launches the desktop app.
- `./gradlew check` runs style checks and tests.
- `./gradlew shadowJar` creates a runnable fat JAR in `build/libs/`.

## Coding conventions

- Separate paragraphs with a blank Javadoc line. Start each paragraph after the first with `<p>` immediately before its first word. Omit closing `</p>` tags in prose paragraphs.
- End every Javadoc summary and each `@param`, `@return`, and `@throws` description with a period, including descriptions that span multiple lines.
- Use one `@throws` entry per exception and explain when it occurs. Declare checked exceptions that escape a method; do not add unchecked exceptions to method declarations.

## Git conventions

- Name branches in lowercase kebab-case describing the change, e.g. `implement-issue-37-renew-membership` or `add-review-script`.
- Keep commits focused on one logical change with its relevant tests.
- When proposing or creating a commit message, include enough detail to explain the rationale for the change.
- Use an imperative, capitalized subject with no trailing period. Aim for about
  50 characters and never exceed 72.
- Add a body for non-trivial changes, separated by a blank line and wrapped at
  72 characters. Explain what and why; let the diff explain how.
- Do not commit or push unless explicitly asked.
