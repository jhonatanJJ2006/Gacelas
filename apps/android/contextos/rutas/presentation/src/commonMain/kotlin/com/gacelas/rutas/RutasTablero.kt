package com.gacelas.rutas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.rutas.domain.modelo.CategoriaRuta
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.FiltrosRuta
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.tracking.Unidades
import com.gacelas.ui.components.BotonVidrio
import com.gacelas.ui.components.ChipVidrio
import com.gacelas.ui.components.LineaC2
import com.gacelas.ui.components.MiniTrazoC2
import com.gacelas.ui.components.RotuloWidget
import com.gacelas.ui.components.Tablero
import com.gacelas.ui.components.TextoAjustableC2
import com.gacelas.ui.components.vidrioC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/**
 * Rutas en la direccion A "Tablero": mapa a pantalla completa con todas las rutas (la elegida
 * resaltada), buscador, Filtros, chips rapidos, controles del mapa a la derecha y un carrusel de
 * tarjetas abajo. "Ver como lista" cambia a la lista clasica.
 */
@Composable
fun RutasTablero(
    lista: List<Ruta>,
    guardadas: Set<String>,
    consulta: String,
    onConsulta: (String) -> Unit,
    filtros: FiltrosRuta,
    onFiltros: (FiltrosRuta) -> Unit,
    cerca: Map<String, Double>?,
    onCerca: () -> Unit,
    m: Boolean,
    puedeDibujar: Boolean,
    onAtras: () -> Unit,
    onRuta: (String) -> Unit,
    onCorrerRuta: (String) -> Unit,
    onDibujar: () -> Unit,
    onAlternarGuardada: (id: String, guardada: Boolean) -> Unit,
    onVerLista: () -> Unit,
    /** Texto de seguridad real por ruta ("seguridad 92", "riesgo bajo"); vacio sin servidor. */
    seguridad: Map<String, String> = emptyMap(),
    /** Mapa: recibe la ruta elegida y el resto; dibuja sus propios controles (columna MAPA y REPORTAR). */
    mapa: @Composable BoxScope.(elegida: Ruta?, otras: List<Ruta>, alElegir: (String) -> Unit) -> Unit,
    /** Espacio del dock flotante (el carrusel queda encima). */
    espacioInferior: Dp = 0.dp,
    cargando: Boolean = false,
) {
    var soloOficiales by rememberSaveable { mutableStateOf(false) }
    var hojaFiltros by rememberSaveable { mutableStateOf(false) }
    val visibles = if (soloOficiales) lista.filter { it.oficial } else lista
    var elegidaId by rememberSaveable { mutableStateOf<String?>(null) }
    val elegida = visibles.firstOrNull { it.id == elegidaId } ?: visibles.firstOrNull()
    val estadoCarrusel = rememberLazyListState()
    LaunchedEffect(elegida?.id) {
        val i = visibles.indexOfFirst { it.id == elegida?.id }
        if (i >= 0 && i != estadoCarrusel.firstVisibleItemIndex) estadoCarrusel.animateScrollToItem(i)
    }
    Box(Modifier.fillMaxSize().background(Tablero.Fondo).testTag("rutas-tablero")) {
        mapa(elegida, visibles.filter { it.id != elegida?.id }) { id -> elegidaId = id }
        Box(Modifier.fillMaxWidth().height(170.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Tablero.Noche.copy(alpha = 0.9f), Color.Transparent))))
        // Cabecera: volver, buscador, Filtros y chips rapidos.
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BotonVidrio(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onAtras, tag = "rutas-volver")
                Row(
                    Modifier.weight(1f).height(48.dp).vidrioC2(RoundedCornerShape(16.dp)).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Search, null, tint = GColors.C2Violeta, modifier = Modifier.size(20.dp))
                    Box(Modifier.weight(1f)) {
                        if (consulta.isEmpty()) Text("Buscar en Loja", style = GType.bodyMd, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
                        BasicTextField(consulta, onConsulta, singleLine = true, textStyle = GType.bodyMd.copy(color = Color.White), cursorBrush = SolidColor(GColors.C2Violeta),
                            modifier = Modifier.fillMaxWidth().testTag("rutas-buscar"))
                    }
                    if (consulta.isNotEmpty()) Icon(Icons.Filled.Close, "Borrar búsqueda", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp).clickable { onConsulta("") })
                }
                val activos = listOfNotNull(filtros.dificultad, filtros.etiqueta, filtros.distancia.takeIf { it != com.gacelas.rutas.domain.modelo.FiltroDistancia.Todas }).size
                Row(
                    Modifier.height(48.dp).vidrioC2(RoundedCornerShape(16.dp)).clickable(role = Role.Button) { hojaFiltros = true }.testTag("boton-filtros").padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Tune, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(if (activos > 0) "Filtros $activos" else "Filtros", style = GType.labelLg.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).testTag("filtro-categoria"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipVidrio("Cerca de mí", onCerca, activo = cerca != null, tag = "chip-cerca")
                CategoriaRuta.entries.forEach { c ->
                    ChipVidrio(c.etiqueta, { onFiltros(filtros.copy(categoria = if (filtros.categoria == c) null else c)) }, activo = filtros.categoria == c, tag = "chip-${c.clave}")
                }
                ChipVidrio("Oficiales", { soloOficiales = !soloOficiales }, activo = soloOficiales, tag = "chip-oficiales")
                if (puedeDibujar) ChipVidrio("Dibujar ruta", onDibujar, icono = Icons.Filled.Draw, tag = "chip-dibujar")
            }
        }
        // Abajo: Ver como lista, encabezado y carrusel de tarjetas.
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = espacioInferior + 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.padding(start = 14.dp).heightIn(min = 40.dp).vidrioC2(CircleShape).clickable(role = Role.Button, onClick = onVerLista).testTag("rutas-ver-lista").padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.List, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Ver como lista · ${visibles.size}", style = GType.labelLg.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
            }
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                RotuloWidget(if (cargando) "Cargando rutas" else "${visibles.size} ${if (visibles.size == 1) "ruta" else "rutas"} en el mapa", Modifier.weight(1f))
                Text(if (cerca != null) "Orden: más cerca" else "Orden: recomendadas", style = GType.labelMd, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
            }
            if (visibles.isEmpty() && !cargando) Text(
                if (consulta.isNotBlank()) "No hay rutas que coincidan con \"$consulta\"." else "No hay rutas con estos filtros.",
                style = GType.bodyMd, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(horizontal = 16.dp).testTag("rutas-vacio"),
            )
            LazyRow(state = estadoCarrusel, contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("rutas-carrusel")) {
                itemsIndexed(visibles, key = { _, r -> r.id }) { _, r ->
                    TarjetaCarrusel(
                        r, elegida?.id == r.id, r.id in guardadas, m, seguridad[r.id.removePrefix("ruta-")] ?: seguridad[r.id], cerca?.get(r.id),
                        onTocar = { if (elegida?.id == r.id) onRuta(r.id) else elegidaId = r.id },
                        onCorrer = { onCorrerRuta(r.id) },
                        onGuardar = { onAlternarGuardada(r.id, r.id in guardadas) },
                    )
                }
            }
        }
        if (hojaFiltros) HojaFiltrosRutas(visibles.size, filtros, onFiltros) { hojaFiltros = false }
    }
}

@Composable
private fun TarjetaCarrusel(
    r: Ruta, elegida: Boolean, guardada: Boolean, m: Boolean, seguridad: String?, distanciaCerca: Double?,
    onTocar: () -> Unit, onCorrer: () -> Unit, onGuardar: () -> Unit,
) {
    val forma = RoundedCornerShape(24.dp)
    Column(
        Modifier.width(300.dp).clip(forma).background(Tablero.FondoWidget).background(Color(0xCC1A0B2E))
            .border(if (elegida) 1.5.dp else 1.dp, if (elegida) GColors.C2Violeta else Tablero.WidgetBorde, forma)
            .clickable(onClickLabel = if (elegida) "Abrir ruta" else "Elegir ruta", onClick = onTocar).testTag("tarjeta-ruta-${r.id}").padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniTrazoC2(r.puntos, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)), cuadricula = false, grosor = 2.5.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val (texto, color) = when (r.dificultad) {
                        Dificultad.Facil -> "FÁCIL" to GColors.C2Menta
                        Dificultad.Moderado -> "MEDIA" to Color(0xFFFBBF24)
                        Dificultad.Dificil -> "DIFÍCIL" to Color(0xFFF472B6)
                    }
                    Text(texto, style = GType.labelMd.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = color,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.16f)).padding(horizontal = 6.dp, vertical = 2.dp))
                    if (seguridad != null) Row(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Shield, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(11.dp))
                        Text(" ${seguridad.removePrefix("seguridad ")}", style = GType.labelMd.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
                    }
                    if (r.oficial) Text("OFICIAL", style = GType.labelMd.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = GColors.C2Violeta)
                }
                TextoAjustableC2(r.nombre, GType.headlineSm.copy(fontSize = 20.sp, lineHeight = 23.sp), Color.White, minimo = 0.7f)
                Text(
                    listOfNotNull(
                        "${Unidades.texto(r.distanciaKm, m, 1).replace('.', ',')} ${Unidades.etiquetaDistancia(m)}",
                        "+${r.elevacionM} m",
                        distanciaCerca?.let { "a ${Unidades.texto(it, m, 1).replace('.', ',')} ${Unidades.etiquetaDistancia(m)}" },
                    ).joinToString(" · "),
                    style = GType.bodySm.copy(fontSize = 11.5.sp), color = Color.White.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (r.perfilElevacion.size > 2) LineaC2(r.perfilElevacion.map { it.toFloat() }, Modifier.height(34.dp).semantics { contentDescription = "Perfil de elevación" }, color = GColors.C2Violeta)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).clickable(role = Role.Button, onClickLabel = "Correr esta ruta", onClick = onCorrer)
                    .testTag("correr-ruta-${r.id}"),
                contentAlignment = Alignment.Center,
            ) { Text("CORRER", style = GType.labelLg.copy(fontWeight = FontWeight.Black), color = Color(0xFF3B0764)) }
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.08f))
                    .clickable(role = Role.Button, onClickLabel = if (guardada) "Quitar de guardadas" else "Guardar ruta", onClick = onGuardar).testTag("guardar-ruta-${r.id}"),
                contentAlignment = Alignment.Center,
            ) { Icon(if (guardada) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (guardada) "Quitar de guardadas" else "Guardar ruta", tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
    }
}
