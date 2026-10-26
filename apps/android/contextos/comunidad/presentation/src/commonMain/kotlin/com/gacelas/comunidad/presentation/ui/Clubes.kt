package com.gacelas.comunidad.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.RolClub
import com.gacelas.comunidad.domain.modelo.TipoClub
import com.gacelas.comunidad.domain.reglas.accionClub
import com.gacelas.comunidad.domain.reglas.borradorDesdeClub
import com.gacelas.comunidad.domain.reglas.estaLleno
import com.gacelas.comunidad.domain.reglas.textoHorario
import com.gacelas.comunidad.domain.reglas.textoRitmo
import com.gacelas.comunidad.domain.reglas.calendarioMes
import com.gacelas.comunidad.domain.reglas.posicionRepeticion
import com.gacelas.comunidad.presentation.ClubesViewModel
import com.gacelas.comunidad.presentation.DetalleClubViewModel
import com.gacelas.comunidad.presentation.EventoEnVivoViewModel
import com.gacelas.mapa.CirculoComun
import com.gacelas.mapa.Coord
import com.gacelas.mapa.TrazoComun
import com.gacelas.shared.Fechas
import com.gacelas.shared.ahoraMs
import com.gacelas.shared.fmt
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.Interruptor
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

private val COLORES = listOf(0xFFA855F7, 0xFF10B981, 0xFF3B82F6, 0xFFEF4444, 0xFFA855F7, 0xFFEC4899, 0xFF14B8A6, 0xFFF97316)
fun colorCorredorArgb(i: Int): Long = COLORES[i % COLORES.size]
fun colorCorredor(i: Int) = Color(colorCorredorArgb(i))

@Composable
private fun SinServidor() = Text("Los clubes necesitan conexión con el servidor.", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.padding(16.dp))

@Composable
private fun DatoGrupo(etiqueta: String, valor: String, modifier: Modifier = Modifier) = Column(
    modifier.clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceHigh).padding(10.dp),
) {
    Text(etiqueta, style = GType.labelMd, color = GColors.TextSecondary)
    Text(valor, style = GType.bodyMd, color = GColors.TextPrimary, maxLines = 2)
}

@Composable
fun DetalleClubContenido(
    id: String,
    vm: DetalleClubViewModel,
    conServidor: Boolean,
    onAtras: () -> Unit,
    onEvento: (String) -> Unit,
    onCrearEvento: (String) -> Unit,
    onAviso: (String) -> Unit,
    onClub: (String) -> Unit = {},
    onReportar: ((Club) -> Unit)? = null,
    selectorPunto: SelectorPunto? = null,
) {
    val datos by vm.estado.collectAsState()
    var editando by remember { mutableStateOf(false) }
    var borrando by remember { mutableStateOf(false) }
    LaunchedEffect(datos.eliminado) { if (datos.eliminado) onAtras() }
    datos.club?.takeIf { editando }?.let { c ->
        FormularioClubContenido(
            titulo = if (c.tipo == TipoClub.comunidad) "Editar comunidad" else "Editar grupo",
            inicial = borradorDesdeClub(c), comunidades = datos.todos.filter { it.tipo == TipoClub.comunidad && it.id != c.id },
            permitirTipo = false, selectorPunto = selectorPunto, onCancelar = { editando = false },
            onGuardar = { b -> vm.editar(b, onListo = { editando = false }, onAviso = onAviso) }, textoGuardar = "Guardar cambios",
        )
        return
    }
    val texto by vm.texto.collectAsState()
    val club = datos.club
    val miembros = datos.miembros
    val posts = datos.posts
    val eventos = datos.eventos
    val aviso = onAviso
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras(club?.nombre ?: "Grupo", onAtras)
        if (!conServidor) return SinServidor()
        val c = club ?: return Text("Cargando...", color = GColors.TextSecondary, modifier = Modifier.padding(16.dp))
        val yo = datos.yo
        val gestiono = datos.gestiono
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LogoClub(c, 56)
                    Column(Modifier.weight(1f)) {
                        Text(c.tipo.etiqueta.uppercase(), style = GType.labelMd, color = GColors.Primary400)
                        datos.comunidad?.let { co -> Text("Parte de ${co.nombre}", style = GType.bodyMd, color = GColors.TextPrimary, modifier = Modifier.clickable { onClub(co.id) }) }
                    }
                    if (gestiono) TextButton(onClick = { editando = true }) { Text("Editar", color = GColors.Primary400) }
                }
                Text(c.descripcion.ifBlank { "Sin descripción." }, style = GType.bodyMd, color = GColors.TextSecondary)
                val miembrosTxt = if (c.cupo != null) "${c.miembros}/${c.cupo} miembros" else "${c.miembros} ${if (c.miembros == 1) "miembro" else "miembros"}"
                Text("$miembrosTxt · ${c.ciudad} · ${if (c.conAprobacion) "Con solicitud" else "Público"}", style = GType.bodySm, color = GColors.TextSecondary)
                if (c.tipo == TipoClub.grupo) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatoGrupo("Encuentro", c.lugarEncuentro.ifBlank { "Por definir" }, Modifier.weight(1f))
                        DatoGrupo("Horario", textoHorario(c.dias, c.hora), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatoGrupo("Nivel", c.nivel.etiqueta, Modifier.weight(1f))
                        DatoGrupo("Ritmo", textoRitmo(c.ritmoSegKm) ?: "Libre", Modifier.weight(1f))
                    }
                }
                if (c.contacto.isNotBlank()) Text("Contacto: ${c.contacto}", style = GType.bodySm, color = GColors.TextSecondary)
                accionClub(yo, c.conAprobacion, c.tipo, estaLleno(c))?.let { a ->
                    BotonPrimario(a, { vm.accionPrincipal(aviso) }, Modifier.testTag("accion-club"), enabled = yo?.activo != false && !(yo == null && estaLleno(c)), icono = null, color = if (yo == null) GColors.Primary else GColors.SurfaceHigh)
                }
            }
            if (c.tipo == TipoClub.comunidad) {
                Text("Grupos (${datos.grupos.size})", style = GType.titleMd, color = GColors.TextPrimary)
                if (datos.grupos.isEmpty()) Text("Esta comunidad aún no tiene grupos.", style = GType.bodySm, color = GColors.TextSecondary)
                datos.grupos.forEach { g -> TarjetaClub(g) { onClub(g.id) } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Eventos", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
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
                CampoTexto("Escribe a los miembros", texto, vm::escribir, "Escribe algo a los miembros...", lineas = 2)
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
            Row {
                if (onReportar != null && yo?.rol != RolClub.owner) TextButton(onClick = { onReportar(c) }) { Text(if (c.tipo == TipoClub.comunidad) "Reportar comunidad" else "Reportar grupo", color = GColors.TextSecondary) }
                Spacer(Modifier.weight(1f))
                if (yo?.rol == RolClub.owner) TextButton(onClick = { borrando = true }) { Text("Eliminar", color = GColors.Danger) }
            }
            if (borrando) com.gacelas.ui.components.DialogoC2(
                onDismissRequest = { borrando = false },
                containerColor = GColors.BgSecondary,
                title = { Text(if (c.tipo == TipoClub.comunidad) "Eliminar comunidad" else "Eliminar grupo") },
                text = { Text("Se borran los miembros y las publicaciones. No se puede deshacer.", color = GColors.TextSecondary) },
                confirmButton = { com.gacelas.ui.components.BotonModalC2("Eliminar", { borrando = false; vm.eliminar(aviso) }, peligro = true) },
                dismissButton = { com.gacelas.ui.components.BotonModalC2("Cancelar", { borrando = false }, secundario = true) },
            )
        }
    }
}

/** Calendario mensual con los eventos (lunes a domingo). */
@Composable
fun CalendarioEventos(eventos: List<Evento>, inscritos: Set<String>, onEvento: (String) -> Unit) {
    val hoy = remember { Fechas.fecha(ahoraMs()) }
    var anio by remember { mutableIntStateOf(hoy.year) }
    var mes by remember { mutableIntStateOf(hoy.monthNumber - 1) }
    var elegido by remember { mutableStateOf<List<Evento>>(emptyList()) }
    val semanas = remember(anio, mes, eventos) { calendarioMes(anio, mes, eventos) }
    val nombreMes = remember(anio, mes) { "${nombreMesEs(mes)} $anio" }
    fun mover(d: Int) { val t = anio * 12 + mes + d; anio = t.floorDiv(12); mes = t.mod(12); elegido = emptyList() }
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

/**
 * Mapa en vivo de todos los corredores del evento, clasificacion y repeticion. [mapa] dibuja el mapa
 * de la plataforma (MapLibre) con los trazos, los circulos y los puntos a encuadrar; null si no hay mapa.
 */
@Composable
fun EventoEnVivoContenido(
    vm: EventoEnVivoViewModel,
    onAtras: () -> Unit,
    onCorrer: () -> Unit,
    mapa: (@Composable (trazos: List<TrazoComun>, circulos: List<CirculoComun>, encuadre: List<Coord>) -> Unit)?,
) {
    val tabla by vm.tabla.collectAsState()
    val repeticion by vm.repeticion.collectAsState()
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras("Evento en vivo", onAtras)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(16.dp)).background(GColors.SurfaceContainer)) {
                if (mapa != null) {
                    // Encuadrar a todos los corredores (posiciones y trazos) cuando llegan datos.
                    val puntos = tabla.flatMap { k -> k.puntos + listOfNotNull(k.lat?.let { la -> k.lng?.let { la to it } }) }.map { Coord(it.first, it.second) }
                    val trazos = mutableListOf<TrazoComun>()
                    val circulos = mutableListOf<CirculoComun>()
                    tabla.forEachIndexed { i, k ->
                        val trazo = k.puntos.let { p -> repeticion?.let { r -> p.take(maxOf(1, (r * p.size).toInt())) } ?: p }.map { Coord(it.first, it.second) }
                        if (trazo.size >= 2) trazos += TrazoComun(trazo, colorCorredorArgb(i), 4f)
                        val pos = repeticion?.let { posicionRepeticion(k, it) } ?: k.lat?.let { la -> k.lng?.let { la to it } }
                        pos?.let { circulos += CirculoComun(Coord(it.first, it.second), 8f, colorCorredorArgb(i), bordeDp = 1.5f) }
                    }
                    mapa(trazos, circulos, puntos)
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
                    Text("%.2f km".fmt(k.distanciaM / 1000), style = GType.labelLg, color = GColors.TextPrimary)
                }
            }
        }
    }
}
