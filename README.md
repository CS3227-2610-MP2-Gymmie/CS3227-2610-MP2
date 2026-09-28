[![Java CI](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/actions/workflows/gradle.yml/badge.svg)](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/actions/workflows/gradle.yml)

# Gymmie

![Member's Browse sessions screen](docs/images/Ui.png)

**Gymmie is a desktop app for managing a small or medium gym**, with tools for Managers, Trainers and Members. The gym is open 24 hours, so sessions can be scheduled and booked at any time, including overnight.

- **Managers** create, edit, archive, restore and delete membership plans; create accounts, edit display names, and deactivate or reactivate accounts.
- **Trainers** maintain their profile; create, edit, cancel and delete unused sessions; and view upcoming sessions and rosters.
- **Members** buy, renew and cancel a membership, browse and book sessions, and view and cancel bookings.

Members choose and buy membership plans in Gymmie; each purchase is recorded as a membership, while payment is handled at the gym.

## Getting started

Requires **JDK 25**. Download [`gymmie-release.jar`](https://github.com/CS3227-2610-MP2-Gymmie/CS3227-2610-MP2/releases) from GitHub Releases and run:

```shell
java -jar gymmie-release.jar
```

The release JAR bundles JavaFX for Windows x64, Apple Silicon macOS and x64 Linux. On other platforms, run Gymmie from source with `./gradlew run` (or `.\gradlew.bat run` on Windows).

Sign in first as the seeded Manager (`manager` / `manager123`) and change the password. Gymmie stores its data in `data/gymmie.db`, relative to the folder you launch it from. See the [User Guide](docs/UserGuide.md) for full instructions on using Gymmie.

## Documentation

- **[User Guide](docs/UserGuide.md)** — instructions for each role's workflows.
- **[Developer Guide](docs/DeveloperGuide.md)** — architecture, development workflow and implementation details.
- **[Product website](https://cs3227-2610-mp2-gymmie.github.io/CS3227-2610-MP2/)**

For development, run `./gradlew check`. On a graphical desktop, run `./gradlew test -PuiTests=true` to include the UI tests.

**Team:** [shawnnygoh](https://github.com/shawnnygoh) (Member features), [naa-siuuuu-ff](https://github.com/naa-siuuuu-ff) (Manager features), and [TaiaYovelaPang](https://github.com/TaiaYovelaPang) (Trainer features).

## Acknowledgements

Built with [JavaFX](https://openjfx.io/), [SQLite JDBC](https://github.com/xerial/sqlite-jdbc), [Gradle](https://gradle.org/) and [Shadow](https://gradleup.com/shadow/), [JUnit 5](https://junit.org/junit5/), and [Checkstyle](https://checkstyle.org/). The documentation structure is adapted from [AddressBook-Level3](https://github.com/se-edu/addressbook-level3) by [SE-EDU](https://se-education.org/). See the Developer Guide's [Acknowledgements](docs/DeveloperGuide.md) for details.
