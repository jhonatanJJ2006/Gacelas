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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    // Rediseno movil: barra C2 con borde superior, capsula en la pestana activa y el boton de correr al
    // centro, mas grande, con halo morado y etiqueta. Respeta la barra de gestos (navigationBarsPadding).
    Column(Modifier.fillMaxWidth().background(GColors.C2Bg).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(GColors.C2Linea))
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsNav.forEach { item ->
                val interaccion = remember { MutableInteractionSource() }
                if (item.principal) {
                    Column(
                        Modifier.weight(1f).wrapContentHeight(Alignment.Bottom, unbounded = true).offset(y = (-4).dp).pulsacionC2(interaccion).clickable(interaccion, null, role = androidx.compose.ui.semantics.Role.Button) { onNavegar(item.ruta) }
                            .testTag("nav-correr"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(66.dp).clip(CircleShape).background(GColors.C2Morado.copy(alpha = 0.24f)), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(56.dp).shadow(10.dp, CircleShape, ambientColor = GColors.C2Morado, spotColor = GColors.C2Morado).clip(CircleShape)
                                    .background(GradienteC2),
                                contentAlignment = Alignment.Center,
                            ) { Icon(item.icono, contentDescription = item.etiqueta, tint = Color.White, modifier = Modifier.size(32.dp)) }
                        }
                        Text(item.etiqueta, style = GType.labelMd.copy(fontWeight = FontWeight.ExtraBold), color = GColors.C2Violeta, maxLines = 1)
                    }
                } else {
                    val activo = rutaActual == item.ruta || (item.ruta == Destinos.COMUNIDAD && rutaActual == Destinos.EVENTOS)
                    val color by animateColorAsState(if (activo) GColors.C2Violeta else GColors.C2Muted, Mov.corto(), label = "pestana")
                    val capsula by animateColorAsState(if (activo) GColors.C2Morado.copy(alpha = 0.22f) else Color.Transparent, Mov.corto(), label = "capsula")
                    Column(
                        Modifier.weight(1f).pulsacionC2(interaccion).clickable(interaccion, null, role = androidx.compose.ui.semantics.Role.Tab) { onNavegar(item.ruta) }
                            .semantics { selected = activo }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.width(54.dp).height(30.dp).clip(CircleShape).background(capsula), contentAlignment = Alignment.Center) {
                            Icon(item.icono, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                        }
                        TextoAjustableC2(item.etiqueta, GType.labelMd.copy(fontWeight = if (activo) FontWeight.ExtraBold else FontWeight.SemiBold), color,
                            Modifier.padding(top = 2.dp), minimo = 0.7f)
                    }
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
