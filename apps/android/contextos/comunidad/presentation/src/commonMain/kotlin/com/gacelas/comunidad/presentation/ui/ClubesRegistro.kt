package com.gacelas.comunidad.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.gacelas.comunidad.domain.reglas.estaLleno
import com.gacelas.ui.components.BotonChipC2
import com.gacelas.ui.components.BotonSecundario
import com.gacelas.ui.components.EtiquetaC2
import com.gacelas.ui.components.FormaBotonC2
import com.gacelas.ui.components.FormaC2
import com.gacelas.ui.components.TituloSeccion
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gacelas.comunidad.domain.modelo.BorradorClub
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.NivelGrupo
import com.gacelas.comunidad.domain.modelo.TipoClub
import com.gacelas.comunidad.domain.reglas.DIAS_CORTOS
import com.gacelas.comunidad.domain.reglas.LIMITE_CREACION
import com.gacelas.comunidad.domain.reglas.LIMITE_DIARIO
import com.gacelas.comunidad.domain.reglas.filtrarClubes
import com.gacelas.comunidad.domain.reglas.textoHorario
import com.gacelas.comunidad.domain.reglas.textoRitmo
import com.gacelas.comunidad.presentation.ClubesViewModel
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.Interruptor
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Selector de punto de encuentro de la plataforma: recibe el punto actual y devuelve el elegido. */
typealias SelectorPunto = @Composable (lat: Double?, lng: Double?, onElegir: (Double, Double) -> Unit) -> Unit

@Composable
internal fun SinServidorClubes() = Text("Los grupos y comunidades necesitan conexión con el servidor.", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.padding(16.dp))

@Composable
private fun Chip(texto: String, activo: Boolean, modifier: Modifier = Modifier, compacto: Boolean = false, onClick: () -> Unit) {
    val forma = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
    Box(
        modifier.clip(forma).background(if (activo) GColors.Primary.copy(alpha = 0.35f) else GColors.SurfaceContainer)
            .border(1.dp, if (activo) GColors.Primary400 else GColors.BorderSubtle, forma).clickable(onClick = onClick).padding(horizontal = if (compacto) 2.dp else 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(texto, style = GType.labelMd, color = if (activo) GColors.TextPrimary else GColors.TextSecondary, maxLines = 1) }
}

@Composable
internal fun LogoClub(c: Club, tam: Int = 44) = Box(
    Modifier.size(tam.dp).clip(RoundedCornerShape(12.dp)).background(GColors.Primary.copy(alpha = 0.25f)),
    contentAlignment = Alignment.Center,
) {
    Text(c.nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase(), style = GType.titleMd, color = GColors.Primary400)
}

/** Lista con pestañas Grupos / Comunidades, búsqueda y filtro de nivel; el + de la barra abre el registro. */
@Composable
fun ClubesContenido(
    vm: ClubesViewModel,
    conServidor: Boolean,
    onAtras: () -> Unit,
    onClub: (String) -> Unit,
    onAviso: (String) -> Unit,
    selectorPunto: SelectorPunto? = null,
    tipoInicial: TipoClub = TipoClub.grupo,
) {
    val datos by vm.datos.collectAsState()
    val error by vm.error.collectAsState()
    val solicitados by vm.solicitados.collectAsState()
    var tipo by remember { mutableStateOf(tipoInicial) }
    var q by remember { mutableStateOf("") }
    var nivel by remember { mutableStateOf<NivelGrupo?>(null) }
    var creando by remember { mutableStateOf<TipoClub?>(null) }
    creando?.let { t ->
        FormularioClubContenido(
            titulo = if (t == TipoClub.comunidad) "Registrar comunidad" else "Registrar grupo",
            inicial = BorradorClub(tipo = t),
            comunidades = datos?.first.orEmpty().filter { it.tipo == TipoClub.comunidad },
            permitirTipo = true,
            selectorPunto = selectorPunto,
            onCancelar = { creando = null },
            onGuardar = { b -> vm.crear(b, onCreado = { creando = null; onClub(it) }, onError = onAviso) },
        )
        return
    }
    val esGrupo = tipo == TipoClub.grupo
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        // El + vive dentro de la barra (respeta la barra de estado) en lugar de flotar sobre ella.
        EncabezadoAtras("Grupos y comunidades", onAtras, accion = if (!conServidor) null else {
            {
                IconButton(
                    onClick = { creando = tipo },
                    modifier = Modifier.padding(end = 8.dp).size(40.dp).clip(FormaBotonC2).background(GColors.C2Morado).testTag("crear-club"),
                ) { Icon(Icons.Filled.Add, if (esGrupo) "Registrar grupo" else "Registrar comunidad", tint = GColors.SobreColor) }
            }
        })
        if (!conServidor) return SinServidorClubes()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val todos = datos?.first.orEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val nG = todos.count { it.tipo == TipoClub.grupo }
                val nC = todos.count { it.tipo == TipoClub.comunidad }
                Chip(if (datos != null && nG > 0) "Grupos · $nG" else "Grupos", esGrupo, Modifier.weight(1f).testTag("pestana-grupos")) { tipo = TipoClub.grupo }
                Chip(if (datos != null && nC > 0) "Comunidades · $nC" else "Comunidades", !esGrupo, Modifier.weight(1f).testTag("pestana-comunidades")) { tipo = TipoClub.comunidad }
            }
            CampoTexto(
                "Buscar", q, { q = it.take(60) }, mostrarEtiqueta = false,
                placeholder = "",
                inicio = { Icon(Icons.Filled.Search, null, tint = GColors.TextSecondary) },
                trailing = if (q.isEmpty()) null else {
                    { IconButton(onClick = { q = "" }, modifier = Modifier.testTag("limpiar-busqueda")) { Icon(Icons.Filled.Close, "Limpiar búsqueda", tint = GColors.TextSecondary) } }
                },
            )
            if (esGrupo) Row(Modifier.horizontalScrollCompat().testTag("filtros-nivel"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("Todos", nivel == null) { nivel = null }
                NivelGrupo.entries.drop(1).forEach { n -> Chip(n.etiqueta, nivel == n) { nivel = if (nivel == n) null else n } }
            }
            error?.let { e -> return@Column ErrorClubes(e, vm::recargar) }
            val (_, mios) = datos ?: return@Column EsqueletoClubes()
            val lista = filtrarClubes(todos, tipo, q, if (esGrupo) nivel else null)
            val comunidades = todos.filter { it.tipo == TipoClub.comunidad }.associateBy { it.id }
            val gruposPorComunidad = todos.filter { it.comunidadId != null }.groupingBy { it.comunidadId!! }.eachCount()
            val propios = lista.filter { it.id in mios }
            val otros = lista.filter { it.id !in mios }
            val filtrando = q.isNotBlank() || (esGrupo && nivel != null)
            val tarjeta: @Composable (Club, Boolean) -> Unit = { c, mio ->
                TarjetaClub(
                    c, comunidades[c.comunidadId]?.nombre, grupos = gruposPorComunidad[c.id], mio = mio,
                    solicitado = c.id in solicitados, onUnirme = { vm.unirse(c, onAviso) },
                ) { onClub(c.id) }
            }
            if (propios.isNotEmpty()) {
                TituloSeccion(if (esGrupo) "Mis grupos" else "Mis comunidades")
                propios.forEach { tarjeta(it, true) }
            }
            when {
                lista.isEmpty() && filtrando -> VacioClubes(
                    Icons.Filled.SearchOff, "Sin resultados",
                    if (esGrupo) "No encontramos grupos con esa búsqueda o nivel." else "No encontramos comunidades con esa búsqueda.",
                    "Limpiar filtros", principal = false,
                ) { q = ""; nivel = null }
                todos.none { it.tipo == tipo } -> VacioClubes(
                    Icons.Filled.Groups,
                    if (esGrupo) "Aún no hay grupos" else "Aún no hay comunidades",
                    if (esGrupo) "Crea el primero: elige punto de encuentro, días, hora y nivel para que otros corredores se sumen."
                    else "Crea la primera para reunir a los grupos de tu ciudad en un solo lugar.",
                    if (esGrupo) "Crear grupo" else "Crear comunidad",
                ) { creando = tipo }
                otros.isNotEmpty() -> {
                    TituloSeccion("Descubrir")
                    otros.forEach { tarjeta(it, false) }
                }
                else -> Text(
                    if (esGrupo) "Ya eres parte de todos los grupos. ¿Creas uno nuevo?" else "Ya eres parte de todas las comunidades.",
                    style = GType.bodySm, color = GColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun Modifier.horizontalScrollCompat() = this.then(Modifier.horizontalScroll(rememberScrollState()))

/** Estado vacío ilustrado con una acción principal (crear) o secundaria (limpiar filtros). */
@Composable
private fun VacioClubes(icono: ImageVector, titulo: String, texto: String, accion: String, principal: Boolean = true, onAccion: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp, start = 8.dp, end = 8.dp).testTag("vacio-clubes"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(96.dp).clip(FormaC2).background(GColors.C2Morado.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
            Icon(icono, null, tint = GColors.C2Violeta, modifier = Modifier.size(48.dp))
        }
        Text(titulo, style = GType.headlineSm, color = GColors.TextPrimary, textAlign = TextAlign.Center)
        Text(texto, style = GType.bodyMd, color = GColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        if (principal) BotonPrimario(accion, onAccion, Modifier.testTag("vacio-accion"), icono = Icons.Filled.Add)
        else BotonSecundario(accion, onAccion, Modifier.testTag("vacio-accion"), color = GColors.Primary400)
    }
}

/** Error de carga con reintento. */
@Composable
private fun ErrorClubes(mensaje: String, onReintentar: () -> Unit) = Tarjeta(Modifier.testTag("error-clubes")) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.CloudOff, null, tint = GColors.Danger, modifier = Modifier.size(32.dp))
        Text(mensaje, style = GType.bodyMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
    }
    BotonPrimario("Reintentar", onReintentar, Modifier.padding(top = 4.dp).testTag("reintentar-clubes"), icono = Icons.Filled.Refresh)
}

/** Esqueleto de carga con la forma de las tarjetas. */
@Composable
private fun EsqueletoClubes() {
    val pulso by rememberInfiniteTransition(label = "esqueleto").animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alfa")
    val tono = GColors.SurfaceHigh.copy(alpha = pulso)
    Column(Modifier.testTag("cargando-clubes"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(tono))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.fillMaxWidth(0.6f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(tono))
                        Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(tono))
                    }
                }
                Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(tono))
            }
        }
    }
}

@Composable
private fun DatoTarjeta(icono: ImageVector, texto: String) = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Icon(icono, null, tint = GColors.C2Violeta, modifier = Modifier.size(16.dp))
    Text(texto, style = GType.bodySm, color = GColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * Tarjeta C2 de grupo o comunidad: nombre, nivel, miembros, horario, ritmo y punto de encuentro
 * (solo los datos que tiene el club). [onUnirme] muestra el botón Unirme en los que no soy miembro.
 */
@Composable
internal fun TarjetaClub(
    c: Club,
    comunidad: String? = null,
    grupos: Int? = null,
    mio: Boolean = false,
    solicitado: Boolean = false,
    onUnirme: (() -> Unit)? = null,
    onClick: () -> Unit,
) = Tarjeta(Modifier.testTag("tarjeta-club"), onClick = onClick) {
    val esGrupo = c.tipo == TipoClub.grupo
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LogoClub(c, 48)
        Column(Modifier.weight(1f)) {
            EtiquetaC2(
                listOfNotNull(
                    if (esGrupo) c.nivel.etiqueta else "Comunidad",
                    comunidad,
                ).joinToString(" · "),
                color = if (esGrupo) GColors.C2Violeta else GColors.Secondary500,
            )
            Text(c.nombre, style = GType.titleMd, color = GColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (mio) EtiquetaC2("Miembro", color = GColors.Secondary500)
    }
    val miembros = if (c.cupo != null) "${c.miembros}/${c.cupo} miembros" else "${c.miembros} ${if (c.miembros == 1) "miembro" else "miembros"}"
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DatoTarjeta(Icons.Filled.Groups, miembros)
        if (esGrupo) textoRitmo(c.ritmoSegKm)?.let { DatoTarjeta(Icons.Filled.Speed, it) }
        else grupos?.let { DatoTarjeta(Icons.AutoMirrored.Filled.DirectionsRun, "$it ${if (it == 1) "grupo" else "grupos"}") }
    }
    if (esGrupo && (c.dias.isNotEmpty() || c.hora != null)) DatoTarjeta(Icons.Filled.Schedule, textoHorario(c.dias, c.hora))
    val lugar = if (esGrupo) c.lugarEncuentro.ifBlank { c.ciudad } else c.ciudad
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { if (lugar.isNotBlank()) DatoTarjeta(Icons.Filled.Place, lugar) }
        if (!mio && onUnirme != null) {
            val lleno = estaLleno(c)
            when {
                solicitado -> BotonChipC2("Solicitud enviada", {}, activo = true)
                lleno -> BotonChipC2("Grupo lleno", {}, activo = true)
                else -> BotonChipC2(if (c.conAprobacion) "Solicitar" else "Unirme", onUnirme, Modifier.testTag("unirme-club"))
            }
        }
    }
}

/** Registro y edicion de comunidades y grupos (mismos campos que la web). */
@Composable
fun FormularioClubContenido(
    titulo: String,
    inicial: BorradorClub,
    comunidades: List<Club>,
    permitirTipo: Boolean,
    selectorPunto: SelectorPunto?,
    onCancelar: () -> Unit,
    onGuardar: (BorradorClub) -> Unit,
    textoGuardar: String = "Registrar",
) {
    var b by remember { mutableStateOf(inicial) }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras(titulo, onCancelar)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (permitirTipo) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Grupo de running", b.tipo == TipoClub.grupo, Modifier.weight(1f)) { b = b.copy(tipo = TipoClub.grupo) }
                Chip("Comunidad", b.tipo == TipoClub.comunidad, Modifier.weight(1f)) { b = b.copy(tipo = TipoClub.comunidad) }
            }
            CampoTexto("Nombre", b.nombre, { b = b.copy(nombre = it.take(60)) }, if (b.tipo == TipoClub.grupo) "Gacelas del Jipiro" else "Runners Loja")
            CampoTexto("Descripción", b.descripcion, { b = b.copy(descripcion = it.take(500)) }, "¿Quiénes son y cómo entrenan?", lineas = 3)
            CampoTexto("Ciudad o zona", b.ciudad, { b = b.copy(ciudad = it.take(80)) }, "Loja")
            CampoTexto("Contacto (opcional)", b.contacto, { b = b.copy(contacto = it.take(200)) }, "@usuario, WhatsApp o web")
            if (b.tipo == TipoClub.grupo) {
                if (comunidades.isNotEmpty()) {
                    Text("Comunidad (opcional)", style = GType.labelMd, color = GColors.TextSecondary)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip("Ninguna", b.comunidadId == null) { b = b.copy(comunidadId = null) }
                        comunidades.forEach { c -> Chip(c.nombre, b.comunidadId == c.id) { b = b.copy(comunidadId = c.id) } }
                    }
                }
                CampoTexto("Punto de encuentro", b.lugarEncuentro, { b = b.copy(lugarEncuentro = it.take(120)) }, "Parque Jipiro, entrada principal")
                if (selectorPunto != null) Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(GColors.SurfaceContainer)) {
                    selectorPunto(b.lat, b.lng) { la, lo -> b = b.copy(lat = la, lng = lo) }
                }
                Text(if (b.lat != null) "Punto marcado en el mapa" else "Toca el mapa para marcar el punto", style = GType.bodySm, color = GColors.TextSecondary)
                Text("Días", style = GType.labelMd, color = GColors.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DIAS_CORTOS.forEachIndexed { i, d ->
                        val dia = i + 1
                        Chip(d, dia in b.dias, Modifier.weight(1f), compacto = true) { b = b.copy(dias = if (dia in b.dias) b.dias - dia else b.dias + dia) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { CampoTexto("Hora", b.hora, { b = b.copy(hora = it.take(5)) }, "06:00") }
                    Box(Modifier.weight(1f)) { CampoTexto("Ritmo (min/km)", b.ritmo, { b = b.copy(ritmo = it.take(5)) }, "6:30") }
                }
                Text("Nivel", style = GType.labelMd, color = GColors.TextSecondary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NivelGrupo.entries.forEach { n -> Chip(n.etiqueta, b.nivel == n) { b = b.copy(nivel = n) } }
                }
                CampoTexto("Cupo (opcional)", b.cupo, { b = b.copy(cupo = it.filter(Char::isDigit).take(4)) }, "Sin límite", tipo = KeyboardType.Number)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Privado, con solicitud", color = GColors.TextPrimary)
                    Text("Apruebas a cada persona antes de que entre", style = GType.bodySm, color = GColors.TextSecondary)
                }
                Interruptor(b.conAprobacion) { b = b.copy(conAprobacion = it) }
            }
            Text(
                "Puedes registrar hasta ${LIMITE_CREACION[TipoClub.comunidad]} comunidades y ${LIMITE_CREACION[TipoClub.grupo]} grupos, y $LIMITE_DIARIO por día. Quedas como fundador y administrador.",
                style = GType.bodySm, color = GColors.TextSecondary,
            )
            BotonPrimario(textoGuardar, { onGuardar(b) }, Modifier.testTag("guardar-club"), icono = null)
        }
    }
}
