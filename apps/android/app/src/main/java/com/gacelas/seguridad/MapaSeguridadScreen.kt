package com.gacelas.seguridad

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.seguridad.domain.IncidentePublico
import com.gacelas.seguridad.ui.colorIncidente
import com.gacelas.seguridad.ui.HojaReportarIncidente
import com.gacelas.ui.components.BotonVidrio
import com.gacelas.ui.components.BotonVolverMapa
import com.gacelas.ui.components.CirculoMapa
import com.gacelas.ui.components.MapaLibre
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/**
 * "Seguridad en tu zona" en el mapa: reportes de corredores (verificados en rojo, sin verificar
 * en ambar), tu posicion y un resumen de la zona con el ECU 911. Mapa en 3D (edificios y relieve).
 */
@Composable
fun MapaSeguridadScreen(onAtras: () -> Unit) {
    val c = contenedor()
    val context = LocalContext.current
    val zona by produceState<com.gacelas.seguridad.domain.ResumenZona?>(null, c.incidentes) { value = c.incidentes?.resumenZona() }
    val incidentes by produceState(emptyList<IncidentePublico>(), c.incidentes) { value = c.incidentes?.recientes().orEmpty() }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var yo by remember { mutableStateOf(com.gacelas.rutas.ultimaUbicacion(context)?.let { LatLng(it.first, it.second) }) }
    var reportar by remember { mutableStateOf(false) }
    val centrar = com.gacelas.ui.rememberPedirUbicacion { u ->
        yo = LatLng(u.first, u.second)
        mapa?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(LatLng(u.first, u.second)).zoom(15.0).tilt(45.0).build()), 900)
    }
    // Al entrar se ubica al corredor (pide permiso o el GPS con el dialogo nativo si hace falta).
    LaunchedEffect(Unit) { centrar() }
    // Con incidentes a menos de 3 km, la primera vista los muestra TODOS junto a ti, en la franja libre entre
    // los botones y el resumen (vista cenital: el encuadre se calcula sin inclinacion). "Centrar" vuelve a ti.
    val dens = androidx.compose.ui.platform.LocalDensity.current.density
    var resumenPx by remember { mutableStateOf(0) }
    val cercanos = remember(incidentes, yo) { yo?.let { y -> incidentes.map { LatLng(it.lat, it.lng) }.filter { com.gacelas.ui.components.MapaMat.distanciaM(y, it) <= 3000.0 } }.orEmpty() }
    com.gacelas.ui.components.EncuadrarRutaEfecto(
        mapa, listOfNotNull(yo) + cercanos,
        com.gacelas.mapa.EncuadreMapa.paddingDp(dens, arriba = 110f, abajo = if (resumenPx > 0) resumenPx / dens + 16f else 260f, izquierda = 40f, derecha = 40f),
        clave = cercanos.size, activo = yo != null && cercanos.isNotEmpty(), zoomUnPunto = 15.0,
    )
    Box(Modifier.fillMaxSize().background(com.gacelas.ui.components.Tablero.Fondo).testTag("mapa-seguridad")) {
        if (Mapas.configurado()) MapaLibre(
            Modifier.fillMaxSize(),
            circulos = incidentes.flatMap { i ->
                val color = colorIncidente(i)
                listOf(CirculoMapa(LatLng(i.lat, i.lng), 22f, color.copy(alpha = 0.18f), Color.Transparent, 0f), CirculoMapa(LatLng(i.lat, i.lng), 8f, color, GColors.C2Bg, 2f, id = i.id))
            } + yo?.let { listOf(CirculoMapa(it, 18f, GColors.Info.copy(alpha = 0.25f), Color.Transparent, 0f), CirculoMapa(it, 8f, GColors.Info, GColors.OnPrimary, 3f)) }.orEmpty(),
            centro = yo ?: Mapas.CENTRO_LOJA, zoom = 14.5,
            alListo = { m -> mapa = m; m.animateCamera(CameraUpdateFactory.tiltTo(45.0)) },
        )
        else com.gacelas.ui.components.MapaEsquemaC2(emptyList(), Modifier.fillMaxSize(), marcadores = incidentes.map { it.lat to it.lng })
        Box(Modifier.fillMaxWidth().padding(14.dp).statusBarsPadding()) {
            BotonVolverMapa(onAtras)
            BotonVidrio(Icons.Filled.MyLocation, "Centrar en tu posición", centrar, Modifier.align(Alignment.TopEnd), tag = "seguridad-centrar")
        }
        // Resumen fijo abajo, sobre el mapa.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { resumenPx = it.height }.navigationBarsPadding().padding(12.dp)
                .clip(RoundedCornerShape(24.dp)).background(GColors.C2Bg.copy(alpha = 0.94f)).padding(16.dp).testTag("seguridad-resumen"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Seguridad en tu zona", style = GType.headlineSm, color = GColors.TextPrimary)
            val z = zona
            Text(
                when {
                    z == null -> "Sin datos de la zona por ahora."
                    z.reportes7d == 0 -> "Sin reportes de corredores en los últimos 7 días."
                    else -> "${z.reportes7d} ${if (z.reportes7d == 1) "reporte" else "reportes"} de corredores en los últimos 7 días."
                }, style = GType.bodyMd, color = GColors.TextPrimary,
            )
            z?.ecu?.take(3)?.takeIf { it.isNotEmpty() }?.let { top ->
                Text("ECU 911: " + top.joinToString(" · ") { "${it.first.lowercase()} (${it.second})" }, style = GType.bodySm, color = GColors.C2Muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Leyenda(GColors.C2Peligro, "Verificado")
                Leyenda(Color(0xFFF5B54A), "Reportado")
                Leyenda(GColors.Info, "Tú")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.gacelas.ui.components.BotonSecundario("Llamar 911", { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))) }, Modifier.weight(1f), color = GColors.C2Peligro)
                com.gacelas.ui.components.BotonPrimario("Reportar", { reportar = true }, Modifier.weight(1f), icono = null)
            }
        }
    }
    if (reportar) HojaReportarIncidente(yo?.let { it.latitude to it.longitude }, onCerrar = { reportar = false })
}

@Composable
private fun Leyenda(color: Color, texto: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color)); Text("  $texto", style = GType.bodySm, color = GColors.C2Muted)
}
