package com.gacelas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog

/**
 * Anfitrion de las hojas y modales C2 dentro de la MISMA ventana de la actividad.
 *
 * v0.9.1: en algunos telefonos con Android 15 la ventana propia de Dialog (aunque sea
 * decorFitsSystemWindows = false) se coloca debajo de la barra de estado y mide la pantalla completa,
 * asi que su borde inferior queda fuera de la pantalla y el ultimo boton se corta bajo la barra de
 * navegacion. Dibujando la hoja como capa encima de la app, los insets son los de la actividad
 * (edge-to-edge) y la hoja queda pegada al borde inferior en todos los dispositivos.
 */
class HojasC2 {
    internal val pila = mutableStateListOf<Pair<Any, @Composable () -> Unit>>()
    val abiertas: Int get() = pila.size
}

val LocalHojasC2 = staticCompositionLocalOf<HojasC2?> { null }

/** Colocar al final del contenedor raiz (fillMaxSize), encima de toda la app. */
@Composable
fun AnfitrionHojasC2(hojas: HojasC2) {
    hojas.pila.forEach { (clave, contenido) -> key(clave) { contenido() } }
}

/** Atras del sistema mientras la hoja esta abierta. */
@Composable
internal expect fun AtrasHojaC2(onAtras: () -> Unit)

/**
 * Capa de una hoja C2: velo + contenido a pantalla completa. Usa el anfitrion si existe; si no
 * (previews, pruebas aisladas) cae a un Dialog con propiedadesHojaC2().
 */
@Composable
fun CapaHojaC2(onDismissRequest: () -> Unit, velo: Boolean = true, contenido: @Composable () -> Unit) {
    val hojas = LocalHojasC2.current
    if (hojas == null) {
        Dialog(onDismissRequest = onDismissRequest, properties = propiedadesHojaC2()) { contenido() }
        return
    }
    val actual by rememberUpdatedState(contenido)
    val cerrar by rememberUpdatedState(onDismissRequest)
    val clave = remember { Any() }
    DisposableEffect(hojas, clave) {
        hojas.pila.add(clave to @Composable {
            AtrasHojaC2 { cerrar() }
            Box(Modifier.fillMaxSize().background(if (velo) Color.Black.copy(alpha = 0.6f) else Color.Transparent).testTag("hoja-c2-velo")) { actual() }
        })
        onDispose { hojas.pila.removeAll { it.first === clave } }
    }
}

/**
 * Hoja inferior C2 generica (sustituye a ModalBottomSheet): pegada al borde inferior, con el alto
 * limitado a lo visible. El contenido aplica navigationBarsPadding() a su final.
 */
@Composable
fun HojaC2(
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    fondo: Color = GColors.C2Bg,
    contenido: @Composable () -> Unit,
) {
    CapaHojaC2(onCerrar) {
        BoxWithConstraints(
            Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = onCerrar)
                .statusBarsPadding().testTag("hoja-c2-ventana"),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val altoMax = maxHeight - 24.dp
            Column(
                modifier.fillMaxWidth().heightIn(max = altoMax)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(fondo)
                    .clickable(remember { MutableInteractionSource() }, null) {}.testTag("hoja-c2"),
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(GColors.C2Linea))
                }
                contenido()
            }
        }
    }
}
