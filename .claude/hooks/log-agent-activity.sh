#!/usr/bin/env bash
# Appends one JSON line per file-changing tool call to .claude/logs/agent-activity.jsonl
#
# DISABLED BY DEFAULT. No-ops unless the flag file exists:
#     touch .claude/logs/.enabled
#
# Never blocks anything, always exits 0. A logger that can fail a tool call is a
# liability; this one may lose a line, but must never stop work.

set -uo pipefail

root="${CLAUDE_PROJECT_DIR:-$PWD}"
log_dir="$root/.claude/logs"

# ---- the off switch ----
[ -f "$log_dir/.enabled" ] || exit 0

payload="$(cat)"

# On first enabled run, keep one raw payload so you can see which fields the
# harness actually sends. Delete the file to capture a fresh one.
[ -f "$log_dir/raw-payload-sample.json" ] || printf '%s' "$payload" > "$log_dir/raw-payload-sample.json" 2>/dev/null

printf '%s' "$payload" | python3 -c '
import json, sys, os, datetime
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit(0)

root = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
path = (d.get("tool_input") or {}).get("file_path", "") or ""
if path.startswith(root + "/"):
    path = path[len(root) + 1:]

# Best-effort agent identity. Which field the harness populates varies — check
# .claude/logs/raw-payload-sample.json after the first run to see what you get.
agent = d.get("agent_name") or d.get("subagent_type") or d.get("agentId") or ""
if not agent:
    tp = d.get("transcript_path", "") or ""
    agent = os.path.basename(tp).rsplit(".", 1)[0] if "/tasks/" in tp else "main"

rec = {
    "ts": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "agent": agent,
    "tool": d.get("tool_name", "") or "",
    "path": path,
}
r = os.environ.get("LOG_RESULT", "")
if r:
    rec["result"] = r

try:
    with open(os.path.join(root, ".claude", "logs", "agent-activity.jsonl"), "a") as f:
        f.write(json.dumps(rec) + "\n")
except Exception:
    pass
' 2>/dev/null

exit 0
