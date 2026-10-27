package com.gacelas.ajustes

import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gacelas.BuildConfig
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.identidad.domain.modelo.Visibilidad
import com.gacelas.ui.ajustesState
import com.gacelas.ui.aviso
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.EtiquetaGrupo
import com.gacelas.ui.components.FilaAjuste
import com.gacelas.ui.components.FilaInterruptor
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Devuelve una funcion que guarda un cambio de ajustes en DataStore. */
@Composable
private fun guardarAjuste(): ((Ajustes) -> Ajustes) -> Unit {
    val vm = com.gacelas.ui.ajustesViewModel()
    return { t -> vm.actualizar(t) }
}

@Composable
fun AjustesPreferenciasScreen(
    onAtras: () -> Unit,
    onEditarPerfil: () -> Unit,
    onPrivacidad: () -> Unit,
    onNotificaciones: () -> Unit,
    onCerrarSesion: () -> Unit,
    onMapasOffline: (() -> Unit)? = null,
    onBrujula: (() -> Unit)? = null,
    onLegal: ((Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val c = contenedor()
    val scope = rememberCoroutineScope()
    val a by ajustesState()
    AjustesContenido(
        a = a, version = com.gacelas.actualizacion.textoVersion(), guardar = guardarAjuste(),
        onAtras = onAtras, onEditarPerfil = onEditarPerfil, onPrivacidad = onPrivacidad, onNotificaciones = onNotificaciones,
        onSoporte = {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:soporte@gacelas.app")).putExtra(Intent.EXTRA_SUBJECT, "Ayuda con Runcity")
            runCatching { context.startActivity(intent) }.onFailure { context.aviso("No hay app de correo instalada") }
        },
        onCerrarSesion = { scope.launch { c.auth.cerrarSesion(); onCerrarSesion() } },
        mapaOffline = { MapaOfflineAjuste() },
        cabecera = { CabeceraAjustes(onEditarPerfil) },
        onMapasOffline = onMapasOffline,
        onBrujula = onBrujula,
        onLegal = onLegal,
    )
}

@Composable
private fun CabeceraAjustes(onEditarPerfil: () -> Unit) {
    val perfil by com.gacelas.ui.sesionState()
    val carreras by com.gacelas.ui.carrerasState()
    val km = carreras.sumOf { it.distanciaKm }
    Row(verticalAlignment = Alignment.CenterVertically) {
        com.gacelas.perfil.FotoPerfil(perfil?.fotoPath, 56.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(perfil?.nombre ?: "Corredor", style = GType.titleMd, color = GColors.TextPrimary)
            com.gacelas.comunidad.presentation.ui.InsigniaNivel(com.gacelas.comunidad.domain.modelo.nivelCorredor(km))
            perfil?.email?.let { Text(it, style = GType.bodySm, color = GColors.TextSecondary) }
            Text("Sede Loja • ${km.roundToInt()} km totales", style = GType.bodySm, color = GColors.Secondary500, modifier = Modifier.testTag("ajustes-km"))
        }
        TextButton(onClick = onEditarPerfil) { Text("Editar perfil", color = GColors.Primary) }
    }
}

private fun Visibilidad.titulo() = when (this) { Visibilidad.Publico -> "Todos"; Visibilidad.Seguidores -> "Seguidores"; Visibilidad.Privado -> "Solo yo" }
private fun Visibilidad.descripcion() = when (this) {
    Visibilidad.Publico -> "Cualquiera puede ver tu perfil y actividades públicas"
    Visibilidad.Seguidores -> "Solo seguidores aceptados pueden ver tus actividades"
    Visibilidad.Privado -> "Solo tú puedes ver tus actividades"
}

/** Fila C2 sin icono con interruptor (figma-proto). */
@Composable
private fun FilaC2(titulo: String, descripcion: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = GType.labelLg, color = GColors.TextPrimary)
            descripcion?.let { Text(it, style = GType.bodySm, color = GColors.C2Muted) }
        }
        com.gacelas.ui.components.Interruptor(checked, onChange)
    }
}

@Composable
private fun GrupoC2(titulo: String, contenido: @Composable () -> Unit) {
    EtiquetaGrupo(titulo)
    Column(Modifier.fillMaxWidth().background(GColors.C2Card)) { contenido() }
}

@Composable
private fun Divisor() = androidx.compose.material3.HorizontalDivider(color = GColors.C2Linea)

@Composable
private fun MapaZonaOculta(activa: Boolean, radio: Int) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(150.dp).background(GColors.C2S2).testTag("zona-oculta-mapa")) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val calle = androidx.compose.ui.graphics.Color(0xFF2C3446)
            listOf(0.15f, 0.45f, 0.75f, 1f).forEach { y -> drawLine(calle, androidx.compose.ui.geometry.Offset(0f, size.height * y), androidx.compose.ui.geometry.Offset(size.width, size.height * y - 40f), 12f) }
            listOf(0.12f, 0.4f, 0.65f, 0.88f).forEach { x -> drawLine(calle, androidx.compose.ui.geometry.Offset(size.width * x, 0f), androidx.compose.ui.geometry.Offset(size.width * x + 40f, size.height), 10f) }
            val casa = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.4f)
            if (activa) drawCircle(GColors.C2Morado.copy(alpha = 0.2f), size.height * 0.28f, casa)
            val ruta = androidx.compose.ui.graphics.Path().apply {
                moveTo(casa.x, casa.y)
                cubicTo(size.width * 0.25f, size.height * 0.8f, size.width * 0.5f, size.height * 0.85f, size.width * 0.7f, size.height * 0.7f)
                cubicTo(size.width * 0.85f, size.height * 0.55f, size.width * 0.8f, size.height * 0.2f, size.width * 0.6f, size.height * 0.15f)
            }
            drawPath(ruta, GColors.C2Violeta, style = androidx.compose.ui.graphics.drawscope.Stroke(8f))
            drawCircle(GColors.C2Morado, 12f, casa)
            drawCircle(androidx.compose.ui.graphics.Color(0xFF68DBA9), 12f, androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.15f))
        }
        if (activa) Text(
            "CASA · ${if (radio >= 1000) "${radio / 1000} KM" else "$radio M"}", style = GType.labelMd,
            color = GColors.BgPrimary, modifier = Modifier.padding(10.dp).background(GColors.C2Violeta).padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun PrivacidadUbicacionScreen(onAtras: () -> Unit, onLegal: (Boolean) -> Unit = {}, onCuentaEliminada: () -> Unit = {}) {
    val a by ajustesState()
    val guardar = guardarAjuste()
    var radio by remember(a.radioHogarM) { mutableStateOf(a.radioHogarM.toFloat()) }
    PantallaConEncabezado("Privacidad", onAtras) {
        Tarjeta(fondo = GColors.C2Card, borde = null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ZONA OCULTA", style = GType.labelMd, color = GColors.C2Violeta, modifier = Modifier.weight(1f))
                com.gacelas.ui.components.Interruptor(a.ocultarHogar) { v -> guardar { it.copy(ocultarHogar = v) } }
            }
            MapaZonaOculta(a.ocultarHogar, radio.roundToInt())
            Text(
                if (a.ocultarHogar) "Ocultamos el inicio y fin de tus carreras dentro de este radio." else "Activa la zona oculta para no revelar dónde vives.",
                style = GType.bodySm, color = GColors.C2Muted,
            )
            if (a.ocultarHogar) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(200, 500, 1000).forEach { r ->
                        androidx.compose.foundation.layout.Box(Modifier.testTag("radio-$r")) {
                            com.gacelas.ui.components.Pastilla(if (r >= 1000) "1 KM" else "$r M", a.radioHogarM == r, { guardar { it.copy(radioHogarM = r) } })
                        }
                    }
                }
                Row {
                    Text("Radio de protección", style = GType.bodyMd, color = GColors.C2Muted, modifier = Modifier.weight(1f))
                    Text("${radio.roundToInt()}m", style = GType.labelLg, color = GColors.C2Violeta)
                }
                Slider(
                    value = radio, onValueChange = { radio = (it / 50f).roundToInt() * 50f }, valueRange = 100f..1000f, steps = 17,
                    onValueChangeFinished = { guardar { it.copy(radioHogarM = radio.roundToInt()) } },
                    colors = SliderDefaults.colors(thumbColor = GColors.C2Morado, activeTrackColor = GColors.C2Morado, inactiveTrackColor = GColors.C2Linea),
                )
            }
        }
        EtiquetaGrupo("Quién ve tus carreras")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Visibilidad.entries.forEach { v ->
                com.gacelas.ui.components.Pastilla(v.titulo().uppercase(), v == a.visibilidad, { guardar { it.copy(visibilidad = v) } })
            }
        }
        Text(a.visibilidad.descripcion(), style = GType.bodySm, color = GColors.C2Muted)
        GrupoC2("Actividad") {
            FilaC2("Aparecer en rankings", "Top semanal de distancia y altimetría de Loja", a.aparecerRankings) { v -> guardar { it.copy(aparecerRankings = v) } }
            Divisor()
            FilaC2("Ocultar datos biométricos", "Pulsaciones y cadencia fuera de los resúmenes públicos", a.ocultarBiometricos) { v -> guardar { it.copy(ocultarBiometricos = v) } }
            Divisor()
            FilaC2("Ubicación precisa", "GPS de alta precisión durante las carreras", a.ubicacionPrecisa) { v -> guardar { it.copy(ubicacionPrecisa = v) } }
            Divisor()
            FilaC2("Compartir en vivo al correr", "Tus seguidores ven tu posición mientras corres (la zona de casa se oculta)", a.compartirEnVivo == true) { v -> guardar { it.copy(compartirEnVivo = v) } }
        }
        com.gacelas.legal.SeccionConsentimientos(onLegal, onCuentaEliminada)
    }
}

@Composable
fun ConfiguracionNotificacionesScreen(onAtras: () -> Unit) {
    val a by ajustesState()
    val g = guardarAjuste()
    val sistema by com.gacelas.core.notificaciones.notificacionesPermitidasState()
    val activar = com.gacelas.core.notificaciones.rememberActivarNotificaciones()
    PantallaConEncabezado("Notificaciones", onAtras) {
        GrupoC2("En este teléfono") {
            // Refleja el estado REAL de Android (permiso + canal), no un ajuste guardado.
            FilaC2("Notificaciones de Runcity", if (sistema) "Activadas en Android" else "Bloqueadas en Android: no verás la carrera en la barra", sistema) { activar() }
        }
        if (!sistema) com.gacelas.core.notificaciones.AvisoNotificacionesBloqueadas(
            "Android tiene bloqueadas las notificaciones de Runcity. Actívalas para ver el tiempo y la distancia de tu carrera y los avisos de la comunidad.",
            activar,
        )
        GrupoC2("Push") {
            FilaC2("Me gusta", null, a.notifLikes) { v -> g { it.copy(notifLikes = v) } }
            Divisor()
            FilaC2("Comentarios", null, a.notifComentarios) { v -> g { it.copy(notifComentarios = v) } }
            Divisor()
            FilaC2("Nuevos seguidores", null, a.notifSeguidores) { v -> g { it.copy(notifSeguidores = v) } }
            Divisor()
            FilaC2("Amigos en vivo", "Cuando alguien te comparte su carrera", a.notifEnVivo) { v -> g { it.copy(notifEnVivo = v) } }
            Divisor()
            FilaC2("Eventos y clubes", null, a.notifEventos) { v -> g { it.copy(notifEventos = v) } }
        }
        GrupoC2("Correo") {
            FilaC2("Resumen semanal", "Tu actividad de la semana, cada lunes", a.notifEmail) { v -> g { it.copy(notifEmail = v) } }
        }
        Text("Los me gusta se agrupan para evitar avisos excesivos.", style = GType.bodySm, color = GColors.C2Muted)
    }
}



/** Reto mensual y preferencias de alertas dentro de la bandeja de notificaciones (Stitch). */
@Composable
fun ExtraNotificaciones() {
    val a by ajustesState()
    val guardar = guardarAjuste()
    val carreras by com.gacelas.ui.carrerasState()
    val reto = remember(carreras) { com.gacelas.actividad.domain.reglas.EstadisticasPerfil.retoMes(carreras, System.currentTimeMillis()) }
    val pct = (reto.km / reto.metaKm).coerceIn(0.0, 1.0)
    EtiquetaGrupo("Esta semana")
    Tarjeta(modifier = Modifier.testTag("reto-notificacion")) {
        Text("Progreso de reto mensual", style = GType.titleMd, color = GColors.TextPrimary)
        Text(reto.titulo, style = GType.bodySm, color = GColors.TextSecondary)
        Row {
            Text("${"%.1f".format(reto.km)} de ${reto.metaKm.roundToInt()} km acumulados", style = GType.labelMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("${(pct * 100).roundToInt()}%", style = GType.labelMd, color = GColors.Primary500)
        }
        androidx.compose.material3.LinearProgressIndicator(progress = { pct.toFloat() }, modifier = Modifier.fillMaxWidth(), color = GColors.Primary, trackColor = GColors.SurfaceHigh)
        Text("Cierra en ${reto.diasRestantes} días", style = GType.bodySm, color = GColors.TextSecondary)
    }
    EtiquetaGrupo("Preferencias de alertas")
    Tarjeta(padding = 0.dp) {
        FilaInterruptor(Icons.Filled.Event, "Eventos del club", "Quedadas en Jipiro, Vilcabamba y Zamora", a.notifEventos, { v -> guardar { it.copy(notifEventos = v) } })
        FilaInterruptor(Icons.Filled.Favorite, "Comentarios y aplausos", "Interacción social de compañeros de ruta", a.notifComentarios && a.notifLikes, { v -> guardar { it.copy(notifComentarios = v, notifLikes = v) } })
        FilaInterruptor(Icons.Filled.PersonAdd, "Nuevos seguidores", "Corredores que empiezan a seguirte", a.notifSeguidores, { v -> guardar { it.copy(notifSeguidores = v) } })
        FilaInterruptor(Icons.Filled.Sensors, "Corredores en vivo", "Aviso cuando alguien que sigues sale a correr", a.notifEnVivo, { v -> guardar { it.copy(notifEnVivo = v) } })
    }
}
