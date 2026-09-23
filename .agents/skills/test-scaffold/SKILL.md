---
name: test-scaffold
description: >-
  Create or extend JUnit 5 tests for Gymmie, a Java 25 JavaFX gym
  management application. Use when the user asks to write tests,
  add test coverage, or scaffold tests for application behavior.
origin:
  adapted_from: "Agensi Learn Examples"
  source_url: "https://www.agensi.io/learn/skill-md-examples"
  modifications: modified to fit the context of this project.
---

# Test Scaffolding for Gymmie

## Inspect the project

Before writing tests:

- Read applicable AGENTS.md instructions.
- Check build.gradle for test dependencies and task configuration.
- Review existing tests under src/test/java and the relevant source.
- Read config/checkstyle/checkstyle.xml for formatting requirements.
- Use the existing JUnit Jupiter setup. Do not assume Mockito or TestFX is installed.

## Match existing conventions

- Place tests in src/test/java, mirroring the source package.
- Name test classes `<ClassName>Test` and extend existing test files when appropriate.
- Use package-private test classes and methods, matching existing tests.
- Use JUnit 5 `@Test` and static imports from `org.junit.jupiter.api.Assertions`.
- Use descriptive camelCase method names that explain the behavior and condition.
- Use `@BeforeEach` for shared setup and `@Nested` only when grouping materially improves readability.
- Use parameterized tests for meaningful sets of equivalent cases.
- Follow the project's Checkstyle and Javadoc conventions.

## Test observable behavior

Read the class and its callers to identify its responsibilities, dependencies, and expected outcomes.

Cover relevant cases:

- Happy path for each exported function/component within the requested scope; in Gymmie's Java code, this means each public method or component's exposed behavior, including successful results and expected state changes.
- Invalid inputs and documented exceptions using `assertThrows`.
- Nulls, empty collections, blank strings, and boundary values where the contract makes them relevant.
- Failure paths that must preserve existing state.
- Gym facility management and workout tracking rules supported by requirements or existing behavior.

Do not invent business rules, test private implementation details, or add tests merely to increase coverage percentages.

## Verify role-based access control

For operations governed by RBAC:

- Determine the intended permissions for Manager, Trainer, and Gym User from requirements and the application's authorization policy.
- Test both permitted and denied access for applicable roles.
- Verify ownership restrictions where operations involve another user's records.
- Verify denied operations leave protected state unchanged.
- Test missing authentication where the operation requires a session.
- Exercise authorization at the layer that enforces the operation; hiding or disabling a JavaFX control alone does not prove access is restricted.

If permissions are unclear, identify the ambiguity instead of encoding an assumed role hierarchy.

## Handle JavaFX and dependencies

- Test business logic independently of JavaFX when possible.
- Initialize the JavaFX toolkit only for tests that require it, and perform scene graph interactions on the JavaFX Application Thread.
- Use existing JavaFX test helpers if available. Avoid arbitrary sleeps and ensure tests clean up windows and resources they create.
- Prefer small fakes or stubs for external dependencies when sufficient. Use an existing mocking library if the project provides one.
- Isolate persistence tests with temporary data, using `@TempDir` for files.
- Control time-dependent behavior through existing clock abstractions where available.
- Keep tests independent of live services, user data, and execution order.
- Add testing dependencies only when needed for the requested scope, and explain the reason.

## Verify and report

1. Run the relevant tests: `./gradlew test --tests 'gymmie.package.ClassNameTest'`. Replace the example with the actual fully qualified test class.
2. Run `./gradlew check` after the targeted tests pass.
3. When coverage analysis is requested, run `./gradlew test jacocoTestReport`. Inspect the generated report rather than inferring coverage from the number of tests.

Report the test files changed, behavior covered, and verification results. Explain any blocked checks or remaining coverage gaps. Do not claim tests passed unless they were executed successfully.
