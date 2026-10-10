package com.gacelas.ui.components

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
fun BarraSuperior(onNotificaciones: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(GColors.BgSecondary).statusBarsPadding().height(64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(GColors.PrimaryLight), contentAlignment = Alignment.Center) {
            Text("GL", style = GType.labelMd, color = GColors.OnPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text("GACELAS LOJA", style = GType.labelMd, color = GColors.PrimaryLight, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = onNotificaciones) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones", tint = GColors.TextSecondary)
            }
            Box(Modifier.align(Alignment.TopEnd).offset((-12).dp, 12.dp).size(8.dp).clip(CircleShape).background(GColors.PrimaryContainer))
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
            if (item.principal) {
                Box(
                    Modifier.offset(y = (-14).dp).size(56.dp).shadow(8.dp, CircleShape).clip(CircleShape)
                        .background(GColors.PrimaryLight).clickable { onNavegar(item.ruta) }.testTag("nav-correr"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icono, contentDescription = item.etiqueta, tint = GColors.OnPrimary, modifier = Modifier.size(32.dp))
                }
            } else {
                val activo = rutaActual == item.ruta || (item.ruta == Destinos.COMUNIDAD && rutaActual == Destinos.EVENTOS)
                val color = if (activo) GColors.PrimaryLight else GColors.TextSecondary
                Column(
                    Modifier.clip(CircleShape).clickable { onNavegar(item.ruta) }.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(item.icono, contentDescription = null, tint = color)
                    Text(item.etiqueta, style = GType.labelMd, color = color)
                }
            }
        }
    }
}

/** Scaffold con barra superior y barra inferior para las pantallas principales. */
@Composable
fun ScaffoldPrincipal(rutaActual: String, onNavegar: (String) -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = GColors.BgPrimary,
        topBar = { BarraSuperior { onNavegar(Destinos.NOTIFICACIONES) } },
        bottomBar = { BarraInferior(rutaActual, onNavegar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
    }
}
