#!/usr/bin/env bash
# The gate, both halves. This is what runs before a PR and what the demo breaks on stage.
set -uo pipefail
cd "$(dirname "$0")"

fail=0

echo "──────────────── backend ────────────────"
./gradlew check --console=plain || fail=1

echo
echo "──────────────── frontend ───────────────"
(cd frontend && npm run check) || fail=1

echo
if [ "$fail" -eq 0 ]; then
  echo "GATE GREEN"
else
  echo "GATE RED — see output above"
fi
exit "$fail"
