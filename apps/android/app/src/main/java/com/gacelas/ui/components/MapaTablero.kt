package com.gacelas.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import com.gacelas.ui.theme.GColors
import org.maplibre.android.geometry.LatLng

/**
 * Mapa de fondo del tablero: MapLibre real con la ruta resaltada, otras rutas tenues y circulos
 * (corredores, incidentes). Sin mapa nativo (pruebas, previas) dibuja el esquema con la misma
 * geometria real.
 */
@Composable
fun MapaTablero(
    puntos: List<LatLng>,
    modifier: Modifier = Modifier,
    otros: List<List<LatLng>> = emptyList(),
    circulos: List<CirculoMapa> = emptyList(),
    centro: LatLng? = null,
    zoom: Double = 14.2,
    avance: Float = 1f,
    yo: LatLng? = null,
    alTocar: ((LatLng) -> Unit)? = null,
    alTocarCirculo: ((String) -> Unit)? = null,
    margenArriba: androidx.compose.ui.unit.Dp = 0.dp,
    margenAbajo: androidx.compose.ui.unit.Dp = 0.dp,
) {
    if (!Mapas.configurado() || LocalInspectionMode.current) {
        MapaEsquemaC2(
            puntos.map { it.latitude to it.longitude }, modifier,
            otros = otros.map { l -> l.map { it.latitude to it.longitude } },
            yo = yo?.let { it.latitude to it.longitude },
            avance = avance,
            marcadores = circulos.filter { it.id != null }.map { it.centro.latitude to it.centro.longitude },
            margenArriba = margenArriba, margenAbajo = margenAbajo,
        )
        return
    }
    val c = centro ?: puntos.takeIf { it.isNotEmpty() }?.let { p -> LatLng(p.sumOf { it.latitude } / p.size, p.sumOf { it.longitude } / p.size) } ?: Mapas.CENTRO_LOJA
    MapaLibre(
        modifier,
        trazos = otros.map { TrazoMapa(it, Color(0xFF8B8FB8), 3f, 0.7f) } +
            (if (puntos.size >= 2) listOf(TrazoMapa(puntos, GColors.C2Violeta, 14f, 0.3f), TrazoMapa(puntos, GColors.C2Violeta, 5f)) else emptyList()),
        circulos = circulos + listOfNotNull(yo?.let { CirculoMapa(it, 8f, GColors.C2Morado) }),
        centro = c, zoom = zoom, alTocar = alTocar, alTocarCirculo = alTocarCirculo,
    )
}
