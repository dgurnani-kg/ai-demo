#!/usr/bin/env bash
# PreToolUse hook for Edit / Write / MultiEdit.
#
# Exit 0 = allow. Exit 2 = block, and stderr is handed back to the model as the reason.
#
# This is the layer that is not persuasion. CLAUDE.md asks agents not to edit the contract;
# this stops them. The difference matters on the third turn of a long session.

set -euo pipefail

payload="$(cat)"

path="$(printf '%s' "$payload" | python3 -c '
import json, sys
try:
    data = json.load(sys.stdin)
except Exception:
    print("")
    sys.exit(0)
print(data.get("tool_input", {}).get("file_path", "") or "")
')"

[ -z "$path" ] && exit 0

# Normalise to a repo-relative path.
root="${CLAUDE_PROJECT_DIR:-$PWD}"
rel="${path#"$root"/}"

case "$rel" in
  api/openapi.yaml)
    if [ "${ALLOW_CONTRACT_EDIT:-}" = "1" ]; then
      exit 0
    fi
    cat >&2 <<'MSG'
BLOCKED: api/openapi.yaml is the contract and is read-only to implementation work.

It changes through one procedure only: the `api-contract-change` skill, which covers
breaking-change rules, additionalProperties, and regenerating the frontend types.

If the feature genuinely needs a field the contract does not have, stop and say so.
Do not add the field in code and do not work around the gap on the client.
MSG
    exit 2
    ;;

  src/main/resources/db/migration/*)
    if [ -f "$path" ]; then
      cat >&2 <<MSG
BLOCKED: $rel already exists and has been applied.

Flyway records a checksum per migration. Editing an applied file makes the application
refuse to start, here and on every shared database.

Add the next migration instead: V{n+1}__description.sql. The \`flyway-migration\` skill
has the naming and column conventions.
MSG
      exit 2
    fi
    ;;

  .claude/hooks/*|.claude/settings.json)
    cat >&2 <<'MSG'
BLOCKED: the guardrails are not editable from inside a session.

A gate an agent can edit is not a gate. Change these through a pull request like any
other code, with a human reviewing the change to the rules themselves.
MSG
    exit 2
    ;;
esac

exit 0
