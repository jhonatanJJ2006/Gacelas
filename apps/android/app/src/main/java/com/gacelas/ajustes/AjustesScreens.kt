package com.gacelas.ajustes

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.EtiquetaGrupo
import com.gacelas.ui.components.FilaAjuste
import com.gacelas.ui.components.FilaInterruptor
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlin.math.roundToInt

@Composable
private fun PantallaConEncabezado(titulo: String, onAtras: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras(titulo, onAtras)
        Pantalla(Modifier.weight(1f).navigationBarsPadding()) { content() }
    }
}

@Composable
fun AjustesPreferenciasScreen(
    onAtras: () -> Unit,
    onEditarPerfil: () -> Unit,
    onPrivacidad: () -> Unit,
    onNotificaciones: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    var oscuro by rememberSaveable { mutableStateOf(true) }
    var metricas by rememberSaveable { mutableStateOf(true) }
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
            FilaInterruptor(Icons.Filled.DarkMode, "Modo oscuro", checked = oscuro, onChange = { oscuro = it })
            FilaInterruptor(Icons.Filled.Straighten, "Unidades métricas", "Kilómetros y metros", checked = metricas, onChange = { metricas = it })
        }
        EtiquetaGrupo("Información")
        Tarjeta(padding = 0.dp) {
            FilaAjuste(Icons.Filled.Info, "Acerca de", "Versión 0.1.0", onClick = {})
            FilaAjuste(Icons.AutoMirrored.Filled.Help, "Ayuda y soporte", onClick = {})
        }
        Tarjeta(padding = 0.dp, fondo = GColors.Danger.copy(alpha = 0.1f), borde = GColors.Danger.copy(alpha = 0.4f)) {
            FilaAjuste(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", colorIcono = GColors.Danger, onClick = onCerrarSesion, trailing = {})
        }
    }
}

private enum class Visibilidad(val titulo: String, val descripcion: String, val icono: ImageVector) {
    Publico("Público", "Cualquiera puede ver tu perfil y actividades públicas", Icons.Filled.Public),
    Seguidores("Solo seguidores", "Solo seguidores aceptados pueden ver tus actividades", Icons.Filled.Group),
    Privado("Privado", "Solo tú puedes ver tus actividades", Icons.Filled.Lock),
}

@Composable
fun PrivacidadUbicacionScreen(onAtras: () -> Unit) {
    var visibilidad by rememberSaveable { mutableStateOf(Visibilidad.Seguidores) }
    var precisa by rememberSaveable { mutableStateOf(true) }
    var hogar by rememberSaveable { mutableStateOf(true) }
    var radio by rememberSaveable { mutableFloatStateOf(300f) }
    PantallaConEncabezado("Privacidad y Ubicación", onAtras) {
        EtiquetaGrupo("Visibilidad del perfil")
        Visibilidad.entries.forEach { v ->
            val activo = v == visibilidad
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (activo) GColors.PrimaryContainer.copy(alpha = 0.2f) else GColors.SurfaceContainer)
                    .border(2.dp, if (activo) GColors.Primary else GColors.BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable { visibilidad = v }.padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(v.icono, null, tint = GColors.Primary500)
                    Text("  ${v.titulo}", style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                    Icon(if (activo) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (activo) GColors.Primary500 else GColors.TextSecondary)
                }
                Text(v.descripcion, style = GType.bodySm, color = GColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
        }
        EtiquetaGrupo("Ubicación GPS")
        Tarjeta(padding = 0.dp) {
            FilaInterruptor(Icons.Filled.MyLocation, "Ubicación precisa", "Graba tu ubicación exacta durante carreras", precisa, { precisa = it })
        }
        EtiquetaGrupo("Protección de hogar")
        Tarjeta(padding = 0.dp) {
            FilaInterruptor(Icons.Filled.Home, "Ocultar ubicación de casa", "Difumina los puntos de inicio/fin cerca de tu casa", hogar, { hogar = it })
            if (hogar) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row {
                        Text("Radio de protección", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                        Text("${radio.roundToInt()}m", style = GType.labelLg, color = GColors.Primary500)
                    }
                    Slider(
                        value = radio, onValueChange = { radio = (it / 50f).roundToInt() * 50f }, valueRange = 100f..500f, steps = 7,
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
    var likes by rememberSaveable { mutableStateOf(true) }
    var comentarios by rememberSaveable { mutableStateOf(true) }
    var seguidores by rememberSaveable { mutableStateOf(true) }
    var vivo by rememberSaveable { mutableStateOf(true) }
    var eventos by rememberSaveable { mutableStateOf(true) }
    var email by rememberSaveable { mutableStateOf(false) }
    PantallaConEncabezado("Notificaciones", onAtras) {
        EtiquetaGrupo("Notificaciones push")
        Tarjeta(padding = 0.dp) {
            FilaInterruptor(Icons.Filled.Favorite, "Likes", "Cuando alguien da like a tu actividad", likes, { likes = it }, GColors.Danger)
            FilaInterruptor(Icons.AutoMirrored.Filled.Chat, "Comentarios", "Cuando alguien comenta tu actividad", comentarios, { comentarios = it })
            FilaInterruptor(Icons.Filled.PersonAdd, "Nuevos seguidores", "Cuando alguien te sigue", seguidores, { seguidores = it }, GColors.Secondary500)
            FilaInterruptor(Icons.Filled.Sensors, "Carreras en vivo", "Cuando alguien inicia seguimiento en vivo", vivo, { vivo = it }, GColors.Secondary500)
            FilaInterruptor(Icons.Filled.Event, "Eventos", "Recordatorios de eventos y nuevos eventos", eventos, { eventos = it }, GColors.Tertiary)
        }
        EtiquetaGrupo("Notificaciones por email")
        Tarjeta(padding = 0.dp) {
            FilaInterruptor(Icons.Filled.Mail, "Resumen semanal", "Recibe un resumen de tu actividad cada semana", email, { email = it }, GColors.Info)
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

