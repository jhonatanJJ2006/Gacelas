package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.gacelas.seguridad.domain.AlertasZona
import com.gacelas.seguridad.domain.IncidentePublico
import com.gacelas.seguridad.domain.TipoIncidente
import com.gacelas.ui.components.AvisoSincronizacionC2
import com.gacelas.ui.components.EsqueletoEscena3D
import com.gacelas.ui.components.EsqueletoMapa
import com.gacelas.ui.components.EsqueletoPantalla
import com.gacelas.ui.components.EsqueletoTarjetas
import com.gacelas.ui.components.textoPendientes
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 0.9.14: esqueletos de carga, aviso de carreras sin subir, alertas de zona y estilo C2 en carrera. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h884dp-xxhdpi")
class Version0914Test {
    @get:Rule val compose = createComposeRule()

    private fun captura(n: String) {
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        compose.onRoot().captureRoboImage("$dir/v0914-$n.png")
    }

    private val ahora = 1_800_000_000_000L
    private fun reporte(id: String, dLat: Double, edadMs: Long = 3_600_000) =
        IncidentePublico(id, TipoIncidente.RoboAsalto, -3.9931 + dLat, -79.2042, ahora - edadMs, 2, false)

    @Test fun alertaDeZonaUnaSolaVezYSoloCerca() {
        val cerca = reporte("a", 0.0013) // unos 145 m
        val lejos = reporte("b", 0.01)
        val viejo = reporte("c", 0.0005, edadMs = 8L * 24 * 3600 * 1000)
        val r = AlertasZona.nuevas(-3.9931, -79.2042, listOf(cerca, lejos, viejo), emptySet(), ahora)
        assertEquals(listOf("a"), r.map { it.first.id })
        assertTrue(r.first().second in 130..160)
        assertTrue(AlertasZona.nuevas(-3.9931, -79.2042, listOf(cerca), setOf("a"), ahora).isEmpty())
        assertEquals("Atención: reporte de robo o asalto a 145 m", AlertasZona.mensaje(cerca, 145))
    }

    @Test fun textoDePendientes() {
        assertEquals("1 carrera sin subir", textoPendientes(1))
        assertEquals("3 carreras sin subir", textoPendientes(3))
    }

    @Test fun avisoDePendientesAparecePorNumeroYSeOcultaEnCero() {
        var pendientes by androidx.compose.runtime.mutableStateOf(2)
        var toques = 0
        compose.setContent {
            GacelasTheme {
                Box(Modifier.fillMaxSize().background(Color(0xFF0C1322)).padding(16.dp)) {
                    AvisoSincronizacionC2(pendientes, subiendo = false, onSubir = { toques++ })
                }
            }
        }
        compose.onNodeWithText("2 carreras sin subir").assertIsDisplayed()
        captura("aviso-pendientes")
        compose.onNodeWithTag("subir-pendientes").performClick()
        assertEquals(1, toques)
        pendientes = 0
        compose.waitForIdle()
        compose.onAllNodesWithTag("aviso-pendientes").assertCountEquals(0)
    }

    @Test fun esqueletosDeCarga() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            GacelasTheme {
                Column(Modifier.fillMaxSize().background(Color(0xFF0C1322)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EsqueletoTarjetas(2, etiqueta = "Cargando rutas")
                    Box(Modifier.height(160.dp)) { EsqueletoMapa() }
                    Box(Modifier.height(170.dp)) { EsqueletoEscena3D() }
                }
            }
        }
        compose.mainClock.advanceTimeBy(450)
        compose.onNodeWithTag("esqueleto").assertIsDisplayed()
        captura("esqueletos")
    }

    @Test fun esqueletoDePantalla() {
        compose.mainClock.autoAdvance = false
        compose.setContent { GacelasTheme { Box(Modifier.fillMaxSize().background(Color(0xFF0C1322))) { EsqueletoPantalla(etiqueta = "Calculando tu reporte") } } }
        compose.mainClock.advanceTimeBy(450)
        captura("esqueleto-pantalla")
    }

    @Test fun cronometroC2() {
        compose.setContent { GacelasTheme { com.gacelas.correr.CronometroScreen(onAtras = {}, onFin = {}) } }
        compose.waitForIdle()
        compose.onNodeWithText("CRONÓMETRO").assertIsDisplayed()
        captura("cronometro")
    }

    @Test fun resumenC2() {
        compose.setContent { GacelasTheme { com.gacelas.correr.ResumenPostCarreraScreen(onCerrar = {}, onGuardar = {}, onDescartar = {}) } }
        compose.waitForIdle()
        captura("resumen")
    }
}
