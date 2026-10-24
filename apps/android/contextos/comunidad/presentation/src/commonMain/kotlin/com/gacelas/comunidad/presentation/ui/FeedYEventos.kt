package com.gacelas.comunidad.presentation.ui

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import com.gacelas.comunidad.domain.modelo.nivelCorredor
import androidx.compose.material.icons.automirrored.filled.Send
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
    /** Foto remota (depende de la plataforma). */
    imagen: @Composable (url: String, modifier: Modifier) -> Unit = { _, mod -> Box(mod.background(GColors.SurfaceHigh)) },
    /** Abre el selector de fotos para una carrera propia; null si la plataforma no lo soporta. */
    onAgregarFotos: ((id: String) -> Unit)? = null,
    eventoClub: Evento? = null,
    inscritoClub: Boolean = false,
    onAsistir: () -> Unit = {},
    onEventoClub: () -> Unit = {},
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
                Text("COMUNIDAD", style = GType.headlineLg, color = GColors.TextPrimary)
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
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Pestanas C2: texto en mayusculas cursiva con subrayado violeta en la activa.
            @Composable
            fun Segmento(texto: String, activo: Boolean, onClick: () -> Unit) {
                Column(Modifier.weight(1f).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                        Text(texto.uppercase(), style = GType.labelLg.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = androidx.compose.ui.text.font.FontWeight.Black), color = if (activo) GColors.TextPrimary else GColors.TextSecondary)
                    }
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (activo) GColors.C2Violeta else GColors.C2Linea))
                }
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
        val kms = remember(lista) { lista.groupBy { if (it.propia) "yo" else it.autorId ?: it.autor }.mapValues { (_, l) -> l.sumOf { it.distanciaKm } } }
        lista.forEachIndexed { indice, p ->
            if (indice == 1 && eventoClub != null) TarjetaClub(eventoClub, inscritoClub, onAsistir, onEventoClub)
            val gusta = if (p.remota) p.meGusta else p.id in ajustes.likes
            val likes = if (p.remota) p.likesBase else p.likesBase + if (gusta) 1 else 0
            val locales = if (p.remota) emptyList() else ajustes.comentarios[p.id].orEmpty()
            var menu by remember { mutableStateOf(false) }
            val texto = "${p.titulo}: ${Unidades.texto(p.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)} en ${formatearMs(p.segundos)} con Gacelas Loja"
            // Publicacion compacta estilo X (C2): avatar a la izquierda, contenido y metricas en linea.
            Row(Modifier.fillMaxWidth().testTag("post-c2").padding(vertical = 10.dp)) {
              AvatarCirculo(40.dp, p.indice)
              Spacer(Modifier.width(10.dp))
              Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.autor, style = GType.labelLg, color = GColors.TextPrimary, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                            Spacer(Modifier.width(6.dp))
                            InsigniaNivel(nivelCorredor(p.autorKm ?: kms[if (p.propia) "yo" else p.autorId ?: p.autor] ?: 0.0))
                        }
                        Text(p.cuando, style = GType.bodySm, color = GColors.TextSecondary)
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
                            if (p.propia && p.remota && onAgregarFotos != null) {
                                DropdownMenuItem(text = { Text("Agregar fotos") }, onClick = { menu = false; onAgregarFotos(p.id) })
                            }
                            DropdownMenuItem(text = { Text("Ocultar publicación") }, onClick = {
                                menu = false
                                vm.ocultar(p.id)
                                onAviso("Publicación oculta")
                            })
                        }
                    }
                }
                Text(p.titulo, style = GType.bodyMd, color = GColors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
                    @Composable
                    fun Dato(valor: String, unidad: String, color: androidx.compose.ui.graphics.Color) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(valor, style = GType.titleMd, color = color)
                            Text(unidad, style = GType.bodySm, color = GColors.TextSecondary)
                        }
                    }
                    Dato(Unidades.texto(p.distanciaKm, m, 1), Unidades.etiquetaDistancia(m), GColors.C2Violeta)
                    Dato(formatearRitmo(Unidades.ritmo(p.ritmoSegKm, m)), "/${Unidades.etiquetaDistancia(m)}", GColors.TextPrimary)
                    Dato(formatearMs(p.segundos), "", GColors.TextPrimary)
                    Dato("${p.desnivelM}", "m+", GColors.C2Cielo)
                }
                CarruselPost(p, imagen)
                locales.takeLast(2).forEach { Text("Tú: $it", style = GType.bodySm, color = GColors.TextSecondary) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.alternarLike(p.id, onAviso) }) {
                        Icon(if (gusta) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (gusta) "Quitar me gusta" else "Me gusta", tint = if (gusta) GColors.Primary500 else GColors.TextSecondary)
                    }
                    Text("$likes", style = GType.labelLg, color = GColors.TextSecondary)
                    IconButton(onClick = { comentando = p }) { Icon(Icons.AutoMirrored.Outlined.Chat, "Comentar", tint = GColors.TextSecondary) }
                    Text("${p.comentariosBase + locales.size}", style = GType.labelLg, color = GColors.TextSecondary)
                    IconButton(onClick = { vm.alternarCompartida(p.id, onAviso) }) {
                        Icon(Icons.Filled.Repeat, if (p.compartida) "Dejar de compartir" else "Compartir en mi perfil", tint = if (p.compartida) GColors.Secondary500 else GColors.TextSecondary)
                    }
                    Text("${p.compartidos}", style = GType.labelLg, color = if (p.compartida) GColors.Secondary500 else GColors.TextSecondary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onCompartir(texto) }) { Icon(Icons.Filled.Share, "Compartir", tint = GColors.TextSecondary) }
                }
                p.ultimoComentario?.let { (autor, cuerpo) ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceLow).clickable { comentando = p }.padding(10.dp),
                    ) {
                        Text("$autor  $cuerpo", style = GType.bodySm, color = GColors.TextPrimary, maxLines = 2)
                        if (p.comentariosBase > 1) Text("Ver los ${p.comentariosBase} comentarios", style = GType.bodySm, color = GColors.Primary400)
                    }
                }
              }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(GColors.C2Linea))
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
    var tipo by rememberSaveable { mutableStateOf<String?>(null) }
    val lista = vm.visibles(eventos, inscritos).filter { tipo == null || it.tipo == tipo }
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
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TIPOS_EVENTO.forEach { (k, v) ->
                val activo = tipo == k
                Text(v, style = GType.labelMd, color = if (activo) GColors.Primary500 else GColors.TextSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, if (activo) GColors.Primary else GColors.SurfaceHigh, RoundedCornerShape(8.dp))
                        .clickable { tipo = if (activo) null else k }.padding(horizontal = 12.dp, vertical = 6.dp).testTag("tipo-$k"))
            }
        }
        if (calendario) CalendarioEventos(eventos, inscritos, onEvento)
        if (lista.isEmpty()) Tarjeta { Text(if (pestana == PestanaEventos.MisEventos) "Aún no te inscribes a ningún evento." else "No hay eventos.", style = GType.bodyMd, color = GColors.TextSecondary) }
        lista.forEachIndexed { i, e ->
            if (i == 1) Text("Más eventos locales", style = GType.titleMd, color = GColors.TextPrimary)
            val inscribir = {
                vm.alternarInscripcion(e.id)
                onAviso(if (e.id in inscritos) "Inscripción cancelada" else "Te inscribiste a ${e.titulo}")
            }
            if (i == 0) EventoPrincipal(e, e.id in inscritos, onClick = { onEvento(e.id) }, onInscribir = inscribir)
            else TarjetaEvento(e, e.id in inscritos, onClick = { onEvento(e.id) }, onInscribir = inscribir)
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

private val TIPOS_EVENTO = listOf("grupal" to "Carrera grupal", "carrera" to "Competencia", "entrenamiento" to "Entrenamiento")
private fun tipoTexto(t: String) = TIPOS_EVENTO.firstOrNull { it.first == t }?.second ?: "Carrera grupal"

@Composable
private fun EventoPrincipal(e: Evento, inscrito: Boolean, onClick: () -> Unit, onInscribir: () -> Unit) {
    val total = e.participantes + if (inscrito && !e.creadoPorUsuario) 1 else 0
    val lleno = e.cupo != null && total >= e.cupo!!
    Tarjeta(padding = 0.dp, onClick = onClick, borde = GColors.Primary.copy(alpha = 0.5f)) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(Brush.horizontalGradient(listOf(GColors.Primary, GColors.Secondary500))))
        Column(Modifier.padding(16.dp).testTag("evento-principal"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Evento principal", style = GType.labelMd, color = GColors.Primary500, modifier = Modifier.weight(1f))
                Text(if (e.yaPaso()) "Finalizado" else if (lleno) "Lista de espera" else "Inscripción abierta", style = GType.labelMd, color = GColors.Secondary500)
            }
            Text(e.titulo, style = GType.headlineMd, color = GColors.TextPrimary)
            if (e.creadoPorUsuario) Text("Organizas tú", style = GType.labelMd, color = GColors.Primary500)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarToday, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
                Text(" ${e.fecha} • ${e.hora}", style = GType.bodySm, color = GColors.TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
                Text(" ${e.ubicacion}", style = GType.bodySm, color = GColors.TextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Distancia" to "${e.distanciaKm} km", "Tipo" to tipoTexto(e.tipo), "Cupo" to (e.cupo?.let { "$total/$it" } ?: "Libre")).forEach { (t, v) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceHigh).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(t, style = GType.labelMd, color = GColors.TextSecondary)
                        Text(v, style = GType.labelLg, color = GColors.Primary500, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$total participantes", style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                if (!e.yaPaso()) Text(if (inscrito) "Inscrito" else "Inscribirse", style = GType.labelLg, color = GColors.TextPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (inscrito) GColors.Secondary else GColors.Primary).clickable(onClick = onInscribir).padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun TarjetaEvento(e: Evento, inscrito: Boolean, onClick: () -> Unit, onInscribir: () -> Unit) {
    Tarjeta(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dm = e.inicioMs?.let { com.gacelas.carreras.domain.reglas.EstadisticasPerfil.diaYMes(it) }
            Column(Modifier.width(56.dp).clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceHigh).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${dm?.first ?: "--"}", style = GType.headlineMd, color = GColors.TextPrimary)
                Text(dm?.second?.uppercase() ?: "", style = GType.labelMd, color = GColors.Primary500)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${e.hora} • ${e.ubicacion}", style = GType.bodySm, color = GColors.TextSecondary, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(tipoTexto(e.tipo), style = GType.labelMd, color = GColors.Primary500)
                }
                Text(e.titulo, style = GType.titleMd, color = GColors.TextPrimary)
                Text("${e.distanciaKm} km" + if (e.creadoPorUsuario) " • Organizas tú" else "", style = GType.bodySm, color = GColors.TextSecondary)
            }
        }
        val total0 = e.participantes + if (inscrito && !e.creadoPorUsuario) 1 else 0
        e.cupo?.let { cupo ->
            Row { Text("Cupos confirmados", style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f)); Text("${minOf(total0, cupo)}/$cupo cupos", style = GType.labelMd, color = GColors.Primary500) }
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(GColors.SurfaceHigh)) {
                Box(Modifier.fillMaxWidth((total0.toFloat() / cupo).coerceIn(0f, 1f)).height(6.dp).background(GColors.Primary))
            }
        }
        Column {
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

/** Insignia de nivel del corredor (misma regla que la web). */
@Composable
fun InsigniaNivel(nivel: String) {
    Row(
        Modifier.clip(CircleShape).background(GColors.Secondary.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = GColors.Secondary500, modifier = Modifier.size(12.dp))
        Text(" $nivel", style = GType.labelMd, color = GColors.Secondary500, maxLines = 1)
    }
}

/** Carrusel: trazado de la ruta (con desnivel) y fotos de la publicacion. */
@Composable
private fun CarruselPost(p: Publicacion, imagen: @Composable (String, Modifier) -> Unit) {
    val mapa = p.puntos.size >= 2 || p.imagenes.isEmpty()
    val total = p.imagenes.size + if (mapa) 1 else 0
    val estado = androidx.compose.foundation.pager.rememberPagerState { total }
    Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(4.dp))) {
        androidx.compose.foundation.pager.HorizontalPager(estado, Modifier.fillMaxSize()) { pagina ->
            if (mapa && pagina == 0) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(GColors.MapParks, GColors.SurfaceLow))), contentAlignment = Alignment.Center) {
                    if (p.puntos.size >= 2) TrazadoRuta(p.puntos, Modifier.fillMaxSize()) else Icon(Icons.Filled.Route, null, tint = GColors.Primary500, modifier = Modifier.size(56.dp))
                    if (p.desnivelM > 0) Text(
                        "Desnivel +${p.desnivelM} m", style = GType.labelMd, color = GColors.TextPrimary,
                        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.8f)).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            } else {
                imagen(p.imagenes[pagina - if (mapa) 1 else 0], Modifier.fillMaxSize())
            }
        }
        if (total > 1) Row(Modifier.align(Alignment.BottomCenter).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(total) { i ->
                Box(Modifier.size(if (i == estado.currentPage) 10.dp else 6.dp, 6.dp).clip(CircleShape).background(if (i == estado.currentPage) GColors.Primary400 else GColors.TextPrimary.copy(alpha = 0.6f)))
            }
        }
    }
}

/** Comunicado del club con el proximo encuentro y boton Asistire (event_rsvps). */
@Composable
private fun TarjetaClub(e: Evento, inscrito: Boolean, onAsistir: () -> Unit, onAbrir: () -> Unit) {
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(GColors.Primary), contentAlignment = Alignment.Center) {
                Text("GL", style = GType.labelLg, color = GColors.OnPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Club Gacelas Loja", style = GType.labelLg, color = GColors.TextPrimary)
                Text("Comunicado oficial", style = GType.bodySm, color = GColors.TextSecondary)
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceLow).clickable(onClick = onAbrir).padding(12.dp)) {
            Text(e.titulo, style = GType.titleMd, color = GColors.TextPrimary)
            Text(e.descripcion, style = GType.bodySm, color = GColors.TextSecondary)
            Text("${e.fecha}, ${e.hora}${if (e.ubicacion.isNotBlank()) " · ${e.ubicacion}" else ""}", style = GType.labelMd, color = GColors.Primary400)
        }
        Row(
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (inscrito) GColors.Secondary else GColors.Primary).clickable(onClick = onAsistir),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (inscrito) Icons.Filled.CheckCircle else Icons.Filled.EventAvailable, null, tint = GColors.OnPrimary, modifier = Modifier.size(18.dp))
            Text(" ${if (inscrito) "Asistiré" else "Asistir"} (${e.participantes + if (inscrito) 1 else 0} confirmados)", style = GType.labelLg, color = GColors.OnPrimary)
        }
    }
}
