#!/usr/bin/env bash
# Publica una version de la app Android: compila el APK (con las claves de local.properties),
# crea el tag y el release de GitHub (pre-release) con el APK como archivo
# runcity-X.Y.Z-arm64.apk, y publica en el bucket publico app-releases de Supabase
# el APK actual y android/latest.json. apkUrl apunta a la URL publica de Supabase
# porque el repo es privado: una URL de GitHub Releases no es descargable sin
# token, y la app no lleva tokens. En el bucket solo queda un APK (se borra el
# anterior despues de actualizar el manifiesto). El historial de versiones sigue
# en GitHub Releases. Es la UNICA forma de publicar: un "gh release create" a
# mano no actualiza latest.json y las apps instaladas no ven la version nueva
# (regresion 0.8.1-0.8.3, ver docs/REGRESIONES.md).
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
# La app descarga el APK desde Supabase (bucket publico). GitHub Releases es el archivo.
DESTINO="android/$ASSET"
PUBLICA="$URL/storage/v1/object/public/app-releases"
APK_URL="$PUBLICA/$DESTINO"
LIMITE_SUPABASE=52428800  # 50 MiB por archivo en el plan gratuito; la actualizacion in-app depende de este tope
AVISO_BYTES=157286400     # 150 MiB: solo aviso, no bloquea el release de GitHub
[ "$BYTES" -gt "$AVISO_BYTES" ] && echo "Aviso: el APK pesa $BYTES bytes (mas de 150 MiB). Revisa recursos y ABIs."

subir() { # $1 archivo local, $2 ruta en el bucket, $3 tipo
  curl -fsS -X POST "$URL/storage/v1/object/app-releases/$2" -H "Authorization: Bearer $KEY" -H "apikey: $KEY" \
    -H "Content-Type: $3" -H "x-upsert: true" -H "Cache-Control: max-age=60" --data-binary "@$1" > /dev/null
}

borrar_objeto() { # $1 ruta en el bucket
  curl -fsS -X DELETE "$URL/storage/v1/object/app-releases/$1" \
    -H "Authorization: Bearer $KEY" -H "apikey: $KEY" > /dev/null
}

# 1) Tag y release de GitHub con el APK como archivo de nombre estable.
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cp "$APK" "$TMP/$ASSET"
git -C "$RAIZ" tag "v$NOMBRE" && git -C "$RAIZ" push origin "v$NOMBRE"
gh release create "v$NOMBRE" "$TMP/$ASSET" --repo "$REPO" --prerelease --verify-tag --title "Runcity v$NOMBRE" --notes "$(echo "$NOTAS" | tr ';' '\n' | sed 's/^ */- /')

APK debug arm64-v8a ($BYTES bytes, sha256 $SHA)."

# 2) Comprobar el asset de archivo en GitHub (hace falta gh autenticado; la URL publica 404 en un repo privado).
mkdir -p "$TMP/gh"
gh release download "v$NOMBRE" --repo "$REPO" --pattern "$ASSET" --dir "$TMP/gh" --clobber
REMOTO="$(sha256sum "$TMP/gh/$ASSET" | cut -d' ' -f1)"
[ "$REMOTO" = "$SHA" ] || { echo "El asset de GitHub no coincide con el APK local; latest.json no se actualiza."; exit 1; }

# 3) El APK publico tiene que caber en Supabase: si no cabe, las apps no podrian descargarlo
#    (el repo es privado). No se actualiza latest.json.
if [ "$BYTES" -gt "$LIMITE_SUPABASE" ]; then
  echo "El APK pesa $BYTES bytes y supera el limite de $LIMITE_SUPABASE de Supabase Storage."
  echo "El release de GitHub v$NOMBRE se creo, pero latest.json no se actualiza: las apps no podrian descargar el APK."
  exit 1
fi

# 4) Subir el APK nuevo, comprobar la URL publica y luego el manifiesto.
subir "$APK" "$DESTINO" application/vnd.android.package-archive
for i in 1 2 3 4 5 6; do
  PUBLICO="$(curl -fsSL "$APK_URL?t=$(date +%s)" -o "$TMP/publico.apk" && sha256sum "$TMP/publico.apk" | cut -d' ' -f1 || true)"
  [ "$PUBLICO" = "$SHA" ] && break
  sleep 5
done
[ "$PUBLICO" = "$SHA" ] || { echo "El APK publico de Supabase no coincide con el APK local; latest.json no se actualiza."; exit 1; }

MIN=null; [ "$OBLIGATORIA" = "--obligatoria" ] && MIN=$CODIGO
jq -n --argjson c "$CODIGO" --arg n "$NOMBRE" --arg notas "$NOTAS" --arg apk "$APK_URL" --arg sha "$SHA" --argjson min "$MIN" --argjson bytes "$BYTES" \
  '($notas | split(";") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0))) as $items
   | {versionCode:$c, versionName:$n, notes:($items | join(". ")), items:$items, apkUrl:$apk, apkBytes:$bytes, sha256:$sha}
   + (if $min == null then {} else {minVersionCode:$min} end)' > "$TMP/latest.json"
subir "$TMP/latest.json" android/latest.json application/json
curl -fsS "$PUBLICA/android/latest.json?t=$(date +%s)" | jq .

# 5) Borrar los APK anteriores del bucket; solo queda el recien publicado y latest.json.
LISTA="$(curl -fsS -X POST "$URL/storage/v1/object/list/app-releases" \
  -H "Authorization: Bearer $KEY" -H "apikey: $KEY" -H "Content-Type: application/json" \
  -d '{"prefix":"android/","limit":100}')"
echo "$LISTA" | jq -r '.[] | .name // empty' | while IFS= read -r name; do
  [ -n "$name" ] || continue
  case "$name" in
    *.apk) ;;
    *) continue ;;
  esac
  case "$name" in
    android/*) dest="$name" ;;
    *) dest="android/$name" ;;
  esac
  if [ "$dest" != "$DESTINO" ]; then
    borrar_objeto "$dest" && echo "Borrado del bucket: $dest"
  fi
done

"$RAIZ/scripts/verificar-manifiesto.sh"
