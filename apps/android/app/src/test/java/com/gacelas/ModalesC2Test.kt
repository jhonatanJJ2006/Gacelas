package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.gacelas.ui.components.BotonModalC2
import com.gacelas.ui.components.DialogoC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Modal C2 (hoja inferior) usado en cerrar sesion, descartar y eliminar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ModalesC2Test {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun cerrarSesion() {
        var confirmado = false
        compose.setContent {
            GacelasTheme {
                Box(Modifier.fillMaxSize().background(GColors.C2Bg))
                DialogoC2(
                    onDismissRequest = {},
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                    title = { Text("¿Cerrar sesión?") },
                    text = { Text("Tus carreras guardadas siguen en tu cuenta.") },
                    confirmButton = { BotonModalC2("Cerrar sesión", { confirmado = true }, peligro = true) },
                    dismissButton = { BotonModalC2("Cancelar", {}, secundario = true) },
                )
            }
        }
        compose.onNodeWithText("¿Cerrar sesión?").assertExists()
        captureScreenRoboImage("$dir/modales/cerrar-sesion.png")
        compose.onNodeWithText("Cerrar sesión").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        assert(confirmado)
    }

    private fun captura(nombre: String, contenido: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { GacelasTheme { Box(Modifier.fillMaxSize().background(GColors.C2Bg)); contenido() } }
        compose.waitForIdle()
        captureScreenRoboImage("$dir/modales/$nombre.png")
    }

    @Test fun terminar() = captura("terminar") {
        com.gacelas.ui.components.ModalC2(Icons.Filled.Flag, "¿Terminar carrera?", "Se guardará tu recorrido y verás el resumen.",
            "Terminar y guardar", {}, {}, "Descartar carrera") { com.gacelas.ui.components.ResumenModalC2(listOf("5,20 km" to "Distancia", "28:14" to "Tiempo", "5:25" to "Ritmo")) }
    }

    @Test fun pausa() = captura("pausa") {
        com.gacelas.ui.components.ModalC2(Icons.Filled.Pause, "Carrera en pausa", "El cronómetro está detenido.", "Reanudar", {}, {}, "Terminar carrera")
    }

    @Test fun gps() = captura("gps-sin-senal") {
        com.gacelas.ui.components.ModalC2(Icons.Filled.GpsOff, "GPS sin señal", "La precisión es baja (±80 m).", "Entendido", {}, {}, "Pausar carrera")
    }

    @Test fun unirse() = captura("unirse-evento") {
        com.gacelas.ui.components.ModalC2(Icons.Filled.Event, "Unirse al evento", "10K Puerta de la Ciudad · 2026-11-02 07:00 · Loja", "Inscribirme", {}, {}, "Ahora no")
    }

    @Test fun dejarDeSeguir() = captura("dejar-de-seguir") {
        com.gacelas.ui.components.ModalC2(Icons.Filled.PersonRemove, "Dejar de seguir", "Ya no verás sus carreras en Siguiendo.", "Dejar de seguir", {}, {}, "Cancelar", peligro = true)
    }

    @Test fun toasts() = captura("toasts") {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            com.gacelas.ui.components.ToastC2("Te inscribiste a 10K Loja", error = false)
            com.gacelas.ui.components.ToastC2("No se pudo guardar la carrera", error = true)
        }
    }
}
