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

/** Lista con pestañas Grupos / Comunidades, busqueda y filtro de nivel; el + abre el registro. */
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
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { EncabezadoAtras("Grupos y comunidades", onAtras) }
            if (conServidor) IconButton(onClick = { creando = tipo }, modifier = Modifier.padding(end = 12.dp).clip(CircleShape).background(GColors.Primary).testTag("crear-club")) {
                Icon(Icons.Filled.Add, if (tipo == TipoClub.comunidad) "Registrar comunidad" else "Registrar grupo", tint = GColors.TextPrimary)
            }
        }
        if (!conServidor) return SinServidorClubes()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Grupos", tipo == TipoClub.grupo, Modifier.weight(1f)) { tipo = TipoClub.grupo }
                Chip("Comunidades", tipo == TipoClub.comunidad, Modifier.weight(1f)) { tipo = TipoClub.comunidad }
            }
            CampoTexto("Buscar", q, { q = it.take(60) }, if (tipo == TipoClub.grupo) "Nombre, barrio o punto de encuentro" else "Nombre o ciudad", mostrarEtiqueta = false)
            if (tipo == TipoClub.grupo) Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("Todos", nivel == null) { nivel = null }
                NivelGrupo.entries.drop(1).forEach { n -> Chip(n.etiqueta, nivel == n) { nivel = if (nivel == n) null else n } }
            }
            val (todos, mios) = datos ?: return@Column Text("Cargando...", color = GColors.TextSecondary)
            val lista = filtrarClubes(todos, tipo, q, nivel)
            val comunidades = todos.filter { it.tipo == TipoClub.comunidad }.associateBy { it.id }
            val propios = lista.filter { it.id in mios }
            if (propios.isNotEmpty()) Text(if (tipo == TipoClub.grupo) "Mis grupos" else "Mis comunidades", style = GType.titleMd, color = GColors.TextPrimary)
            propios.forEach { TarjetaClub(it, comunidades[it.comunidadId]?.nombre) { onClub(it.id) } }
            Text("Descubrir", style = GType.titleMd, color = GColors.TextPrimary)
            val otros = lista.filter { it.id !in mios }
            if (otros.isEmpty()) Text(
                if (q.isNotBlank() || nivel != null) "No hay resultados con esos filtros."
                else if (tipo == TipoClub.grupo) "Aún no hay grupos. Registra el primero." else "Aún no hay comunidades. Registra la primera.",
                style = GType.bodySm, color = GColors.TextSecondary,
            )
            otros.forEach { TarjetaClub(it, comunidades[it.comunidadId]?.nombre) { onClub(it.id) } }
        }
    }
}

@Composable
private fun Modifier.horizontalScrollCompat() = this.then(Modifier.horizontalScroll(rememberScrollState()))

@Composable
internal fun TarjetaClub(c: Club, comunidad: String? = null, onClick: () -> Unit) = Tarjeta(Modifier.testTag("tarjeta-club"), onClick = onClick) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LogoClub(c)
        Column(Modifier.weight(1f)) {
            Text(c.nombre, style = GType.titleMd, color = GColors.TextPrimary, maxLines = 1)
            val miembros = if (c.cupo != null) "${c.miembros}/${c.cupo}" else "${c.miembros} ${if (c.miembros == 1) "miembro" else "miembros"}"
            val detalle = if (c.tipo == TipoClub.grupo) listOfNotNull(textoHorario(c.dias, c.hora), c.nivel.takeIf { it != NivelGrupo.todos }?.etiqueta, textoRitmo(c.ritmoSegKm))
            else listOf(c.ciudad)
            Text(detalle.joinToString(" · "), style = GType.bodySm, color = GColors.Primary400, maxLines = 1)
            Text(listOfNotNull(miembros, comunidad, if (c.conAprobacion) "Con solicitud" else null).joinToString(" · "), style = GType.bodySm, color = GColors.TextSecondary, maxLines = 1)
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
