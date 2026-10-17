#!/usr/bin/env bash
# Ejecuta SQL contra el proyecto Supabase con la Management API (HTTPS).
# Uso: scripts/sb-query.sh archivo.sql   |   echo "select 1" | scripts/sb-query.sh -
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
TOKEN="${SUPABASE_ACCESS_TOKEN:-$(grep -E '^SUPABASE_ACCESS_TOKEN=' "$RAIZ/backend/.env" | cut -d= -f2-)}"
REF="${SUPABASE_PROJECT_REF:-yampnicdjlmimeoqmowb}"
SQL="$(cat "${1:--}")"
jq -n --arg q "$SQL" '{query:$q}' | curl -sS -X POST "https://api.supabase.com/v1/projects/$REF/database/query" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' --data-binary @-
echo
