package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Tarjeta de seguridad, hoja de reporte y mapa con incidentes, con datos de ejemplo (sin servidor). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class IncidentesCapturasTest {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @Test fun tarjetaSeguridad() {
        compose.setContent {
            GacelasTheme {
                Column(Modifier.fillMaxSize().background(GColors.C2Bg).padding(16.dp)) {
                    com.gacelas.incidentes.TarjetaSeguridad("ruta-ejemplo")
                }
            }
        }
        compose.onNodeWithText("Seguridad de la zona").assertExists()
        compose.onNodeWithText("EJEMPLO").assertExists()
        captureScreenRoboImage("$dir/incidentes/android-tarjeta-seguridad.png")
    }

    @Test fun hojaReportar() {
        compose.setContent {
            GacelasTheme {
                Box(Modifier.fillMaxSize().background(GColors.C2Bg))
                com.gacelas.incidentes.HojaReportarIncidente(-3.994 to -79.203, onCerrar = {})
            }
        }
        compose.onNodeWithTag("tipo-perros_agresivos").performClick()
        compose.onNodeWithText("Enviar reporte").assertExists()
        captureScreenRoboImage("$dir/incidentes/android-hoja-reportar.png")
    }

    @Test fun mapaConIncidentes() {
        compose.setContent { GacelasTheme { com.gacelas.vivo.MapaVivoScreen(onAtras = {}, onPrivacidad = {}, inicial = emptyList()) } }
        compose.onNodeWithTag("boton-reportar-incidente").assertExists()
        compose.onNodeWithText("Incidentes reportados").assertExists()
        captureScreenRoboImage("$dir/incidentes/android-mapa.png")
    }
}
