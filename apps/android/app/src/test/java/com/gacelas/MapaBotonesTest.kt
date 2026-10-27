package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.gacelas.actividad.domain.EstadoCarrera
import com.gacelas.actividad.domain.FaseCarrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.correr.ContenidoCarreraC4
import com.gacelas.correr.TrazadoRuta
import com.gacelas.correr.VistaCarrera
import com.gacelas.ui.components.BotonReportarMapa
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Pantallas de mapa: flecha de retroceso arriba a la izquierda y reportar incidente al borde derecho. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class MapaBotonesTest {
    @get:Rule val compose = createComposeRule()
    private var atras = 0

    @Test fun carreraTieneVolverYReportarLateral() {
        val e = EstadoCarrera(
            fase = FaseCarrera.Corriendo, segundos = 300, distanciaKm = 1.1, ultimaPrecisionM = 8f,
            puntos = (0..20).map { PuntoGps(-4.0 + it * 0.0005, -79.2, it * 1000L, 5f) },
        )
        compose.setContent {
            GacelasTheme {
                ContenidoCarreraC4(VistaCarrera(e), onPausa = {}, onStop = {}, onVuelta = {}, onAtras = { atras++ },
                    extraCabecera = { BotonReportarMapa({}) }) { _, controles, _ ->
                    Box(Modifier.fillMaxSize().background(Color(0xFF0A0E16))) {
                        TrazadoRuta(e.puntos, Modifier.fillMaxSize())
                        controles({}, {}, 0f, true)
                    }
                }
            }
        }
        val ancho = compose.onRoot().getBoundsInRoot().right
        val volver = compose.onNodeWithTag("boton-volver-mapa").assertIsDisplayed().getBoundsInRoot()
        assertTrue("volver arriba a la izquierda", volver.left.value < 40f && volver.top.value < 120f)
        val rep = compose.onNodeWithTag("boton-reportar-incidente").assertIsDisplayed().getBoundsInRoot()
        assertTrue("reportar en el borde derecho, no centrado", (rep.left + rep.right).value / 2 > ancho.value * 0.75f)
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        compose.onRoot().captureRoboImage("$dir/mapa/android-carrera.png")
        compose.onNodeWithTag("boton-volver-mapa").performClick()
        assertTrue("volver llama a onAtras (confirmacion)", atras == 1)
    }
}
