package com.gacelas.incidentes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.legal.TipoConsentimiento
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

private val Ambar = Color(0xFFF5B54A)

fun colorNivel(n: NivelSeguridad) = when (n) {
    NivelSeguridad.Bajo -> GColors.C2Menta
    NivelSeguridad.Medio -> Ambar
    NivelSeguridad.Alto -> GColors.C2Peligro
}

fun textoNivel(n: NivelSeguridad) = when (n) {
    NivelSeguridad.Bajo -> "Bajo"
    NivelSeguridad.Medio -> "Medio"
    NivelSeguridad.Alto -> "Alto"
}

/** Color de un marcador de incidente en el mapa (verificados mas intensos). */
fun colorIncidente(i: IncidentePublico): Color = if (i.verificado) GColors.C2Peligro else Ambar

/**
 * Reencodifica la foto como JPEG desde el bitmap: el archivo nuevo no lleva EXIF (ni GPS, ni modelo
 * de telefono). Lado mayor maximo 1600 px y calidad 80 para quedar bajo 3 MB.
 */
fun limpiarFoto(bitmap: Bitmap, ladoMax: Int = 1600): ByteArray {
    val escala = minOf(1f, ladoMax.toFloat() / maxOf(bitmap.width, bitmap.height))
    val b = if (escala < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true) else bitmap
    var calidad = 80
    while (true) {
        val out = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, calidad, out)
        if (out.size() <= Incidentes.MAX_BYTES_FOTO || calidad <= 40) return out.toByteArray()
        calidad -= 15
    }
}

private fun leerFoto(context: Context, uri: Uri): ByteArray? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }?.let { limpiarFoto(it) }
}.getOrNull()

@Suppress("DEPRECATION")
private suspend fun direccionDe(context: Context, lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
    runCatching {
        android.location.Geocoder(context).getFromLocation(lat, lng, 1)?.firstOrNull()?.let { a ->
            listOfNotNull(a.thoroughfare, a.subLocality ?: a.locality).joinToString(", ").ifBlank { null }
        }
    }.getOrNull() ?: "Cerca de %.4f, %.4f".format(lat, lng)
}

@Composable
private fun TarjetaC2(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) = Column(
    modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GColors.C2Card)
        .border(1.dp, GColors.C2Linea, RoundedCornerShape(16.dp)).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
) { contenido() }

/** Tarjeta "Seguridad de la zona" del detalle de ruta. Sin backend muestra datos de ejemplo. */
@Composable
fun TarjetaSeguridad(rutaId: String, inicial: SeguridadZona? = null) {
    val repo = contenedor().incidentes
    var zona by remember(rutaId) { mutableStateOf(inicial ?: if (repo == null) Incidentes.EJEMPLO else null) }
    var cargando by remember(rutaId) { mutableStateOf(zona == null) }
    LaunchedEffect(rutaId, repo) {
        if (inicial == null && repo != null) { zona = repo.seguridadZona(rutaId); cargando = false }
    }
    TarjetaC2(Modifier.testTag("tarjeta-seguridad")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Seguridad de la zona", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            if (zona?.ejemplo == true) Text("EJEMPLO", style = GType.labelMd, color = GColors.C2Violeta)
        }
        val z = zona
        when {
            cargando -> Text("Cargando…", style = GType.bodySm, color = GColors.C2Muted)
            z == null || z.dias.isEmpty() -> Text("Aún no hay datos para esta zona.", style = GType.bodySm, color = GColors.C2Muted)
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..6).forEach { d ->
                        val dia = z.dias.firstOrNull { it.dia == d }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp))
                                .background(dia?.let { colorNivel(it.nivel).copy(alpha = 0.85f) } ?: GColors.C2S2))
                            Text(Incidentes.DIAS_CORTOS[d], style = GType.labelMd, color = GColors.C2Muted)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    NivelSeguridad.entries.forEach { n ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(colorNivel(n)))
                            Text("  ${textoNivel(n)}", style = GType.bodySm, color = GColors.C2Muted)
                        }
                    }
                }
                if (z.tipos.isNotEmpty()) {
                    Text("Más frecuente", style = GType.labelLg, color = GColors.TextPrimary)
                    z.tipos.forEach { t ->
                        Row {
                            Text(t.subtipo.ifBlank { t.servicio }, style = GType.bodySm, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                            Text("${t.conteo}", style = GType.labelMd, color = GColors.C2Muted)
                        }
                    }
                }
                Text("Datos hasta ${z.actualizadoHasta ?: "—"} · ${z.fuente}", style = GType.bodySm, color = GColors.C2Muted)
            }
        }
        Text(Incidentes.AVISO, style = GType.bodySm, color = GColors.C2Muted)
    }
}

/** Boton "Reportar incidente" que abre la hoja. [posicion] da la ubicacion actual. */
@Composable
fun BotonReportarIncidente(modifier: Modifier = Modifier, compacto: Boolean = false, posicion: () -> Pair<Double, Double>?) {
    var abierta by remember { mutableStateOf(false) }
    Row(
        modifier.clip(RoundedCornerShape(50)).background(GColors.C2Card).border(1.dp, GColors.C2Peligro, RoundedCornerShape(50))
            .clickable { abierta = true }.padding(horizontal = if (compacto) 10.dp else 14.dp, vertical = 8.dp).testTag("boton-reportar-incidente"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.ReportProblem, "Reportar incidente", tint = GColors.C2Peligro, modifier = Modifier.size(18.dp))
        if (!compacto) Text("  Reportar incidente", style = GType.labelLg, color = GColors.TextPrimary)
    }
    if (abierta) HojaReportarIncidente(posicion(), onCerrar = { abierta = false })
}

/** Hoja inferior: direccion y hora se toman solas; solo se pide el tipo. Descripcion y fotos opcionales. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HojaReportarIncidente(posicion: Pair<Double, Double>?, onCerrar: () -> Unit, onEnviado: () -> Unit = {}) {
    val context = LocalContext.current
    val c = contenedor()
    val scope = rememberCoroutineScope()
    val ahora = remember { com.gacelas.shared.ahoraMs() }
    var direccion by remember { mutableStateOf(if (posicion == null) "Ubicación no disponible" else "Buscando dirección…") }
    var tipo by remember { mutableStateOf<TipoIncidente?>(null) }
    var descripcion by remember { mutableStateOf("") }
    var fotos by remember { mutableStateOf<List<ByteArray>>(emptyList()) }
    var estado by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    val elegir = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(Incidentes.MAX_FOTOS)) { uris ->
        scope.launch {
            val nuevas = withContext(Dispatchers.Default) { uris.mapNotNull { leerFoto(context, it) } }
            fotos = (fotos + nuevas).take(Incidentes.MAX_FOTOS)
        }
    }
    LaunchedEffect(posicion) { posicion?.let { direccion = direccionDe(context, it.first, it.second) } }
    ModalBottomSheet(onDismissRequest = onCerrar, containerColor = GColors.C2Bg, contentColor = GColors.TextPrimary) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp).testTag("hoja-reportar"),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reportar incidente", style = GType.headlineSm, color = GColors.TextPrimary)
            TarjetaC2 {
                Text(direccion, style = GType.bodyMd, color = GColors.TextPrimary)
                val hora = java.text.SimpleDateFormat("HH:mm", java.util.Locale("es", "EC")).format(java.util.Date(ahora))
                Text("Hoy, $hora · se registra la hora aproximada", style = GType.bodySm, color = GColors.C2Muted)
            }
            Text("¿Qué pasó?", style = GType.labelLg, color = GColors.TextPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoIncidente.entries.forEach { t ->
                    val sel = t == tipo
                    Text(
                        t.nombre, style = GType.labelLg, color = if (sel) GColors.C2Bg else GColors.TextPrimary,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) GColors.C2Violeta else GColors.C2Card)
                            .border(1.dp, if (sel) GColors.C2Violeta else GColors.C2Linea, RoundedCornerShape(50))
                            .clickable { tipo = t }.padding(horizontal = 12.dp, vertical = 8.dp).testTag("tipo-${t.id}"),
                    )
                }
            }
            OutlinedTextField(
                descripcion, { descripcion = it.take(280) }, Modifier.fillMaxWidth(),
                placeholder = { Text("Detalles (opcional)", color = GColors.C2Muted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GColors.TextPrimary, unfocusedTextColor = GColors.TextPrimary,
                    focusedBorderColor = GColors.C2Violeta, unfocusedBorderColor = GColors.C2Linea,
                    focusedContainerColor = GColors.C2Card, unfocusedContainerColor = GColors.C2Card,
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (fotos.isEmpty()) "Añadir fotos (opcional, máx. 3)" else "${fotos.size} de 3 fotos · sin datos de ubicación",
                    style = GType.bodySm, color = GColors.C2Cielo,
                    modifier = Modifier.weight(1f).clickable(enabled = fotos.size < Incidentes.MAX_FOTOS) {
                        elegir.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }.testTag("añadir-fotos"),
                )
                if (fotos.isNotEmpty()) Text("Quitar", style = GType.bodySm, color = GColors.C2Muted, modifier = Modifier.clickable { fotos = emptyList() })
            }
            estado?.let { Text(it, style = GType.bodySm, color = GColors.C2Muted) }
            val listo = tipo != null && posicion != null && !enviando
            Text(
                if (enviando) "Enviando…" else "Enviar reporte", style = GType.labelLg,
                color = if (listo) GColors.C2Bg else GColors.C2Muted,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(if (listo) GColors.C2Menta else GColors.C2S2)
                    .clickable(enabled = listo) {
                        val t = tipo ?: return@clickable
                        val p = posicion ?: return@clickable
                        val repo = c.incidentes
                        if (repo == null) { estado = "Vista previa: el reporte no se envía."; return@clickable }
                        enviando = true
                        scope.launch {
                            estado = if (!c.consentimientos.otorgado(TipoConsentimiento.GpsCarrera)) {
                                "Para reportar necesitas aceptar el uso de tu ubicación (Ajustes > Privacidad)."
                            } else runCatching {
                                repo.reportar(t, p.first, p.second, direccion, ahora, descripcion, fotos, UUID.randomUUID().toString())
                            }.fold({ onEnviado(); onCerrar(); null }, { "No se pudo enviar. Si reportaste varias veces, espera un rato." })
                            enviando = false
                        }
                    }.padding(vertical = 14.dp).testTag("enviar-reporte"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text("Tu reporte se publica sin tu nombre, con la ubicación redondeada (unos 100 m) y la hora aproximada.",
                style = GType.bodySm, color = GColors.C2Muted)
        }
    }
}

/** Incidentes recientes (vista anonima). Sin backend o en vista previa devuelve los de ejemplo. */
@Composable
fun incidentesRecientes(ejemplo: Boolean = false): List<IncidentePublico> {
    val repo = contenedor().incidentes
    var lista by remember { mutableStateOf(if (ejemplo || repo == null) Incidentes.ejemplos(com.gacelas.shared.ahoraMs()) else emptyList()) }
    LaunchedEffect(repo, ejemplo) { if (!ejemplo && repo != null) lista = repo.recientes() }
    return lista
}

@Composable
fun ListaIncidentes(lista: List<IncidentePublico>) {
    if (lista.isEmpty()) return
    val repo = contenedor().incidentes
    val scope = rememberCoroutineScope()
    val ahora = remember { com.gacelas.shared.ahoraMs() }
    var confirmados by remember { mutableStateOf(emptySet<String>()) }
    Text("Incidentes reportados", style = GType.labelLg, color = GColors.TextPrimary)
    lista.take(10).forEach { i ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GColors.C2Card).padding(12.dp).testTag("incidente-${i.id}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(colorIncidente(i)))
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(i.tipo.nombre, style = GType.labelLg, color = GColors.TextPrimary)
                Text("${Incidentes.haceTexto(i.ocurridoMs, ahora)} · ${i.confirmaciones + if (i.id in confirmados) 1 else 0} lo vieron" +
                    if (i.verificado) " · verificado" else "", style = GType.bodySm, color = GColors.C2Muted)
            }
            if (i.id !in confirmados) Text("Yo también lo vi", style = GType.labelMd, color = GColors.C2Cielo,
                modifier = Modifier.clickable {
                    scope.launch { if (repo == null || repo.confirmar(i.id)) confirmados = confirmados + i.id }
                }.padding(6.dp))
        }
    }
}
