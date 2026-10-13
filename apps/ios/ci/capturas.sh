#!/usr/bin/env bash
# Capturas del simulador por grupo, para correr los grupos en paralelo (matriz de CI).
# Uso: capturas.sh boot                                   (arranca el simulador sin esperar)
#      capturas.sh <ruta/iosApp.app> <dir-salida> <grupo>...  (espera el arranque, instala y captura)
set -uo pipefail
T0=$(date +%s); marca() { echo "[capturas +$(( $(date +%s) - T0 ))s] $*"; }
APPID=com.gacelas.app
UDID=$(xcrun simctl list devices available -j | python3 -c "import json,sys;d=json.load(sys.stdin)['devices'];p=[x for k,v in d.items() if 'iOS' in k for x in v if x['name'].startswith('iPhone')];print(next((x['udid'] for x in p if x['name']=='iPhone 16'),p[0]['udid']))")
if [ "$1" = boot ]; then
  # En segundo plano: arranque + calentamiento (la primera app lanzada en un simulador recien
  # creado tarda ~2 min mientras el sistema termina de inicializarse).
  nohup bash -c "xcrun simctl boot $UDID; xcrun simctl bootstatus $UDID -b; xcrun simctl launch $UDID com.apple.Preferences; sleep 20; xcrun simctl terminate $UDID com.apple.Preferences; touch /tmp/sim-listo" > /tmp/sim-boot.log 2>&1 &
  exit 0
fi
APP=$1 OUT=$2; shift 2
mkdir -p "$OUT"
xcrun simctl boot "$UDID" 2>/dev/null || true
marca bootstatus; xcrun simctl bootstatus "$UDID" -b
[ -f /tmp/sim-boot.log ] && { for i in $(seq 1 60); do [ -f /tmp/sim-listo ] && break; sleep 3; done; cat /tmp/sim-boot.log | tail -5; }
marca install
# Limite de tiempo para comandos simctl que a veces se cuelgan en un simulador recien arrancado.
limite() { local t=$1; shift; perl -e 'alarm shift; exec @ARGV' "$t" "$@"; }
limite 120 xcrun simctl install "$UDID" "$APP"
xcrun simctl status_bar "$UDID" override --time "9:41" --batteryState charged --batteryLevel 100 || true
ubicacion() { limite 30 xcrun simctl privacy "$UDID" grant location-always $APPID || limite 30 xcrun simctl privacy "$UDID" grant location $APPID || true; }
# shot <nombre> <espera> [args...]: lanza, espera, captura y cierra.
shot() {
  local n=$1 s=$2; shift 2
  xcrun simctl launch "$UDID" $APPID "$@"
  sleep "$s"
  xcrun simctl io "$UDID" screenshot "$OUT/$n.png"
  xcrun simctl terminate "$UDID" $APPID || true
}
for GRUPO in "$@"; do
marca "$GRUPO"
case "$GRUPO" in
  pantallas)
    # Paridad: pantallas compartidas con Android.
    shot bienvenida 12
    shot login 8 --pantalla=login
    shot inicio 8 --pantalla=inicio
    shot modal 8 --pantalla=modal
    shot ajustes 8 --pantalla=ajustes
    shot rutas 8 --pantalla=rutas ;;
  carrera)
    # La carrera pide ubicacion: se concede antes para que el aviso no tape la captura.
    ubicacion
    shot carrera 20 --pantalla=carrera --carrera-muestra
    shot carrera-panel 15 --pantalla=carrera --carrera-muestra --panel
    # Misma zona horaria que la captura Android (Loja) para la fecha del detalle.
    SIMCTL_CHILD_TZ=America/Guayaquil shot detalle 20 --pantalla=detalle ;;
  mapa)
    # Etapa 4: mapa MapLibre de rutas y estado de la descarga offline de Loja.
    shot rutas-mapa 20 --pantalla=rutas --ver-mapa
    shot ajustes-mapa-offline 45 --pantalla=ajustes ;;
  sensores)
    # Etapa 3: ubicacion simulada (Loja, ~3.3 m/s) y pantalla de sensores.
    ubicacion
    xcrun simctl launch "$UDID" $APPID --sensores
    sleep 5
    # "City Run" de Apple se reproduce en vivo en el simulador headless.
    xcrun simctl location "$UDID" run "City Run" || \
      printf -- '-3.99310,-79.20420\n-3.99600,-79.20430\n-3.99900,-79.20440\n' | xcrun simctl location "$UDID" start --speed=3.3 --interval=1 -
    sleep 12
    xcrun simctl io "$UDID" screenshot "$OUT/sensores-12s.png"
    sleep 15
    xcrun simctl io "$UDID" screenshot "$OUT/sensores.png"
    xcrun simctl terminate "$UDID" $APPID || true
    # Lecturas crecientes por el mismo pipeline: --gps-simulado.
    shot recorrido 20 --sensores --gps-simulado
    xcrun simctl location "$UDID" clear || true ;;
  *) echo "grupo desconocido: $GRUPO"; exit 2 ;;
esac
done
marca fin
ls -la "$OUT"
