#!/usr/bin/env bash
# Writes a Markdown test summary (Surefire totals + Allure statuses per scenario) to $GITHUB_STEP_SUMMARY,
# or to stdout when run locally. Never fails the build: the Maven step already sets the job status.
# Usage: test-summary.sh <title> <surefire-reports-dir> <allure-results-dir>
set -u
title="$1"
surefire_dir="$2"
allure_dir="$3"
out="${GITHUB_STEP_SUMMARY:-/dev/stdout}"
export PYTHONIOENCODING=utf-8

python3 - "$title" "$surefire_dir" "$allure_dir" >> "$out" <<'PY' || echo "Summary could not be generated" >> "$out"
import glob, json, os, sys
import xml.etree.ElementTree as ET

title, surefire_dir, allure_dir = sys.argv[1:4]
print(f"## {title}\n")

totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
for path in glob.glob(os.path.join(surefire_dir, "TEST-*.xml")):
    root = ET.parse(path).getroot()
    for key in totals:
        totals[key] += int(root.get(key, 0))
if totals["tests"]:
    print("| Surefire | Tests | Failures | Errors | Skipped |\n|---|---|---|---|---|")
    print(f"| JUnit Platform | {totals['tests']} | {totals['failures']} | {totals['errors']} | {totals['skipped']} |\n")
else:
    print("No Surefire reports found: the build failed before running tests (check the Maven step log).\n")

icons = {"passed": "✅", "failed": "❌", "broken": "⚠️", "skipped": "⏭️"}
rows = []
for path in glob.glob(os.path.join(allure_dir, "*-result.json")):
    with open(path, encoding="utf-8") as handle:
        result = json.load(handle)
    tags = sorted(t for t in (l.get("value", "").lstrip("@") for l in result.get("labels", []) if l.get("name") == "tag") if t.startswith(("TC-", "esc-")))
    message = (result.get("statusDetails") or {}).get("message", "").strip().splitlines()
    rows.append((result.get("status", "unknown"), " ".join(tags), result.get("name", ""), message[0][:160] if message else ""))
if rows:
    counts = {}
    for status, *_ in rows:
        counts[status] = counts.get(status, 0) + 1
    print("**Scenarios (Allure):** " + " · ".join(f"{icons.get(s, '')} {s}: {n}" for s, n in sorted(counts.items())) + "\n")
    print("| Status | Tags | Scenario | Message |\n|---|---|---|---|")
    for status, tags, name, message in sorted(rows, key=lambda r: (r[1], r[2])):
        print(f"| {icons.get(status, '')} {status} | {tags} | {name} | {message.replace('|', '/')} |")
else:
    print("No Allure results found.")
PY
exit 0
