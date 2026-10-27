package com.gacelas.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.ajustes.PantallaConEncabezado
import com.gacelas.ui.components.ModalC2
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.launch

/** Documento legal legible en fondo oscuro C2. */
@Composable
fun DocumentoLegalScreen(terminos: Boolean, onAtras: () -> Unit) {
    val titulo = if (terminos) "Términos y condiciones" else "Política de privacidad"
    val version = if (terminos) VersionesLegales.TERMINOS else VersionesLegales.PRIVACIDAD
    val secciones = if (terminos) TextosLegales.terminos else TextosLegales.privacidad
    PantallaConEncabezado(titulo, onAtras) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GColors.C2Card)
                .border(1.dp, GColors.C2Linea, RoundedCornerShape(12.dp)).padding(16.dp).testTag(if (terminos) "doc-terminos" else "doc-privacidad"),
        ) {
            Text("Versión $version", style = GType.labelMd, color = GColors.C2Violeta)
            Text("Adaptado a la Ley Orgánica de Protección de Datos Personales del Ecuador.", style = GType.bodySm, color = GColors.C2Muted, modifier = Modifier.padding(top = 4.dp))
        }
        secciones.forEach { s ->
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(s.titulo, style = GType.titleMd, color = GColors.TextPrimary)
                Text(s.texto, style = GType.bodyMd, color = GColors.C2Muted, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Pide aceptar de nuevo los Términos cuando cambia su versión (solo si ya había aceptado una anterior). */
@Composable
fun ReaceptarTerminos(onVer: (terminos: Boolean) -> Unit) {
    val repo = contenedor().consentimientos
    val scope = rememberCoroutineScope()
    var pedir by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val h = repo.historial()
        pedir = vigente(h, TipoConsentimiento.Terminos) != null && requiereAceptarTerminos(h)
    }
    if (pedir) ModalC2(
        icono = Icons.Filled.Gavel,
        titulo = "Actualizamos los Términos",
        texto = "Publicamos una nueva versión (${VersionesLegales.TERMINOS}) de los Términos y la Política de privacidad. Léelos y acéptalos para seguir usando Gacelas.",
        primario = "Aceptar",
        onPrimario = { scope.launch { repo.registrar(TipoConsentimiento.Terminos, true); repo.registrar(TipoConsentimiento.Privacidad, true); pedir = false } },
        onCerrar = {},
        secundario = "Leer términos",
        onSecundario = { onVer(true) },
        tag = "modal-reaceptar",
    )
}

/** Consentimientos revocables, descarga de datos y eliminacion de cuenta (pantalla Privacidad). */
@Composable
fun SeccionConsentimientos(onLegal: (terminos: Boolean) -> Unit, onCuentaEliminada: () -> Unit) {
    val c = contenedor()
    val repo = c.consentimientos
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var gps by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var explicar by remember { mutableStateOf(false) }
    var eliminar by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { gps = repo.otorgado(TipoConsentimiento.GpsCarrera); visible = repo.otorgado(TipoConsentimiento.UbicacionVisible) }
    com.gacelas.ui.components.EtiquetaGrupo("Consentimientos")
    com.gacelas.ui.components.Tarjeta(padding = 0.dp) {
        com.gacelas.ui.components.FilaInterruptor(Icons.Filled.MyLocation, "Seguimiento GPS en carrera", "Usar tu GPS mientras grabas una carrera", gps,
            { v -> gps = v; scope.launch { repo.registrar(TipoConsentimiento.GpsCarrera, v) } })
        com.gacelas.ui.components.FilaInterruptor(Icons.Filled.Sensors, "Ubicación visible", "Aparecer en el mapa en vivo para otros corredores", visible,
            { v -> if (v) explicar = true else { visible = false; scope.launch { repo.registrar(TipoConsentimiento.UbicacionVisible, false); c.mapaVivo?.dejarDeCompartir() } } },
            GColors.C2Menta)
    }
    com.gacelas.ui.components.EtiquetaGrupo("Tus datos")
    com.gacelas.ui.components.Tarjeta(padding = 0.dp) {
        FilaAccion(Icons.Filled.Description, "Términos y condiciones", "pv-terminos") { onLegal(true) }
        FilaAccion(Icons.Filled.Policy, "Política de privacidad", "pv-privacidad") { onLegal(false) }
        FilaAccion(Icons.Filled.Download, "Descargar mis datos", "pv-descargar") {
            scope.launch {
                val datos = buildString {
                    append("{\"consentimientos\":[")
                    append(repo.historial().joinToString(",") { "{\"tipo\":\"${it.tipo}\",\"version\":\"${it.version}\",\"otorgado\":${it.otorgado},\"fechaMs\":${it.fechaMs}}" })
                    append("]}")
                }
                val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/json")
                    .putExtra(android.content.Intent.EXTRA_SUBJECT, "Mis datos de Runcity").putExtra(android.content.Intent.EXTRA_TEXT, datos)
                runCatching { context.startActivity(android.content.Intent.createChooser(i, "Descargar mis datos")) }
            }
        }
        FilaAccion(Icons.Filled.DeleteForever, "Eliminar cuenta y datos", "pv-eliminar", GColors.C2Peligro) { eliminar = true }
    }
    if (explicar) ModalC2(
        icono = Icons.Filled.Sensors, titulo = "¿Mostrar tu ubicación?", texto = TextosLegales.UBICACION_VISIBLE,
        primario = "Activar", onPrimario = { explicar = false; visible = true; scope.launch { repo.registrar(TipoConsentimiento.UbicacionVisible, true) } },
        onCerrar = { explicar = false }, secundario = "Ahora no", onSecundario = { explicar = false }, tag = "modal-ubicacion-visible",
    )
    if (eliminar) ModalC2(
        icono = Icons.Filled.DeleteForever, titulo = "¿Eliminar tu cuenta?",
        texto = "Se borrarán tu perfil, carreras, rutas, publicaciones y consentimientos. Esta acción no se puede deshacer.",
        primario = "Eliminar", peligro = true,
        onPrimario = { eliminar = false; scope.launch { val ok = repo.eliminarCuenta(); if (ok) { runCatching { c.auth.cerrarSesion() }; onCuentaEliminada() } else com.gacelas.ui.components.AvisosC2.mostrar("No se pudo eliminar la cuenta. Inténtalo con conexión.", error = true) } },
        onCerrar = { eliminar = false }, secundario = "Cancelar", onSecundario = { eliminar = false }, tag = "modal-eliminar-cuenta",
    )
}

@Composable
private fun FilaAccion(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, tag: String, color: androidx.compose.ui.graphics.Color = GColors.TextPrimary, onClick: () -> Unit) =
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp).testTag(tag),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(icono, null, tint = if (color == GColors.TextPrimary) GColors.C2Violeta else color)
        Text("  $texto", style = GType.labelLg, color = color, modifier = Modifier.weight(1f))
    }
