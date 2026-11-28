package com.gacelas.administracion.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.administracion.domain.AlertaSos
import com.gacelas.administracion.domain.EscenarioVivo
import com.gacelas.administracion.domain.EstadoSla
import com.gacelas.administracion.domain.ImportacionEcu
import com.gacelas.administracion.domain.Prioridad
import com.gacelas.administracion.domain.RecuadroLoja
import com.gacelas.administracion.domain.ReporteAdmin
import com.gacelas.administracion.domain.RespuestaRapida
import com.gacelas.administracion.domain.SlaHoras
import com.gacelas.administracion.domain.etiquetaPlataforma
import com.gacelas.administracion.domain.estadoSla
import com.gacelas.administracion.domain.megas
import com.gacelas.administracion.domain.respuestaValida
import com.gacelas.administracion.domain.texto
import com.gacelas.ui.components.BotonModalC2
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.DialogoC2
import com.gacelas.ui.theme.GType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/*
 * Piezas del panel que llegaron con la migracion 050: mapa en vivo con SOS, prioridad y plazo de
 * los casos, respuestas rapidas, versiones de la app e importaciones del ECU 911.
 */

/** Hora actual del panel; las pruebas y capturas la fijan para que el plazo no dependa del dia. */
val LocalAhoraAdmin = staticCompositionLocalOf<() -> Instant> { { Clock.System.now() } }

/**
 * Exportar un archivo de texto desde el panel (nombre, tipo MIME, contenido). La app Android abre
 * el selector de compartir; si no se provee (iOS por ahora), el boton de exportar no aparece.
 */
val LocalExportarAdmin = staticCompositionLocalOf<((String, String, String) -> Unit)?> { null }

internal fun msDe(iso: String?): Long? = try {
    iso?.let { Instant.parse(it).toEpochMilliseconds() }
} catch (_: IllegalArgumentException) {
    null
}

// ---------- Mapa en vivo y SOS ----------

/**
 * Esquema de Loja con posiciones reales proyectadas al recuadro de la ciudad (como MapaEscenario
 * de la web): corredores en verde, incidentes por revisar en ambar y SOS activos en rojo. Sin datos
 * dice "Sin fuente" y no dibuja nada.
 */
@Composable
internal fun MapaVivo(escenario: EscenarioVivo?, casosAbiertos: String) {
    val lineas = Esc.Borde
    val rio = Esc.Cielo
    val menta = Esc.Menta
    val ambar = Esc.Ambar
    val peligro = Esc.Peligro
    val descripcion = if (escenario == null) "Mapa de Loja sin datos en vivo" else
        "Mapa de Loja: ${escenario.corredores.size} corredores, ${escenario.incidentes.size} incidentes por revisar, ${escenario.sosActivas.size} SOS activos"
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Esc.Card).testTag("resumen-mapa")) {
        Box(Modifier.fillMaxWidth().height(170.dp).background(Brush.verticalGradient(listOf(Esc.CardAlta, Esc.Card)))) {
            Canvas(Modifier.fillMaxWidth().height(170.dp).semantics { contentDescription = descripcion }) {
                for (i in 1..5) drawLine(lineas, Offset(size.width * i / 6f, 0f), Offset(size.width * i / 6f, size.height), 2f)
                for (i in 1..3) drawLine(lineas, Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), 2f)
                val p = Path().apply {
                    moveTo(0f, size.height * 0.75f)
                    cubicTo(size.width * 0.3f, size.height * 0.35f, size.width * 0.6f, size.height * 1.0f, size.width, size.height * 0.45f)
                }
                drawPath(p, rio.copy(alpha = 0.55f), style = Stroke(10f))
                if (escenario != null) {
                    fun punto(lat: Double, lng: Double, color: Color, r: Float) {
                        val (x, y) = RecuadroLoja.proyectar(com.gacelas.administracion.domain.PuntoGeo(lat, lng)) ?: return
                        drawCircle(color.copy(alpha = 0.25f), r * 2.2f, Offset(x * size.width, y * size.height))
                        drawCircle(color, r, Offset(x * size.width, y * size.height))
                    }
                    escenario.corredores.forEach { punto(it.lat, it.lng, menta, 6f) }
                    escenario.incidentes.forEach { punto(it.lat, it.lng, ambar, 6f) }
                    escenario.sosActivas.forEach { s -> s.posicion?.let { punto(it.lat, it.lng, peligro, 9f) } }
                }
            }
            val (texto, tono, tag) = if (escenario != null) Triple("EN VIVO", Tono.Menta, "mapa-en-vivo") else Triple("SIN FUENTE", Tono.Neutro, "mapa-sin-fuente")
            ChipEsc(texto, tono, Modifier.align(Alignment.TopStart).padding(10.dp).testTag(tag))
            if (escenario != null && escenario.sosActivas.isNotEmpty()) {
                ChipEsc("SOS activos: ${escenario.sosActivas.size}", Tono.Peligro, Modifier.align(Alignment.TopEnd).padding(10.dp).testTag("mapa-sos-activos"))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            CifraVivo(escenario?.corredores?.size?.toString() ?: "-", "Corredores", Esc.Menta)
            CifraVivo(escenario?.incidentes?.size?.toString() ?: "-", "Incidentes", Esc.Ambar)
            CifraVivo(escenario?.sosActivas?.size?.toString() ?: "-", "SOS activos", Esc.Peligro)
            CifraVivo(casosAbiertos, "Casos abiertos", Esc.Violeta)
        }
    }
}

@Composable
private fun CifraVivo(valor: String, etiqueta: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = GType.bodyMd.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold), color = color)
        Text(etiqueta, style = GType.bodySm, color = Esc.Texto2)
    }
}

/** Alertas SOS de las ultimas 24 h: las activas con su ultima posicion, las cerradas sin posicion. */
@Composable
internal fun AlertasSos(alertas: List<AlertaSos>) {
    val ahora = LocalAhoraAdmin.current()
    Text("SOS de las últimas 24 h", style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto)
    if (alertas.isEmpty()) {
        TarjetaEsc(Modifier.testTag("sos-vacio")) {
            Text("Sin alertas SOS", style = GType.bodyMd.copy(fontWeight = FontWeight.Bold), color = Esc.Texto)
            Text("Cuando alguien mantenga pulsado el botón SOS aparecerá aquí con su última posición.", style = GType.bodySm, color = Esc.Texto2)
        }
        return
    }
    alertas.forEach { a ->
        TarjetaEsc(
            Modifier.testTag("sos-${a.id}"),
            fondo = if (a.activa) Esc.Peligro.copy(alpha = 0.10f) else Esc.Card,
            borde = if (a.activa) Esc.Peligro.copy(alpha = 0.5f) else Esc.Borde,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ChipEsc(if (a.activa) "Activa" else "Cerrada", if (a.activa) Tono.Peligro else Tono.Neutro)
                Text(a.nombre, style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto, maxLines = 1, modifier = Modifier.weight(1f))
                Text(haceCuanto(a.iniciadaEn, ahora), style = GType.bodySm, color = Esc.Texto3)
            }
            val pos = a.posicion
            if (a.activa && pos != null) {
                Text("Última posición ${coord(pos.lat)}, ${coord(pos.lng)} · ${haceCuanto(a.ultimaEn ?: a.iniciadaEn, ahora)}", style = GType.bodySm, color = Esc.Texto2)
            } else if (a.activa) {
                Text("Todavía sin posición", style = GType.bodySm, color = Esc.Texto2)
            } else {
                Text("Terminó ${haceCuanto(a.terminadaEn ?: a.ultimaEn, ahora)}. La posición deja de mostrarse al cerrar la alerta.", style = GType.bodySm, color = Esc.Texto2)
            }
            Text("${a.puntos} posiciones enviadas", style = GType.bodySm, color = Esc.Texto3)
        }
    }
    Text("Ante una emergencia real, llama al 911.", style = GType.bodySm, color = Esc.Texto3)
}

private fun coord(v: Double): String {
    val n = kotlin.math.round(v * 10000) / 10000
    return n.toString()
}

// ---------- Prioridad, plazo y respuestas rapidas ----------

internal fun tonoPrioridad(p: Prioridad) = when (p) {
    Prioridad.Alta -> Tono.Peligro
    Prioridad.Media -> Tono.Ambar
    Prioridad.Baja -> Tono.Cielo
}

/** Chips de prioridad y plazo de un caso. */
@Composable
internal fun ChipsSla(r: ReporteAdmin) {
    val ahora = LocalAhoraAdmin.current().toEpochMilliseconds()
    ChipEsc("Prioridad ${r.prioridad.etiqueta.lowercase()}", tonoPrioridad(r.prioridad), Modifier.testTag("prioridad-${r.id}"))
    val sla = estadoSla(msDe(r.creadoEn), msDe(r.venceEn), ahora, r.estado.abierto) ?: return
    val tono = when (sla) {
        is EstadoSla.Vencido -> Tono.Peligro
        is EstadoSla.PorVencer -> Tono.Ambar
        is EstadoSla.EnPlazo -> Tono.Menta
    }
    ChipEsc(sla.texto(), tono, Modifier.testTag("sla-${r.id}"))
}

/** Cambiar la prioridad de un caso abierto. */
@Composable
internal fun SelectorPrioridad(r: ReporteAdmin, ocupado: Boolean, onElegir: (Prioridad) -> Unit) {
    Text("Prioridad", style = GType.labelMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Violeta)
    Box(Modifier.testTag("selector-prioridad")) {
        FiltrosEsc(Prioridad.entries, r.prioridad, { it.etiqueta }, { p -> if (!ocupado && p != r.prioridad) onElegir(p) })
    }
}

/** Respuestas rapidas: al tocar una, su texto pasa a la nota del caso. */
@Composable
internal fun RespuestasRapidasCaso(respuestas: List<RespuestaRapida>, onUsar: (RespuestaRapida) -> Unit) {
    if (respuestas.isEmpty()) return
    Text("Respuestas rápidas", style = GType.labelMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Violeta)
    Box(Modifier.testTag("respuestas-rapidas")) {
        FiltrosEsc<RespuestaRapida?>(respuestas, null, { it?.titulo.orEmpty() }, { r -> r?.let(onUsar) })
    }
}

// ---------- Reportes: ECU 911 y versiones ----------

@Composable
internal fun ImportacionesEcu(lista: List<ImportacionEcu>) {
    EtiquetaOp("Importaciones del ECU 911")
    if (lista.isEmpty()) {
        TarjetaEsc(Modifier.testTag("ecu-vacio")) {
            Text("Sin cargas del ECU 911", style = GType.bodyMd.copy(fontWeight = FontWeight.Bold), color = Esc.Texto)
            Text("Todavía no hay registros en importaciones_ecu911.", style = GType.bodySm, color = Esc.Texto2)
        }
        return
    }
    lista.forEach { i ->
        TarjetaEsc(Modifier.testTag("ecu-${i.periodo}-${i.fecha.take(10)}")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ChipEsc(if (i.ok) "Correcta" else "Con error", if (i.ok) Tono.Menta else Tono.Peligro)
                Text("Periodo ${i.periodo}", style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto, modifier = Modifier.weight(1f))
                Text(i.fecha.take(10), style = GType.bodySm, color = Esc.Texto3)
            }
            Text(i.archivo, style = GType.bodySm, color = Esc.Texto2, maxLines = 1)
            if (i.ok) Text("${miles(i.filas)} filas leídas · ${miles(i.filasLoja)} de Loja", style = GType.bodySm, color = Esc.Texto2)
            if (i.mensaje.isNotBlank()) Text(i.mensaje, style = GType.bodySm, color = if (i.ok) Esc.Texto3 else Esc.Peligro)
        }
    }
}

@Composable
internal fun VersionesApp(op: OperacionApp) {
    EtiquetaOp("Versiones de la app")
    TarjetaEsc(Modifier.testTag("versiones-en-uso")) {
        Text("En uso (últimos 30 días)", style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto)
        Text("Versión mínima exigida en Android: ${op.versionMinima ?: "sin definir"}", style = GType.bodySm, color = Esc.Texto2)
        if (op.enUso.isEmpty()) {
            Text("Todavía no hay datos: la app registra su versión al abrirse desde la 1.0.2.", style = GType.bodySm, color = Esc.Texto3)
        }
        op.enUso.forEach { v ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${etiquetaPlataforma(v.plataforma)} ${v.versionName}".trim(),
                    style = GType.bodyMd.copy(fontWeight = FontWeight.Bold), color = Esc.Texto, modifier = Modifier.weight(1f),
                )
                Text("${v.usuarios} ${if (v.usuarios == 1L) "usuario" else "usuarios"}", style = GType.bodySm, color = Esc.Texto2)
            }
        }
    }
    TarjetaEsc(Modifier.testTag("versiones-publicadas")) {
        Text("Publicadas", style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto)
        if (op.publicadas.isEmpty()) Text("Sin versiones registradas", style = GType.bodySm, color = Esc.Texto3)
        op.publicadas.take(8).forEach { v ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("v${v.versionName}", style = GType.bodyMd.copy(fontWeight = FontWeight.Bold), color = Esc.Texto, modifier = Modifier.weight(1f))
                ChipEsc(if (v.estable) "Estable" else "Prueba", if (v.estable) Tono.Menta else Tono.Neutro)
                Text("${v.publicadaEn.take(10)} · ${megas(v.apkBytes)}", style = GType.bodySm, color = Esc.Texto2)
            }
        }
    }
}

private fun miles(n: Long): String = n.toString().reversed().chunked(3).joinToString(".").reversed()

@Composable
private fun EtiquetaOp(texto: String) =
    Text(texto.uppercase(), style = GType.labelMd.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp), color = Esc.Texto3)

// ---------- Ajustes: plazos y respuestas ----------

/** Horas objetivo por prioridad. */
@Composable
internal fun AjustesSla(actual: SlaHoras, ocupado: Boolean, onGuardar: (SlaHoras) -> Unit) {
    var alta by remember(actual) { mutableStateOf(actual.alta.toString()) }
    var media by remember(actual) { mutableStateOf(actual.media.toString()) }
    var baja by remember(actual) { mutableStateOf(actual.baja.toString()) }
    EtiquetaOp("Plazos de moderación")
    TarjetaEsc(Modifier.testTag("ajustes-sla")) {
        Text("Horas para atender un caso según su prioridad. Al crearse, el motivo decide la prioridad: acoso, suplantación o peligro van como alta; spam como baja.", style = GType.bodySm, color = Esc.Texto2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { CampoTexto("Alta (h)", alta, { alta = it }, tipo = KeyboardType.Number) }
            Box(Modifier.weight(1f)) { CampoTexto("Media (h)", media, { media = it }, tipo = KeyboardType.Number) }
            Box(Modifier.weight(1f)) { CampoTexto("Baja (h)", baja, { baja = it }, tipo = KeyboardType.Number) }
        }
        BotonEsc("Guardar plazos", {
            onGuardar(SlaHoras(alta.trim().toIntOrNull() ?: 0, media.trim().toIntOrNull() ?: 0, baja.trim().toIntOrNull() ?: 0))
        }, Modifier.fillMaxWidth(), enabled = !ocupado)
    }
}

/** Lista y edicion de respuestas rapidas. */
@Composable
internal fun AjustesRespuestas(lista: List<RespuestaRapida>, ocupado: Boolean, onGuardar: (RespuestaRapida) -> Unit, onBorrar: (RespuestaRapida) -> Unit) {
    var editando by remember { mutableStateOf<RespuestaRapida?>(null) }
    var borrar by remember { mutableStateOf<RespuestaRapida?>(null) }
    EtiquetaOp("Respuestas rápidas")
    TarjetaEsc(Modifier.testTag("ajustes-respuestas")) {
        Text("Plantillas para la nota del moderador. La nota llega a quien reportó al cerrar el caso.", style = GType.bodySm, color = Esc.Texto2)
        lista.forEach { r ->
            Row(Modifier.testTag("respuesta-${r.id}"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.titulo, style = GType.bodyMd.copy(fontWeight = FontWeight.Bold), color = Esc.Texto)
                    Text(r.texto, style = GType.bodySm, color = Esc.Texto2, maxLines = 2)
                }
                BotonEsc("Editar", { editando = r }, tipo = TipoBoton.Fantasma, enabled = !ocupado)
            }
        }
        BotonEsc("Nueva respuesta", { editando = RespuestaRapida(null, "", "") }, Modifier.fillMaxWidth(), TipoBoton.Secundario, enabled = !ocupado)
    }
    editando?.let { r ->
        FormularioRespuesta(r, onGuardar = { onGuardar(it); editando = null }, onBorrar = if (r.id != null) ({ borrar = r; editando = null }) else null, onCancelar = { editando = null })
    }
    borrar?.let { r ->
        DialogoC2(
            onDismissRequest = { borrar = null },
            confirmButton = { BotonModalC2("Eliminar", { onBorrar(r); borrar = null }, peligro = true) },
            dismissButton = { BotonModalC2("Cancelar", { borrar = null }, secundario = true) },
            title = { Text("Eliminar respuesta") },
            text = { Text("Se eliminará \"${r.titulo}\" para todo el equipo.") },
        )
    }
}

@Composable
private fun FormularioRespuesta(inicial: RespuestaRapida, onGuardar: (RespuestaRapida) -> Unit, onBorrar: (() -> Unit)?, onCancelar: () -> Unit) {
    var titulo by remember { mutableStateOf(inicial.titulo) }
    var texto by remember { mutableStateOf(inicial.texto) }
    val error = respuestaValida(titulo, texto)
    DialogoC2(
        onDismissRequest = onCancelar,
        confirmButton = { BotonModalC2("Guardar", { onGuardar(inicial.copy(titulo = titulo, texto = texto)) }, enabled = error == null) },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onBorrar != null) BotonModalC2("Eliminar", onBorrar, secundario = true)
                BotonModalC2("Cancelar", onCancelar, secundario = true)
            }
        },
        title = { Text(if (inicial.id == null) "Nueva respuesta" else "Editar respuesta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto("Título", titulo, { titulo = it }, ayuda = "${titulo.trim().length}/60")
                CampoTexto("Texto", texto, { texto = it }, lineas = 4, ayuda = "${texto.trim().length}/500")
            }
        },
    )
}

/** Punto de leyenda del mapa. */
@Composable
internal fun Leyenda() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("Corredores" to Esc.Menta, "Incidentes" to Esc.Ambar, "SOS" to Esc.Peligro).forEach { (t, c) ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(c))
                Text(t, style = GType.bodySm, color = Esc.Texto2)
            }
        }
    }
}
