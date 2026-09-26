#!/usr/bin/env python3
"""Validate invariants that JSON Schema alone cannot enforce in a model grade."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


EXPECTED_IDS = {
    "minimal_scope",
    "ui_coherence",
    "documentation_accuracy",
    "maintainability",
}


def validate(data: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(data, dict):
        return ["top-level result must be an object"]

    allowed_top = {"overall_pass", "score", "checks", "summary"}
    if set(data) != allowed_top:
        errors.append(
            f"top-level keys must be exactly {sorted(allowed_top)}; got {sorted(data)}"
        )

    checks = data.get("checks")
    if not isinstance(checks, list):
        return errors + ["checks must be an array"]
    if len(checks) != 4:
        errors.append(f"checks must contain exactly four entries; got {len(checks)}")

    ids = [check.get("id") for check in checks if isinstance(check, dict)]
    if set(ids) != EXPECTED_IDS or len(ids) != len(set(ids)):
        errors.append(
            f"check IDs must each appear once; expected {sorted(EXPECTED_IDS)}, got {ids}"
        )

    calculated_score = 0
    all_checks_pass = True
    allowed_check = {"id", "pass", "score", "evidence", "notes"}
    for index, check in enumerate(checks):
        if not isinstance(check, dict):
            errors.append(f"checks[{index}] must be an object")
            all_checks_pass = False
            continue
        if set(check) != allowed_check:
            errors.append(
                f"checks[{index}] keys must be exactly {sorted(allowed_check)}"
            )
        score = check.get("score")
        if not isinstance(score, int) or isinstance(score, bool) or not 0 <= score <= 25:
            errors.append(f"checks[{index}].score must be an integer from 0 to 25")
            all_checks_pass = False
            continue
        calculated_score += score
        expected_pass = score >= 18
        if check.get("pass") is not expected_pass:
            errors.append(
                f"checks[{index}].pass must be {expected_pass} for score {score}"
            )
        all_checks_pass = all_checks_pass and expected_pass
        evidence = check.get("evidence")
        if not isinstance(evidence, list) or not evidence or not all(
            isinstance(item, str) and item.strip() for item in evidence
        ):
            errors.append(f"checks[{index}].evidence must contain non-empty strings")
        if not isinstance(check.get("notes"), str):
            errors.append(f"checks[{index}].notes must be a string")

    if data.get("score") != calculated_score:
        errors.append(
            f"top-level score must equal the check-score sum {calculated_score}; "
            f"got {data.get('score')}"
        )
    expected_overall = all_checks_pass and calculated_score >= 72
    if data.get("overall_pass") is not expected_overall:
        errors.append(
            f"overall_pass must be {expected_overall}; got {data.get('overall_pass')}"
        )
    if not isinstance(data.get("summary"), str):
        errors.append("summary must be a string")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("grade", type=Path)
    args = parser.parse_args()

    try:
        data = json.loads(args.grade.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        print(f"INVALID: {error}")
        return 1

    errors = validate(data)
    if errors:
        print("INVALID model grade")
        for error in errors:
            print(f"- {error}")
        return 1
    print(f"VALID model grade: score={data['score']}, pass={data['overall_pass']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
