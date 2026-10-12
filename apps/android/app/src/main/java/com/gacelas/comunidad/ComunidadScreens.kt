package com.gacelas.comunidad

import com.gacelas.data.eventos.yaPaso
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gacelas.correr.TrazadoRuta
import com.gacelas.data.eventos.PestanaEventos
import com.gacelas.data.eventos.filtrarEventos
import com.gacelas.data.mock.Evento
import com.gacelas.data.mock.MockData
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.ajustesState
import com.gacelas.ui.aviso
import com.gacelas.ui.carrerasState
import com.gacelas.ui.compartir
import com.gacelas.ui.components.AvatarCirculo
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.contenedor
import com.gacelas.ui.sesionState
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

/** Publicacion del feed: actividad de la comunidad o carrera publica propia. */
data class Publicacion(
    val id: String,
    val autor: String,
    val propia: Boolean,
    val indice: Int,
    val cuando: String,
    val titulo: String,
    val distanciaKm: Double,
    val segundos: Int,
    val ritmoSegKm: Int?,
    val likesBase: Int,
    val comentariosBase: Int,
    val puntos: List<com.gacelas.data.carreras.PuntoGps> = emptyList(),
)

enum class FiltroFeed(val etiqueta: String) { Todos("Todos"), Comunidad("Comunidad"), Mias("Mis carreras") }

fun filtrarFeed(lista: List<Publicacion>, filtro: FiltroFeed, ocultas: Set<String>): List<Publicacion> = lista
    .filter { it.id !in ocultas }
    .filter {
        when (filtro) {
            FiltroFeed.Todos -> true
            FiltroFeed.Comunidad -> !it.propia
            FiltroFeed.Mias -> it.propia
        }
    }

@Composable
fun FeedScreen(onEventos: () -> Unit, onEnVivo: () -> Unit = {}) {
    val context = LocalContext.current
    val c = contenedor()
    val scope = rememberCoroutineScope()
    val ajustes by ajustesState()
    val carreras by carrerasState()
    val perfil by sesionState()
    val m = ajustes.unidadesMetricas
    var filtro by rememberSaveable { mutableStateOf(FiltroFeed.Todos) }
    var menuFiltro by remember { mutableStateOf(false) }
    var comentando by remember { mutableStateOf<Publicacion?>(null) }
    val propias = carreras.filter { it.publica }.map {
        Publicacion("carrera-${it.id}", perfil?.nombre ?: "Tú", true, 0, "Tu carrera", it.titulo, it.distanciaKm, it.segundos, it.ritmoSegKm, 0, 0, it.puntos)
    }
    val comunidad = MockData.actividades.mapIndexed { i, a ->
        Publicacion("act-${a.id}", MockData.usuario(a.usuarioId)?.nombre ?: "", false, i + 1, a.fecha, a.titulo, a.distanciaKm, a.duracionSegundos, a.duracionSegundos.let { s -> (s / a.distanciaKm).toInt() }, a.likes, a.comentarios)
    }
    val lista = filtrarFeed(propias + comunidad, filtro, ajustes.ocultas)
    Pantalla {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Comunidad", style = GType.headlineLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onEnVivo) { Icon(Icons.Filled.Sensors, "Corredores en vivo", tint = GColors.Danger) }
            Box {
                IconButton(onClick = { menuFiltro = true }) { Icon(Icons.Filled.FilterList, "Filtrar", tint = GColors.TextSecondary) }
                DropdownMenu(expanded = menuFiltro, onDismissRequest = { menuFiltro = false }) {
                    FiltroFeed.entries.forEach { f ->
                        DropdownMenuItem(text = { Text(f.etiqueta) }, onClick = { filtro = f; menuFiltro = false })
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pastilla("Feed", true)
            Pastilla("Eventos", false, onEventos)
            if (filtro != FiltroFeed.Todos) Pastilla(filtro.etiqueta, true, { filtro = FiltroFeed.Todos })
        }
        if (lista.isEmpty()) Tarjeta { Text("No hay publicaciones con este filtro.", style = GType.bodyMd, color = GColors.TextSecondary) }
        lista.forEach { p ->
            val gusta = p.id in ajustes.likes
            val comentarios = ajustes.comentarios[p.id].orEmpty()
            var menu by remember { mutableStateOf(false) }
            val texto = "${p.titulo}: ${Unidades.texto(p.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)} en ${formatearMs(p.segundos)} con Gacelas Loja"
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarCirculo(40.dp, p.indice)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.autor, style = GType.labelLg, color = GColors.TextPrimary)
                        Text(p.cuando, style = GType.bodySm, color = GColors.TextSecondary)
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Más opciones", tint = GColors.TextSecondary) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Compartir") }, onClick = { menu = false; context.compartir(texto) })
                            DropdownMenuItem(text = { Text("Ocultar publicación") }, onClick = {
                                menu = false
                                scope.launch { c.ajustes.actualizar { it.copy(ocultas = it.ocultas + p.id) } }
                                context.aviso("Publicación oculta")
                            })
                        }
                    }
                }
                Text(p.titulo, style = GType.titleMd, color = GColors.TextPrimary)
                Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(GColors.MapParks, GColors.SurfaceLow))), contentAlignment = Alignment.Center) {
                    if (p.puntos.size >= 2) TrazadoRuta(p.puntos, Modifier.fillMaxSize()) else Icon(Icons.Filled.Route, null, tint = GColors.Primary500, modifier = Modifier.size(56.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Metrica(Unidades.texto(p.distanciaKm, m, 1), Unidades.etiquetaDistancia(m), color = GColors.Primary500)
                    Metrica(formatearMs(p.segundos), "tiempo")
                    Metrica(formatearRitmo(Unidades.ritmo(p.ritmoSegKm, m)), Unidades.etiquetaRitmo(m), color = GColors.Secondary500)
                }
                comentarios.takeLast(2).forEach { Text("Tú: $it", style = GType.bodySm, color = GColors.TextSecondary) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        scope.launch { c.ajustes.actualizar { a -> a.copy(likes = if (gusta) a.likes - p.id else a.likes + p.id) } }
                    }) {
                        Icon(if (gusta) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (gusta) "Quitar me gusta" else "Me gusta", tint = if (gusta) GColors.Danger else GColors.TextSecondary)
                    }
                    Text("${p.likesBase + if (gusta) 1 else 0}", style = GType.labelLg, color = GColors.TextSecondary)
                    IconButton(onClick = { comentando = p }) { Icon(Icons.AutoMirrored.Outlined.Chat, "Comentar", tint = GColors.TextSecondary) }
                    Text("${p.comentariosBase + comentarios.size}", style = GType.labelLg, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { context.compartir(texto) }) { Icon(Icons.Filled.Share, "Compartir", tint = GColors.TextSecondary) }
                }
            }
        }
    }
    comentando?.let { p ->
        var texto by remember(p.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { comentando = null },
            title = { Text("Comentar") },
            text = { CampoTexto("Comentario", texto, { texto = it }, "Escribe algo motivador...", lineas = 3) },
            confirmButton = {
                TextButton(enabled = texto.isNotBlank(), onClick = {
                    val t = texto.trim()
                    scope.launch { c.ajustes.actualizar { a -> a.copy(comentarios = a.comentarios + (p.id to (a.comentarios[p.id].orEmpty() + t))) } }
                    comentando = null
                }) { Text("Publicar") }
            },
            dismissButton = { TextButton(onClick = { comentando = null }) { Text("Cancelar") } },
            containerColor = GColors.BgSecondary,
        )
    }
}

/** Eventos de ejemplo mas los creados por el usuario. */
@Composable
fun eventosState(): Pair<List<Evento>, Set<String>> {
    val c = contenedor()
    val flujo = remember(c) { combine(c.eventos.creados, c.eventos.inscritos) { cr, ins -> (MockData.eventos + cr) to ins } }
    val estado by flujo.collectAsState(initial = MockData.eventos to emptySet())
    return estado
}

@Composable
fun EventosScreen(onEvento: (String) -> Unit, onClubes: () -> Unit = {}) {
    val c = contenedor()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (eventos, inscritos) = eventosState()
    var pestana by rememberSaveable { mutableStateOf(PestanaEventos.Proximos) }
    val clubId = remember { com.gacelas.data.clubes.EventoDeClub.id.also { com.gacelas.data.clubes.EventoDeClub.id = null } }
    var creando by remember { mutableStateOf(clubId != null) }
    var calendario by rememberSaveable { mutableStateOf(false) }
    val lista = filtrarEventos(eventos, pestana, inscritos)
    Pantalla {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Eventos", style = GType.headlineLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            if (c.clubes != null) IconButton(onClick = onClubes) { Icon(Icons.Filled.Groups, "Clubes", tint = GColors.TextPrimary) }
            IconButton(onClick = { calendario = !calendario }) { Icon(Icons.Filled.CalendarMonth, if (calendario) "Ver lista" else "Ver calendario", tint = GColors.TextPrimary) }
            IconButton(onClick = { creando = true }, modifier = Modifier.clip(CircleShape).background(GColors.Primary)) {
                Icon(Icons.Filled.Add, "Crear evento", tint = GColors.TextPrimary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PestanaEventos.entries.forEach { p -> Pastilla(p.etiqueta, p == pestana, { pestana = p }) }
        }
        if (calendario) com.gacelas.comunidad.CalendarioEventos(eventos, inscritos, onEvento)
        if (lista.isEmpty()) Tarjeta { Text(if (pestana == PestanaEventos.MisEventos) "Aún no te inscribes a ningún evento." else "No hay eventos.", style = GType.bodyMd, color = GColors.TextSecondary) }
        lista.forEach { e ->
            TarjetaEvento(e, e.id in inscritos, onClick = { onEvento(e.id) }) {
                scope.launch { c.eventos.alternarInscripcion(e.id) }
                context.aviso(if (e.id in inscritos) "Inscripción cancelada" else "Te inscribiste a ${e.titulo}")
            }
        }
    }
    if (creando) {
        DialogoCrearEvento(onCerrar = { creando = false }) { base ->
            val nuevo = base.copy(clubId = clubId)
            scope.launch { c.eventos.crear(nuevo) }
            creando = false
            pestana = PestanaEventos.MisEventos
            context.aviso("Evento creado")
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
    val inicio = remember(fecha, hora) { runCatching { java.time.LocalDateTime.parse("${fecha.trim()}T${hora.trim()}").atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull() }
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
                val etiqueta = java.time.Instant.ofEpochMilli(inicio!!).atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", java.util.Locale("es", "EC"))).replaceFirstChar { it.uppercase() }
                onCrear(Evento("u-${UUID.randomUUID()}", titulo.trim(), "Evento creado por la comunidad Gacelas.", etiqueta, hora.trim(), lugar.trim(), 1, km.toInt(),
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
fun DetalleEventoScreen(id: String, onAtras: () -> Unit, onEnVivo: () -> Unit = {}) {
    val c = contenedor()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (eventos, inscritos) = eventosState()
    val remoto by androidx.compose.runtime.produceState<Evento?>(null, id) { value = c.clubes?.evento(id) }
    val e = eventos.firstOrNull { it.id == id } ?: remoto ?: MockData.eventos.firstOrNull { it.id == id } ?: MockData.eventos.first()
    val inscrito = e.id in inscritos
    var estado by remember { mutableStateOf<com.gacelas.data.clubes.EstadoEvento?>(null) }
    androidx.compose.runtime.LaunchedEffect(id, inscritos) { estado = c.clubes?.estadoEvento(id) }
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
                    IconButton(onClick = { context.compartir("${e.titulo} - ${e.fecha}, ${e.hora} en ${e.ubicacion}. Corre con Gacelas Loja.") }, modifier = Modifier.clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.6f))) {
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
                estado?.let { Text(com.gacelas.data.clubes.textoCupo(it) + if (enEspera) " · estás en la lista de espera (puesto ${it.miPosicion})" else "", style = GType.bodySm, color = GColors.TextSecondary) }
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
                    scope.launch {
                        c.eventos.alternarInscripcion(e.id)
                        estado = c.clubes?.estadoEvento(e.id)
                        context.aviso(if (inscrito) "Inscripción cancelada" else if (estado?.miEstado == "waitlist") "Evento lleno: quedaste en la lista de espera" else "Inscripción confirmada")
                    }
                }, icono = Icons.Filled.CheckCircle, color = if (inscrito) GColors.Secondary else GColors.Primary)
            }
        }
    }
}
