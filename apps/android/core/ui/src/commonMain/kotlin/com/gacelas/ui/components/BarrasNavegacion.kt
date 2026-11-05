package com.gacelas.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.gacelas.ui.movimiento.Mov
import com.gacelas.ui.movimiento.pulsacionC2
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import com.gacelas.navigation.Destinos
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

data class ItemNav(val ruta: String, val etiqueta: String, val icono: ImageVector, val principal: Boolean = false)

val itemsNav = listOf(
    ItemNav(Destinos.INICIO, "Inicio", Icons.Filled.Home),
    ItemNav(Destinos.RUTAS, "Rutas", Icons.Filled.Map),
    ItemNav(Destinos.CORRER, "Correr", Icons.Filled.PlayArrow, principal = true),
    ItemNav(Destinos.COMUNIDAD, "Comunidad", Icons.Filled.Groups),
    ItemNav(Destinos.PERFIL, "Perfil", Icons.Filled.Person),
)

@Composable
fun BarraSuperior(sinLeer: Int, onNotificaciones: () -> Unit, onPerfil: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().background(GColors.BgSecondary).statusBarsPadding().height(64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(GColors.PrimaryLight).clickable(onClick = onPerfil), contentAlignment = Alignment.Center) {
            Text("GL", style = GType.labelMd, color = GColors.OnPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text("RUNCITY LOJA", style = GType.labelMd, color = GColors.PrimaryLight, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = onNotificaciones) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones", tint = GColors.TextSecondary)
            }
            if (sinLeer > 0) Box(Modifier.align(Alignment.TopEnd).offset((-12).dp, 12.dp).size(8.dp).clip(CircleShape).background(GColors.PrimaryContainer))
        }
    }
}

@Composable
fun BarraInferior(rutaActual: String, onNavegar: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(GColors.BgSecondary).navigationBarsPadding().height(72.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsNav.forEach { item ->
            val interaccion = remember { MutableInteractionSource() }
            if (item.principal) {
                Box(
                    Modifier.offset(y = (-14).dp).pulsacionC2(interaccion).size(56.dp).shadow(8.dp, CircleShape).clip(CircleShape)
                        .background(GColors.PrimaryLight).clickable(interaccion, androidx.compose.material3.ripple()) { onNavegar(item.ruta) }.testTag("nav-correr"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icono, contentDescription = item.etiqueta, tint = GColors.OnPrimary, modifier = Modifier.size(32.dp))
                }
            } else {
                val activo = rutaActual == item.ruta || (item.ruta == Destinos.COMUNIDAD && rutaActual == Destinos.EVENTOS)
                // Pestana activa: color e icono animados (CORTO); el icono sube 2 dp y crece un poco.
                val color by animateColorAsState(if (activo) GColors.PrimaryLight else GColors.TextSecondary, Mov.corto(), label = "pestana")
                val realce by animateFloatAsState(if (activo) 1f else 0f, Mov.medio(), label = "pestana-realce")
                Column(
                    Modifier.pulsacionC2(interaccion).clip(CircleShape).clickable(interaccion, androidx.compose.material3.ripple()) { onNavegar(item.ruta) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(item.icono, contentDescription = null, tint = color, modifier = Modifier.graphicsLayer {
                        val e = 1f + 0.1f * realce
                        scaleX = e; scaleY = e; translationY = -2.dp.toPx() * realce
                    })
                    Text(item.etiqueta, style = GType.labelMd, color = color)
                }
            }
        }
    }
}

/** Scaffold con barra superior y barra inferior para las pantallas principales. */
@Composable
fun ScaffoldPrincipal(rutaActual: String, sinLeer: Int, onNavegar: (String) -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = GColors.BgPrimary,
        topBar = {
            // Comunidad trae su propia cabecera C2 (avatar, titulo centrado y buscar).
            if (rutaActual == Destinos.COMUNIDAD) Spacer(Modifier.fillMaxWidth().background(GColors.C2Bg).statusBarsPadding())
            else BarraSuperior(sinLeer, onNotificaciones = { onNavegar(Destinos.NOTIFICACIONES) }, onPerfil = { onNavegar(Destinos.PERFIL) })
        },
        bottomBar = { BarraInferior(rutaActual, onNavegar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
    }
}
