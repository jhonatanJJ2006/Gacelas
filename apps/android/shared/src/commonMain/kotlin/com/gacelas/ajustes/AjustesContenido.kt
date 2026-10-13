package com.gacelas.ajustes

import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.unit.dp
import com.gacelas.data.ajustes.Ajustes
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.EtiquetaGrupo
import com.gacelas.ui.components.FilaAjuste
import com.gacelas.ui.components.FilaInterruptor
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

@Composable
fun PantallaConEncabezado(titulo: String, onAtras: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras(titulo, onAtras)
        Pantalla(Modifier.weight(1f).navigationBarsPadding()) { content() }
    }
}

/**
 * Ajustes (compartido Android/iOS). [guardar] aplica un cambio a los ajustes; [onSoporte] abre el
 * correo de soporte; [onCerrarSesion] se llama tras confirmar; [mapaOffline] es la tarjeta del mapa.
 */
@Composable
fun AjustesContenido(
    a: Ajustes,
    version: String,
    guardar: ((Ajustes) -> Ajustes) -> Unit,
    onAtras: () -> Unit,
    onEditarPerfil: () -> Unit,
    onPrivacidad: () -> Unit,
    onNotificaciones: () -> Unit,
    onSoporte: () -> Unit,
    onCerrarSesion: () -> Unit,
    mapaOffline: @Composable () -> Unit,
) {
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
        mapaOffline()
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
            FilaAjuste(Icons.Filled.Info, "Acerca de", version, onClick = { dialogo = "acerca" })
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
            text = { Text("${version}. App del grupo de running Gacelas de Loja, Ecuador. Graba tus carreras con GPS, descubre rutas y entrena con la comunidad.") },
            containerColor = GColors.BgSecondary,
        )
        "ayuda" -> AlertDialog(
            onDismissRequest = { dialogo = null },
            confirmButton = {
                TextButton(onClick = {
                    dialogo = null
                    onSoporte()
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
                    onCerrarSesion()
                }) { Text("Cerrar sesión", color = GColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { dialogo = null }) { Text("Cancelar") } },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tus carreras siguen guardadas en este dispositivo.") },
            containerColor = GColors.BgSecondary,
        )
    }
}
