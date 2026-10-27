#!/usr/bin/env bash
# Publica una version de la app Android: compila el APK (con las claves de local.properties),
# crea el tag y el release de GitHub (pre-release) con el APK como asset runcity-X.Y.Z-arm64.apk y
# sube a Supabase Storage (bucket publico app-releases) solo android/latest.json, con apkUrl apuntando
# al asset de GitHub. Asi el tamano del APK no depende del limite de 50 MiB de Supabase.
# Es la UNICA forma de publicar: un "gh release create" a mano no actualiza latest.json y las apps
# instaladas no ven la version nueva (regresion 0.8.1-0.8.3, ver docs/REGRESIONES.md).
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
BYTES="$(stat -c %s "$APK")"
ASSET="runcity-$NOMBRE-arm64.apk"
REPO="jhonatanJJ2006/Gacelas"
# El APK vive en GitHub Releases (limite 2 GB por archivo). Supabase Storage solo guarda latest.json.
APK_URL="https://github.com/$REPO/releases/download/v$NOMBRE/$ASSET"
PUBLICA="$URL/storage/v1/object/public/app-releases"
LIMITE_SUPABASE=52428800  # 50 MiB por archivo en el plan gratuito
AVISO_BYTES=157286400     # 150 MiB: solo aviso, no bloquea
[ "$BYTES" -gt "$AVISO_BYTES" ] && echo "Aviso: el APK pesa $BYTES bytes (mas de 150 MiB). Revisa recursos y ABIs."

subir() { # $1 archivo local, $2 ruta en el bucket, $3 tipo
  curl -fsS -X POST "$URL/storage/v1/object/app-releases/$2" -H "Authorization: Bearer $KEY" -H "apikey: $KEY" \
    -H "Content-Type: $3" -H "x-upsert: true" -H "Cache-Control: max-age=60" --data-binary "@$1" > /dev/null
}

# 1) Tag y release de GitHub con el APK como asset de nombre estable.
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cp "$APK" "$TMP/$ASSET"
git -C "$RAIZ" tag "v$NOMBRE" && git -C "$RAIZ" push origin "v$NOMBRE"
gh release create "v$NOMBRE" "$TMP/$ASSET" --repo "$REPO" --prerelease --verify-tag --title "Runcity v$NOMBRE" --notes "$(echo "$NOTAS" | tr ';' '\n' | sed 's/^ */- /')

APK debug arm64-v8a ($BYTES bytes, sha256 $SHA)."

# 2) Comprobar que el asset publico descarga completo (sigue la redireccion 302 de GitHub).
for i in 1 2 3 4 5 6; do
  REMOTO="$(curl -fsSL "$APK_URL" -o "$TMP/remoto.apk" && sha256sum "$TMP/remoto.apk" | cut -d' ' -f1 || true)"
  [ "$REMOTO" = "$SHA" ] && break; sleep 5
done
[ "$REMOTO" = "$SHA" ] || { echo "El asset de GitHub no coincide con el APK local; latest.json no se actualiza."; exit 1; }

# 3) Copia de respaldo en Supabase solo si cabe (las apps no la usan; sirve para descargas manuales).
if [ "$BYTES" -le "$LIMITE_SUPABASE" ]; then
  subir "$APK" "android/$ASSET" application/vnd.android.package-archive || echo "Aviso: no se subio la copia de respaldo a Supabase."
fi

# 4) Manifiesto: solo latest.json en Supabase, apuntando al asset de GitHub.
MIN=null; [ "$OBLIGATORIA" = "--obligatoria" ] && MIN=$CODIGO
jq -n --argjson c "$CODIGO" --arg n "$NOMBRE" --arg notas "$NOTAS" --arg apk "$APK_URL" --arg sha "$SHA" --argjson min "$MIN" --argjson bytes "$BYTES" \
  '($notas | split(";") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0))) as $items
   | {versionCode:$c, versionName:$n, notes:($items | join(". ")), items:$items, apkUrl:$apk, apkBytes:$bytes, sha256:$sha}
   + (if $min == null then {} else {minVersionCode:$min} end)' > "$TMP/latest.json"
subir "$TMP/latest.json" android/latest.json application/json
curl -fsS "$PUBLICA/android/latest.json?t=$(date +%s)" | jq .
"$RAIZ/scripts/verificar-manifiesto.sh"
