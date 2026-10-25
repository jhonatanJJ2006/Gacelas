package com.gacelas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.gacelas.legal.DocumentoLegalScreen
import com.gacelas.ui.theme.GacelasTheme
import com.gacelas.vivo.CorredorEnMapa
import com.gacelas.vivo.MapaVivoScreen
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Terminos, Politica de privacidad, Privacidad con consentimientos y mapa en vivo. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class PantallasLegalesTest {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @Test fun terminos() {
        compose.setContent { GacelasTheme { DocumentoLegalScreen(terminos = true) {} } }
        compose.onNodeWithTag("doc-terminos").assertExists()
        compose.onNodeWithText("1. Aceptación").assertExists()
        captureScreenRoboImage("$dir/legal/terminos.png")
    }

    @Test fun politicaPrivacidad() {
        compose.setContent { GacelasTheme { DocumentoLegalScreen(terminos = false) {} } }
        compose.onNodeWithText("5. Ubicación visible en el mapa en vivo").assertExists()
        captureScreenRoboImage("$dir/legal/privacidad.png")
    }

    @Test fun privacidadPideExplicarAntesDeMostrarUbicacion() {
        compose.setContent { GacelasTheme { com.gacelas.ajustes.PrivacidadUbicacionScreen(onAtras = {}) } }
        compose.onNodeWithText("Ubicación visible").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("¿Mostrar tu ubicación?").assertExists()
        captureScreenRoboImage("$dir/legal/modal-ubicacion-visible.png")
    }

    @Test fun privacidadEliminarCuenta() {
        compose.setContent { GacelasTheme { com.gacelas.ajustes.PrivacidadUbicacionScreen(onAtras = {}) } }
        compose.onNodeWithTag("pv-eliminar").performScrollTo().performClick()
        compose.onNodeWithText("¿Eliminar tu cuenta?").assertExists()
        captureScreenRoboImage("$dir/legal/modal-eliminar-cuenta.png")
    }

    @Test fun mapaVivoConMarcadores() {
        val ahora = com.gacelas.shared.ahoraMs()
        val lista = listOf(
            CorredorEnMapa("u-1", "Ana Torres", null, -3.990, -79.205, true, ahora - 5_000),
            CorredorEnMapa("u-2", "Luis Pardo", null, -3.996, -79.198, false, ahora - 40_000),
        )
        compose.setContent { GacelasTheme { MapaVivoScreen(onAtras = {}, onPrivacidad = {}, inicial = lista) } }
        compose.onNodeWithTag("marcador-u-1").assertExists()
        captureScreenRoboImage("$dir/legal/mapa-vivo.png")
        compose.onNodeWithTag("marcador-u-1").performClick()
        compose.onNodeWithTag("perfil-corredor").assertExists()
        captureScreenRoboImage("$dir/legal/mapa-vivo-perfil.png")
    }
}
