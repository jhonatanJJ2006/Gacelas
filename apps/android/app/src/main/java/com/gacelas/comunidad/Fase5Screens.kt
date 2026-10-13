package com.gacelas.comunidad

import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.CorredorEvento
import com.gacelas.comunidad.domain.modelo.MiembroClub
import com.gacelas.comunidad.domain.modelo.PostClub
import com.gacelas.comunidad.domain.modelo.RolClub
import com.gacelas.comunidad.domain.reglas.accionClub
import com.gacelas.comunidad.domain.reglas.calendarioMes
import com.gacelas.comunidad.domain.reglas.clasificacion
import com.gacelas.comunidad.domain.reglas.posicionRepeticion
import com.gacelas.comunidad.domain.reglas.puedeGestionar
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.ui.aviso
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.Interruptor
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import org.maplibre.android.geometry.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private val COLORES = listOf(0xFFF59E0B, 0xFF10B981, 0xFF3B82F6, 0xFFEF4444, 0xFFA855F7, 0xFFEC4899, 0xFF14B8A6, 0xFFF97316).map { Color(it) }
fun colorCorredor(i: Int) = COLORES[i % COLORES.size]

@Composable
private fun SinServidor() = Text("Los clubes necesitan conexión con el servidor.", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.padding(16.dp))

@Composable
fun ClubesScreen(onAtras: () -> Unit, onClub: (String) -> Unit) {
    val contenedorVm = contenedor()
    val repo = contenedorVm.clubes
    val context = LocalContext.current
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { contenedorVm.clubesViewModel() }
    val datos by vm.datos.collectAsState()
    var creando by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { EncabezadoAtras("Clubes", onAtras) }
            if (repo != null) IconButton(onClick = { creando = true }, modifier = Modifier.padding(end = 12.dp).clip(CircleShape).background(GColors.Primary)) { Icon(Icons.Filled.Add, "Crear club", tint = GColors.TextPrimary) }
        }
        if (repo == null) return SinServidor()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val (todos, mios) = datos ?: return@Column Text("Cargando...", color = GColors.TextSecondary)
            if (mios.isNotEmpty()) Text("Mis clubes", style = GType.titleMd, color = GColors.TextPrimary)
            todos.filter { it.id in mios }.forEach { TarjetaClub(it) { onClub(it.id) } }
            Text("Descubrir", style = GType.titleMd, color = GColors.TextPrimary)
            val otros = todos.filter { it.id !in mios }
            if (otros.isEmpty()) Text("No hay más clubes. ¡Crea el primero!", style = GType.bodySm, color = GColors.TextSecondary)
            otros.forEach { TarjetaClub(it) { onClub(it.id) } }
        }
    }
    if (creando && repo != null) {
        var nombre by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var lugar by remember { mutableStateOf("Loja") }
        var aprobacion by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { creando = false },
            containerColor = GColors.BgSecondary,
            title = { Text("Crear club") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoTexto("Nombre", nombre, { nombre = it.take(60) }, "Gacelas Loja")
                    CampoTexto("Descripción", desc, { desc = it.take(500) }, "¿De qué va el club?", lineas = 2)
                    CampoTexto("Ubicación", lugar, { lugar = it.take(80) })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Aprobar a quien se una", color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                        Interruptor(aprobacion) { aprobacion = it }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.crear(nombre, desc, lugar, aprobacion, onCreado = { creando = false; onClub(it) }, onError = { context.aviso(it) })
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { creando = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun TarjetaClub(c: Club, onClick: () -> Unit) = Tarjeta(onClick = onClick) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(GColors.Primary.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Groups, null, tint = GColors.Primary400) }
        Column(Modifier.weight(1f)) {
            Text(c.nombre, style = GType.titleMd, color = GColors.TextPrimary, maxLines = 1)
            Text("${c.miembros} ${if (c.miembros == 1) "miembro" else "miembros"}" + (if (c.ubicacion.isNotBlank()) " · ${c.ubicacion}" else "") + (if (c.conAprobacion) " · con aprobación" else ""),
                style = GType.bodySm, color = GColors.TextSecondary, maxLines = 1)
        }
    }
}

@Composable
fun DetalleClubScreen(id: String, onAtras: () -> Unit, onEvento: (String) -> Unit, onCrearEvento: (String) -> Unit) {
    val cont = contenedor()
    val repo = cont.clubes
    val context = LocalContext.current
    val vm = androidx.lifecycle.viewmodel.compose.viewModel(key = "detalle-club-$id") { cont.detalleClubViewModel(id) }
    val datos by vm.estado.collectAsState()
    val texto by vm.texto.collectAsState()
    val club = datos.club
    val miembros = datos.miembros
    val posts = datos.posts
    val eventos = datos.eventos
    val aviso: (String) -> Unit = { context.aviso(it) }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras(club?.nombre ?: "Club", onAtras)
        if (repo == null) return SinServidor()
        val c = club ?: return Text("Cargando...", color = GColors.TextSecondary, modifier = Modifier.padding(16.dp))
        val yo = datos.yo
        val gestiono = datos.gestiono
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Tarjeta {
                Text(c.descripcion.ifBlank { "Sin descripción." }, style = GType.bodyMd, color = GColors.TextSecondary)
                Text("${c.miembros} ${if (c.miembros == 1) "miembro" else "miembros"} · ${if (c.conAprobacion) "Con aprobación" else "Abierto"}", style = GType.bodySm, color = GColors.TextSecondary)
                accionClub(yo, c.conAprobacion)?.let { a ->
                    BotonPrimario(a, { vm.accionPrincipal(aviso) }, enabled = yo?.activo != false, icono = null, color = if (yo == null) GColors.Primary else GColors.SurfaceHigh)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Eventos del club", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                if (gestiono) TextButton(onClick = { onCrearEvento(id) }) { Text("+ Crear", color = GColors.Primary400) }
            }
            if (eventos.isEmpty()) Text("Sin eventos programados.", style = GType.bodySm, color = GColors.TextSecondary)
            eventos.forEach { e -> Tarjeta(padding = 12.dp, onClick = { onEvento(e.id) }) { Text(e.titulo, style = GType.labelLg, color = GColors.TextPrimary); Text("${e.fecha} · ${e.hora}", style = GType.bodySm, color = GColors.TextSecondary) } }
            val pendientes = miembros.filter { !it.activo }
            if (gestiono && pendientes.isNotEmpty()) {
                Text("Solicitudes (${pendientes.size})", style = GType.titleMd, color = GColors.TextPrimary)
                pendientes.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.nombre, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.aprobar(m.usuarioId, aviso) }) { Text("Aprobar", color = GColors.Secondary500) }
                        TextButton(onClick = { vm.rechazar(m.usuarioId, aviso) }) { Text("Rechazar", color = GColors.TextSecondary) }
                    }
                }
            }
            Text("Publicaciones", style = GType.titleMd, color = GColors.TextPrimary)
            if (yo?.activo == true) {
                CampoTexto("Escribe al club", texto, vm::escribir, "Escribe algo al club...", lineas = 2)
                BotonPrimario("Publicar", { vm.publicar(aviso) }, icono = null)
            }
            if (posts.isEmpty()) Text(if (yo?.activo == true || !c.conAprobacion) "Aún no hay publicaciones." else "Únete para ver las publicaciones.", style = GType.bodySm, color = GColors.TextSecondary)
            posts.forEach { p -> Tarjeta(padding = 12.dp) { Text(p.nombre, style = GType.labelLg, color = GColors.TextPrimary); Text(p.texto, style = GType.bodyMd, color = GColors.TextSecondary) } }
            Text("Miembros", style = GType.titleMd, color = GColors.TextPrimary)
            miembros.filter { it.activo }.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.nombre, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text(m.rol.etiqueta, style = GType.labelMd, color = GColors.TextSecondary)
                    if (yo?.rol == RolClub.owner && m.rol != RolClub.owner) TextButton(onClick = { vm.alternarAdmin(m, aviso) }) { Text(if (m.rol == RolClub.admin) "Quitar admin" else "Hacer admin", color = GColors.Primary400) }
                }
            }
        }
    }
}

/** Calendario mensual con los eventos (lunes a domingo). */
@Composable
fun CalendarioEventos(eventos: List<Evento>, inscritos: Set<String>, onEvento: (String) -> Unit) {
    val hoy = remember { Calendar.getInstance() }
    var anio by remember { mutableIntStateOf(hoy[Calendar.YEAR]) }
    var mes by remember { mutableIntStateOf(hoy[Calendar.MONTH]) }
    var elegido by remember { mutableStateOf<List<Evento>>(emptyList()) }
    val semanas = remember(anio, mes, eventos) { calendarioMes(anio, mes, eventos) }
    val nombreMes = remember(anio, mes) { java.text.SimpleDateFormat("LLLL yyyy", java.util.Locale("es", "EC")).format(Calendar.getInstance().apply { set(anio, mes, 1) }.time) }
    fun mover(d: Int) { val c = Calendar.getInstance().apply { set(anio, mes, 1); add(Calendar.MONTH, d) }; anio = c[Calendar.YEAR]; mes = c[Calendar.MONTH]; elegido = emptyList() }
    Tarjeta(Modifier.testTag("calendario")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { mover(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior", tint = GColors.TextPrimary) }
            Text(nombreMes.replaceFirstChar { it.uppercase() }, style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { mover(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente", tint = GColors.TextPrimary) }
        }
        Row { listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
        semanas.forEach { semana ->
            Row {
                semana.forEach { d ->
                    val mio = d.eventos.any { it.id in inscritos }
                    Column(
                        Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (d.eventos.isNotEmpty()) GColors.SurfaceHigh else Color.Transparent)
                            .clickable(enabled = d.eventos.isNotEmpty()) { elegido = d.eventos },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Text("${d.dia}", style = GType.labelMd, color = if (d.delMes) GColors.TextPrimary else GColors.TextSecondary.copy(alpha = 0.5f))
                        if (d.eventos.isNotEmpty()) Box(Modifier.size(5.dp).clip(CircleShape).background(if (mio) GColors.Secondary500 else GColors.Primary500))
                    }
                }
            }
        }
        elegido.forEach { e -> Text("${e.titulo} · ${e.hora}", style = GType.bodyMd, color = GColors.Primary400, modifier = Modifier.clickable { onEvento(e.id) }.padding(vertical = 6.dp)) }
    }
}

/** Mapa en vivo de todos los corredores del evento, clasificacion y repeticion. */
@Composable
fun EventoEnVivoScreen(id: String, onAtras: () -> Unit, onCorrer: () -> Unit) {
    val c = contenedor()
    val vm = androidx.lifecycle.viewmodel.compose.viewModel(key = "evento-en-vivo-$id") { c.eventoEnVivoViewModel(id) }
    val tabla by vm.tabla.collectAsState()
    val repeticion by vm.repeticion.collectAsState()
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras("Evento en vivo", onAtras)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(16.dp)).background(GColors.SurfaceContainer)) {
                if (Mapas.configurado()) {
                    var mapaEvento by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
                    // Encuadrar a todos los corredores (posiciones y trazos) cuando llegan datos.
                    val puntos = tabla.flatMap { k -> k.puntos + listOfNotNull(k.lat?.let { la -> k.lng?.let { la to it } }) }
                    LaunchedEffect(puntos.size, mapaEvento) {
                        val m = mapaEvento ?: return@LaunchedEffect
                        if (puntos.size < 2) return@LaunchedEffect
                        runCatching { m.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(com.gacelas.ui.components.MapaMat.limites(puntos.map { LatLng(it.first, it.second) }), 120)) }
                    }
                    val trazos = mutableListOf<com.gacelas.ui.components.TrazoMapa>()
                    val circulos = mutableListOf<com.gacelas.ui.components.CirculoMapa>()
                    tabla.forEachIndexed { i, k ->
                        val trazo = k.puntos.let { p -> repeticion?.let { r -> p.take(maxOf(1, (r * p.size).toInt())) } ?: p }.map { LatLng(it.first, it.second) }
                        if (trazo.size >= 2) trazos += com.gacelas.ui.components.TrazoMapa(trazo, colorCorredor(i), 4f)
                        val pos = repeticion?.let { posicionRepeticion(k, it) } ?: k.lat?.let { la -> k.lng?.let { la to it } }
                        pos?.let { circulos += com.gacelas.ui.components.CirculoMapa(LatLng(it.first, it.second), 8f, colorCorredor(i), bordeDp = 1.5f) }
                    }
                    com.gacelas.ui.components.MapaLibre(Modifier.fillMaxSize().testTag("mapa-evento"), trazos = trazos, circulos = circulos, alListo = { mapaEvento = it })
                } else Text("El mapa no está disponible en este dispositivo.", color = GColors.TextSecondary, modifier = Modifier.align(Alignment.Center))
            }
            BotonPrimario("Correr y compartir en el evento", { vm.correrEnEvento(); onCorrer() }, icono = null)
            if (tabla.any { it.puntos.size > 1 }) {
                TextButton(onClick = vm::alternarRepeticion) { Text(if (repeticion == null) "Ver repetición" else "Salir de la repetición", color = GColors.Primary400) }
                repeticion?.let { r -> Slider(r, { vm.mover(it) }) }
            }
            Text("Clasificación", style = GType.titleMd, color = GColors.TextPrimary)
            if (tabla.isEmpty()) Text("Cuando los inscritos compartan en vivo, aparecerán aquí.", style = GType.bodySm, color = GColors.TextSecondary)
            tabla.forEachIndexed { i, k ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(colorCorredor(i)), contentAlignment = Alignment.Center) { Text("${i + 1}", style = GType.labelMd, color = GColors.BgPrimary) }
                    Text(k.nombre + (if (k.oculto) " · oculto" else "") + (if (k.terminado) " · terminó" else ""), color = GColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("%.2f km".format(k.distanciaM / 1000), style = GType.labelLg, color = GColors.TextPrimary)
                }
            }
        }
    }
}
