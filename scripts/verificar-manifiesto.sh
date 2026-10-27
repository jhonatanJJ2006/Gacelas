#!/usr/bin/env bash
# Comprueba que el manifiesto publico de Android (app-releases/android/latest.json) anuncie la
# version de build.gradle.kts. Si no coincide, las apps instaladas no ven la nueva version.
# Tambien comprueba que apkUrl (asset de GitHub Releases) descarga con el tamano de apkBytes.
# Uso: scripts/verificar-manifiesto.sh   (VERIFICAR_SHA=1 descarga el APK y compara sha256)
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
AND="$RAIZ/apps/android"
URL="${SUPABASE_URL:-$(grep -sE '^SUPABASE_URL=' "$AND/local.properties" | cut -d= -f2- || true)}"
URL="${URL:-https://yampnicdjlmimeoqmowb.supabase.co}"
NOMBRE="$(grep -oE 'versionName = "[^"]+"' "$AND/app/build.gradle.kts" | cut -d'"' -f2)"
MANIFIESTO="$(curl -fsS "$URL/storage/v1/object/public/app-releases/android/latest.json?t=$(date +%s)")"
PUBLICADO="$(echo "$MANIFIESTO" | jq -r .versionName)"
if [ "$PUBLICADO" != "$NOMBRE" ]; then
  echo "Manifiesto atrasado: anuncia $PUBLICADO y el repo esta en $NOMBRE. Publica con scripts/publicar-android.sh."
  exit 1
fi
# El APK anunciado debe descargarse (siguiendo la redireccion de GitHub) con el tamano declarado.
APK_URL="$(echo "$MANIFIESTO" | jq -r .apkUrl)"
ESPERADO="$(echo "$MANIFIESTO" | jq -r '.apkBytes // empty')"
REMOTO="$(curl -fsSLI "$APK_URL" | tr -d '\r' | awk 'tolower($1)=="content-length:"{v=$2} END{print v}')"
if [ -z "$REMOTO" ] || { [ -n "$ESPERADO" ] && [ "$REMOTO" != "$ESPERADO" ]; }; then
  echo "El APK de latest.json no responde o no coincide en tamano: $APK_URL (esperado $ESPERADO, recibido ${REMOTO:-nada})."
  exit 1
fi
if [ "${VERIFICAR_SHA:-}" = "1" ]; then
  SHA="$(curl -fsSL "$APK_URL" | sha256sum | cut -d' ' -f1)"
  [ "$SHA" = "$(echo "$MANIFIESTO" | jq -r .sha256)" ] || { echo "sha256 del APK no coincide con latest.json."; exit 1; }
fi
echo "Manifiesto al dia: $NOMBRE ($APK_URL, $REMOTO bytes)"
