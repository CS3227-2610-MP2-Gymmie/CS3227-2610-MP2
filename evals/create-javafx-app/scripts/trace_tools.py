#!/usr/bin/env python3
"""Shared parsing utilities for Codex JSONL evaluation traces."""

from __future__ import annotations

import json
import re
from collections import Counter
from pathlib import Path
from typing import Any


def load_jsonl(path: Path) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Load a JSONL trace and return valid events plus parse errors."""
    events: list[dict[str, Any]] = []
    errors: list[dict[str, Any]] = []

    with path.open(encoding="utf-8") as stream:
        for line_number, raw_line in enumerate(stream, start=1):
            line = raw_line.strip()
            if not line:
                continue
            try:
                event = json.loads(line)
            except json.JSONDecodeError as error:
                errors.append(
                    {
                        "line": line_number,
                        "error": str(error),
                        "raw": raw_line.rstrip("\n"),
                    }
                )
                continue
            event["_line"] = line_number
            events.append(event)

    return events, errors


def _aggregate_usage(events: list[dict[str, Any]]) -> dict[str, int]:
    fields = (
        "input_tokens",
        "cached_input_tokens",
        "cache_write_input_tokens",
        "output_tokens",
        "reasoning_output_tokens",
    )
    usage = {field: 0 for field in fields}
    for event in events:
        if event.get("type") != "turn.completed":
            continue
        event_usage = event.get("usage") or {}
        for field in fields:
            usage[field] += int(event_usage.get(field, 0) or 0)
    usage["uncached_input_tokens"] = max(
        0,
        usage["input_tokens"]
        - usage["cached_input_tokens"]
        - usage["cache_write_input_tokens"],
    )
    return usage


def _skill_path_pattern(skill_name: str) -> re.Pattern[str]:
    escaped = re.escape(skill_name)
    return re.compile(rf"(?:^|[/\\]){escaped}[/\\]SKILL\.md(?:$|[\s'\"])" )


def build_summary(
    trace_path: Path,
    events: list[dict[str, Any]],
    parse_errors: list[dict[str, Any]],
    skill_name: str,
) -> dict[str, Any]:
    """Convert raw events into a stable summary used by reports and graders."""
    event_counts = Counter(event.get("type", "<missing>") for event in events)
    item_counts: Counter[str] = Counter()
    messages: list[dict[str, Any]] = []
    commands: list[dict[str, Any]] = []
    file_change_batches: list[dict[str, Any]] = []
    unique_changed_files: set[str] = set()
    completed_item_lines: dict[str, int] = {}

    for event in events:
        if event.get("type") != "item.completed":
            continue
        item = event.get("item") or {}
        item_type = item.get("type", "<missing>")
        item_counts[item_type] += 1
        item_id = str(item.get("id", ""))
        completed_item_lines[item_id] = event["_line"]

        if item_type == "agent_message":
            messages.append(
                {
                    "line": event["_line"],
                    "id": item_id,
                    "text": item.get("text", ""),
                }
            )
        elif item_type == "command_execution":
            commands.append(
                {
                    "line": event["_line"],
                    "id": item_id,
                    "command": item.get("command", ""),
                    "status": item.get("status"),
                    "exit_code": item.get("exit_code"),
                    "output": item.get("aggregated_output", ""),
                }
            )
        elif item_type == "file_change":
            changes = item.get("changes") or []
            normalized_changes = []
            for change in changes:
                path = str(change.get("path", ""))
                if path:
                    unique_changed_files.add(path)
                normalized_changes.append(
                    {"path": path, "kind": change.get("kind", "unknown")}
                )
            file_change_batches.append(
                {
                    "line": event["_line"],
                    "id": item_id,
                    "status": item.get("status"),
                    "changes": normalized_changes,
                }
            )

    skill_pattern = _skill_path_pattern(skill_name)
    skill_reads = []
    for command in commands:
        if skill_pattern.search(command["command"]):
            skill_reads.append(
                {
                    "line": command["line"],
                    "id": command["id"],
                    "status": command["status"],
                    "exit_code": command["exit_code"],
                    "command": command["command"],
                }
            )

    skill_mentions = [
        {"line": message["line"], "id": message["id"], "text": message["text"]}
        for message in messages
        if skill_name.casefold() in message["text"].casefold()
    ]
    successful_skill_reads = [
        read
        for read in skill_reads
        if read["status"] == "completed" and read["exit_code"] == 0
    ]
    first_change_line = min(
        (batch["line"] for batch in file_change_batches), default=None
    )
    first_skill_read_line = min(
        (read["line"] for read in successful_skill_reads), default=None
    )

    thread_ids = [
        event.get("thread_id")
        for event in events
        if event.get("type") == "thread.started" and event.get("thread_id")
    ]
    failed_commands = [
        command
        for command in commands
        if command["status"] == "failed"
        or (command["exit_code"] is not None and command["exit_code"] != 0)
    ]

    return {
        "schema_version": 1,
        "source_trace": str(trace_path.resolve()),
        "thread_id": thread_ids[0] if thread_ids else None,
        "parse_errors": parse_errors,
        "counts": {
            "events": len(events),
            "event_types": dict(sorted(event_counts.items())),
            "completed_item_types": dict(sorted(item_counts.items())),
            "commands": len(commands),
            "failed_commands": len(failed_commands),
            "agent_messages": len(messages),
            "file_change_batches": len(file_change_batches),
            "unique_changed_files": len(unique_changed_files),
        },
        "usage": _aggregate_usage(events),
        "skill": {
            "name": skill_name,
            "trigger_observed": bool(successful_skill_reads),
            "detection_method": "successful_skill_file_read",
            "successful_reads": successful_skill_reads,
            "all_read_attempts": skill_reads,
            "explicit_name_mentions": skill_mentions,
            "first_successful_read_line": first_skill_read_line,
            "first_file_change_line": first_change_line,
            "read_before_first_file_change": (
                first_skill_read_line < first_change_line
                if first_skill_read_line is not None and first_change_line is not None
                else None
            ),
        },
        "messages": messages,
        "commands": commands,
        "failed_commands": failed_commands,
        "file_change_batches": file_change_batches,
        "unique_changed_files": sorted(unique_changed_files),
        "final_message": messages[-1]["text"] if messages else None,
        "turn_completed": event_counts.get("turn.completed", 0) > 0,
        "raw_event_count": len(events),
    }


def parse_trace(trace_path: Path, skill_name: str) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    events, parse_errors = load_jsonl(trace_path)
    return build_summary(trace_path, events, parse_errors, skill_name), events


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
