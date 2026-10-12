package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.gacelas.correr.ContenidoCarreraC4
import com.gacelas.correr.TrazadoRuta
import com.gacelas.correr.VistaCarrera
import com.gacelas.data.carreras.Parcial
import com.gacelas.data.carreras.PuntoGps
import com.gacelas.tracking.EstadoCarrera
import com.gacelas.tracking.FaseCarrera
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h884dp-xxhdpi")
class CarreraC4Test {
    @get:Rule val compose = createComposeRule()

    private val estado = EstadoCarrera(
        fase = FaseCarrera.Corriendo, segundos = 892, distanciaKm = 2.34, desnivelM = 18.0, ultimaPrecisionM = 9f, ritmoActualSegKm = 381,
        puntos = (0..40).map { PuntoGps(-4.0 + it * 0.0005, -79.2 + Math.sin(it / 6.0) * 0.001, it * 1000L, 5f) },
        parciales = listOf(Parcial(1, 1.0, 378), Parcial(2, 1.0, 385), Parcial(1, 0.34, 129, manual = true)),
    )
    private var vueltas = 0

    private fun mostrar(panel: Boolean = false, e: EstadoCarrera = estado) = compose.setContent {
        GacelasTheme {
            ContenidoCarreraC4(VistaCarrera(e), onPausa = {}, onStop = {}, onVuelta = { vueltas++ }, onAtras = {}, panelInicial = panel) { _, controles ->
                Box(Modifier.fillMaxSize().background(Color(0xFF0A0E16))) {
                    TrazadoRuta(e.puntos, Modifier.fillMaxSize())
                    controles({}, {}, 0f)
                }
            }
        }
    }

    private fun captura(n: String) {
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        compose.onRoot().captureRoboImage("$dir/carrera-$n.png")
    }

    @Test fun cerradoMuestraMetricasYControles() {
        mostrar()
        compose.onNodeWithText("00:14:52").assertIsDisplayed()
        compose.onNodeWithTag("distancia").assertIsDisplayed()
        compose.onNodeWithText("±9 m", substring = true).assertIsDisplayed()
        listOf("boton-pausa", "boton-stop", "boton-cronometro", "boton-3d", "boton-recentrar", "boton-brujula").forEach { compose.onNodeWithTag(it).assertIsDisplayed() }
        compose.onAllNodesWithTag("panel-cronometro").assertCountEquals(0)
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        captura("cerrado")
    }

    @Test fun panelSeAbreConParcialesYVuelta() {
        mostrar()
        compose.onNodeWithTag("boton-cronometro").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithTag("panel-cronometro").assertIsDisplayed()
        captura("abierto")
        compose.onAllNodesWithTag("parcial", useUnmergedTree = true).assertCountEquals(3)
        compose.onNodeWithText("Vuelta 1").assertIsDisplayed()
        compose.onNodeWithTag("boton-vuelta").performClick()
        assertEquals(1, vueltas)
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        captura("abierto")
    }

    @Test fun panelSeCierraConSwipeYConX() {
        mostrar(panel = true)
        compose.onNodeWithTag("panel-cronometro").performTouchInput { swipeRight() }
        compose.mainClock.advanceTimeBy(600)
        compose.onAllNodesWithTag("panel-cronometro").assertCountEquals(0)
        compose.onNodeWithTag("boton-cronometro").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithTag("cerrar-cronometro").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.onAllNodesWithTag("panel-cronometro").assertCountEquals(0)
    }

    @Config(qualifiers = "w360dp-h640dp")
    @Test fun pantallaChicaSinScroll() {
        mostrar(panel = true, e = estado.copy(parciales = (1..12).map { Parcial(it, 1.0, 380) }))
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithTag("boton-vuelta").assertIsDisplayed()
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
    }
}
