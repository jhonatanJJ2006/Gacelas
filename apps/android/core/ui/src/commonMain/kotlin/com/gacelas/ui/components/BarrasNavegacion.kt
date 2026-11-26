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
import com.gacelas.ui.LocalIdioma
import com.gacelas.ui.tr
import com.gacelas.ui.navegacion.BarraNavColapsable
import com.gacelas.ui.navegacion.ItemNavColapsable
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
        Text("RUNCITY", style = GType.headlineSm.copy(fontWeight = FontWeight.Normal), color = GColors.TextPrimary, modifier = Modifier.weight(1f))
        BotonVidrio(Icons.Filled.Notifications, "Notificaciones", onNotificaciones, tag = "boton-notificaciones", tamano = 42.dp, punto = sinLeer > 0)
        Spacer(Modifier.width(8.dp))
        BotonVidrio(Icons.Filled.Person, "Perfil", onPerfil, tag = "boton-perfil-barra", tamano = 42.dp)
    }
}

/**
 * Barra inferior de las pantallas principales: navbar B "menu colapsable" (burbuja morada elevada
 * que salta a la seccion activa), el mismo componente que usa el panel de administracion. Las
 * secciones son las de [itemsNav]; no lleva insignias (no hay conteos reales por pestana).
 */
@Composable
fun BarraInferior(rutaActual: String, onNavegar: (String) -> Unit) {
    val idioma = LocalIdioma.current
    val items = itemsNav.map { ItemNavColapsable(id = it.ruta, etiqueta = tr(idioma, it.etiqueta), icono = it.icono) }
    val activa = if (rutaActual == Destinos.EVENTOS) Destinos.COMUNIDAD else rutaActual
    BarraNavColapsable(items = items, seleccionado = activa, onElegir = onNavegar, prefijoTag = "nav")
}

/** Alto de la barra (burbuja + barra + margenes, sin la barra de gestos); el contenido deja este espacio libre. */
private val AltoBarraInferior = 94.dp

/** Espacio que el dock ocupa abajo; las pantallas que dibujan bajo el dock (mapa de Rutas) lo usan para no tapar botones. */
val LocalEspacioDock = androidx.compose.runtime.staticCompositionLocalOf { 0.dp }

/**
 * Scaffold de las pantallas principales (direccion A "Tablero"): fondo morado con degradado, dock
 * barra inferior con burbuja (navbar B colapsable) y, salvo en Inicio y Rutas (el mapa llega al borde superior), la
 * barra superior. El contenido reserva el alto del dock para que nunca tape un boton; Rutas dibuja el
 * mapa por debajo y usa [LocalEspacioDock] para subir sus tarjetas.
 */
@Composable
fun ScaffoldPrincipal(rutaActual: String, sinLeer: Int, onNavegar: (String) -> Unit, content: @Composable () -> Unit) {
    val nav = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val espacio = AltoBarraInferior + nav
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
        // Barra clasica con Correr al centro: el usuario la prefiere al dock flotante de 0.9.17.
        Box(Modifier.align(Alignment.BottomCenter)) { BarraInferior(rutaActual, onNavegar) }
    }
}
