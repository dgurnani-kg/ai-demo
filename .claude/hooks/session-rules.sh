#!/usr/bin/env bash
# SessionStart hook. Cheap, visible, and sets expectations before the first prompt lands.
set -euo pipefail

cat <<'MSG'
library-service — house rules

  contract      api/openapi.yaml is read-only. Use the api-contract-change skill.
  migrations    never edit an applied file. Add V{n+1}__.
  layers        web -> service -> domain. Controllers do not touch repositories.
  the gate      ./gradlew check   and   cd frontend && npm run check

  Multi-layer feature? Start with the vertical-slice skill.
MSG
