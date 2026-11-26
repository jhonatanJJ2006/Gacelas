package com.gacelas.rutas

import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
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
import com.gacelas.rutas.domain.modelo.CategoriaRuta
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.rutas.domain.modelo.FiltrosRuta
import com.gacelas.rutas.domain.modelo.PestanaRutas
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
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    /** Abrir ya en la vista de mapa (capturas de paridad). */
    verMapaInicial: Boolean = false,
    /** Rutas del servidor aun sin llegar: esqueleto en lugar de la lista. */
    cargando: Boolean = false,
    /** Tablero: vuelve al mapa a pantalla completa en lugar del mapa embebido. */
    onVerMapa: (() -> Unit)? = null,
    /** Foto remota (Coil en Android); sin ella las tarjetas muestran el trazo. */
    imagen: (@Composable (url: String, modifier: Modifier) -> Unit)? = null,
) {
    var verMapa by rememberSaveable { mutableStateOf(verMapaInicial) }
    var hojaFiltros by rememberSaveable { mutableStateOf(false) }
    // Paginacion: 20 rutas y otras 20 al acercarse al final (todas ya estan en memoria).
    var limite by rememberSaveable(pestana, lista.size) { mutableStateOf(PAGINA_RUTAS) }
    val visibles = if (cargando) emptyList() else lista.take(limite)
    val estado = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(estado, lista.size) {
        androidx.compose.runtime.snapshotFlow { estado.layoutInfo.visibleItemsInfo.lastOrNull()?.index to estado.layoutInfo.totalItemsCount }
            .collect { (ultimo, total) -> if (ultimo != null && ultimo >= total - 3 && limite < lista.size) limite += PAGINA_RUTAS }
    }
    // Rutas no tiene barra superior: el titulo reserva la barra de estado. Abajo deja el alto del dock
    // flotante para que la ultima tarjeta no quede tapada por la barra de secciones.
    val dock = com.gacelas.ui.components.LocalEspacioDock.current
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize().background(com.gacelas.ui.components.Tablero.Fondo).statusBarsPadding().testTag("lista-rutas"),
        state = estado,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = dock + 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "cabecera") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rutas de Loja", style = GType.headlineLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                    if (onVerMapa == null) IconButton(onClick = { verMapa = !verMapa }, modifier = Modifier.testTag("rutas-ver-mapa")) { Icon(if (verMapa) Icons.AutoMirrored.Filled.List else Icons.Filled.Map, if (verMapa) "Ver lista" else "Ver mapa", tint = GColors.TextPrimary) }
                    if (puedeDibujar) IconButton(onClick = onDibujar) { Icon(Icons.Filled.Draw, "Dibujar ruta", tint = GColors.TextPrimary) }
                }
                if (onVerMapa != null) com.gacelas.ui.components.SelectorSegmentadoC2(listOf("Lista", "Mapa"), 0, { if (it == 1) onVerMapa() }, tag = "rutas-vista")
                if (verMapa) mapaRutas(Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(16.dp)))
                // Pestanas segmentadas, buscador con "Filtros" (hoja inferior) y una sola fila de chips rapidos.
                val pestanas = PestanaRutas.entries
                com.gacelas.ui.components.SelectorSegmentadoC2(
                    pestanas.map { p -> if (p == PestanaRutas.Guardadas) "${p.etiqueta} (${guardadas.size})" else p.etiqueta },
                    pestanas.indexOf(pestana), { onPestana(pestanas[it]) }, tag = "pestanas-rutas",
                )
                val activos = listOfNotNull(filtros.dificultad, filtros.etiqueta, filtros.distancia.takeIf { it != com.gacelas.rutas.domain.modelo.FiltroDistancia.Todas }).size
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        CampoTexto("Buscar rutas", consulta, onConsulta, "Buscar rutas", mostrarEtiqueta = false, trailing = {
                            IconButton(onClick = onBuscar) { Icon(Icons.Filled.Search, "Abrir buscador en el mapa", tint = GColors.TextSecondary) }
                        })
                    }
                    // Mismos chips que el modo Mapa (RutasTablero) para que los dos modos se lean igual.
                    com.gacelas.ui.components.ChipVidrio(if (activos > 0) "Filtros ($activos)" else "Filtros", { hojaFiltros = true }, icono = Icons.Filled.Tune, activo = activos > 0, tag = "boton-filtros")
                }
                Row(Modifier.horizontalScroll(rememberScrollState()).testTag("filtro-categoria"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.gacelas.ui.components.ChipVidrio("Cerca de mí", onCerca, activo = cerca != null)
                    com.gacelas.ui.components.ChipVidrio("Todas", { onFiltros(filtros.copy(categoria = null)) }, activo = filtros.categoria == null)
                    CategoriaRuta.entries.forEach { c -> com.gacelas.ui.components.ChipVidrio(c.etiqueta, { onFiltros(filtros.copy(categoria = if (filtros.categoria == c) null else c)) }, activo = filtros.categoria == c) }
                }
                if (cargando) com.gacelas.ui.components.EsqueletoTarjetas(4, etiqueta = "Cargando rutas")
                else if (lista.isEmpty()) VacioRutas(pestana, puedeDibujar, onDibujar, { onPestana(PestanaRutas.Explorar) }) { onConsulta(""); onFiltros(FiltrosRuta()) }
                // El titulo de la pantalla ya dice "Rutas de Loja": aqui solo el recuento.
                else Text(
                    "${lista.size} ${if (lista.size == 1) "ruta" else "rutas"}", style = GType.bodySm, color = GColors.TextSecondary,
                    modifier = Modifier.testTag("encabezado-lista-rutas"),
                )
            }
        }
        items(visibles, key = { it.id }) { ruta ->
            TarjetaRutaLista(ruta, ruta.id in guardadas, m, cerca?.get(ruta.id), imagen, { onRuta(ruta.id) }) { onAlternarGuardada(ruta.id, ruta.id in guardadas) }
        }
        if (visibles.size < lista.size && !cargando) item(key = "mas") {
            com.gacelas.ui.components.EsqueletoTarjetas(1, etiqueta = "Cargando más rutas")
        }
    }
    if (hojaFiltros) HojaFiltrosRutas(lista.size, filtros, onFiltros) { hojaFiltros = false }
}

private const val PAGINA_RUTAS = 20

/** Estados vacios de cada pestana: crear la primera ruta, explorar para guardar o limpiar filtros. */
@Composable
private fun VacioRutas(pestana: PestanaRutas, puedeDibujar: Boolean, onDibujar: () -> Unit, onExplorar: () -> Unit, onLimpiar: () -> Unit) = when (pestana) {
    PestanaRutas.MisRutas -> com.gacelas.ui.components.EstadoVacioC2(
        Icons.Filled.Route, "Aún no creaste rutas",
        if (puedeDibujar) "Dibuja tu recorrido favorito en el mapa y compártelo con otros corredores de Loja."
        else "Inicia sesión para dibujar tus recorridos y compartirlos con otros corredores de Loja.",
        accion = "Crear ruta".takeIf { puedeDibujar }, onAccion = onDibujar, iconoAccion = Icons.Filled.Draw,
        secundaria = "Explorar rutas", onSecundaria = onExplorar, tag = "vacio-mis-rutas",
    )
    PestanaRutas.Guardadas -> com.gacelas.ui.components.EstadoVacioC2(
        Icons.Filled.BookmarkBorder, "Sin rutas guardadas",
        "Toca el marcador de una ruta para tenerla a mano y correrla cuando quieras.",
        accion = "Explorar rutas", onAccion = onExplorar, iconoAccion = Icons.Filled.Search, tag = "vacio-guardadas",
    )
    PestanaRutas.Explorar -> com.gacelas.ui.components.EstadoVacioC2(
        Icons.Filled.SearchOff, "Sin resultados",
        "No hay rutas que coincidan con la búsqueda o los filtros.",
        secundaria = "Limpiar filtros", onSecundaria = onLimpiar, tag = "vacio-explorar",
    )
}

/** Tarjeta de la lista: galeria de fotos arriba (si la ruta tiene) y datos debajo; sin fotos, el trazo. */
@Composable
private fun TarjetaRutaLista(
    ruta: Ruta, guardada: Boolean, m: Boolean, distanciaCerca: Double?,
    imagen: (@Composable (String, Modifier) -> Unit)?, onAbrir: () -> Unit, onGuardar: () -> Unit,
) {
    val conFotos = imagen != null && ruta.fotos.isNotEmpty()
    Tarjeta(padding = 12.dp, onClick = onAbrir, modifier = Modifier.testTag("tarjeta-lista-${ruta.id}")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (conFotos) com.gacelas.ui.components.GaleriaC2(ruta.fotos.size, alto = 168.dp, onTocar = onAbrir, tag = "galeria-${ruta.id}") { i ->
                imagen!!(ruta.fotos[i], Modifier.fillMaxSize())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Sin fotos: mini mapa con el trazo real de la ruta (Canvas, sin cargar el mapa en cada tarjeta).
                if (!conFotos) {
                    if (ruta.puntos.size >= 2) com.gacelas.ui.components.MiniTrazoC2(ruta.puntos, Modifier.size(92.dp).clip(RoundedCornerShape(6.dp)), grosor = 2.5.dp)
                    else Box(
                        Modifier.size(92.dp).clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(listOf(GColors.C2S2, GColors.C2Bg))),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Route, null, tint = GColors.C2Violeta, modifier = Modifier.size(36.dp)) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(ruta.nombre, style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 2)
                        IconButton(onClick = onGuardar, modifier = Modifier.size(32.dp)) {
                            Icon(if (guardada) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (guardada) "Quitar de guardadas" else "Guardar ruta", tint = GColors.C2Violeta)
                        }
                    }
                    if (ruta.oficial) InsigniaOficial()
                    Text(ruta.descripcion, style = GType.bodySm, color = GColors.C2Muted, maxLines = 2)
                    val extra = listOfNotNull(
                        ruta.valoracion?.let { "★ ${decimalLocal(it)} (${ruta.resenas})" },
                        ruta.meGusta.takeIf { it > 0 }?.let { "♥ $it" },
                        distanciaCerca?.let { "a ${Unidades.texto(it, m, 1).replace('.', ',')} ${Unidades.etiquetaDistancia(m)}" },
                        ruta.autorNombre?.let { "por $it" },
                    )
                    if (extra.isNotEmpty()) Text(extra.joinToString("  ·  "), style = GType.labelMd, color = GColors.C2Muted)
                    Row(Modifier.fillMaxWidth().testTag("metricas-ruta"), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${Unidades.texto(ruta.distanciaKm, m, 1).replace('.', ',')} ${Unidades.etiquetaDistancia(m)}", style = GType.labelLg, color = GColors.C2Violeta)
                        Text("+${ruta.elevacionM} m", style = GType.labelLg, color = GColors.C2Cielo)
                        EtiquetaDificultad(ruta.dificultad)
                        ruta.categoria?.let { Text(it.etiqueta, style = GType.labelMd, color = GColors.C2Muted) }
                    }
                    if (ruta.perfilElevacion.size > 2) com.gacelas.ui.components.LineaC2(ruta.perfilElevacion.map { it.toFloat() }, Modifier.height(24.dp).testTag("perfil-ruta"))
                }
            }
        }
    }
}

/** Color unico por dificultad para etiquetas, tarjetas y trazos del mapa: facil verde, media ambar, dificil rojo. */
fun colorDificultad(d: Dificultad): Color = when (d) {
    Dificultad.Facil -> GColors.Secondary500
    Dificultad.Moderado -> GColors.Alerta
    Dificultad.Dificil -> GColors.Danger
}

@Composable
fun EtiquetaDificultad(d: Dificultad) {
    val color = colorDificultad(d)
    Text(d.etiqueta, style = GType.labelMd, color = color, modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp))
}

/** Insignia C2 de las rutas del catalogo oficial de Loja. */
@Composable
fun InsigniaOficial(modifier: Modifier = Modifier) {
    Row(
        modifier.testTag("insignia-oficial").clip(RoundedCornerShape(50)).border(1.dp, GColors.C2Violeta.copy(alpha = 0.6f), RoundedCornerShape(50))
            .background(GColors.C2Violeta.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Filled.Verified, null, tint = GColors.C2Violeta, modifier = Modifier.size(14.dp))
        Text("RUTA OFICIAL", style = GType.labelMd, color = GColors.C2Violeta)
    }
}


/** Hoja de filtros de rutas (dificultad, distancia y etiquetas), pegada al borde inferior. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun HojaFiltrosRutas(total: Int, filtros: FiltrosRuta, onFiltros: (FiltrosRuta) -> Unit, onCerrar: () -> Unit) {
    com.gacelas.ui.components.HojaC2(onCerrar) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding().testTag("hoja-filtros"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Filtros", style = GType.headlineSm, color = GColors.TextPrimary)
            com.gacelas.ui.components.EtiquetaC2("Dificultad", color = GColors.C2Muted)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pastilla("Toda dificultad", filtros.dificultad == null, { onFiltros(filtros.copy(dificultad = null)) })
                com.gacelas.rutas.domain.modelo.Dificultad.entries.forEach { d -> Pastilla(d.etiqueta, filtros.dificultad == d, { onFiltros(filtros.copy(dificultad = d)) }) }
            }
            com.gacelas.ui.components.EtiquetaC2("Distancia", color = GColors.C2Muted)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.gacelas.rutas.domain.modelo.FiltroDistancia.entries.forEach { d -> Pastilla(d.etiqueta, filtros.distancia == d, { onFiltros(filtros.copy(distancia = d)) }) }
            }
            com.gacelas.ui.components.EtiquetaC2("Etiquetas", color = GColors.C2Muted)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.gacelas.rutas.domain.modelo.ETIQUETAS_RUTA.forEach { (k, v) -> Pastilla("#$v", filtros.etiqueta == k, { onFiltros(filtros.copy(etiqueta = if (filtros.etiqueta == k) null else k)) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.gacelas.ui.components.BotonSecundario("Limpiar", { onFiltros(FiltrosRuta(categoria = filtros.categoria)) }, Modifier.weight(1f))
                com.gacelas.ui.components.BotonPrimario("Ver $total rutas", onCerrar, Modifier.weight(1f))
            }
        }
    }
}
