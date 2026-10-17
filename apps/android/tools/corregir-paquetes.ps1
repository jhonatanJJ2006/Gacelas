$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$android = Join-Path $repo "apps\android"
$k = "kotlin\com\gacelas"

function Fix-Path {
    param([string]$Module, [string]$Ss, [string]$From, [string]$To)
    $base = Join-Path $android "$Module\src\$Ss\$k"
    $src = Join-Path $base $From
    $dst = Join-Path $base $To
    if (-not (Test-Path $src)) { return }
    New-Item -ItemType Directory -Force -Path (Split-Path $dst -Parent) | Out-Null
    git -C $repo mv $src $dst 2>$null
    if ($LASTEXITCODE -ne 0) { Move-Item -Force $src $dst }
}

$sets = @("commonMain", "androidMain", "iosMain", "commonTest")

foreach ($ss in $sets) {
    Fix-Path "core\comun" $ss "Comun.kt" "shared\Comun.kt"
    Fix-Path "core\comun" $ss "Plataforma.kt" "shared\Plataforma.kt"
    Fix-Path "core\comun" $ss "Comun.android.kt" "shared\Comun.android.kt"
    Fix-Path "core\comun" $ss "Comun.ios.kt" "shared\Comun.ios.kt"
    Fix-Path "core\comun" $ss "Plataforma.android.kt" "shared\Plataforma.android.kt"
    Fix-Path "core\comun" $ss "Plataforma.ios.kt" "shared\Plataforma.ios.kt"
    Fix-Path "core\comun" $ss "Geo.kt" "tracking\Geo.kt"
    Fix-Path "core\comun" $ss "ComunTest.kt" "shared\ComunTest.kt"
    Fix-Path "core\comun" $ss "LogicaComunTest.kt" "shared\LogicaComunTest.kt"

    Fix-Path "core\datos" $ss "Almacen.kt" "data\Almacen.kt"
    Fix-Path "core\datos" $ss "remoto" "data\remoto"
    Fix-Path "core\datos" $ss "actualizacion" "data\actualizacion"

    Fix-Path "core\ui" $ss "sensores" "core\sensores"
    Fix-Path "core\ui" $ss "SensoresTest.kt" "shared\SensoresTest.kt"
    Fix-Path "core\ui" $ss "MapaComunTest.kt" "shared\MapaComunTest.kt"

    foreach ($ctx in @("carreras", "rutas", "cuenta", "comunidad")) {
        Fix-Path "contextos\$ctx\domain" $ss "domain" "$ctx\domain"
        Fix-Path "contextos\$ctx\data" $ss "data" "$ctx\data"
        Fix-Path "contextos\$ctx\presentation" $ss "presentation" "$ctx\presentation"
    }

    Fix-Path "contextos\carreras\domain" $ss "PlanIntervalos.kt" "tracking\PlanIntervalos.kt"
    Fix-Path "contextos\carreras\domain" $ss "NotificacionCarrera.kt" "tracking\NotificacionCarrera.kt"
    Fix-Path "contextos\carreras\domain" $ss "DominioCarrerasTest.kt" "carreras\DominioCarrerasTest.kt"
    Fix-Path "contextos\carreras\presentation" $ss "DetalleCarreraContenido.kt" "perfil\DetalleCarreraContenido.kt"
    Fix-Path "contextos\carreras\presentation" $ss "ViewModelsCarrerasTest.kt" "carreras\ViewModelsCarrerasTest.kt"

    Fix-Path "contextos\rutas\presentation" $ss "ListaRutasContenido.kt" "rutas\ListaRutasContenido.kt"
    Fix-Path "contextos\rutas\presentation" $ss "Decimal.android.kt" "rutas\Decimal.android.kt"
    Fix-Path "contextos\rutas\presentation" $ss "Decimal.ios.kt" "rutas\Decimal.ios.kt"
    Fix-Path "contextos\rutas\domain" $ss "RutasTest.kt" "rutas\RutasTest.kt"
    Fix-Path "contextos\rutas\presentation" $ss "DibujarRutaTest.kt" "rutas\DibujarRutaTest.kt"

    Fix-Path "contextos\cuenta\domain" $ss "CuentaTest.kt" "cuenta\CuentaTest.kt"
    Fix-Path "contextos\comunidad\domain" $ss "ComunidadTest.kt" "comunidad\ComunidadTest.kt"
    Fix-Path "contextos\comunidad\presentation" $ss "ComunidadDetalleTest.kt" "comunidad\ComunidadDetalleTest.kt"
}

# Mover restos duplicados en shared -> modulos correctos
$sharedBase = Join-Path $android "shared\src"
foreach ($ss in $sets) {
    $sb = Join-Path $sharedBase "$ss\$k"
    if (-not (Test-Path $sb)) { continue }
    foreach ($ctx in @("carreras", "rutas", "cuenta", "comunidad")) {
        foreach ($layer in @("domain", "data", "presentation")) {
            $p = Join-Path $sb "$ctx\$layer"
            if (Test-Path $p) {
                $dest = Join-Path $android "contextos\$ctx\$layer\src\$ss\$k\$ctx\$layer"
                New-Item -ItemType Directory -Force -Path (Split-Path $dest -Parent) | Out-Null
                Get-ChildItem $p -Recurse -File | ForEach-Object {
                    $rel = $_.FullName.Substring($p.Length + 1)
                    $target = Join-Path (Join-Path $android "contextos\$ctx\$layer\src\$ss\$k\$ctx\$layer") $rel
                    New-Item -ItemType Directory -Force -Path (Split-Path $target -Parent) | Out-Null
                    if (-not (Test-Path $target)) {
                        git -C $repo mv $_.FullName $target 2>$null
                        if ($LASTEXITCODE -ne 0) { Move-Item -Force $_.FullName $target }
                    }
                }
            }
        }
    }
    foreach ($extra in @(
        @{ m = "core\ui"; p = "ui" },
        @{ m = "core\ui"; p = "mapa" },
        @{ m = "core\ui"; p = "core\sensores" },
        @{ m = "core\datos"; p = "data\remoto" },
        @{ m = "core\datos"; p = "data\Almacen.kt" },
        @{ m = "core\datos"; p = "data\actualizacion" },
        @{ m = "contextos\cuenta\presentation"; p = "acceso" },
        @{ m = "contextos\cuenta\presentation"; p = "ajustes" },
        @{ m = "contextos\cuenta\presentation"; p = "actualizacion" },
        @{ m = "contextos\carreras\presentation"; p = "correr" }
    )) {
        $from = Join-Path $sb $extra.p
        if (-not (Test-Path $from)) { continue }
        $toParent = Join-Path $android "$($extra.m)\src\$ss\$k"
        if ($extra.p -match '\.') {
            $to = Join-Path $toParent ($extra.p -replace '\\', '\')
        } else {
            $to = Join-Path $toParent $extra.p
        }
        if (-not (Test-Path $to)) {
            New-Item -ItemType Directory -Force -Path (Split-Path $to -Parent) | Out-Null
            git -C $repo mv $from $to 2>$null
            if ($LASTEXITCODE -ne 0) { Move-Item -Force $from $to }
        }
    }
}

Write-Host "Paquetes corregidos"
