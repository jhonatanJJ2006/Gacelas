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
    /** Reporta una publicacion ajena; null si no hay servidor. */
    onReportar: ((id: String) -> Unit)? = null,
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
    val italica = androidx.compose.ui.text.font.FontStyle.Italic
    val negra = androidx.compose.ui.text.font.FontWeight.Black
    // Feed C2 (frame Movil / Comunidad / Feed): cabecera centrada, pestanas, compositor y posts estilo X a todo el ancho.
    Column(Modifier.fillMaxSize().background(GColors.C2Bg).verticalScroll(rememberScrollState()).testTag("feed-c2")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarLetra("Tú", 34.dp)
            Text(
                "COMUNIDAD", style = GType.headlineMd.copy(fontStyle = italica, fontWeight = negra), color = GColors.TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f),
            )
            Box {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(GColors.C2Card).clickable { menuFiltro = true }.testTag("boton-buscar-feed"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Search, "Buscar y filtrar", tint = GColors.TextPrimary, modifier = Modifier.size(20.dp)) }
                DropdownMenu(expanded = menuFiltro, onDismissRequest = { menuFiltro = false }) {
                    FiltroFeed.entries.forEach { f ->
                        DropdownMenuItem(text = { Text(f.etiqueta) }, onClick = { vm.filtro(f); menuFiltro = false })
                    }
                    DropdownMenuItem(text = { Text("Corredores en vivo") }, onClick = { menuFiltro = false; onEnVivo() })
                    DropdownMenuItem(text = { Text("Eventos") }, onClick = { menuFiltro = false; onEventos() })
                    DropdownMenuItem(text = { Text("Actualizar") }, onClick = { menuFiltro = false; vm.refrescar() })
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            @Composable
            fun Segmento(texto: String, activo: Boolean, onClick: () -> Unit) {
                Column(Modifier.weight(1f).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                        Text(texto.uppercase(), style = GType.labelLg.copy(fontStyle = italica, fontWeight = negra), color = if (activo) GColors.TextPrimary else GColors.C2Muted)
                    }
                    Box(Modifier.fillMaxWidth().height(if (activo) 3.dp else 1.dp).background(if (activo) GColors.C2Violeta else GColors.C2Linea))
                }
            }
            Segmento("Para ti", filtro == FiltroFeed.Todos) { vm.filtro(FiltroFeed.Todos) }
            Segmento("Siguiendo", filtro == FiltroFeed.Siguiendo) { vm.filtro(FiltroFeed.Siguiendo) }
            if (onClubes != null) Segmento("Clubes", false, onClubes) else Segmento("Eventos", false, onEventos)
        }
        // Compositor: las carreras se publican al guardarlas; aqui se explica y se abre el filtro de las propias.
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp).testTag("compositor-feed")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarLetra("Tú", 36.dp)
                Spacer(Modifier.width(12.dp))
                Text("¿Cómo te fue hoy?", style = GType.bodyLg, color = GColors.C2Muted, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(start = 48.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Image, "Foto", tint = GColors.C2Violeta, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(18.dp))
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, "Carrera", tint = GColors.C2Violeta, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(18.dp))
                Icon(Icons.Filled.Map, "Ruta", tint = GColors.C2Violeta, modifier = Modifier.size(20.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    "PUBLICAR", style = GType.labelLg.copy(fontStyle = italica, fontWeight = negra), color = GColors.TextPrimary,
                    modifier = Modifier.clip(com.gacelas.ui.components.FormaBotonC2).background(androidx.compose.ui.graphics.Color(0xFF7B1FA2))
                        .clickable { vm.filtro(FiltroFeed.Mias); onAviso("Tus carreras se publican al guardarlas. Desde el menú de cada una puedes agregar fotos.") }
                        .padding(horizontal = 18.dp, vertical = 9.dp).testTag("boton-publicar"),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(GColors.C2Linea))
        if (filtro == FiltroFeed.Comunidad || filtro == FiltroFeed.Mias) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { Pastilla(filtro.etiqueta, true, { vm.filtro(FiltroFeed.Todos) }) }
        }
        if (lista.isEmpty()) Text(
            when (filtro) {
                FiltroFeed.Siguiendo -> "Las carreras de quienes sigues aparecerán aquí."
                FiltroFeed.Mias -> "Aún no tienes carreras públicas."
                else -> "No hay publicaciones todavía."
            },
            style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.padding(16.dp),
        )
        val kms = remember(lista) { lista.groupBy { if (it.propia) "yo" else it.autorId ?: it.autor }.mapValues { (_, l) -> l.sumOf { it.distanciaKm } } }
        lista.forEachIndexed { indice, p ->
            if (indice == 1 && eventoClub != null) Box(Modifier.padding(16.dp)) { TarjetaClub(eventoClub, inscritoClub, onAsistir, onEventoClub) }
            val gusta = if (p.remota) p.meGusta else p.id in ajustes.likes
            val likes = if (p.remota) p.likesBase else p.likesBase + if (gusta) 1 else 0
            val locales = if (p.remota) emptyList() else ajustes.comentarios[p.id].orEmpty()
            var menu by remember { mutableStateOf(false) }
            val texto = "${p.titulo}: ${Unidades.texto(p.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)} en ${formatearMs(p.segundos)} con Gacelas Loja"
            val hilo = p.ultimoComentario != null
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp).testTag("post-c2")) {
                Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AvatarLetra(p.autor, 36.dp)
                    if (hilo) Box(Modifier.padding(top = 6.dp).width(2.dp).weight(1f).background(GColors.C2Linea))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.autor, style = GType.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = GColors.TextPrimary, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(6.dp))
                        InsigniaNivel(nivelCorredor(p.autorKm ?: kms[if (p.propia) "yo" else p.autorId ?: p.autor] ?: 0.0))
                        Spacer(Modifier.width(6.dp))
                        Text("${handle(p.autor)} · ${p.cuando}", style = GType.bodySm, color = GColors.C2Muted, maxLines = 1, modifier = Modifier.weight(1f))
                        Box {
                            Icon(Icons.Filled.MoreHoriz, "Más opciones", tint = GColors.C2Muted, modifier = Modifier.size(18.dp).clickable { menu = true })
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                p.autorId?.let { autor ->
                                    val sigo = autor in siguiendo
                                    DropdownMenuItem(text = { Text(if (sigo) "Dejar de seguir" else "Seguir a ${p.autor}") }, onClick = {
                                        menu = false
                                        vm.alternarSeguir(autor)
                                        if (!sigo) com.gacelas.ui.components.AvisosC2.mostrar("Ahora sigues a ${p.autor}", error = false)
                                    })
                                }
                                DropdownMenuItem(text = { Text("Compartir") }, onClick = { menu = false; onCompartir(texto) })
                                if (p.propia && p.remota && onAgregarFotos != null) {
                                    DropdownMenuItem(text = { Text("Agregar fotos") }, onClick = { menu = false; onAgregarFotos(p.id) })
                                }
                                DropdownMenuItem(text = { Text("Ocultar publicación") }, onClick = {
                                    menu = false
                                    vm.ocultar(p.id)
                                    onAviso("Publicación oculta")
                                })
                                if (!p.propia && onReportar != null) {
                                    DropdownMenuItem(text = { Text("Reportar publicación", color = GColors.C2Peligro) }, onClick = { menu = false; onReportar(p.id) })
                                }
                            }
                        }
                    }
                    Text(p.titulo, style = GType.bodyMd, color = GColors.TextPrimary)
                    if (p.distanciaKm > 0) Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
                        @Composable
                        fun Dato(valor: String, unidad: String, color: androidx.compose.ui.graphics.Color) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(valor, style = GType.titleMd.copy(fontStyle = italica, fontWeight = negra), color = color)
                                Text(unidad, style = GType.bodySm, color = GColors.C2Muted)
                            }
                        }
                        Dato(Unidades.texto(p.distanciaKm, m, 1), Unidades.etiquetaDistancia(m), GColors.C2Violeta)
                        Dato(formatearRitmo(Unidades.ritmo(p.ritmoSegKm, m)), "/${Unidades.etiquetaDistancia(m)}", GColors.TextPrimary)
                        Dato(formatearMs(p.segundos), "", GColors.TextPrimary)
                        Dato("${p.desnivelM}", "m+", GColors.C2Cielo)
                    }
                    if (p.distanciaKm > 0 || p.imagenes.isNotEmpty()) MediaPost(p, imagen)
                    locales.takeLast(2).forEach { Text("Tú: $it", style = GType.bodySm, color = GColors.TextSecondary) }
                    Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        @Composable
                        fun Accion(icono: androidx.compose.ui.graphics.vector.ImageVector, desc: String, valor: Int, color: androidx.compose.ui.graphics.Color, tag: String, onClick: () -> Unit) {
                            Row(Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(vertical = 6.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icono, desc, tint = color, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("$valor", style = GType.labelLg, color = color)
                            }
                        }
                        Accion(Icons.AutoMirrored.Outlined.Chat, "Comentar", p.comentariosBase + locales.size, GColors.C2Muted, "accion-comentar") { comentando = p }
                        Accion(Icons.Filled.Repeat, if (p.compartida) "Dejar de compartir" else "Compartir en mi perfil", p.compartidos, if (p.compartida) GColors.C2Menta else GColors.C2Muted, "accion-repost") { vm.alternarCompartida(p.id, onAviso) }
                        Accion(if (gusta) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (gusta) "Quitar me gusta" else "Me gusta", likes, if (gusta) GColors.C2Violeta else GColors.C2Muted, "accion-like") { vm.alternarLike(p.id, onAviso) }
                        Icon(Icons.Filled.IosShare, "Compartir", tint = GColors.C2Muted, modifier = Modifier.size(18.dp).clickable { onCompartir(texto) })
                    }
                    p.ultimoComentario?.let { (autor, cuerpo) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GColors.C2Card).clickable { comentando = p }.padding(10.dp),
                        ) {
                            AvatarLetra(autor, 26.dp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(autor, style = GType.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = GColors.TextPrimary, maxLines = 1)
                                Text(cuerpo, style = GType.bodySm, color = GColors.TextPrimary, maxLines = 2)
                            }
                        }
                        if (p.comentariosBase > 1) Text("Ver los ${p.comentariosBase} comentarios", style = GType.labelLg, color = GColors.C2Violeta, modifier = Modifier.clickable { comentando = p })
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(GColors.C2Linea))
        }
        Spacer(Modifier.height(16.dp))
    }
    comentando?.let { p ->
        var texto by remember(p.id) { mutableStateOf("") }
        val hilo by produceState(emptyList<com.gacelas.comunidad.domain.modelo.ComentarioFeed>(), p.id) { value = vm.comentarios(p.id) }
        com.gacelas.ui.components.DialogoC2(
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
            dismissButton = { com.gacelas.ui.components.BotonModalC2("Cancelar", { comentando = null }, secundario = true) },
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
    var uniendo by remember { mutableStateOf<Evento?>(null) }
    var calendario by rememberSaveable { mutableStateOf(false) }
    var tipo by rememberSaveable { mutableStateOf<String?>(null) }
    val lista = vm.visibles(eventos, inscritos).filter { tipo == null || it.tipo == tipo }
    Pantalla {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("EVENTOS", style = GType.headlineLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black), color = GColors.TextPrimary, modifier = Modifier.weight(1f))
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
                if (e.id in inscritos) { vm.alternarInscripcion(e.id); onAviso("Inscripción cancelada") }
                else uniendo = e
            }
            if (i == 0) EventoPrincipal(e, e.id in inscritos, onClick = { onEvento(e.id) }, onInscribir = inscribir)
            else TarjetaEvento(e, e.id in inscritos, onClick = { onEvento(e.id) }, onInscribir = inscribir)
        }
    }
    uniendo?.let { e ->
        com.gacelas.ui.components.ModalC2(
            icono = Icons.Filled.Event, titulo = "Unirse al evento",
            texto = "${e.titulo} · ${e.fecha} ${e.hora} · ${e.ubicacion}",
            primario = "Inscribirme", onCerrar = { uniendo = null }, secundario = "Ahora no", tag = "modal-unirse",
            onPrimario = { uniendo = null; vm.alternarInscripcion(e.id); onAviso("Te inscribiste a ${e.titulo}") },
        ) {
            com.gacelas.ui.components.ResumenModalC2(listOf("${e.distanciaKm} km" to "Distancia", "${e.participantes}" to "Inscritos", (e.cupo?.toString() ?: "Libre") to "Cupo"))
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
    com.gacelas.ui.components.DialogoC2(
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
        dismissButton = { com.gacelas.ui.components.BotonModalC2("Cancelar", onCerrar, secundario = true) },
        containerColor = GColors.BgSecondary,
    )
}

private val TIPOS_EVENTO = listOf("grupal" to "Carrera grupal", "carrera" to "Competencia", "entrenamiento" to "Entrenamiento")
private fun tipoTexto(t: String) = TIPOS_EVENTO.firstOrNull { it.first == t }?.second ?: "Carrera grupal"

@Composable
private fun EventoPrincipal(e: Evento, inscrito: Boolean, onClick: () -> Unit, onInscribir: () -> Unit) {
    val total = e.participantes + if (inscrito && !e.creadoPorUsuario) 1 else 0
    val lleno = e.cupo != null && total >= e.cupo!!
    Tarjeta(padding = 0.dp, onClick = onClick, fondo = GColors.C2Card) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(listOf(GColors.C2Morado, GColors.C2Violeta))))
        Column(Modifier.padding(16.dp).testTag("evento-principal"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Evento principal", style = GType.labelMd, color = GColors.Primary500, modifier = Modifier.weight(1f))
                Text(if (e.yaPaso()) "Finalizado" else if (lleno) "Lista de espera" else "Inscripción abierta", style = GType.labelMd, color = GColors.Secondary500)
            }
            Text(e.titulo, style = GType.headlineMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black), color = GColors.TextPrimary)
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
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(GColors.C2S2).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(t.uppercase(), style = GType.labelMd.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)), color = GColors.C2Muted)
                        Text(v, style = GType.labelLg, color = GColors.C2Violeta, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$total participantes", style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                if (!e.yaPaso()) com.gacelas.ui.components.BotonChipC2(if (inscrito) "Inscrito" else "Inscribirse", onInscribir, activo = inscrito)
            }
        }
    }
}

@Composable
private fun TarjetaEvento(e: Evento, inscrito: Boolean, onClick: () -> Unit, onInscribir: () -> Unit) {
    // C2: bloque de fecha, titular Anton en cursiva, datos en una linea y boton inclinado.
    val total = e.participantes + if (inscrito && !e.creadoPorUsuario) 1 else 0
    Tarjeta(onClick = onClick, fondo = GColors.C2Card, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dm = e.inicioMs?.let { com.gacelas.carreras.domain.reglas.EstadisticasPerfil.diaYMes(it) }
            com.gacelas.ui.components.FechaC2("${dm?.first ?: "--"}", dm?.second ?: "")
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(e.titulo, style = GType.titleMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black), color = GColors.TextPrimary, maxLines = 2)
                Text("${e.hora} · ${e.ubicacion}", style = GType.bodySm, color = GColors.C2Muted, maxLines = 1)
                Text(
                    "${e.distanciaKm} km · ${tipoTexto(e.tipo)} · " +
                        (e.cupo?.let { "${minOf(total, it)}/$it cupos" } ?: "$total participantes") +
                        if (e.creadoPorUsuario) " · Organizas tú" else "",
                    style = GType.bodySm, color = GColors.C2Muted, maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (!e.yaPaso()) com.gacelas.ui.components.BotonChipC2(if (inscrito) "Inscrito" else "Inscribirse", onInscribir, activo = inscrito)
            else Text("Finalizado", style = GType.labelMd, color = GColors.C2Muted)
        }
        e.cupo?.let { cupo ->
            Box(Modifier.fillMaxWidth().height(3.dp).background(GColors.C2S2)) {
                Box(Modifier.fillMaxWidth((total.toFloat() / cupo).coerceIn(0f, 1f)).height(3.dp).background(GColors.C2Violeta))
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

/** Insignia de nivel C2: etiqueta corta en violeta, cursiva y con corte diagonal (misma regla que la web). */
@Composable
fun InsigniaNivel(nivel: String) {
    val corto = when {
        nivel.contains("lite") -> "ÉLITE"
        nivel.contains("Avanzada") -> "NV 3"
        nivel.contains("Intermedia") -> "NV 2"
        else -> "NV 1"
    }
    Text(
        corto, style = GType.labelMd.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = androidx.compose.ui.text.font.FontWeight.Black),
        color = GColors.C2Violeta, maxLines = 1,
        modifier = Modifier.clip(com.gacelas.ui.components.FormaBotonC2).background(GColors.C2Violeta.copy(alpha = 0.18f)).padding(horizontal = 6.dp, vertical = 1.dp).testTag("insignia-nivel"),
    )
}

/** Avatar C2: circulo violeta con la inicial. */
@Composable
internal fun AvatarLetra(nombre: String, tamano: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(tamano).clip(CircleShape).background(GColors.C2Violeta), contentAlignment = Alignment.Center) {
        Text(nombre.trim().firstOrNull()?.uppercase() ?: "G", style = GType.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black), color = GColors.C2Bg)
    }
}

/** Usuario visible tipo @nombre a partir del nombre del autor. */
internal fun handle(nombre: String): String {
    val base = nombre.lowercase().map { c -> when (c) { 'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú', 'ü' -> 'u'; 'ñ' -> 'n'; else -> c } }
        .filter { it.isLetterOrDigit() }.joinToString("")
    return "@" + base.take(14).ifEmpty { "gacela" }
}

/** Media del post (max 200 dp): ruta a la izquierda y hasta dos fotos apiladas a la derecha, como el frame C2. */
@Composable
private fun MediaPost(p: Publicacion, imagen: @Composable (String, Modifier) -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    val fotos = p.imagenes.take(2)
    Row(Modifier.fillMaxWidth().height(if (fotos.isEmpty()) 170.dp else 200.dp).clip(forma).border(1.dp, GColors.C2Linea, forma).testTag("media-post"), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.weight(if (fotos.isEmpty()) 1f else 1.15f).fillMaxHeight().background(Brush.linearGradient(listOf(GColors.MapParks, GColors.C2Card)))) {
            if (p.puntos.size >= 2) TrazadoRuta(p.puntos, Modifier.fillMaxSize()) else Icon(Icons.Filled.Route, null, tint = GColors.C2Violeta, modifier = Modifier.align(Alignment.Center).size(48.dp))
            Text("RUTA", style = GType.labelMd.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = androidx.compose.ui.text.font.FontWeight.Black), color = GColors.TextPrimary, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
        }
        if (fotos.isNotEmpty()) Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            fotos.forEach { imagen(it, Modifier.fillMaxWidth().weight(1f)) }
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
