package com.gacelas.ajustes

import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gacelas.BuildConfig
import com.gacelas.data.ajustes.Ajustes
import com.gacelas.data.ajustes.Visibilidad
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

@Composable
private fun PantallaConEncabezado(titulo: String, onAtras: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras(titulo, onAtras)
        Pantalla(Modifier.weight(1f).navigationBarsPadding()) { content() }
    }
}

/** Devuelve una funcion que guarda un cambio de ajustes en DataStore. */
@Composable
private fun guardarAjuste(): ((Ajustes) -> Ajustes) -> Unit {
    val c = contenedor()
    val scope = rememberCoroutineScope()
    return { t -> scope.launch { c.ajustes.actualizar(t) } }
}

@Composable
fun AjustesPreferenciasScreen(
    onAtras: () -> Unit,
    onEditarPerfil: () -> Unit,
    onPrivacidad: () -> Unit,
    onNotificaciones: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val context = LocalContext.current
    val c = contenedor()
    val scope = rememberCoroutineScope()
    val a by ajustesState()
    val guardar = guardarAjuste()
    var dialogo by remember { mutableStateOf<String?>(null) }
    PantallaConEncabezado("Ajustes", onAtras) {
        EtiquetaGrupo("Cuenta")
        Tarjeta(padding = 0.dp) { FilaAjuste(Icons.Filled.Person, "Editar perfil", onClick = onEditarPerfil) }
        EtiquetaGrupo("Privacidad")
        Tarjeta(padding = 0.dp) {
            FilaAjuste(Icons.Filled.Lock, "Privacidad y ubicación", onClick = onPrivacidad)
            FilaAjuste(Icons.Filled.Notifications, "Notificaciones", onClick = onNotificaciones)
        }
        EtiquetaGrupo("General")
        Tarjeta(padding = 0.dp) {
            FilaInterruptor(Icons.Filled.DarkMode, "Modo oscuro", if (a.modoOscuro) "Activado" else "Tema claro activo", checked = a.modoOscuro, onChange = { v -> guardar { it.copy(modoOscuro = v) } })
            FilaInterruptor(Icons.Filled.Straighten, "Unidades métricas", if (a.unidadesMetricas) "Kilómetros y metros" else "Millas", checked = a.unidadesMetricas, onChange = { v -> guardar { it.copy(unidadesMetricas = v) } })
        }
        EtiquetaGrupo("Mapa")
        MapaOfflineAjuste()
        EtiquetaGrupo("Rachas")
        Tarjeta {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    Text("Meta semanal", style = GType.bodyLg, color = GColors.TextPrimary)
                    Text("Carreras por semana para mantener la racha semanal", style = GType.bodySm, color = GColors.TextSecondary)
                }
                IconButton(onClick = { guardar { it.copy(metaSemanal = (it.metaSemanal - 1).coerceAtLeast(1)) } }) {
                    Icon(Icons.Filled.Remove, "Menos carreras por semana", tint = GColors.Primary500)
                }
                Text("${a.metaSemanal}", style = GType.titleMd, color = GColors.TextPrimary)
                IconButton(onClick = { guardar { it.copy(metaSemanal = (it.metaSemanal + 1).coerceAtMost(7)) } }) {
                    Icon(Icons.Filled.Add, "Más carreras por semana", tint = GColors.Primary500)
                }
            }
        }
        EtiquetaGrupo("Información")
        Tarjeta(padding = 0.dp) {
            FilaAjuste(Icons.Filled.Info, "Acerca de", com.gacelas.actualizacion.textoVersion(), onClick = { dialogo = "acerca" })
            FilaAjuste(Icons.AutoMirrored.Filled.Help, "Ayuda y soporte", onClick = { dialogo = "ayuda" })
        }
        Tarjeta(padding = 0.dp, fondo = GColors.Danger.copy(alpha = 0.1f), borde = GColors.Danger.copy(alpha = 0.4f)) {
            FilaAjuste(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", colorIcono = GColors.Danger, onClick = { dialogo = "salir" }, trailing = {})
        }
    }
    when (dialogo) {
        "acerca" -> AlertDialog(
            onDismissRequest = { dialogo = null },
            confirmButton = { TextButton(onClick = { dialogo = null }) { Text("Cerrar") } },
            title = { Text("Gacelas Loja") },
            text = { Text("${com.gacelas.actualizacion.textoVersion()}. App del grupo de running Gacelas de Loja, Ecuador. Graba tus carreras con GPS, descubre rutas y entrena con la comunidad.") },
            containerColor = GColors.BgSecondary,
        )
        "ayuda" -> AlertDialog(
            onDismissRequest = { dialogo = null },
            confirmButton = {
                TextButton(onClick = {
                    dialogo = null
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:soporte@gacelas.app")).putExtra(Intent.EXTRA_SUBJECT, "Ayuda con Gacelas")
                    runCatching { context.startActivity(intent) }.onFailure { context.aviso("No hay app de correo instalada") }
                }) { Text("Escribir a soporte") }
            },
            dismissButton = { TextButton(onClick = { dialogo = null }) { Text("Cerrar") } },
            title = { Text("Ayuda y soporte") },
            text = { Text("Para grabar una carrera toca el botón central Correr y da permiso de ubicación. Las carreras se guardan en tu teléfono y aparecen en Perfil e Inicio.") },
            containerColor = GColors.BgSecondary,
        )
        "salir" -> AlertDialog(
            onDismissRequest = { dialogo = null },
            confirmButton = {
                TextButton(onClick = {
                    dialogo = null
                    scope.launch {
                        c.auth.cerrarSesion()
                        onCerrarSesion()
                    }
                }) { Text("Cerrar sesión", color = GColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { dialogo = null }) { Text("Cancelar") } },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tus carreras siguen guardadas en este dispositivo.") },
            containerColor = GColors.BgSecondary,
        )
    }
}

private fun Visibilidad.titulo() = when (this) { Visibilidad.Publico -> "Público"; Visibilidad.Seguidores -> "Solo seguidores"; Visibilidad.Privado -> "Privado" }
private fun Visibilidad.descripcion() = when (this) {
    Visibilidad.Publico -> "Cualquiera puede ver tu perfil y actividades públicas"
    Visibilidad.Seguidores -> "Solo seguidores aceptados pueden ver tus actividades"
    Visibilidad.Privado -> "Solo tú puedes ver tus actividades"
}
private fun Visibilidad.icono(): ImageVector = when (this) { Visibilidad.Publico -> Icons.Filled.Public; Visibilidad.Seguidores -> Icons.Filled.Group; Visibilidad.Privado -> Icons.Filled.Lock }

@Composable
fun PrivacidadUbicacionScreen(onAtras: () -> Unit) {
    val a by ajustesState()
    val guardar = guardarAjuste()
    var radio by remember(a.radioHogarM) { mutableStateOf(a.radioHogarM.toFloat()) }
    PantallaConEncabezado("Privacidad y Ubicación", onAtras) {
        EtiquetaGrupo("Visibilidad del perfil")
        Visibilidad.entries.forEach { v ->
            val activo = v == a.visibilidad
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (activo) GColors.PrimaryContainer.copy(alpha = 0.2f) else GColors.SurfaceContainer)
                    .border(2.dp, if (activo) GColors.Primary else GColors.BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable { guardar { it.copy(visibilidad = v) } }.padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(v.icono(), null, tint = GColors.Primary500)
                    Text("  ${v.titulo()}", style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                    Icon(if (activo) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (activo) GColors.Primary500 else GColors.TextSecondary)
                }
                Text(v.descripcion(), style = GType.bodySm, color = GColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
        }
        EtiquetaGrupo("Ubicación GPS")
        Tarjeta(padding = 0.dp) {
            FilaInterruptor(Icons.Filled.MyLocation, "Ubicación precisa", "Graba tu ubicación exacta durante carreras", a.ubicacionPrecisa, { v -> guardar { it.copy(ubicacionPrecisa = v) } })
        }
        EtiquetaGrupo("Protección de hogar")
        Tarjeta(padding = 0.dp) {
            FilaInterruptor(Icons.Filled.Home, "Ocultar ubicación de casa", "Difumina los puntos de inicio/fin cerca de tu casa", a.ocultarHogar, { v -> guardar { it.copy(ocultarHogar = v) } })
            if (a.ocultarHogar) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row {
                        Text("Radio de protección", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                        Text("${radio.roundToInt()}m", style = GType.labelLg, color = GColors.Primary500)
                    }
                    Slider(
                        value = radio, onValueChange = { radio = (it / 50f).roundToInt() * 50f }, valueRange = 100f..500f, steps = 7,
                        onValueChangeFinished = { guardar { it.copy(radioHogarM = radio.roundToInt()) } },
                        colors = SliderDefaults.colors(thumbColor = GColors.Primary, activeTrackColor = GColors.Primary, inactiveTrackColor = GColors.SurfaceHigh),
                    )
                    Row {
                        Text("100m", style = GType.bodySm, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                        Text("500m", style = GType.bodySm, color = GColors.TextSecondary)
                    }
                }
            }
        }
        EtiquetaGrupo("Compartir en vivo")
        Tarjeta(fondo = GColors.Secondary.copy(alpha = 0.1f), borde = GColors.Secondary.copy(alpha = 0.4f)) {
            Text("Cuando compartes tu ubicación en vivo, solo las personas que elijas pueden verte. Puedes detener el seguimiento en cualquier momento.",
                style = GType.bodyMd, color = GColors.TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, null, tint = GColors.Secondary500)
                Text("  Tu privacidad está protegida", style = GType.labelLg, color = GColors.Secondary500)
            }
        }
    }
}

@Composable
fun ConfiguracionNotificacionesScreen(onAtras: () -> Unit) {
    val a by ajustesState()
    val g = guardarAjuste()
    val sistema by com.gacelas.core.notificaciones.notificacionesPermitidasState()
    val activar = com.gacelas.core.notificaciones.rememberActivarNotificaciones()
    PantallaConEncabezado("Notificaciones", onAtras) {
        EtiquetaGrupo("En este teléfono")
        Tarjeta(padding = 0.dp) {
            // Refleja el estado REAL de Android (permiso + canal), no un ajuste guardado.
            FilaInterruptor(
                androidx.compose.material.icons.Icons.Filled.Notifications, "Notificaciones de Gacelas",
                if (sistema) "Activadas en Android" else "Bloqueadas en Android: no verás la carrera en la barra",
                sistema, { activar() }, if (sistema) GColors.Secondary500 else GColors.Danger,
            )
        }
        if (!sistema) com.gacelas.core.notificaciones.AvisoNotificacionesBloqueadas(
            "Android tiene bloqueadas las notificaciones de Gacelas. Actívalas para ver el tiempo y la distancia de tu carrera y los avisos de la comunidad.",
            activar,
        )
        EtiquetaGrupo("Notificaciones push")
        Tarjeta(padding = 0.dp) {
            FilaInterruptor(Icons.Filled.Favorite, "Likes", "Cuando alguien da like a tu actividad", a.notifLikes, { v -> g { it.copy(notifLikes = v) } }, GColors.Danger)
            FilaInterruptor(Icons.AutoMirrored.Filled.Chat, "Comentarios", "Cuando alguien comenta tu actividad", a.notifComentarios, { v -> g { it.copy(notifComentarios = v) } })
            FilaInterruptor(Icons.Filled.PersonAdd, "Nuevos seguidores", "Cuando alguien te sigue", a.notifSeguidores, { v -> g { it.copy(notifSeguidores = v) } }, GColors.Secondary500)
            FilaInterruptor(Icons.Filled.Sensors, "Carreras en vivo", "Cuando alguien inicia seguimiento en vivo", a.notifEnVivo, { v -> g { it.copy(notifEnVivo = v) } }, GColors.Secondary500)
            FilaInterruptor(Icons.Filled.Event, "Eventos", "Recordatorios de eventos y nuevos eventos", a.notifEventos, { v -> g { it.copy(notifEventos = v) } }, GColors.Tertiary)
        }
        EtiquetaGrupo("Notificaciones por email")
        Tarjeta(padding = 0.dp) {
            FilaInterruptor(Icons.Filled.Mail, "Resumen semanal", "Recibe un resumen de tu actividad cada semana", a.notifEmail, { v -> g { it.copy(notifEmail = v) } }, GColors.Info)
        }
        EtiquetaGrupo("Frecuencia")
        Tarjeta {
            Text("Los likes se agrupan cada 10 o cada 6 horas para evitar notificaciones excesivas.", style = GType.bodyMd, color = GColors.TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, tint = GColors.Info)
                Text("  Configuración optimizada", style = GType.labelLg, color = GColors.Info)
            }
        }
    }
}
