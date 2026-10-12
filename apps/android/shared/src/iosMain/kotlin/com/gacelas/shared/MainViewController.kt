package com.gacelas.shared

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.gacelas.core.sensores.SeguimientoSensores
import com.gacelas.core.sensores.fuenteRumboNativa
import com.gacelas.core.sensores.fuenteUbicacionNativa
import kotlinx.coroutines.delay
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIViewController

/** Argumento de lanzamiento (CI): abre directamente la pantalla de sensores con una carrera en curso. */
private fun argumento(a: String) = NSProcessInfo.processInfo.arguments.contains(a)

/** Punto de entrada de iOS: SwiftUI (iosApp) muestra este UIViewController de Compose. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val auto = remember { argumento("--sensores") }
    var enSensores by remember { mutableStateOf(auto) }
    if (!enSensores) {
        InicioComun(onEmpezar = { enSensores = true })
    } else {
        val gps = remember { fuenteUbicacionNativa() }
        val s = remember { SeguimientoSensores(gps, fuenteRumboNativa()) }
        DisposableEffect(s) { s.iniciar(carrera = auto); onDispose { s.detener() } }
        LaunchedEffect(s) { while (true) { delay(1000); s.tic() } }
        val e by s.estado.collectAsState()
        PantallaSensores(
            e,
            diagnostico = gps?.diagnostico.orEmpty(),
            onCarrera = { if (s.tracker.estado.value.activa) s.tracker.finalizar() else s.tracker.iniciar(); s.tic() },
            onVolver = { enSensores = false },
        )
    }
}
