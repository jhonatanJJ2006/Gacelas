package com.gacelas.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.gacelas.data.mock.MockData
import com.gacelas.data.mock.Notificacion
import com.gacelas.data.mock.TipoNotificacion
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

fun iconoNotificacion(tipo: TipoNotificacion): Pair<ImageVector, Color> = when (tipo) {
    TipoNotificacion.Like -> Icons.Filled.Favorite to GColors.Danger
    TipoNotificacion.Comentario -> Icons.AutoMirrored.Filled.Chat to GColors.Primary500
    TipoNotificacion.Seguidor -> Icons.Filled.PersonAdd to GColors.Secondary500
    TipoNotificacion.Evento -> Icons.Filled.Event to GColors.Tertiary
    TipoNotificacion.EnVivo -> Icons.Filled.Sensors to GColors.Secondary500
}

/** Filtra notificaciones segun la pestana activa. */
fun filtrarNotificaciones(lista: List<Notificacion>, filtro: String): List<Notificacion> = when (filtro) {
    "Sin leer" -> lista.filter { !it.leida }
    "En vivo" -> lista.filter { it.tipo == TipoNotificacion.EnVivo }
    else -> lista
}

@Composable
fun NotificacionesScreen(onAtras: () -> Unit) {
    var filtro by rememberSaveable { mutableStateOf("Todas") }
    var todasLeidas by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras("Notificaciones", onAtras) {
            Text("Marcar todas leídas", style = GType.labelMd, color = GColors.Primary500, modifier = Modifier.clickable { todasLeidas = true }.padding(12.dp))
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "Sin leer", "En vivo").forEach { Pastilla(it, it == filtro, { filtro = it }) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            filtrarNotificaciones(MockData.notificaciones.map { if (todasLeidas) it.copy(leida = true) else it }, filtro).forEach { n ->
                val (icono, color) = iconoNotificacion(n.tipo)
                Row(
                    Modifier.fillMaxWidth().background(if (!n.leida) GColors.SurfaceContainer.copy(alpha = 0.5f) else Color.Transparent)
                        .clickable {}.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(icono, null, tint = color)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = GColors.TextPrimary)) { append(n.usuario) }
                                append(" ${n.mensaje}")
                            },
                            style = GType.bodyMd, color = GColors.TextSecondary,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(n.tiempo, style = GType.bodySm, color = GColors.TextSecondary)
                            if (!n.leida) Box(Modifier.padding(start = 8.dp).size(8.dp).clip(CircleShape).background(GColors.Primary))
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = GColors.TextSecondary)
                }
                HorizontalDivider(color = GColors.BorderSubtle.copy(alpha = 0.5f))
            }
        }
    }
}
