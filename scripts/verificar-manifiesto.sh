#!/usr/bin/env bash
# Comprueba que el manifiesto publico de Android (app-releases/android/latest.json) anuncie la
# version de build.gradle.kts. Si no coincide, las apps instaladas no ven la nueva version.
# Uso: scripts/verificar-manifiesto.sh   (sale con error si el manifiesto esta atrasado)
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
AND="$RAIZ/apps/android"
URL="${SUPABASE_URL:-$(grep -sE '^SUPABASE_URL=' "$AND/local.properties" | cut -d= -f2- || true)}"
URL="${URL:-https://yampnicdjlmimeoqmowb.supabase.co}"
NOMBRE="$(grep -oE 'versionName = "[^"]+"' "$AND/app/build.gradle.kts" | cut -d'"' -f2)"
PUBLICADO="$(curl -fsS "$URL/storage/v1/object/public/app-releases/android/latest.json?t=$(date +%s)" | jq -r .versionName)"
if [ "$PUBLICADO" != "$NOMBRE" ]; then
  echo "Manifiesto atrasado: anuncia $PUBLICADO y el repo esta en $NOMBRE. Publica con scripts/publicar-android.sh."
  exit 1
fi
echo "Manifiesto al dia: $NOMBRE"
