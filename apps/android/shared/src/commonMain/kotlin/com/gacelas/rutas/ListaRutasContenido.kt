package com.gacelas.rutas

import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Draw
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gacelas.data.mock.Dificultad
import com.gacelas.data.mock.Ruta
import com.gacelas.data.rutas.FiltrosRuta
import com.gacelas.data.rutas.PestanaRutas
import com.gacelas.tracking.Unidades
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Numero con un decimal en el idioma del telefono (Android: "%.1f".format). */
expect fun decimalLocal(x: Double): String

/** Lista de rutas (compartida Android/iOS). El mapa de rutas agrupadas llega como slot. */
@Composable
fun ListaRutasContenido(
    lista: List<Ruta>,
    pestana: PestanaRutas,
    onPestana: (PestanaRutas) -> Unit,
    guardadas: Set<String>,
    consulta: String,
    onConsulta: (String) -> Unit,
    filtros: FiltrosRuta,
    onFiltros: (FiltrosRuta) -> Unit,
    cerca: Map<String, Double>?,
    onCerca: () -> Unit,
    m: Boolean,
    puedeDibujar: Boolean,
    onBuscar: () -> Unit,
    onRuta: (String) -> Unit,
    onDibujar: () -> Unit,
    onAlternarGuardada: (id: String, guardada: Boolean) -> Unit,
    mapaRutas: @Composable (Modifier) -> Unit,
) {
    Pantalla {
        var verMapa by rememberSaveable { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rutas de Loja", style = GType.headlineLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = { verMapa = !verMapa }) { Icon(if (verMapa) Icons.AutoMirrored.Filled.List else Icons.Filled.Map, if (verMapa) "Ver lista" else "Ver mapa", tint = GColors.TextPrimary) }
            if (puedeDibujar) IconButton(onClick = onDibujar) { Icon(Icons.Filled.Draw, "Dibujar ruta", tint = GColors.TextPrimary) }
        }
        if (verMapa) {
            mapaRutas(Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(16.dp)))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PestanaRutas.entries.forEach { p -> Pastilla(if (p == PestanaRutas.Guardadas) "${p.etiqueta} (${guardadas.size})" else p.etiqueta, p == pestana, { onPestana(p) }) }
        }
        CampoTexto("Buscar", consulta, onConsulta, "Buscar rutas...", trailing = {
            IconButton(onClick = onBuscar) { Icon(Icons.Filled.Search, "Abrir buscador en el mapa", tint = GColors.TextSecondary) }
        })
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pastilla("Cerca de mí", cerca != null, onCerca)
            Pastilla("Toda dificultad", filtros.dificultad == null, { onFiltros(filtros.copy(dificultad = null)) })
            com.gacelas.data.mock.Dificultad.entries.forEach { d -> Pastilla(d.etiqueta, filtros.dificultad == d, { onFiltros(filtros.copy(dificultad = d)) }) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.gacelas.data.rutas.FiltroDistancia.entries.forEach { d -> Pastilla(d.etiqueta, filtros.distancia == d, { onFiltros(filtros.copy(distancia = d)) }) }
            com.gacelas.data.rutas.ETIQUETAS_RUTA.forEach { (k, v) -> Pastilla("#$v", filtros.etiqueta == k, { onFiltros(filtros.copy(etiqueta = if (filtros.etiqueta == k) null else k)) }) }
        }
        if (lista.isEmpty()) {
            Tarjeta {
                Text(
                    when (pestana) {
                        PestanaRutas.Guardadas -> "Aún no guardas rutas. Toca el marcador de una ruta para guardarla."
                        PestanaRutas.MisRutas -> "Todavía no creaste rutas."
                        PestanaRutas.Explorar -> "No hay rutas que coincidan con la búsqueda."
                    },
                    style = GType.bodyMd, color = GColors.TextSecondary,
                )
            }
        }
        lista.forEach { ruta ->
            val guardada = ruta.id in guardadas
            Tarjeta(padding = 0.dp, onClick = { onRuta(ruta.id) }) {
                Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.linearGradient(listOf(GColors.MapParks, GColors.SurfaceLow))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Route, null, tint = GColors.Primary500.copy(alpha = 0.7f), modifier = Modifier.size(56.dp))
                    IconButton(
                        onClick = { onAlternarGuardada(ruta.id, guardada) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.6f)),
                    ) {
                        Icon(if (guardada) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (guardada) "Quitar de guardadas" else "Guardar ruta", tint = GColors.Primary500)
                    }
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ruta.nombre, style = GType.titleMd, color = GColors.TextPrimary)
                    Text(ruta.descripcion, style = GType.bodySm, color = GColors.TextSecondary)
                    val extra = listOfNotNull(
                        ruta.valoracion?.let { "★ ${decimalLocal(it)} (${ruta.resenas})" },
                        ruta.meGusta.takeIf { it > 0 }?.let { "♥ $it" },
                        cerca?.get(ruta.id)?.let { "a ${Unidades.texto(it, m, 1)} ${Unidades.etiquetaDistancia(m)}" },
                        ruta.autorNombre?.let { "por $it" },
                    )
                    if (extra.isNotEmpty()) Text(extra.joinToString("  ·  "), style = GType.labelMd, color = GColors.TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Straighten, null, tint = GColors.TextSecondary, modifier = Modifier.size(16.dp))
                        Text(" ${Unidades.texto(ruta.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)}   ", style = GType.bodyMd, color = GColors.TextSecondary)
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = GColors.TextSecondary, modifier = Modifier.size(16.dp))
                        Text(" ${ruta.elevacionM}m", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                        EtiquetaDificultad(ruta.dificultad)
                    }
                }
            }
        }
    }
}

@Composable
fun EtiquetaDificultad(d: Dificultad) {
    val color = when (d) {
        Dificultad.Facil -> GColors.Secondary500
        Dificultad.Moderado -> GColors.Primary500
        Dificultad.Dificil -> GColors.Danger
    }
    Text(d.etiqueta, style = GType.labelMd, color = color, modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp))
}

