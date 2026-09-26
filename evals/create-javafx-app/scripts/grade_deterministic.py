#!/usr/bin/env python3
"""Grade a Codex trace and its generated JavaFX repository deterministically."""

from __future__ import annotations

import argparse
import hashlib
import re
import shlex
from pathlib import Path
from typing import Any

from trace_tools import parse_trace, write_json


def parse_bool(value: str) -> bool:
    normalized = value.casefold()
    if normalized in {"true", "yes", "1"}:
        return True
    if normalized in {"false", "no", "0"}:
        return False
    raise argparse.ArgumentTypeError("expected true or false")


class GradeBook:
    def __init__(self) -> None:
        self.checks: list[dict[str, Any]] = []

    def add(
        self,
        check_id: str,
        scope: str,
        passed: bool,
        expected: Any,
        actual: Any,
        evidence: list[str],
        severity: str = "error",
    ) -> None:
        self.checks.append(
            {
                "id": check_id,
                "scope": scope,
                "severity": severity,
                "pass": bool(passed),
                "expected": expected,
                "actual": actual,
                "evidence": evidence,
            }
        )

    def result(self, trace: Path, artifact: Path) -> dict[str, Any]:
        failures = [
            check
            for check in self.checks
            if not check["pass"] and check["severity"] == "error"
        ]
        warnings = [
            check
            for check in self.checks
            if not check["pass"] and check["severity"] == "warning"
        ]
        return {
            "schema_version": 1,
            "trace": str(trace.resolve()),
            "artifact": str(artifact.resolve()),
            "overall_pass": not failures,
            "counts": {
                "checks": len(self.checks),
                "passed": sum(check["pass"] for check in self.checks),
                "failures": len(failures),
                "warnings": len(warnings),
            },
            "checks": self.checks,
        }


def successful_gradle_task(summary: dict[str, Any], task: str) -> list[dict[str, Any]]:
    task_pattern = re.compile(rf"(?<![\w-])(?::)?{re.escape(task)}(?![\w-])")
    matches = []
    for command in summary["commands"]:
        command_text = command["command"]
        invokes_gradle = "./gradlew" in command_text or re.search(r"(?:^|\s)gradle\s", command_text)
        if (
            invokes_gradle
            and task_pattern.search(command_text)
            and command["status"] == "completed"
            and command["exit_code"] == 0
        ):
            matches.append(command)
    return matches


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.is_file() else ""


def documented_gradle_tasks(content: str) -> set[str]:
    """Return tasks used in documented Gradle wrapper command lines."""
    tasks: set[str] = set()
    invocation_pattern = re.compile(
        r"(?:\./)?gradlew(?:\.bat)?(?P<arguments>[^`\r\n]*)"
    )
    for match in invocation_pattern.finditer(content):
        arguments = match.group("arguments")
        try:
            tokens = shlex.split(arguments)
        except ValueError:
            tokens = arguments.split()
        for token in tokens:
            normalized = token.strip(";,()[]{}")
            if normalized.startswith("-"):
                continue
            tasks.add(normalized.rsplit(":", maxsplit=1)[-1])
    return tasks


def grade_trace(
    book: GradeBook,
    summary: dict[str, Any],
    expected_trigger: bool,
    max_commands: int,
    max_failed_commands: int,
) -> None:
    skill = summary["skill"]
    counts = summary["counts"]
    usage = summary["usage"]

    book.add(
        "trace.valid_jsonl",
        "trace",
        not summary["parse_errors"],
        "zero malformed JSONL lines",
        len(summary["parse_errors"]),
        [f"line {error['line']}: {error['error']}" for error in summary["parse_errors"]],
    )
    book.add(
        "trace.turn_completed",
        "trace",
        summary["turn_completed"],
        True,
        summary["turn_completed"],
        [f"event counts: {counts['event_types']}"],
    )
    book.add(
        "trace.skill_trigger",
        "trace",
        skill["trigger_observed"] == expected_trigger,
        expected_trigger,
        skill["trigger_observed"],
        [
            f"line {read['line']}: {read['command']}"
            for read in skill["successful_reads"]
        ] or ["no successful read of the target SKILL.md was observed"],
    )
    if expected_trigger:
        book.add(
            "trace.skill_read_before_changes",
            "trace",
            skill["read_before_first_file_change"] is True,
            True,
            skill["read_before_first_file_change"],
            [
                f"skill line={skill['first_successful_read_line']}, "
                f"first file-change line={skill['first_file_change_line']}"
            ],
        )

    check_commands = successful_gradle_task(summary, "check")
    book.add(
        "trace.gradle_check_succeeded",
        "trace",
        bool(check_commands),
        "at least one successful Gradle check command",
        len(check_commands),
        [f"line {command['line']}: {command['command']}" for command in check_commands],
    )
    if expected_trigger:
        shadow_commands = successful_gradle_task(summary, "shadowJar")
        book.add(
            "trace.shadow_jar_succeeded",
            "trace",
            bool(shadow_commands),
            "at least one successful Gradle shadowJar command",
            len(shadow_commands),
            [f"line {command['line']}: {command['command']}" for command in shadow_commands],
        )

    book.add(
        "trace.usage_present",
        "trace",
        usage["input_tokens"] > 0 and usage["output_tokens"] > 0,
        "positive input_tokens and output_tokens",
        usage,
        ["usage aggregated from turn.completed events"],
    )
    book.add(
        "trace.command_budget",
        "trace",
        counts["commands"] <= max_commands,
        f"at most {max_commands}",
        counts["commands"],
        [f"completed command_execution items: {counts['commands']}"],
        severity="warning",
    )
    book.add(
        "trace.failed_command_budget",
        "trace",
        counts["failed_commands"] <= max_failed_commands,
        f"at most {max_failed_commands}",
        counts["failed_commands"],
        [
            f"line {command['line']} exit={command['exit_code']}: {command['command']}"
            for command in summary["failed_commands"]
        ],
        severity="warning",
    )


def grade_artifact(
    book: GradeBook,
    artifact: Path,
    reference: Path,
    package_name: str,
    app_name: str,
) -> None:
    package_path = Path(*package_name.split("."))
    java_root = Path("src/main/java") / package_path
    resource_root = Path("src/main/resources") / package_path
    test_root = Path("src/test/java") / package_path
    css_name = package_name.rsplit(".", maxsplit=1)[-1] + ".css"
    required_files = [
        java_root / "App.java",
        java_root / "Launcher.java",
        java_root / "MainController.java",
        resource_root / "view/MainWindow.fxml",
        resource_root / "css" / css_name,
        test_root / "TrivialTest.java",
        Path("gradle/wrapper/gradle-wrapper.jar"),
        Path("gradle/wrapper/gradle-wrapper.properties"),
        Path("config/checkstyle/checkstyle.xml"),
        Path("config/checkstyle/suppressions.xml"),
        Path(".github/workflows/gradle.yml"),
        Path("build.gradle"),
        Path("settings.gradle"),
        Path("gradlew"),
        Path("gradlew.bat"),
        Path("README.md"),
        Path("AGENTS.md"),
        Path(".gitignore"),
    ]
    missing = [str(path) for path in required_files if not (artifact / path).is_file()]
    book.add(
        "artifact.required_files",
        "artifact",
        not missing,
        [str(path) for path in required_files],
        {"missing": missing},
        [f"artifact root: {artifact.resolve()}"],
    )

    java_files = []
    for source_root in (artifact / java_root, artifact / test_root):
        if source_root.is_dir():
            java_files.extend(sorted(source_root.glob("*.java")))
    invalid_packages = [
        str(path.relative_to(artifact))
        for path in java_files
        if not re.search(rf"(?m)^package\s+{re.escape(package_name)}\s*;", read_text(path))
    ]
    book.add(
        "artifact.package_convention",
        "artifact",
        bool(java_files) and not invalid_packages,
        f"Java sources under {java_root} declare package {package_name};",
        {
            "java_files_found": [str(path.relative_to(artifact)) for path in java_files],
            "invalid_package_declarations": invalid_packages,
        },
        invalid_packages or (["no Java files found at the required package path"] if not java_files else []),
    )

    legacy_path = artifact / "src/main/java/com" / package_path
    book.add(
        "artifact.no_com_prefix",
        "artifact",
        not legacy_path.exists(),
        f"no sources under src/main/java/com/{package_path}",
        legacy_path.exists(),
        [str(legacy_path)],
    )

    fxml_path = artifact / resource_root / "view/MainWindow.fxml"
    fxml_text = read_text(fxml_path)
    book.add(
        "artifact.app_name_in_root_view",
        "artifact",
        app_name in fxml_text,
        f"{app_name!r} appears in MainWindow.fxml",
        app_name in fxml_text,
        [str(fxml_path.relative_to(artifact)) if fxml_path.exists() else "MainWindow.fxml missing"],
    )

    reference_pairs = [
        (Path("config/checkstyle/checkstyle.xml"), reference / "checkstyle.xml"),
        (Path("config/checkstyle/suppressions.xml"), reference / "suppressions.xml"),
        (Path(".github/workflows/gradle.yml"), reference / "gradle.yml"),
    ]
    for destination, source in reference_pairs:
        generated = artifact / destination
        both_exist = generated.is_file() and source.is_file()
        match = both_exist and generated.read_bytes() == source.read_bytes()
        book.add(
            f"artifact.reference_match.{source.stem}",
            "artifact",
            match,
            f"byte-for-byte match with {source.name}",
            {
                "generated_exists": generated.is_file(),
                "reference_exists": source.is_file(),
                "generated_sha256": sha256(generated) if generated.is_file() else None,
                "reference_sha256": sha256(source) if source.is_file() else None,
            },
            [str(destination), str(source.resolve())],
        )

    wrapper_path = artifact / "gradle/wrapper/gradle-wrapper.properties"
    wrapper_text = read_text(wrapper_path)
    book.add(
        "artifact.gradle_wrapper_version",
        "artifact",
        "gradle-9.7.1-bin.zip" in wrapper_text,
        "distributionUrl contains gradle-9.7.1-bin.zip",
        next(
            (line for line in wrapper_text.splitlines() if line.startswith("distributionUrl=")),
            None,
        ),
        ["gradle/wrapper/gradle-wrapper.properties"],
    )

    build_path = artifact / "build.gradle"
    build_text = read_text(build_path)
    required_build_patterns = {
        "application plugin": r"\bid\s+['\"]application['\"]",
        "JaCoCo plugin": r"\bid\s+['\"]jacoco['\"]",
        "Checkstyle plugin": r"\bid\s+['\"]checkstyle['\"]",
        "JavaFX plugin": r"\bid\s+['\"]org\.openjfx\.javafxplugin['\"]\s+version\s+['\"]0\.1\.0['\"]",
        "Shadow plugin": r"\bid\s+['\"]com\.gradleup\.shadow['\"]\s+version\s+['\"]9\.6\.1['\"]",
        "JDK 25 toolchain": r"JavaLanguageVersion\.of\(25\)",
        "JUnit Platform": r"useJUnitPlatform\(\)",
        "JUnit launcher": r"junit-platform-launcher",
    }
    missing_fragments = [
        label
        for label, pattern in required_build_patterns.items()
        if not re.search(pattern, build_text)
    ]
    book.add(
        "artifact.gradle_configuration",
        "artifact",
        not missing_fragments,
        list(required_build_patterns),
        {"missing": missing_fragments},
        ["build.gradle"],
    )

    documentation_requirements = {
        "README.md": ["./gradlew run", "./gradlew check", "./gradlew shadowJar"],
        "AGENTS.md": ["./gradlew run", "./gradlew check", "./gradlew shadowJar"],
    }
    missing_doc_entries: dict[str, list[str]] = {}
    for relative_path, fragments in documentation_requirements.items():
        content = read_text(artifact / relative_path)
        observed_tasks = documented_gradle_tasks(content)
        missing_values = [
            fragment
            for fragment in fragments
            if fragment.rsplit(maxsplit=1)[-1] not in observed_tasks
        ]
        if missing_values:
            missing_doc_entries[relative_path] = missing_values
    book.add(
        "artifact.documentation_commands",
        "artifact",
        not missing_doc_entries,
        documentation_requirements,
        missing_doc_entries,
        sorted(documentation_requirements),
    )

    gitignore_path = artifact / ".gitignore"
    gitignore_text = read_text(gitignore_path)
    gitignore_groups = {
        "build output": ["build/", "**/build/"],
        "Gradle cache": [".gradle/", ".gradle-home/", ".gradle-user-home/"],
        "IDE metadata": [".idea/", ".vscode/", "*.iml"],
    }
    missing_groups = [
        label
        for label, alternatives in gitignore_groups.items()
        if not any(alternative in gitignore_text for alternative in alternatives)
    ]
    book.add(
        "artifact.gitignore",
        "artifact",
        gitignore_path.is_file() and not missing_groups,
        "ignores build output, Gradle caches, and IDE metadata",
        {"exists": gitignore_path.is_file(), "missing_groups": missing_groups},
        [".gitignore"],
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--trace", type=Path, required=True)
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--reference", type=Path, default=Path("evals/reference"))
    parser.add_argument("--skill", default="create-javafx-app")
    parser.add_argument("--expected-trigger", type=parse_bool, required=True)
    parser.add_argument("--package", default="gymmie")
    parser.add_argument("--app-name", default="Gymmie")
    parser.add_argument(
        "--artifact-profile",
        choices=("scaffold", "trigger-only"),
        help="Defaults to scaffold for positive trigger cases and trigger-only otherwise.",
    )
    parser.add_argument("--max-commands", type=int, default=25)
    parser.add_argument("--max-failed-commands", type=int, default=8)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    trace = args.trace.resolve()
    artifact = args.artifact.resolve()
    reference = args.reference.resolve()
    summary, _ = parse_trace(trace, args.skill)

    book = GradeBook()
    grade_trace(
        book,
        summary,
        args.expected_trigger,
        args.max_commands,
        args.max_failed_commands,
    )
    artifact_profile = args.artifact_profile or (
        "scaffold" if args.expected_trigger else "trigger-only"
    )
    if artifact_profile == "scaffold":
        grade_artifact(book, artifact, reference, args.package, args.app_name)
    result = book.result(trace, artifact)
    result["artifact_profile"] = artifact_profile
    write_json(args.out, result)

    print(
        f"{'PASS' if result['overall_pass'] else 'FAIL'}: "
        f"{result['counts']['passed']}/{result['counts']['checks']} checks passed, "
        f"{result['counts']['failures']} failures, {result['counts']['warnings']} warnings"
    )
    print(f"Grade: {args.out}")
    return 0 if result["overall_pass"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
