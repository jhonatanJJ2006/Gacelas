package com.gacelas.ajustes

import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
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
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.EtiquetaGrupo
import com.gacelas.ui.components.FilaAjuste
import com.gacelas.ui.components.FilaInterruptor
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

@Composable
private fun EtiquetaSeccion(titulo: String, detalle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { EtiquetaGrupo(titulo) }
        Text(detalle, style = GType.bodySm, color = GColors.TextSecondary)
    }
}

@Composable
private fun Paso(texto: String, tag: String, menos: () -> Unit, mas: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = menos) { Icon(Icons.Filled.Remove, "Menos $tag", tint = GColors.Primary500) }
        Text(texto, style = GType.labelMd, color = GColors.Primary, modifier = Modifier.testTag(tag))
        IconButton(onClick = mas) { Icon(Icons.Filled.Add, "Más $tag", tint = GColors.Primary500) }
    }
}

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
    cabecera: @Composable () -> Unit = {},
    onMapasOffline: (() -> Unit)? = null,
    onBrujula: (() -> Unit)? = null,
    onLegal: ((terminos: Boolean) -> Unit)? = null,
    // Solo llega con valor si el servidor confirmo rol admin o moderador (docs/ADMIN.md).
    onAdmin: (() -> Unit)? = null,
) {
    var dialogo by remember { mutableStateOf<String?>(null) }
    PantallaConEncabezado("Ajustes", onAtras) {
        Text("Club Gacelas Loja • 2,100 msnm", style = GType.bodySm, color = GColors.TextSecondary)
        Tarjeta { cabecera() }
        EtiquetaSeccion("Preferencias de carrera", "Entrenamiento")
        Tarjeta(padding = 0.dp) {
            FilaAjuste(Icons.Filled.Straighten, "Unidades de distancia", "Sistema métrico para rutas andinas", trailing = {
                Row(Modifier.clip(RoundedCornerShape(8.dp)).background(GColors.SurfaceHigh).padding(2.dp)) {
                    listOf("km" to true, "mi" to false).forEach { (t, v) ->
                        val activo = a.unidadesMetricas == v
                        Text(t, style = GType.labelMd, color = if (activo) GColors.OnPrimary else GColors.TextSecondary,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (activo) GColors.Primary else GColors.SurfaceHigh)
                                .clickable { guardar { it.copy(unidadesMetricas = v) } }.padding(horizontal = 12.dp, vertical = 4.dp).testTag("unidad-$t"))
                    }
                }
            })
            FilaAjuste(Icons.Filled.Speed, "Objetivo de ritmo (Pacer)", "Ritmo objetivo predeterminado", trailing = {
                Paso("${a.ritmoObjetivoSeg / 60}:${(a.ritmoObjetivoSeg % 60).toString().padStart(2, '0')} /${if (a.unidadesMetricas) "km" else "mi"}", "ritmo",
                    { guardar { it.copy(ritmoObjetivoSeg = (it.ritmoObjetivoSeg - 15).coerceAtLeast(180)) } },
                    { guardar { it.copy(ritmoObjetivoSeg = (it.ritmoObjetivoSeg + 15).coerceAtMost(600)) } })
            })
            FilaInterruptor(Icons.Filled.PauseCircle, "Autopausa", "Detener al parar en semáforos o cruces", checked = a.autopausa, onChange = { v -> guardar { it.copy(autopausa = v) } })
            FilaInterruptor(Icons.Filled.RecordVoiceOver, "Anuncios de voz", "Métricas audibles cada 1 ${if (a.unidadesMetricas) "km" else "mi"}", checked = a.anunciosVoz, onChange = { v -> guardar { it.copy(anunciosVoz = v) } })
            FilaAjuste(Icons.Filled.Timer, "Cuenta regresiva de inicio", "Tiempo antes de arrancar el GPS", trailing = {
                val opciones = listOf(0, 3, 5, 10)
                Text(if (a.cuentaRegresivaSeg == 0) "Sin cuenta" else "${a.cuentaRegresivaSeg} seg", style = GType.labelMd, color = GColors.Secondary500,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GColors.SurfaceHigh)
                        .clickable { guardar { it.copy(cuentaRegresivaSeg = opciones[(opciones.indexOf(it.cuentaRegresivaSeg) + 1) % opciones.size]) } }
                        .padding(horizontal = 10.dp, vertical = 4.dp).testTag("cuenta-regresiva"))
            })
        }
        EtiquetaSeccion("Apariencia", "Interfaz")
        Tarjeta(padding = 0.dp) {
            FilaAjuste(Icons.Filled.DarkMode, "Tema", "Optimizado para salidas nocturnas y al alba", onClick = { guardar { it.copy(modoOscuro = !it.modoOscuro) } }, trailing = {
                Text(if (a.modoOscuro) "Oscuro" else "Claro", style = GType.labelMd, color = GColors.Primary)
            })
            FilaAjuste(Icons.Filled.Language, "Idioma de la aplicación", "Configuración regional", onClick = { guardar { it.copy(idioma = if (it.idioma == "en") "es-EC" else "en") } }, trailing = {
                Text(if (a.idioma == "en") "English" else "Español (Ecuador)", style = GType.labelMd, color = GColors.TextPrimary)
            })
        }
        EtiquetaGrupo("Mapa")
        if (onMapasOffline != null || onBrujula != null) Tarjeta(padding = 0.dp) {
            onMapasOffline?.let { FilaAjuste(Icons.Filled.Download, "Mapas offline", "Rutas de Loja sin conexión", onClick = it) }
            onBrujula?.let { FilaAjuste(Icons.Filled.Explore, "Brújula", "Rumbo y calibración", onClick = it) }
        }
        mapaOffline()
        EtiquetaSeccion("Rachas", "Constancia")
        Tarjeta(padding = 0.dp) {
            FilaAjuste(Icons.Filled.LocalFireDepartment, "Meta semanal", "Carreras por semana para mantener la racha", trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { guardar { it.copy(metaSemanal = (it.metaSemanal - 1).coerceAtLeast(1)) } }) {
                        Icon(Icons.Filled.Remove, "Menos carreras por semana", tint = GColors.Primary500)
                    }
                    Text("${a.metaSemanal}", style = GType.titleMd, color = GColors.TextPrimary)
                    IconButton(onClick = { guardar { it.copy(metaSemanal = (it.metaSemanal + 1).coerceAtMost(7)) } }) {
                        Icon(Icons.Filled.Add, "Más carreras por semana", tint = GColors.Primary500)
                    }
                }
            })
        }
        if (onAdmin != null) {
            EtiquetaSeccion("Administración", "Solo para tu rol")
            Tarjeta(padding = 0.dp, modifier = Modifier.testTag("ajustes-admin")) {
                FilaAjuste(Icons.Filled.AdminPanelSettings, "Panel de administración", "Usuarios, moderación y métricas", colorIcono = GColors.C2Violeta, onClick = onAdmin)
            }
        }
        EtiquetaSeccion("Cuenta", "Seguridad y dispositivos")
        Tarjeta(padding = 0.dp) {
            FilaAjuste(Icons.Filled.Person, "Editar perfil", onClick = onEditarPerfil)
            FilaAjuste(Icons.Filled.Lock, "Privacidad y ubicación", onClick = onPrivacidad)
            FilaAjuste(Icons.Filled.Notifications, "Notificaciones", onClick = onNotificaciones)
            FilaAjuste(Icons.Filled.Info, "Acerca de", version, onClick = { dialogo = "acerca" })
            FilaAjuste(Icons.AutoMirrored.Filled.Help, "Ayuda y soporte", onClick = { dialogo = "ayuda" })
            if (onLegal != null) {
                FilaAjuste(Icons.Filled.Info, "Términos y condiciones", onClick = { onLegal(true) })
                FilaAjuste(Icons.Filled.Info, "Política de privacidad", onClick = { onLegal(false) })
            }
        }
        Tarjeta(padding = 0.dp, fondo = GColors.Danger.copy(alpha = 0.1f), borde = GColors.Danger.copy(alpha = 0.4f)) {
            FilaAjuste(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", colorIcono = GColors.Danger, onClick = { dialogo = "salir" }, trailing = {})
        }
    }
    when (dialogo) {
        "acerca" -> com.gacelas.ui.components.DialogoC2(
            onDismissRequest = { dialogo = null },
            confirmButton = { com.gacelas.ui.components.BotonModalC2("Cerrar", { dialogo = null }) },
            title = { com.gacelas.ui.components.MarcaGacelasA(40.dp, "LOJA, ECUADOR", tamTexto = 18f) },
            text = { Text("${version}. App del grupo de running Runcity de Loja, Ecuador. Graba tus carreras con GPS, descubre rutas y entrena con la comunidad.\n\nDatos del clima: Open-Meteo.com (CC BY 4.0).") },
            containerColor = GColors.BgSecondary,
        )
        "ayuda" -> com.gacelas.ui.components.DialogoC2(
            onDismissRequest = { dialogo = null },
            confirmButton = {
                com.gacelas.ui.components.BotonModalC2("Escribir a soporte", {
                    dialogo = null
                    onSoporte()
                })
            },
            dismissButton = { com.gacelas.ui.components.BotonModalC2("Cerrar", { dialogo = null }, secundario = true) },
            title = { Text("Ayuda y soporte") },
            text = { Text("Para grabar una carrera toca el botón central Correr y da permiso de ubicación. Las carreras se guardan en tu teléfono y aparecen en Perfil e Inicio.") },
            containerColor = GColors.BgSecondary,
        )
        "salir" -> com.gacelas.ui.components.DialogoC2(
            onDismissRequest = { dialogo = null },
            confirmButton = {
                com.gacelas.ui.components.BotonModalC2("Cerrar sesión", {
                    dialogo = null
                    onCerrarSesion()
                }, peligro = true)
            },
            dismissButton = { com.gacelas.ui.components.BotonModalC2("Cancelar", { dialogo = null }, secundario = true) },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tus carreras guardadas siguen en tu cuenta. Las que estén sin sincronizar se subirán la próxima vez que entres.") },
            containerColor = GColors.BgSecondary,
        )
    }
}
