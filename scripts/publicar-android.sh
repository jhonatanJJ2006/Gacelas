#!/usr/bin/env bash
# Publica una version de la app Android: compila el APK (con las claves de local.properties),
# lo sube a Supabase Storage (bucket publico app-releases) junto con android/latest.json y crea
# el tag y el release de GitHub (pre-release) con el mismo APK.
#
# Uso: scripts/publicar-android.sh "Novedad 1; Novedad 2; Novedad 3" [--obligatoria]
# Las novedades separadas por ";" van como lista (items) al modal; notes guarda el texto completo.
# Requisitos:
#   - apps/android/local.properties con SUPABASE_URL, SUPABASE_ANON_KEY y GOOGLE_WEB_CLIENT_ID.
#   - SUPABASE_SERVICE_ROLE_KEY en el entorno o en backend/.env (nunca va dentro del APK).
#   - versionName ya subido en apps/android/app/build.gradle.kts (versionCode se calcula solo).
#   - gh autenticado, jq, sha256sum y el mismo keystore de depuracion (SHA-1 23:CF:...:F9).
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
NOTAS="${1:?Falta el texto de las notas}"
OBLIGATORIA="${2:-}"
AND="$RAIZ/apps/android"
URL="$(grep -E '^SUPABASE_URL=' "$AND/local.properties" | cut -d= -f2-)"
KEY="${SUPABASE_SERVICE_ROLE_KEY:-$(grep -sE '^SUPABASE_SERVICE_ROLE_KEY=' "$RAIZ/backend/.env" | cut -d= -f2- || true)}"
[ -n "$URL" ] || { echo "local.properties no tiene SUPABASE_URL"; exit 1; }
[ -n "$KEY" ] || { echo "Falta SUPABASE_SERVICE_ROLE_KEY (entorno o backend/.env)"; exit 1; }

NOMBRE="$(grep -oE 'versionName = "[^"]+"' "$AND/app/build.gradle.kts" | cut -d'"' -f2)"
IFS='.' read -r MA MI PA <<< "$NOMBRE"
CODIGO=$(( MA * 10000 + MI * 100 + PA ))
echo "Publicando $NOMBRE (versionCode $CODIGO)"

(cd "$AND" && ./gradlew clean assembleDebug testDebugUnitTest -Pgacelas.abi=arm64-v8a -Pgacelas.release=true --console=plain -q)
APK="$AND/app/build/outputs/apk/debug/app-debug.apk"
SHA="$(sha256sum "$APK" | cut -d' ' -f1)"
DESTINO="android/gacelas-$NOMBRE-arm64.apk"
PUBLICA="$URL/storage/v1/object/public/app-releases"

subir() { # $1 archivo local, $2 ruta en el bucket, $3 tipo
  curl -fsS -X POST "$URL/storage/v1/object/app-releases/$2" -H "Authorization: Bearer $KEY" -H "apikey: $KEY" \
    -H "Content-Type: $3" -H "x-upsert: true" -H "Cache-Control: max-age=60" --data-binary "@$1" > /dev/null
}
subir "$APK" "$DESTINO" application/vnd.android.package-archive

MIN=null; [ "$OBLIGATORIA" = "--obligatoria" ] && MIN=$CODIGO
BYTES="$(stat -c %s "$APK")"
jq -n --argjson c "$CODIGO" --arg n "$NOMBRE" --arg notas "$NOTAS" --arg apk "$PUBLICA/$DESTINO" --arg sha "$SHA" --argjson min "$MIN" --argjson bytes "$BYTES" \
  '($notas | split(";") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0))) as $items
   | {versionCode:$c, versionName:$n, notes:($items | join(". ")), items:$items, apkUrl:$apk, apkBytes:$bytes, sha256:$sha}
   + (if $min == null then {} else {minVersionCode:$min} end)' > /tmp/latest.json
subir /tmp/latest.json android/latest.json application/json
curl -fsS "$PUBLICA/android/latest.json" | jq .

cp "$APK" /tmp/gacelas-debug-arm64.apk
git -C "$RAIZ" tag "v$NOMBRE" && git -C "$RAIZ" push origin "v$NOMBRE"
gh release create "v$NOMBRE" /tmp/gacelas-debug-arm64.apk --prerelease --title "Gacelas v$NOMBRE" --notes "$(echo "$NOTAS" | tr ';' '\n' | sed 's/^ */- /')

APK debug arm64-v8a."
