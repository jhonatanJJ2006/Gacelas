package com.gacelas.social

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

private fun icono(l: Logro) = when (l) {
    Logro.PrimeraCarrera -> Icons.Filled.DirectionsRun
    Logro.Km5 -> Icons.Filled.Flag
    Logro.Km10 -> Icons.Filled.MilitaryTech
    Logro.Racha7 -> Icons.Filled.LocalFireDepartment
}

/** Logros en Perfil y modal C2 cuando una carrera nueva desbloquea uno. */
@Composable
fun ListaLogros(carreras: List<Carrera>) {
    val repo = contenedor().social
    var hechos by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var cola by remember { mutableStateOf<List<Logro>>(emptyList()) }
    LaunchedEffect(carreras) {
        val nuevos = repo.desbloquearNuevos(carreras)
        if (nuevos.isNotEmpty()) cola = cola + nuevos
        hechos = repo.desbloqueados()
    }
    cola.firstOrNull()?.let { l ->
        ModalC2(
            icono = icono(l), titulo = "Logro desbloqueado: ${l.titulo}", texto = l.descripcion,
            primario = "Genial", onPrimario = { cola = cola.drop(1) }, onCerrar = { cola = cola.drop(1) }, tag = "modal-logro",
        )
    }
    val fecha = remember { SimpleDateFormat("d MMM yyyy", Locale("es", "EC")) }
    Tarjeta(Modifier.testTag("logros")) {
        Text("LOGROS", style = GType.titleMd, color = GColors.TextPrimary)
        Logro.entries.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { l ->
                    val f = hechos[l.codigo]
                    Column(
                        Modifier.weight(1f).background(if (f != null) GColors.C2S2 else GColors.C2Card)
                            .border(1.dp, if (f != null) GColors.C2Violeta else GColors.C2Linea).padding(10.dp).testTag("logro-${l.codigo}"),
                    ) {
                        Icon(icono(l), null, tint = if (f != null) GColors.C2Violeta else GColors.C2Muted)
                        Text(l.titulo, style = GType.labelLg, color = if (f != null) GColors.TextPrimary else GColors.C2Muted)
                        Text(if (f != null) "Desbloqueado el ${fecha.format(Date(f))}" else l.descripcion, style = GType.bodySm, color = GColors.C2Muted)
                    }
                }
            }
        }
    }
}
