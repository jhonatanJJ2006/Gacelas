package com.gacelas.comunidad.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.FiltroFeed
import com.gacelas.comunidad.domain.modelo.PestanaEventos
import com.gacelas.comunidad.domain.modelo.Publicacion
import com.gacelas.comunidad.domain.reglas.yaPaso
import com.gacelas.comunidad.presentation.DetalleEventoViewModel
import com.gacelas.comunidad.presentation.EventosViewModel
import com.gacelas.comunidad.presentation.FeedViewModel
import com.gacelas.correr.TrazadoRuta
import com.gacelas.shared.nuevoUuid
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.components.AvatarCirculo
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

// Pantallas de comunidad compartidas por Android e iOS (antes en apps/android/app/.../comunidad).

@Composable
fun FeedContenido(
    vm: FeedViewModel,
    onEventos: () -> Unit,
    onEnVivo: () -> Unit,
    onCompartir: (String) -> Unit,
    onAviso: (String) -> Unit,
    onClubes: (() -> Unit)? = null,
) {
    val ajustes by vm.ajustes.collectAsState()
    val m = ajustes.unidadesMetricas
    val filtro by vm.filtro.collectAsState()
    val siguiendo by vm.siguiendo.collectAsState()
    var menuFiltro by remember { mutableStateOf(false) }
    var comentando by remember { mutableStateOf<Publicacion?>(null) }
    val lista by vm.publicaciones.collectAsState()
    Pantalla {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GACELAS LOJA", style = GType.labelMd, color = GColors.Primary500)
                Text("Comunidad", style = GType.headlineLg, color = GColors.TextPrimary)
            }
            IconButton(onClick = { vm.refrescar() }) { Icon(Icons.Filled.Refresh, "Actualizar", tint = GColors.TextSecondary) }
            IconButton(onClick = onEnVivo) { Icon(Icons.Filled.Sensors, "Corredores en vivo", tint = GColors.Danger) }
            Box {
                IconButton(onClick = { menuFiltro = true }) { Icon(Icons.Filled.FilterList, "Filtrar", tint = GColors.TextSecondary) }
                DropdownMenu(expanded = menuFiltro, onDismissRequest = { menuFiltro = false }) {
                    FiltroFeed.entries.forEach { f ->
                        DropdownMenuItem(text = { Text(f.etiqueta) }, onClick = { vm.filtro(f); menuFiltro = false })
                    }
                }
            }
        }
        // Pestanas segmentadas como en Stitch: Para ti, Siguiendo, Clubes, Eventos.
        Row(
            Modifier.fillMaxWidth().clip(CircleShape).background(GColors.SurfaceContainer).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            @Composable
            fun Segmento(texto: String, activo: Boolean, onClick: () -> Unit) {
                Box(
                    Modifier.weight(1f).height(40.dp).clip(CircleShape)
                        .background(if (activo) GColors.BgPrimary else GColors.SurfaceContainer)
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) { Text(texto, style = GType.labelLg, color = if (activo) GColors.Primary500 else GColors.TextSecondary) }
            }
            Segmento("Para ti", filtro == FiltroFeed.Todos) { vm.filtro(FiltroFeed.Todos) }
            Segmento("Siguiendo", filtro == FiltroFeed.Siguiendo) { vm.filtro(FiltroFeed.Siguiendo) }
            if (onClubes != null) Segmento("Clubes", false, onClubes)
            Segmento("Eventos", false, onEventos)
        }
        if (filtro == FiltroFeed.Comunidad || filtro == FiltroFeed.Mias) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pastilla(filtro.etiqueta, true, { vm.filtro(FiltroFeed.Todos) }) }
        }
        if (lista.isEmpty()) Tarjeta {
            Text(
                when (filtro) {
                    FiltroFeed.Siguiendo -> "Las carreras de quienes sigues aparecerán aquí."
                    FiltroFeed.Mias -> "Aún no tienes carreras públicas."
                    else -> "No hay publicaciones todavía."
                },
                style = GType.bodyMd, color = GColors.TextSecondary,
            )
        }
        lista.forEach { p ->
            val gusta = if (p.remota) p.meGusta else p.id in ajustes.likes
            val likes = if (p.remota) p.likesBase else p.likesBase + if (gusta) 1 else 0
            val locales = if (p.remota) emptyList() else ajustes.comentarios[p.id].orEmpty()
            var menu by remember { mutableStateOf(false) }
            val texto = "${p.titulo}: ${Unidades.texto(p.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)} en ${formatearMs(p.segundos)} con Gacelas Loja"
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarCirculo(44.dp, p.indice)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.autor, style = GType.labelLg, color = GColors.TextPrimary)
                        Text("${p.titulo} · ${p.cuando}", style = GType.bodySm, color = GColors.TextSecondary)
                    }
                    p.autorId?.let { autor ->
                        val sigo = autor in siguiendo
                        TextButton(onClick = { vm.alternarSeguir(autor) }) {
                            Text(if (sigo) "Siguiendo" else "Seguir", color = if (sigo) GColors.TextSecondary else GColors.Primary500, style = GType.labelLg)
                        }
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Más opciones", tint = GColors.TextSecondary) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Compartir") }, onClick = { menu = false; onCompartir(texto) })
                            DropdownMenuItem(text = { Text("Ocultar publicación") }, onClick = {
                                menu = false
                                vm.ocultar(p.id)
                                onAviso("Publicación oculta")
                            })
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(GColors.MapParks, GColors.SurfaceLow))), contentAlignment = Alignment.Center) {
                    if (p.puntos.size >= 2) TrazadoRuta(p.puntos, Modifier.fillMaxSize()) else Icon(Icons.Filled.Route, null, tint = GColors.Primary500, modifier = Modifier.size(56.dp))
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceLow).padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Metrica(Unidades.texto(p.distanciaKm, m, 1), "Distancia (${Unidades.etiquetaDistancia(m)})")
                    Metrica(formatearRitmo(Unidades.ritmo(p.ritmoSegKm, m)), "Ritmo", color = GColors.Primary500)
                    Metrica(formatearMs(p.segundos), "Tiempo")
                }
                locales.takeLast(2).forEach { Text("Tú: $it", style = GType.bodySm, color = GColors.TextSecondary) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.alternarLike(p.id, onAviso) }) {
                        Icon(if (gusta) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (gusta) "Quitar me gusta" else "Me gusta", tint = if (gusta) GColors.Primary500 else GColors.TextSecondary)
                    }
                    Text("$likes", style = GType.labelLg, color = GColors.TextSecondary)
                    IconButton(onClick = { comentando = p }) { Icon(Icons.AutoMirrored.Outlined.Chat, "Comentar", tint = GColors.TextSecondary) }
                    Text("${p.comentariosBase + locales.size}", style = GType.labelLg, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onCompartir(texto) }) { Icon(Icons.Filled.Share, "Compartir", tint = GColors.TextSecondary) }
                }
            }
        }
    }
    comentando?.let { p ->
        var texto by remember(p.id) { mutableStateOf("") }
        val hilo by produceState(emptyList<com.gacelas.comunidad.domain.modelo.ComentarioFeed>(), p.id) { value = vm.comentarios(p.id) }
        AlertDialog(
            onDismissRequest = { comentando = null },
            title = { Text("Comentarios") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        hilo.forEach { c -> Text("${c.autor}: ${c.texto}", style = GType.bodySm, color = GColors.TextPrimary) }
                    }
                    CampoTexto("Comentario", texto, { texto = it }, "Escribe un aplauso o comentario...", lineas = 3)
                }
            },
            confirmButton = {
                TextButton(enabled = texto.isNotBlank(), onClick = {
                    vm.comentar(p.id, texto, onAviso)
                    comentando = null
                }) { Text("Publicar") }
            },
            dismissButton = { TextButton(onClick = { comentando = null }) { Text("Cancelar") } },
            containerColor = GColors.BgSecondary,
        )
    }
}

/**
 * Lista de eventos. [eventos] e [inscritos] son la lista base (ejemplo + creados); [clubId] abre el
 * dialogo de crear para ese club; [conClubes] muestra el acceso a Clubes (hay servidor).
 */
@Composable
fun EventosContenido(
    vm: EventosViewModel,
    eventos: List<Evento>,
    inscritos: Set<String>,
    conClubes: Boolean,
    clubId: String?,
    onEvento: (String) -> Unit,
    onClubes: () -> Unit,
    onAviso: (String) -> Unit,
) {
    val pestana by vm.pestana.collectAsState()
    var creando by remember { mutableStateOf(clubId != null) }
    var calendario by rememberSaveable { mutableStateOf(false) }
    val lista = vm.visibles(eventos, inscritos)
    Pantalla {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Eventos", style = GType.headlineLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            if (conClubes) IconButton(onClick = onClubes) { Icon(Icons.Filled.Groups, "Clubes", tint = GColors.TextPrimary) }
            IconButton(onClick = { calendario = !calendario }) { Icon(Icons.Filled.CalendarMonth, if (calendario) "Ver lista" else "Ver calendario", tint = GColors.TextPrimary) }
            IconButton(onClick = { creando = true }, modifier = Modifier.clip(CircleShape).background(GColors.Primary)) {
                Icon(Icons.Filled.Add, "Crear evento", tint = GColors.TextPrimary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PestanaEventos.entries.forEach { p -> Pastilla(p.etiqueta, p == pestana, { vm.pestana(p) }) }
        }
        if (calendario) CalendarioEventos(eventos, inscritos, onEvento)
        if (lista.isEmpty()) Tarjeta { Text(if (pestana == PestanaEventos.MisEventos) "Aún no te inscribes a ningún evento." else "No hay eventos.", style = GType.bodyMd, color = GColors.TextSecondary) }
        lista.forEach { e ->
            TarjetaEvento(e, e.id in inscritos, onClick = { onEvento(e.id) }) {
                vm.alternarInscripcion(e.id)
                onAviso(if (e.id in inscritos) "Inscripción cancelada" else "Te inscribiste a ${e.titulo}")
            }
        }
    }
    if (creando) {
        DialogoCrearEvento(onCerrar = { creando = false }) { base ->
            val nuevo = base.copy(clubId = clubId)
            vm.crear(nuevo)
            creando = false
            onAviso("Evento creado")
        }
    }
}

@Composable
private fun DialogoCrearEvento(onCerrar: () -> Unit, onCrear: (Evento) -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("06:00") }
    var cupo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("grupal") }
    var lugar by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("5") }
    val inicio = remember(fecha, hora) { inicioEvento(fecha, hora) }
    val valido = titulo.isNotBlank() && inicio != null && lugar.isNotBlank() && km.toIntOrNull() != null
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nuevo evento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto("Título", titulo, { titulo = it }, "Fondo del sábado")
                CampoTexto("Fecha (AAAA-MM-DD)", fecha, { fecha = it.take(10) }, "2026-10-18")
                CampoTexto("Hora (HH:MM)", hora, { hora = it.take(5) })
                CampoTexto("Cupo (opcional)", cupo, { cupo = it.filter(Char::isDigit).take(4) }, "Sin límite", tipo = KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("grupal" to "Grupal", "carrera" to "Competencia", "entrenamiento" to "Entreno").forEach { (k, v) -> Pastilla(v, k == tipo, { tipo = k }) }
                }
                CampoTexto("Lugar", lugar, { lugar = it }, "Parque Jipiro")
                CampoTexto("Distancia (km)", km, { km = it.filter(Char::isDigit) }, tipo = KeyboardType.Number)
            }
        },
        confirmButton = {
            TextButton(enabled = valido, onClick = {
                val etiqueta = etiquetaFechaEvento(inicio!!)
                onCrear(Evento("u-${nuevoUuid()}", titulo.trim(), "Evento creado por la comunidad Gacelas.", etiqueta, hora.trim(), lugar.trim(), 1, km.toInt(),
                    creadoPorUsuario = true, inicioMs = inicio, cupo = cupo.toIntOrNull()?.coerceAtLeast(1), tipo = tipo))
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
        containerColor = GColors.BgSecondary,
    )
}

@Composable
private fun TarjetaEvento(e: Evento, inscrito: Boolean, onClick: () -> Unit, onInscribir: () -> Unit) {
    Tarjeta(padding = 0.dp, onClick = onClick) {
        Box(Modifier.fillMaxWidth().height(110.dp).background(Brush.linearGradient(listOf(if (e.yaPaso()) GColors.SurfaceHigh else GColors.Primary700, GColors.SurfaceLow)))) {
            Icon(Icons.Filled.Event, null, tint = GColors.TextPrimary.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Center).size(48.dp))
            Text("${e.distanciaKm} km", style = GType.labelMd, color = GColors.TextPrimary,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 4.dp))
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(e.titulo, style = GType.titleMd, color = GColors.TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarToday, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
                Text(" ${e.fecha}, ${e.hora}", style = GType.bodySm, color = GColors.TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
                Text(" ${e.ubicacion}", style = GType.bodySm, color = GColors.TextSecondary)
            }
            val total = e.participantes + if (inscrito && !e.creadoPorUsuario) 1 else 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(64.dp)) { (0 until minOf(3, total)).forEach { i -> Box(Modifier.offset(x = (i * 16).dp)) { AvatarCirculo(28.dp, i) } } }
                Text(if (total > 3) "+${total - 3} participantes" else "$total participantes",
                    style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                if (!e.yaPaso()) {
                    Text(if (inscrito) "Inscrito" else "Inscribirse", style = GType.labelMd, color = GColors.TextPrimary,
                        modifier = Modifier.clip(CircleShape).background(if (inscrito) GColors.Secondary else GColors.Primary).clickable(onClick = onInscribir).padding(horizontal = 12.dp, vertical = 6.dp))
                } else {
                    Text("Finalizado", style = GType.labelMd, color = GColors.TextSecondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetalleEventoContenido(vm: DetalleEventoViewModel, onAtras: () -> Unit, onEnVivo: () -> Unit, onCompartir: (String) -> Unit, onAviso: (String) -> Unit) {
    val (_, inscritos) = vm.datos.collectAsState().value
    val e by vm.evento.collectAsState()
    val inscrito = e.id in inscritos
    val estado by vm.estado.collectAsState()
    val participantes = estado?.let { e.participantes + it.inscritos } ?: (e.participantes + if (inscrito && !e.creadoPorUsuario) 1 else 0)
    val enEspera = estado?.miEstado == "waitlist"
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(220.dp).background(Brush.verticalGradient(listOf(GColors.Primary700, GColors.BgPrimary)))) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                    IconButton(onClick = onAtras, modifier = Modifier.clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.6f))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = GColors.TextPrimary)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onCompartir("${e.titulo} - ${e.fecha}, ${e.hora} en ${e.ubicacion}. Corre con Gacelas Loja.") }, modifier = Modifier.clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.6f))) {
                        Icon(Icons.Filled.Share, "Compartir evento", tint = GColors.TextPrimary)
                    }
                }
                Icon(Icons.Filled.Event, null, tint = GColors.TextPrimary.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center).size(72.dp))
                Row(Modifier.align(Alignment.BottomStart).padding(16.dp).clip(CircleShape).background(GColors.Primary).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = GColors.TextPrimary, modifier = Modifier.size(14.dp))
                    Text(" ${e.distanciaKm} km", style = GType.labelMd, color = GColors.TextPrimary)
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(e.titulo, style = GType.headlineLg, color = GColors.TextPrimary)
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarToday, null, tint = GColors.Primary500)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(if (e.fecha.any(Char::isDigit) && !e.fecha.contains("20")) "${e.fecha}, 2026" else e.fecha, style = GType.labelLg, color = GColors.TextPrimary)
                            Text(e.hora, style = GType.bodySm, color = GColors.TextSecondary)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = GColors.Primary500)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(e.ubicacion, style = GType.labelLg, color = GColors.TextPrimary)
                            Text("Loja, Ecuador", style = GType.bodySm, color = GColors.TextSecondary)
                        }
                    }
                }
                Text("Descripción", style = GType.titleMd, color = GColors.TextPrimary)
                Text(e.descripcion, style = GType.bodyMd, color = GColors.TextSecondary)
                androidx.compose.material3.TextButton(onClick = onEnVivo) { Icon(Icons.Filled.Sensors, null, tint = GColors.Danger); Text(if (e.yaPaso()) " Repetición del evento" else " Ver evento en vivo", color = GColors.Danger) }
                Text("Participantes ($participantes)", style = GType.titleMd, color = GColors.TextPrimary)
                estado?.let { Text(com.gacelas.comunidad.domain.reglas.textoCupo(it) + if (enEspera) " · estás en la lista de espera (puesto ${it.miPosicion})" else "", style = GType.bodySm, color = GColors.TextSecondary) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(minOf(participantes, 30)) { AvatarCirculo(36.dp, it) }
                }
                Tarjeta {
                    Text("Detalles de la ruta", style = GType.titleMd, color = GColors.TextPrimary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Metrica("${e.distanciaKm}", "km", color = GColors.Primary500)
                        Metrica(if (e.distanciaKm > 10) "Media" else "Fácil", "dificultad", color = GColors.Secondary500)
                    }
                }
            }
        }
        Box(Modifier.background(GColors.BgSecondary).navigationBarsPadding().padding(16.dp)) {
            if (e.yaPaso()) {
                BotonPrimario("Evento finalizado", {}, icono = null, enabled = false)
            } else {
                val lleno = estado?.let { it.cupo.let { c -> c != null && it.inscritos >= c } } == true
                BotonPrimario(if (inscrito) (if (enEspera) "En lista de espera (toca para salir)" else "Inscrito (toca para cancelar)") else if (lleno) "Unirme a la lista de espera" else "Inscribirme al evento", {
                    vm.alternarInscripcion(onAviso)
                }, icono = Icons.Filled.CheckCircle, color = if (inscrito) GColors.Secondary else GColors.Primary)
            }
        }
    }
}
