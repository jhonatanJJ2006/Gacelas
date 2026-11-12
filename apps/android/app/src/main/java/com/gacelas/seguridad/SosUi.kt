package com.gacelas.seguridad

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.identidad.domain.modelo.ContactoEmergencia
import com.gacelas.seguridad.domain.Sos
import com.gacelas.seguridad.infrastructure.SosRepositorio
import com.gacelas.ui.components.BotonModalC2
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.HojaC2
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import kotlinx.coroutines.GlobalScope
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Estado global del SOS: una alerta a la vez, sobrevive a recomposiciones y a cambiar de pantalla. */
object EstadoSos {
    val alertaId = MutableStateFlow<String?>(null)
    val activando = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    var ultimaPos: Pair<Double, Double>? = null

    suspend fun activar(repo: SosRepositorio?, pos: Pair<Double, Double>?) {
        if (alertaId.value != null) return
        ultimaPos = pos
        if (repo == null) { error.value = "Sin conexión con el servidor: llama al 911."; return }
        activando.value = true
        error.value = null
        val id = runCatching { repo.activar(pos?.first, pos?.second, com.gacelas.shared.ahoraMs()) }.getOrNull()
        activando.value = false
        if (id == null) error.value = "No se pudo crear el enlace de ubicación. Llama al 911."
        alertaId.value = id
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    fun terminar(repo: SosRepositorio?) {
        if (alertaId.value == null) return
        alertaId.value = null
        GlobalScope.launch { runCatching { repo?.terminar() } }
    }
}

/**
 * Boton SOS de la carrera: hay que mantenerlo pulsado 3 s (anillo de progreso y vibracion). Un toque
 * corto solo explica como usarlo. Con el SOS activo, tocarlo abre las opciones.
 */
@Composable
fun BotonSOS(posicion: Pair<Double, Double>?) {
    val c = contenedor()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vibrar = LocalHapticFeedback.current
    val id by EstadoSos.alertaId.collectAsState()
    var hoja by remember { mutableStateOf(false) }
    val progreso = remember { Animatable(0f) }
    val activo = id != null
    LaunchedEffect(activo, posicion) {
        val p = posicion ?: return@LaunchedEffect
        EstadoSos.ultimaPos = p
        if (activo) c.sos?.posicion(p.first, p.second, com.gacelas.shared.ahoraMs())
    }
    val activar = {
        vibrar.performHapticFeedback(HapticFeedbackType.LongPress)
        hoja = true
        scope.launch { EstadoSos.activar(c.sos, posicion) }
        Unit
    }
    Box(
        Modifier.size(58.dp).clip(CircleShape).background(if (activo) Color(0xFFDC2626) else Color(0xFFB91C1C))
            .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
            .pointerInput(activo) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (activo) { waitForUpOrCancellation(); hoja = true; return@awaitEachGesture }
                    val animacion = scope.launch { progreso.snapTo(0f); progreso.animateTo(1f, tween(Sos.MANTENER_MS.toInt(), easing = LinearEasing)) }
                    val arriba = waitForUpOrCancellation()
                    val completo = progreso.value >= 0.999f
                    animacion.cancel()
                    scope.launch { progreso.snapTo(0f) }
                    if (completo) return@awaitEachGesture
                    if (arriba != null) com.gacelas.ui.components.AvisosC2.mostrar("Mantén pulsado 3 s para pedir ayuda")
                }
            }
            .semantics {
                contentDescription = if (activo) "SOS activo: ver opciones" else "SOS: mantén pulsado 3 segundos para pedir ayuda"
                onLongClick("Activar SOS") { activar(); true }
            }
            .testTag("boton-sos"),
        contentAlignment = Alignment.Center,
    ) {
        LaunchedEffect(progreso.value >= 0.999f) { if (progreso.value >= 0.999f) { progreso.snapTo(0f); activar() } }
        Canvas(Modifier.size(52.dp)) {
            if (progreso.value > 0f) drawArc(Color.White, -90f, 360f * progreso.value, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SOS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(if (activo) "ACTIVO" else "MANTÉN", color = Color.White.copy(alpha = 0.9f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
    if (hoja) HojaSOS(onCerrar = { hoja = false }, context = context)
}


/** Opciones del SOS: llamar al 911, avisar a los contactos por SMS o WhatsApp, compartir y terminar. */
@Composable
fun HojaSOS(onCerrar: () -> Unit, context: Context = LocalContext.current) {
    val c = contenedor()
    val id by EstadoSos.alertaId.collectAsState()
    val activando by EstadoSos.activando.collectAsState()
    val error by EstadoSos.error.collectAsState()
    val ajustes by com.gacelas.ui.ajustesState()
    val perfil by com.gacelas.ui.sesionState()
    val contactos = ajustes.contactosEmergencia
    val pos = EstadoSos.ultimaPos
    val mensaje = id?.let { Sos.mensaje(perfil?.nombre ?: "Un corredor", it, pos?.first, pos?.second) }
    HojaC2(onCerrar, fondo = GColors.C2Card) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding().testTag("hoja-sos"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (id != null) "SOS activo" else if (activando) "Activando SOS" else "SOS", color = Color.White, fontSize = 26.sp, fontFamily = com.gacelas.ui.theme.SpaceGrotesk)
            Text(
                when {
                    id != null -> "Tu ubicación se comparte con un enlace privado mientras el SOS esté activo. Avisa a tus contactos o llama al 911."
                    activando -> "Creando el enlace de tu ubicación..."
                    else -> "Puedes llamar al 911 aunque no haya conexión con Runcity."
                },
                color = GColors.C2Muted, fontSize = 14.sp,
            )
            error?.let { Text(it, color = Color(0xFFF472B6), fontSize = 13.sp) }
            BotonSosAccion("Llamar al 911 (ECU 911)", Icons.Filled.Call, Color(0xFFDC2626), "llamar-911") {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Sos.NUMERO_EMERGENCIA}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            if (mensaje != null && contactos.isNotEmpty()) {
                BotonSosAccion("Enviar SMS a mis contactos (${contactos.size})", Icons.Filled.Sms, Color(0xFF7B1FA2), "sos-sms") {
                    val numeros = contactos.joinToString(";") { Sos.normalizarTelefono(it.telefono) }
                    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$numeros")).putExtra("sms_body", mensaje)
                    runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                contactos.forEach { ct ->
                    Row(
                        Modifier.fillMaxWidth().background(GColors.C2S2).border(1.dp, GColors.C2Linea).clickable {
                            val url = "https://wa.me/${Sos.normalizarTelefono(ct.telefono).removePrefix("+")}?text=${Uri.encode(mensaje)}"
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("WhatsApp a ${ct.nombre}", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(ct.telefono, color = GColors.C2Muted, fontSize = 12.sp)
                    }
                }
            }
            if (mensaje != null && contactos.isEmpty()) Text(
                "No tienes contactos de emergencia. Comparte el enlace con alguien de confianza o agrégalos en Ajustes, Privacidad.",
                color = GColors.C2Muted, fontSize = 13.sp, modifier = Modifier.testTag("sos-sin-contactos"),
            )
            if (mensaje != null) BotonSosAccion("Compartir enlace de ubicación", Icons.Filled.Share, GColors.C2S2, "sos-compartir") {
                val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, mensaje)
                context.startActivity(Intent.createChooser(i, "Compartir SOS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            if (id != null) Text(
                "Estoy bien, terminar SOS", color = GColors.C2Muted, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().clickable { EstadoSos.terminar(c.sos); onCerrar() }.padding(12.dp).testTag("terminar-sos"),
            )
        }
    }
}


@Composable
private fun BotonSosAccion(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, fondo: Color, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(50.dp).background(fondo).clickable(onClick = onClick).padding(horizontal = 14.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icono, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

/** Editor de contactos de emergencia en Ajustes, Privacidad (maximo 3, guardados en prefs). */
@Composable
fun ContactosEmergenciaC2(contactos: List<ContactoEmergencia>, onCambiar: (List<ContactoEmergencia>) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().testTag("contactos-emergencia"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("CONTACTOS DE EMERGENCIA (SOS)", color = GColors.C2Violeta, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Column(Modifier.fillMaxWidth().background(GColors.C2Card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Si mantienes pulsado el botón SOS durante una carrera, podrás enviarles por SMS o WhatsApp un enlace con tu ubicación en vivo. Máximo ${Sos.MAX_CONTACTOS}.", color = GColors.C2Muted, fontSize = 13.sp)
            if (contactos.isEmpty()) Text("Aún no tienes contactos de emergencia.", color = Color.White, fontSize = 14.sp)
            contactos.forEach { ct ->
                Row(Modifier.fillMaxWidth().background(GColors.C2S2).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ct.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(ct.telefono, color = GColors.C2Muted, fontSize = 13.sp)
                    }
                    Icon(
                        Icons.Filled.Delete, "Quitar a ${ct.nombre}", tint = GColors.C2Muted,
                        modifier = Modifier.size(40.dp).clickable { onCambiar(contactos.filter { it.telefono != ct.telefono }) }.padding(8.dp),
                    )
                }
            }
            if (contactos.size < Sos.MAX_CONTACTOS) {
                CampoTexto("Nombre del contacto", nombre, { nombre = it }, placeholder = "Por ejemplo, Mamá")
                CampoTexto("Teléfono del contacto", telefono, { telefono = it }, placeholder = "0991234567", tipo = KeyboardType.Phone)
                error?.let { Text(it, color = Color(0xFFF472B6), fontSize = 13.sp) }
                BotonModalC2("Agregar contacto", {
                    val tel = Sos.normalizarTelefono(telefono)
                    val e = Sos.validar(nombre, telefono) ?: if (contactos.any { it.telefono == tel }) "Ese teléfono ya está en tus contactos" else null
                    error = e
                    if (e == null) { onCambiar(contactos + ContactoEmergencia(nombre.trim(), tel)); nombre = ""; telefono = "" }
                }, Modifier.fillMaxWidth().testTag("agregar-contacto"))
            }
        }
    }
}
