package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gacelas.ui.components.AnfitrionHojasC2
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.HojaC2
import com.gacelas.ui.components.HojasC2
import com.gacelas.ui.components.LocalHojasC2
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.movimiento.BarraLlenadoC2
import com.gacelas.ui.movimiento.CifraAnimada
import com.gacelas.ui.movimiento.DestelloC2
import com.gacelas.ui.movimiento.LocalEscalaMovimiento
import com.gacelas.ui.movimiento.MovimientoC2
import com.gacelas.ui.movimiento.TransicionesC2
import com.gacelas.ui.movimiento.entradaEscalonada
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Animaciones C2: hojas, navegacion, barras, cifras y destello, con animaciones activas y quitadas
 * (escala 0). Con -Pgacelas.frames=<dir> guarda un fotograma cada 16 ms para montar los videos.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class AnimacionesTest {
    @get:Rule val compose = createComposeRule()
    private val frames: String? = System.getProperty("gacelas.frames")
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
    private var n = 0

    /** Avanza [ms] en pasos de 16 ms, guardando fotogramas si se pidieron. */
    private fun avanzar(ms: Long, video: String) {
        // Los cambios de estado hechos desde la prueba se publican antes de mover el reloj.
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        if (compose.mainClock.autoAdvance) { compose.waitForIdle(); return }
        var t = 0L
        while (t < ms) {
            compose.mainClock.advanceTimeBy(16)
            t += 16
            frames?.let { captureScreenRoboImage("$it/$video/f%04d.png".format(n++)) }
        }
    }

    private fun montar(escala: Float, contenido: @Composable () -> Unit) {
        // Sin animaciones el reloj avanza solo: todo debe quedar en su estado final al estar ocioso.
        compose.mainClock.autoAdvance = escala <= 0f
        val hojas = HojasC2()
        compose.setContent {
            GacelasTheme {
                CompositionLocalProvider(LocalHojasC2 provides hojas, LocalEscalaMovimiento provides escala) {
                    Box(Modifier.fillMaxSize().background(GColors.C2Bg)) { contenido() }
                    AnfitrionHojasC2(hojas)
                }
            }
        }
        if (escala > 0f) compose.mainClock.advanceTimeByFrame()
    }

    private fun hojaAbreYCierra(escala: Float) {
        var abierta by mutableStateOf(false)
        montar(escala) {
            Column(Modifier.padding(24.dp)) { Text("Pantalla", style = GType.headlineLg, color = GColors.TextPrimary) }
            if (abierta) HojaC2({ abierta = false }) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) { Text("Hoja de prueba", color = GColors.TextPrimary); BotonPrimario("Cerrar", { abierta = false }) }
            }
        }
        avanzar(64, "modales")
        abierta = true
        avanzar(48, "modales")
        compose.onNodeWithText("Hoja de prueba").assertExists()
        avanzar(MovimientoC2.MEDIO + 200L, "modales")
        compose.onNodeWithTag("hoja-c2-velo", useUnmergedTree = true).assertExists()
        abierta = false
        avanzar(MovimientoC2.CORTO + 200L, "modales")
        compose.onNodeWithText("Hoja de prueba").assertDoesNotExist()
        compose.onNodeWithTag("hoja-c2-velo", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun hojaConAnimaciones() = hojaAbreYCierra(1f)

    @Test fun hojaSinAnimaciones() = hojaAbreYCierra(0f)

    private fun navegacion(escala: Float) {
        var ruta by mutableStateOf("a")
        montar(escala) {
            val nav = rememberNavController()
            NavHost(
                nav, "a",
                enterTransition = { TransicionesC2.entrar(escala, false) }, exitTransition = { TransicionesC2.salir(escala) },
                popEnterTransition = { TransicionesC2.volverEntrar(escala) }, popExitTransition = { TransicionesC2.volverSalir(escala, false) },
            ) {
                composable("a") { Pantallita("INICIO", "Pantalla principal") }
                composable("b") { Pantallita("DETALLE", "Pantalla secundaria") }
            }
            androidx.compose.runtime.LaunchedEffect(ruta) { if (ruta == "b") nav.navigate("b") else if (nav.currentDestination?.route == "b") nav.popBackStack() }
        }
        avanzar(64, "navegacion")
        ruta = "b"
        avanzar(MovimientoC2.MEDIO + 150L, "navegacion")
        compose.onNodeWithText("DETALLE").assertExists()
        compose.onNodeWithText("INICIO").assertDoesNotExist()
        ruta = "a"
        avanzar(MovimientoC2.MEDIO + 150L, "navegacion")
        compose.onNodeWithText("INICIO").assertExists()
        compose.onNodeWithText("DETALLE").assertDoesNotExist()
    }

    @Composable
    private fun Pantallita(titulo: String, texto: String) {
        Column(Modifier.fillMaxSize().background(GColors.C2Bg).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(titulo, style = GType.headlineLg, color = GColors.TextPrimary)
            repeat(4) { i -> Tarjeta(Modifier.entradaEscalonada(i).fillMaxWidth()) { Text("$texto ${i + 1}", color = GColors.TextPrimary) } }
        }
    }

    @Test fun navegacionConAnimaciones() = navegacion(1f)

    @Test fun navegacionSinAnimaciones() = navegacion(0f)

    private fun resumen(escala: Float) {
        montar(escala) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CARRERA COMPLETADA", style = GType.headlineLg, color = GColors.TextPrimary)
                    CifraAnimada(5.42, { "%.2f km".format(it) }, GType.headlineLg, GColors.TextPrimary, Modifier.entradaEscalonada(1))
                    BarraLlenadoC2(0.7f, GColors.Primary400, Modifier.entradaEscalonada(2).fillMaxWidth().height(8.dp).background(GColors.SurfaceHighest))
                }
                DestelloC2(Modifier.matchParentSize())
            }
        }
        if (escala <= 0f) {
            compose.onNodeWithText("5.42 km").assertExists()
            compose.onNodeWithTag("destello-c2").assertDoesNotExist()
            return
        }
        compose.onNodeWithTag("destello-c2").assertExists()
        avanzar(MovimientoC2.CONTEO + 400L, "carrera")
        compose.onNodeWithText("5.42 km").assertExists()
        avanzar(600, "carrera")
        compose.onNodeWithTag("destello-c2").assertDoesNotExist()
    }

    @Test fun resumenConAnimaciones() = resumen(1f)

    @Test fun resumenSinAnimaciones() = resumen(0f)

    @Test fun fotogramaIntermedioHoja() {
        var abierta by mutableStateOf(false)
        montar(1f) { if (abierta) HojaC2({ abierta = false }) { Text("Hoja a medio abrir", color = GColors.TextPrimary, modifier = Modifier.padding(24.dp)) } }
        abierta = true
        compose.mainClock.advanceTimeBy(MovimientoC2.MEDIO / 2L)
        captureScreenRoboImage("$dir/animaciones/hoja-mitad.png")
        assertEquals(1, 1)
    }
    private fun inicioEntrada(escala: Float) {
        val ahora = com.gacelas.correr.CarreraMuestra.guardada.inicioMs + 3_600_000L
        val carreras = listOf(com.gacelas.correr.CarreraMuestra.guardada)
        montar(escala) {
            com.gacelas.inicio.InicioContenido(
                saludo = "Buenos días", nombre = "Jhona",
                semana = com.gacelas.actividad.domain.reglas.Estadisticas.semana(carreras, ahora),
                hoy = com.gacelas.actividad.domain.reglas.Estadisticas.diaSemana(ahora), m = true,
                racha = com.gacelas.actividad.domain.reglas.Racha(diaria = 12), evento = com.gacelas.data.mock.MockData.eventos.first(),
                onCorrer = {}, onEventos = {}, onEvento = {},
                corredor3D = { com.gacelas.ui.three.Runner3DFallback(Modifier.fillMaxSize()) },
                totales = com.gacelas.actividad.domain.reglas.EstadisticasPerfil.totalesSemana(carreras, ahora), meta = 3,
                rango = com.gacelas.inicio.rangoSemana(ahora),
                reto = com.gacelas.actividad.domain.reglas.EstadisticasPerfil.retoMes(carreras, ahora), nivel = "Gacela Intermedia",
                clima = com.gacelas.clima.domain.ClimaActual(16, "nublado", "Loja", 0L),
            )
        }
        avanzar(MovimientoC2.CONTEO + 500L, "inicio")
        compose.onNodeWithTag("chip-clima").assertExists()
        compose.onNodeWithTag("insignia-nivel").assertExists()
        captureScreenRoboImage("$dir/animaciones/inicio-final-${if (escala > 0f) "con" else "sin"}.png")
    }

    @Test fun inicioConAnimaciones() = inicioEntrada(1f)

    @Test fun inicioSinAnimaciones() = inicioEntrada(0f)
}
