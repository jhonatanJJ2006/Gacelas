package com.gacelas.notificaciones

import com.gacelas.comunidad.domain.reglas.notificacionesVisibles
import com.gacelas.comunidad.domain.reglas.filtrarNotificaciones
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.runtime.rememberCoroutineScope
import com.gacelas.data.mock.MockData
import com.gacelas.ui.ajustesState
import com.gacelas.ui.contenedor
import kotlinx.coroutines.launch
import com.gacelas.comunidad.domain.modelo.Notificacion
import com.gacelas.comunidad.domain.modelo.TipoNotificacion
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
    TipoNotificacion.Racha -> Icons.Filled.LocalFireDepartment to GColors.Primary500
}

@Composable
fun NotificacionesScreen(onAtras: () -> Unit, onAbrir: (Notificacion) -> Unit = {}) {
    val c = contenedor()
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { com.gacelas.comunidad.presentation.NotificacionesViewModel(c.notificaciones, c.ajustes, MockData.notificaciones) }
    val filtro by vm.filtro.collectAsState()
    val lista by vm.lista.collectAsState()
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras("Notificaciones", onAtras) {
            Text("Marcar todas leídas", style = GType.labelMd, color = GColors.Primary500, modifier = Modifier.clickable {
                vm.marcarTodas()
            }.padding(12.dp))
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "Sin leer", "En vivo").forEach { Pastilla(it, it == filtro, { vm.filtro(it) }) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            if (lista.isEmpty()) Text("No hay notificaciones.", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.padding(16.dp))
            lista.forEach { n ->
                val (icono, color) = iconoNotificacion(n.tipo)
                Row(
                    Modifier.fillMaxWidth().background(if (!n.leida) GColors.SurfaceContainer.copy(alpha = 0.5f) else Color.Transparent)
                        .clickable {
                            vm.marcar(listOf(n.id))
                            onAbrir(n)
                        }.padding(horizontal = 16.dp, vertical = 14.dp),
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

/** Notificaciones de la cuenta si hay servidor; si no, las de ejemplo. Se refrescan al mostrarse. */
@Composable
fun notificacionesBase(): androidx.compose.runtime.State<List<Notificacion>> {
    val repo = contenedor().notificaciones ?: return androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(MockData.notificaciones) }
    androidx.compose.runtime.LaunchedEffect(repo) { repo.refrescar() }
    return repo.lista.collectAsState()
}
