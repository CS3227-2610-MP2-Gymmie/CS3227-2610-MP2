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
round=$(awk '/^## Independent review$/ { inside=1; next } inside && /^## / { exit } inside && /^### Round [0-9]+$/ { value=$3 + 0; if (value > max) max=value } END { print max + 1 }' "$report")
head_short=$(git rev-parse --short HEAD)
if [[ -n $(git status --porcelain --untracked-files=all) ]]; then
    working_tree_note="Uncommitted changes: yes."
else
    working_tree_note="Uncommitted changes: no."
fi
review_file=$(mktemp); body_file=$(mktemp); report_file=$(mktemp)
trap 'rm -f "$review_file" "$body_file" "$report_file"' EXIT
IFS= read -r -d '' prompt <<EOF || true
You are an independent, read-only reviewer. You did not write this change. Read AGENTS.md, relevant documentation, and the report at $report.
Review the diff from merge base $merge_base (base ref $base_ref) to the current working tree, including uncommitted changes and untracked files (use git status to find them). Do not edit files.
Earlier review rounds in the report are context only. Judge the current code on its own merits. You may reject the implementer's reasons for dismissing earlier findings.
Use **Overall:** fail only for blocking findings: an unmet acceptance criterion, a bug, a missing test for a criterion, or a violation of documented repository conventions. Otherwise use **Overall:** pass, even if non-blocking findings remain.
Return exactly this Markdown format and nothing else:
- Table header | Area | Score (1–5) | Evidence |; one row per report acceptance criterion, then one row for each of its four self-assessment areas in original order. Score 1–5 and give evidence.
- **Overall:** pass or **Overall:** fail, followed by one sentence.
- **Blocking:** a Markdown list of blocking findings, or None.
- **Non-blocking:** a Markdown list of other findings, or None.
EOF
codex exec -m "$model" --sandbox read-only -o "$review_file" "$prompt"
{
    printf '\n### Round %s\n\n' "$round"
    cat "$review_file"
    printf '\n\nReviewed: %s; base: %s; model: %s; HEAD: %s; %s\n\n' "$(date +%F)" "$base_ref" "$model" "$head_short" "$working_tree_note"
} > "$body_file"
awk -v body_file="$body_file" '
    /^## Independent review$/ { print; inside=1; next }
    inside && /^## / {
        while ((getline line < body_file) > 0) print line
        close(body_file)
        inside=0
        print
        next
    }
    inside && /^\[Rounds are appended by the independent review command\./ { next }
    { print }
    END {
        if (inside) {
            while ((getline line < body_file) > 0) print line
            close(body_file)
        }
    }
' "$report" > "$report_file"
cat "$report_file" > "$report"
