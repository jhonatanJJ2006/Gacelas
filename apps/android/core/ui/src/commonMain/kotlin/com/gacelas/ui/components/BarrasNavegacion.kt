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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
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
    // Tablero: cabecera transparente sobre el degradado, marca a la izquierda y botones de vidrio.
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(62.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("RUNCITY", style = GType.headlineSm.copy(fontWeight = FontWeight.Normal), color = Color.White, modifier = Modifier.weight(1f))
        BotonVidrio(Icons.Filled.Notifications, "Notificaciones", onNotificaciones, tag = "boton-notificaciones", tamano = 42.dp, punto = sinLeer > 0)
        Spacer(Modifier.width(8.dp))
        BotonVidrio(Icons.Filled.Person, "Perfil", onPerfil, tag = "boton-perfil-barra", tamano = 42.dp)
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

/** Espacio que el dock ocupa abajo; las pantallas que dibujan bajo el dock (mapa de Rutas) lo usan para no tapar botones. */
val LocalEspacioDock = androidx.compose.runtime.staticCompositionLocalOf { 0.dp }

/**
 * Scaffold de las pantallas principales (direccion A "Tablero"): fondo morado con degradado, dock
 * flotante con CORRER a la derecha y, salvo en Inicio y Rutas (el mapa llega al borde superior), la
 * barra superior. El contenido reserva el alto del dock para que nunca tape un boton; Rutas dibuja el
 * mapa por debajo y usa [LocalEspacioDock] para subir sus tarjetas.
 */
@Composable
fun ScaffoldPrincipal(rutaActual: String, sinLeer: Int, onNavegar: (String) -> Unit, content: @Composable () -> Unit) {
    val nav = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val espacio = AltoDock + nav
    val bajoDock = rutaActual == Destinos.RUTAS
    Box(Modifier.fillMaxSize().background(Tablero.Fondo)) {
        Column(Modifier.fillMaxSize()) {
            when (rutaActual) {
                Destinos.INICIO, Destinos.RUTAS -> {}
                Destinos.COMUNIDAD -> Spacer(Modifier.fillMaxWidth().statusBarsPadding())
                else -> BarraSuperior(sinLeer, onNotificaciones = { onNavegar(Destinos.NOTIFICACIONES) }, onPerfil = { onNavegar(Destinos.PERFIL) })
            }
            Box(Modifier.fillMaxSize().padding(bottom = if (bajoDock) 0.dp else espacio)) {
                androidx.compose.runtime.CompositionLocalProvider(LocalEspacioDock provides if (bajoDock) espacio else 0.dp) { content() }
            }
        }
        DockTablero(rutaActual, onNavegar, Modifier.align(Alignment.BottomCenter))
    }
}
