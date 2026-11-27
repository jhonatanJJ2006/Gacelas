package com.gacelas.social

import com.gacelas.ui.movimiento.entradaLista
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.ui.components.AvisosC2
import com.gacelas.ui.components.ModalC2
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Modal C2 para reportar una publicacion ajena. */
@Composable
fun ModalReporte(
    postId: String, repo: SocialRepository,
    tipo: com.gacelas.social.TipoContenido = com.gacelas.social.TipoContenido.Post, onCerrar: () -> Unit,
) {
    val motivos = com.gacelas.social.motivosReporte(tipo)
    var motivo by remember { mutableStateOf(motivos.first().first) }
    var detalle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    ModalC2(
        icono = Icons.Filled.Flag, titulo = tipo.titulo,
        texto = "Cuéntanos qué pasa. El equipo de moderación lo revisará; la persona no sabrá quién la reportó.",
        primario = "Enviar reporte", peligro = true, secundario = "Cancelar", tag = "modal-reporte",
        onCerrar = onCerrar,
        onPrimario = {
            scope.launch {
                val ok = repo.reportar(postId, motivo, detalle.trim(), tipo)
                AvisosC2.mostrar(if (ok) "Reporte enviado. Gracias por cuidar la comunidad" else "No se pudo enviar el reporte. Revisa tu conexión", error = !ok)
                onCerrar()
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            motivos.forEach { (v, etiqueta) ->
                val sel = motivo == v
                Row(
                    Modifier.fillMaxWidth().background(if (sel) GColors.C2S2 else GColors.C2Card)
                        .border(1.dp, if (sel) GColors.C2Violeta else GColors.C2Linea).clickable { motivo = v }.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(sel, { motivo = v }, colors = RadioButtonDefaults.colors(selectedColor = GColors.C2Violeta))
                    Text(etiqueta, style = GType.bodyMd, color = GColors.TextPrimary)
                }
            }
            androidx.compose.material3.OutlinedTextField(
                value = detalle, onValueChange = { if (it.length <= 500) detalle = it },
                label = { Text("Detalle (opcional)") }, minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("reporte-detalle"),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GColors.C2Violeta, unfocusedBorderColor = GColors.C2Linea,
                    focusedTextColor = GColors.TextPrimary, unfocusedTextColor = GColors.TextPrimary,
                    focusedContainerColor = GColors.C2S2, unfocusedContainerColor = GColors.C2S2,
                    focusedLabelColor = GColors.C2Violeta, unfocusedLabelColor = GColors.C2Muted, cursorColor = GColors.C2Violeta,
                ),
            )
        }
    }
}

/** Escucha la conectividad del sistema y muestra un modal C2 cuando se pierde la red. */
@Composable
fun ModalSinConexion() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var sinRed by remember { mutableStateOf(false) }
    var descartado by remember { mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val cb = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { sinRed = false; descartado = false }
            override fun onLost(network: android.net.Network) {
                val activa = cm?.activeNetwork
                if (activa == null || activa == network) sinRed = true
            }
        }
        val registrado = runCatching { cm?.registerDefaultNetworkCallback(cb) }.isSuccess && cm != null
        onDispose { if (registrado) runCatching { cm?.unregisterNetworkCallback(cb) } }
    }
    if (sinRed && !descartado) ModalSinConexionContenido { descartado = true }
}

@Composable
fun ModalSinConexionContenido(onCerrar: () -> Unit) {
    ModalC2(
        icono = Icons.Filled.CloudOff, titulo = "Sin conexión",
        texto = "No hay internet. Puedes seguir corriendo: tu carrera se guarda en el teléfono y se sincroniza cuando vuelva la señal.",
        primario = "Entendido", onPrimario = onCerrar, onCerrar = onCerrar, tag = "modal-sin-conexion",
    )
}

private fun icono(l: Logro): androidx.compose.ui.graphics.vector.ImageVector = when (l) {
    Logro.PrimeraCarrera -> Icons.AutoMirrored.Filled.DirectionsRun
    Logro.Km5 -> Icons.Filled.Flag
    Logro.Km10 -> Icons.Filled.MilitaryTech
    Logro.Km15 -> Icons.Filled.EmojiEvents
    Logro.Km21, Logro.Km42 -> Icons.Filled.WorkspacePremium
    Logro.Total50, Logro.Total100, Logro.Total500, Logro.Total1000 -> Icons.Filled.Route
    Logro.Racha3, Logro.Racha7, Logro.Racha30 -> Icons.Filled.LocalFireDepartment
    Logro.Carreras10, Logro.Carreras50 -> Icons.Filled.Repeat
    Logro.SemanasMeta4 -> Icons.Filled.EventAvailable
    Logro.Ritmo6, Logro.Ritmo5, Logro.Ritmo430 -> Icons.Filled.Speed
    Logro.Escalador -> Icons.Filled.Terrain
    Logro.Desnivel1000, Logro.Desnivel5000 -> Icons.Filled.Landscape
    Logro.Explorador -> Icons.Filled.Explore
    Logro.Madrugador -> Icons.Filled.WbTwilight
    Logro.Nocturno -> Icons.Filled.NightsStay
    Logro.FinDeSemana -> Icons.Filled.Weekend
}

private fun colorCategoria(c: com.gacelas.social.CategoriaLogro) = when (c) {
    com.gacelas.social.CategoriaLogro.Distancia -> GColors.C2Violeta
    com.gacelas.social.CategoriaLogro.Constancia -> androidx.compose.ui.graphics.Color(0xFFF97316)
    com.gacelas.social.CategoriaLogro.Velocidad -> GColors.C2Menta
    com.gacelas.social.CategoriaLogro.Exploracion -> GColors.C2Cielo
    com.gacelas.social.CategoriaLogro.Horario -> androidx.compose.ui.graphics.Color(0xFFFBBF24)
}

/** Logros guardados y la cola de recien ganados (modal); comparte estado entre Perfil y la pantalla de logros. */
@Composable
private fun estadoLogros(carreras: List<Carrera>): Map<String, Long> {
    val repo = contenedor().social
    var hechos by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var cola by remember { mutableStateOf<List<Logro>>(emptyList()) }
    LaunchedEffect(carreras) {
        val nuevos = repo.desbloquearNuevos(carreras)
        if (nuevos.isNotEmpty()) cola = cola + nuevos
        hechos = repo.desbloqueados()
    }
    // Varios a la vez (p. ej. al estrenar logros nuevos): un solo aviso en lugar de una fila de modales.
    if (cola.size > 2) ModalC2(
        icono = Icons.Filled.EmojiEvents, titulo = "${cola.size} logros nuevos", texto = cola.joinToString(", ") { it.titulo } + ".",
        primario = "Genial", onPrimario = { cola = emptyList() }, onCerrar = { cola = emptyList() }, tag = "modal-logro",
    ) else cola.firstOrNull()?.let { l ->
        ModalC2(
            icono = icono(l), titulo = "Logro desbloqueado: ${l.titulo}", texto = l.descripcion,
            primario = "Genial", onPrimario = { cola = cola.drop(1) }, onCerrar = { cola = cola.drop(1) }, tag = "modal-logro",
        )
    }
    return hechos
}

@Composable
private fun DetalleLogro(l: Logro, carreras: List<Carrera>, hecho: Long?, onCerrar: () -> Unit) {
    val fecha = remember { SimpleDateFormat("d MMM yyyy", Locale("es", "EC")) }
    val actual = com.gacelas.social.progresoLogro(l, carreras, System.currentTimeMillis())
    com.gacelas.ui.components.HojaInsigniaC2(
        icono(l), l.titulo, l.descripcion, l.comoConseguirlo,
        actual = if (hecho != null) l.meta else actual, meta = l.meta, unidad = l.unidad,
        fecha = hecho?.let { fecha.format(Date(it)) }, lograda = hecho != null || actual >= l.meta, onCerrar = onCerrar, color = colorCategoria(l.categoria),
    )
}

@Composable
private fun CeldaLogro(l: Logro, carreras: List<Carrera>, hecho: Long?, modifier: Modifier, onClick: () -> Unit) {
    val actual = com.gacelas.social.progresoLogro(l, carreras, System.currentTimeMillis())
    val lograda = hecho != null || actual >= l.meta
    com.gacelas.ui.components.InsigniaC2(
        icono(l), l.titulo, if (lograda) "Conseguido" else "$actual/${l.meta}", lograda, actual.toFloat() / l.meta, onClick,
        modifier, colorCategoria(l.categoria), tag = "logro-${l.codigo}",
    )
}

/** Logros en Perfil: los 6 mas avanzados (ganados primero), cada uno abre su detalle; "Ver todos" abre la pantalla. */
@Composable
fun ListaLogros(carreras: List<Carrera>, onVerTodos: () -> Unit = {}) {
    val hechos = estadoLogros(carreras)
    TarjetaLogros(carreras, hechos, onVerTodos)
}

/** La tarjeta de Perfil sin estado propio (la pintan tambien las pruebas de capturas). */
@Composable
fun TarjetaLogros(carreras: List<Carrera>, hechos: Map<String, Long>, onVerTodos: () -> Unit = {}) {
    var abierto by remember { mutableStateOf<Logro?>(null) }
    val ahora = remember { System.currentTimeMillis() }
    val orden = remember(carreras, hechos) {
        Logro.entries.sortedByDescending { l -> if (l.codigo in hechos) 2f else com.gacelas.social.progresoLogro(l, carreras, ahora).toFloat() / l.meta }.take(6)
    }
    Tarjeta(Modifier.testTag("logros")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LOGROS", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("${hechos.size} de ${Logro.entries.size} logros", style = GType.labelMd, color = GColors.C2Muted)
        }
        orden.chunked(3).forEachIndexed { filaIdx, fila ->
            Row(Modifier.entradaLista(filaIdx), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { l -> CeldaLogro(l, carreras, hechos[l.codigo], Modifier.weight(1f)) { abierto = l } }
                repeat(3 - fila.size) { Box(Modifier.weight(1f)) }
            }
        }
        com.gacelas.ui.components.BotonSecundario("Ver todos los logros", onVerTodos, Modifier.testTag("ver-logros"), color = GColors.Primary400)
    }
    abierto?.let { l -> DetalleLogro(l, carreras, hechos[l.codigo]) { abierto = null } }
}

/** Todos los logros por categoria; los bloqueados muestran su progreso y como conseguirlos. */
@Composable
fun LogrosScreen(onAtras: () -> Unit) {
    val carreras by com.gacelas.ui.carrerasState()
    LogrosContenido(carreras, estadoLogros(carreras), onAtras)
}

/** La lista completa sin estado propio (la pintan tambien las pruebas de capturas). */
@Composable
fun LogrosContenido(carreras: List<Carrera>, hechos: Map<String, Long>, onAtras: () -> Unit) {
    var abierto by remember { mutableStateOf<Logro?>(null) }
    Column(Modifier.background(com.gacelas.ui.components.Tablero.Fondo)) {
        com.gacelas.ui.components.EncabezadoAtras("Logros", onAtras)
        com.gacelas.ui.components.Pantalla {
            Text("${hechos.size} de ${Logro.entries.size} conseguidos", style = GType.bodyMd, color = GColors.C2Muted, modifier = Modifier.testTag("logros-resumen"))
            com.gacelas.social.CategoriaLogro.entries.forEach { cat ->
                val delGrupo = Logro.entries.filter { it.categoria == cat }
                Text(cat.titulo, style = GType.titleMd, color = colorCategoria(cat))
                delGrupo.chunked(3).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEach { l -> CeldaLogro(l, carreras, hechos[l.codigo], Modifier.weight(1f)) { abierto = l } }
                        repeat(3 - fila.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
    abierto?.let { l -> DetalleLogro(l, carreras, hechos[l.codigo]) { abierto = null } }
}
