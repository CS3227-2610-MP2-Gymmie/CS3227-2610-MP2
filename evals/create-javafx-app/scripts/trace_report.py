#!/usr/bin/env python3
"""Generate normalized JSON and a readable HTML report from a Codex JSONL trace."""

from __future__ import annotations

import argparse
import html
import json
from pathlib import Path
from typing import Any

from trace_tools import parse_trace, write_json


def _number(value: Any) -> str:
    return f"{int(value or 0):,}"


def _badge(passed: bool, true_text: str, false_text: str) -> str:
    css_class = "pass" if passed else "fail"
    text = true_text if passed else false_text
    return f'<span class="badge {css_class}">{html.escape(text)}</span>'


def _render_messages(summary: dict[str, Any]) -> str:
    sections = []
    for message in summary["messages"]:
        sections.append(
            '<article class="event message">'
            f'<header>Line {message["line"]} · Agent message</header>'
            f'<pre>{html.escape(message["text"])}</pre>'
            "</article>"
        )
    return "".join(sections) or "<p>No agent messages found.</p>"


def _render_commands(summary: dict[str, Any]) -> str:
    sections = []
    for index, command in enumerate(summary["commands"], start=1):
        failed = command["exit_code"] not in (None, 0) or command["status"] == "failed"
        status_class = "fail" if failed else "pass"
        output = command["output"] or "(no output)"
        sections.append(
            '<details class="event command" open>'
            "<summary>"
            f'<span class="badge {status_class}">{html.escape(str(command["status"]))}</span> '
            f'Command {index} · line {command["line"]} · exit {html.escape(str(command["exit_code"]))}'
            "</summary>"
            f'<pre class="command-text">{html.escape(command["command"])}</pre>'
            '<div class="label">Output</div>'
            f'<pre>{html.escape(output)}</pre>'
            "</details>"
        )
    return "".join(sections) or "<p>No commands found.</p>"


def _render_changes(summary: dict[str, Any]) -> str:
    sections = []
    for batch in summary["file_change_batches"]:
        rows = "".join(
            f'<li><code>{html.escape(change["kind"])}</code> {html.escape(change["path"])}</li>'
            for change in batch["changes"]
        )
        sections.append(
            '<article class="event change">'
            f'<header>Line {batch["line"]} · File change · {html.escape(str(batch["status"]))}</header>'
            f"<ul>{rows}</ul>"
            "</article>"
        )
    return "".join(sections) or "<p>No file changes found.</p>"


def render_html(summary: dict[str, Any], events: list[dict[str, Any]]) -> str:
    usage = summary["usage"]
    counts = summary["counts"]
    skill = summary["skill"]
    raw_json = "\n".join(
        json.dumps({key: value for key, value in event.items() if key != "_line"}, ensure_ascii=False)
        for event in events
    )
    parse_state = _badge(
        not summary["parse_errors"], "JSONL parsed cleanly", "JSONL parse errors"
    )
    trigger_state = _badge(
        skill["trigger_observed"], "Skill trigger observed", "No skill trigger observed"
    )

    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Codex trace report</title>
<style>
:root {{ color-scheme: light dark; --bg:#f6f7f9; --panel:#fff; --text:#17202a; --muted:#64748b; --line:#d8dee8; --pass:#137333; --pass-bg:#e6f4ea; --fail:#b3261e; --fail-bg:#fce8e6; --accent:#1d4ed8; }}
@media (prefers-color-scheme: dark) {{ :root {{ --bg:#101318; --panel:#181c22; --text:#edf2f7; --muted:#a6b0bf; --line:#343b46; --pass:#81c995; --pass-bg:#163c24; --fail:#f28b82; --fail-bg:#4b1f1c; --accent:#8ab4f8; }} }}
* {{ box-sizing:border-box; }} body {{ margin:0; background:var(--bg); color:var(--text); font:15px/1.5 ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif; }}
main {{ max-width:1180px; margin:0 auto; padding:36px 24px 80px; }} h1 {{ margin:0 0 6px; font-size:30px; }} h2 {{ margin:38px 0 14px; font-size:21px; }}
.source {{ color:var(--muted); overflow-wrap:anywhere; }} .badges {{ display:flex; gap:8px; flex-wrap:wrap; margin:18px 0; }} .badge {{ display:inline-block; border-radius:999px; padding:3px 9px; font-size:12px; font-weight:700; }}
.badge.pass {{ color:var(--pass); background:var(--pass-bg); }} .badge.fail {{ color:var(--fail); background:var(--fail-bg); }}
.cards {{ display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:10px; }} .card {{ background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:14px; }} .card strong {{ display:block; font-size:24px; }} .card span,.label {{ color:var(--muted); font-size:12px; text-transform:uppercase; letter-spacing:.05em; }}
table {{ width:100%; border-collapse:collapse; background:var(--panel); }} th,td {{ border:1px solid var(--line); padding:9px 11px; text-align:left; }} th {{ color:var(--muted); }}
.event {{ background:var(--panel); border:1px solid var(--line); border-radius:10px; margin:10px 0; overflow:hidden; }} .event header,.event summary {{ padding:11px 14px; font-weight:700; cursor:pointer; }} .event pre {{ margin:0; border-top:1px solid var(--line); padding:14px; max-height:520px; overflow:auto; white-space:pre-wrap; overflow-wrap:anywhere; font:12px/1.45 ui-monospace,SFMono-Regular,Menlo,monospace; }}
.event .label {{ padding:10px 14px 6px; border-top:1px solid var(--line); }} .event .label + pre {{ border-top:0; padding-top:4px; }} .event ul {{ margin:0; border-top:1px solid var(--line); padding:12px 34px; }} code {{ font-family:ui-monospace,SFMono-Regular,Menlo,monospace; }}
</style>
</head>
<body><main>
<h1>Codex trace report</h1>
<div class="source">{html.escape(summary["source_trace"])}</div>
<div class="badges">{parse_state}{trigger_state}</div>
<div class="cards">
  <div class="card"><strong>{counts["commands"]}</strong><span>Commands</span></div>
  <div class="card"><strong>{counts["failed_commands"]}</strong><span>Failed commands</span></div>
  <div class="card"><strong>{counts["unique_changed_files"]}</strong><span>Changed files</span></div>
  <div class="card"><strong>{_number(usage["input_tokens"])}</strong><span>Input tokens</span></div>
  <div class="card"><strong>{_number(usage["output_tokens"])}</strong><span>Output tokens</span></div>
  <div class="card"><strong>{_number(usage["reasoning_output_tokens"])}</strong><span>Reasoning tokens</span></div>
</div>
<h2>Usage</h2>
<table><tbody>
<tr><th>Input tokens</th><td>{_number(usage["input_tokens"])}</td></tr>
<tr><th>Cached input tokens</th><td>{_number(usage["cached_input_tokens"])}</td></tr>
<tr><th>Cache-write input tokens</th><td>{_number(usage["cache_write_input_tokens"])}</td></tr>
<tr><th>Derived uncached input tokens</th><td>{_number(usage["uncached_input_tokens"])}</td></tr>
<tr><th>Output tokens</th><td>{_number(usage["output_tokens"])}</td></tr>
<tr><th>Reasoning output tokens</th><td>{_number(usage["reasoning_output_tokens"])}</td></tr>
</tbody></table>
<h2>Skill trigger evidence</h2>
<p>Detection method: <code>{html.escape(skill["detection_method"])}</code>. Successful reads: {len(skill["successful_reads"])}. Read before first file change: {html.escape(str(skill["read_before_first_file_change"]))}.</p>
<h2>Agent messages</h2>{_render_messages(summary)}
<h2>Commands and full output</h2>{_render_commands(summary)}
<h2>File changes</h2>{_render_changes(summary)}
<h2>Raw JSONL</h2><details class="event"><summary>Show all {summary["raw_event_count"]} raw events</summary><pre>{html.escape(raw_json)}</pre></details>
</main></body></html>"""


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("trace", type=Path, help="Input trace.jsonl")
    parser.add_argument("--skill", default="create-javafx-app")
    parser.add_argument("--summary-out", type=Path)
    parser.add_argument("--html-out", type=Path)
    args = parser.parse_args()

    trace = args.trace.resolve()
    summary_out = args.summary_out or trace.with_suffix(".summary.json")
    html_out = args.html_out or trace.with_suffix(".report.html")
    summary, events = parse_trace(trace, args.skill)
    write_json(summary_out, summary)
    html_out.parent.mkdir(parents=True, exist_ok=True)
    html_out.write_text(render_html(summary, events), encoding="utf-8")

    print(f"Summary: {summary_out}")
    print(f"Report:  {html_out}")
    return 1 if summary["parse_errors"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
