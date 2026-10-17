package com.gacelas.rutas.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gacelas.mapa.CirculoComun
import com.gacelas.mapa.Coord
import com.gacelas.mapa.TrazoComun
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.ETIQUETAS_RUTA
import com.gacelas.rutas.presentation.DibujarRutaViewModel
import com.gacelas.shared.fmt
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/**
 * Crear una ruta tocando el mapa punto a punto (compartida Android/iOS). [mapa] dibuja el mapa de la
 * plataforma (MapLibre, zoom 15) con los trazos y circulos y avisa cada toque; null si no hay mapa.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DibujarRutaContenido(
    vm: DibujarRutaViewModel,
    onAtras: () -> Unit,
    onCreada: (String) -> Unit,
    onAviso: (String) -> Unit,
    mapa: (@Composable (trazos: List<TrazoComun>, circulos: List<CirculoComun>, alTocar: (Coord) -> Unit) -> Unit)?,
) {
    val e by vm.estado.collectAsState()
    val puntos = e.puntos.map { Coord(it.first, it.second) }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras("Dibujar ruta", onAtras)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(16.dp)).background(GColors.SurfaceContainer)) {
                if (mapa != null) {
                    mapa(
                        if (puntos.size >= 2) listOf(TrazoComun(puntos, 0xFFF59E0B, 4.5f)) else emptyList(),
                        puntos.mapIndexed { i, p -> CirculoComun(p, if (i == 0) 7f else 4.5f, if (i == 0) 0xFF10B981 else 0xFFF59E0B, bordeDp = 1.5f) },
                    ) { vm.agregar(it.lat, it.lng) }
                } else Text("El mapa no está disponible en este dispositivo.", color = GColors.TextSecondary, modifier = Modifier.align(Alignment.Center))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%.2f km · %d puntos".fmt(e.km, puntos.size), style = GType.bodyMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::deshacer, enabled = puntos.isNotEmpty()) { Text("Deshacer", color = GColors.Primary400) }
                TextButton(onClick = vm::borrar, enabled = puntos.isNotEmpty()) { Text("Borrar", color = GColors.Primary400) }
            }
            Text("Toca el mapa para ir agregando puntos en orden.", style = GType.bodySm, color = GColors.TextSecondary)
            CampoTexto("Nombre", e.nombre, vm::nombre, "Nombre de la ruta")
            CampoTexto("Descripción", e.descripcion, vm::descripcion, "Descripción (opcional)", lineas = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Dificultad.entries.forEach { d -> Pastilla(d.etiqueta, d == e.dificultad, { vm.dificultad(d) }) } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ETIQUETAS_RUTA.forEach { (k, v) -> Pastilla("#$v", k in e.etiquetas, { vm.alternarEtiqueta(k) }) }
            }
            BotonPrimario(if (e.guardando) "Publicando..." else "Publicar ruta", { vm.publicar(onAviso, onCreada) }, enabled = !e.guardando, icono = null)
        }
    }
}
