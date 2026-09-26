#!/usr/bin/env bash
set -euo pipefail
if [[ $# -lt 1 || $# -gt 2 ]]; then echo "Usage: $0 <report-path> [base-ref]" >&2; exit 2; fi
report=$1
base_ref=${2:-master}
model=${REVIEW_MODEL:-gpt-6-astra}
[[ -f "$report" ]] || { echo "Report not found: $report" >&2; exit 1; }
grep -q '^## Independent review$' "$report" || { echo "Report is missing its Independent review section: $report" >&2; exit 1; }
repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"
merge_base=$(git merge-base "$base_ref" HEAD)
review_file=$(mktemp); body_file=$(mktemp); report_file=$(mktemp)
trap 'rm -f "$review_file" "$body_file" "$report_file"' EXIT
prompt=$(cat <<EOF
You are an independent, read-only reviewer. You did not write this change. Read AGENTS.md, relevant documentation, and the report at $report.
Review the diff from merge base $merge_base (base ref $base_ref) to the current working tree, including uncommitted changes and untracked files (use git status to find them). Do not edit files.
Return exactly this Markdown format and nothing else:
- Table header | Area | Score (1–5) | Evidence |; one row per report acceptance criterion, then one row for each of its four self-assessment areas in original order. Score 1–5 and give evidence.
- **Overall:** pass or **Overall:** fail, followed by one sentence.
- **Findings:** up to five concrete convention violations, missing tests, or bugs, or None.
EOF
)
codex exec -m "$model" --sandbox read-only -o "$review_file" "$prompt"
{ echo; cat "$review_file"; printf '\n\nReviewed: %s (base: %s, model: %s)\n\n' "$(date +%F)" "$base_ref" "$model"; } > "$body_file"
awk -v body_file="$body_file" '/^## Independent review$/ { print; while ((getline line < body_file) > 0) print line; close(body_file); inside=1; next } inside && /^## / { inside=0 } !inside { print }' "$report" > "$report_file"
cat "$report_file" > "$report"
