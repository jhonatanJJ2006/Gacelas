# Mueve fuentes de shared/ a modulos Gradle (preserva source sets).
$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$android = Join-Path $repo "apps\android"
$shared = Join-Path $android "shared\src"
$k = "kotlin\com\gacelas"

function Git-Mv {
    param([string]$From, [string]$ToDir)
    if (-not (Test-Path $From)) { return }
    New-Item -ItemType Directory -Force -Path $ToDir | Out-Null
    $dest = Join-Path $ToDir (Split-Path $From -Leaf)
    if (Test-Path $dest) { return }
    git -C $repo mv $From $dest
    if ($LASTEXITCODE -ne 0) {
        Move-Item -Force $From $dest
    }
}

function ModSrc([string]$module, [string]$ss) {
    Join-Path $android "$module\src\$ss\$k"
}

$sets = @("commonMain", "androidMain", "iosMain", "commonTest")

foreach ($ss in $sets) {
    $base = Join-Path $shared "$ss\$k"

    Git-Mv (Join-Path $base "data\remoto") (ModSrc "core\datos" $ss)
    Git-Mv (Join-Path $base "data\Almacen.kt") (ModSrc "core\datos" $ss)
    Git-Mv (Join-Path $base "data\actualizacion") (ModSrc "core\datos" $ss)

    foreach ($f in @("Comun.kt", "Plataforma.kt", "Comun.android.kt", "Comun.ios.kt", "Plataforma.android.kt", "Plataforma.ios.kt")) {
        Git-Mv (Join-Path $base "shared\$f") (ModSrc "core\comun" $ss)
    }
    Git-Mv (Join-Path $base "tracking\Geo.kt") (ModSrc "core\comun" $ss)

    Git-Mv (Join-Path $base "ui") (ModSrc "core\ui" $ss)
    Git-Mv (Join-Path $base "mapa") (ModSrc "core\ui" $ss)
    Git-Mv (Join-Path $base "core\sensores") (ModSrc "core\ui" $ss)

    Git-Mv (Join-Path $base "carreras\domain") (ModSrc "contextos\carreras\domain" $ss)
    Git-Mv (Join-Path $base "carreras\data") (ModSrc "contextos\carreras\data" $ss)
    Git-Mv (Join-Path $base "carreras\presentation") (ModSrc "contextos\carreras\presentation" $ss)
    Git-Mv (Join-Path $base "correr") (ModSrc "contextos\carreras\presentation" $ss)
    Git-Mv (Join-Path $base "perfil\DetalleCarreraContenido.kt") (ModSrc "contextos\carreras\presentation" $ss)
    Git-Mv (Join-Path $base "tracking\PlanIntervalos.kt") (ModSrc "contextos\carreras\domain" $ss)
    Git-Mv (Join-Path $base "tracking\NotificacionCarrera.kt") (ModSrc "contextos\carreras\domain" $ss)

    Git-Mv (Join-Path $base "rutas\domain") (ModSrc "contextos\rutas\domain" $ss)
    Git-Mv (Join-Path $base "rutas\data") (ModSrc "contextos\rutas\data" $ss)
    Git-Mv (Join-Path $base "rutas\presentation") (ModSrc "contextos\rutas\presentation" $ss)
    Git-Mv (Join-Path $base "rutas\ListaRutasContenido.kt") (ModSrc "contextos\rutas\presentation" $ss)
    Git-Mv (Join-Path $base "rutas\Decimal.android.kt") (ModSrc "contextos\rutas\presentation" $ss)
    Git-Mv (Join-Path $base "rutas\Decimal.ios.kt") (ModSrc "contextos\rutas\presentation" $ss)

    Git-Mv (Join-Path $base "cuenta\domain") (ModSrc "contextos\cuenta\domain" $ss)
    Git-Mv (Join-Path $base "cuenta\data") (ModSrc "contextos\cuenta\data" $ss)
    Git-Mv (Join-Path $base "cuenta\presentation") (ModSrc "contextos\cuenta\presentation" $ss)
    Git-Mv (Join-Path $base "acceso") (ModSrc "contextos\cuenta\presentation" $ss)
    Git-Mv (Join-Path $base "ajustes") (ModSrc "contextos\cuenta\presentation" $ss)
    Git-Mv (Join-Path $base "actualizacion") (ModSrc "contextos\cuenta\presentation" $ss)

    Git-Mv (Join-Path $base "comunidad\domain") (ModSrc "contextos\comunidad\domain" $ss)
    Git-Mv (Join-Path $base "comunidad\data") (ModSrc "contextos\comunidad\data" $ss)
    Git-Mv (Join-Path $base "comunidad\presentation") (ModSrc "contextos\comunidad\presentation" $ss)

    Git-Mv (Join-Path $base "shared\ComunTest.kt") (ModSrc "core\comun" $ss)
    Git-Mv (Join-Path $base "shared\LogicaComunTest.kt") (ModSrc "core\comun" $ss)
    Git-Mv (Join-Path $base "shared\MapaComunTest.kt") (ModSrc "core\ui" $ss)
    Git-Mv (Join-Path $base "shared\SensoresTest.kt") (ModSrc "core\ui" $ss)
    Git-Mv (Join-Path $base "carreras\DominioCarrerasTest.kt") (ModSrc "contextos\carreras\domain" $ss)
    Git-Mv (Join-Path $base "carreras\ViewModelsCarrerasTest.kt") (ModSrc "contextos\carreras\presentation" $ss)
    Git-Mv (Join-Path $base "rutas\RutasTest.kt") (ModSrc "contextos\rutas\domain" $ss)
    Git-Mv (Join-Path $base "rutas\DibujarRutaTest.kt") (ModSrc "contextos\rutas\presentation" $ss)
    Git-Mv (Join-Path $base "cuenta\CuentaTest.kt") (ModSrc "contextos\cuenta\domain" $ss)
    Git-Mv (Join-Path $base "comunidad\ComunidadTest.kt") (ModSrc "contextos\comunidad\domain" $ss)
    Git-Mv (Join-Path $base "comunidad\ComunidadDetalleTest.kt") (ModSrc "contextos\comunidad\presentation" $ss)
}

Write-Host "OK"
