package com.gacelas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.gacelas.actividad.domain.reglas.Estadisticas
import com.gacelas.actividad.domain.reglas.EstadisticasPerfil
import com.gacelas.actividad.domain.reglas.Racha
import com.gacelas.correr.CarreraMuestra
import com.gacelas.clima.domain.ClimaActual
import com.gacelas.data.mock.MockData
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import com.gacelas.inicio.InicioContenido
import com.gacelas.inicio.rangoSemana
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Inicio C2 (Figma Movil / Inicio): capturas en los tamanos de QA. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class InicioC2Test {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
    private val ahora = CarreraMuestra.guardada.inicioMs + 3_600_000L
    private val carreras = listOf(CarreraMuestra.guardada)

    private fun inicio(nombre: String, clima: com.gacelas.clima.domain.ClimaActual? = ClimaActual(16, "nublado", "Loja", 0L)) {
        compose.setContent {
            GacelasTheme {
                InicioContenido(
                    saludo = "Buenos días", nombre = "Jhona", semana = Estadisticas.semana(carreras, ahora), hoy = Estadisticas.diaSemana(ahora), m = true,
                    racha = Racha(diaria = 12), evento = MockData.eventos.first(), onCorrer = {}, onEventos = {}, onEvento = {},
                    corredor3D = { com.gacelas.ui.three.Runner3DFallback(Modifier.fillMaxSize()) },
                    totales = EstadisticasPerfil.totalesSemana(carreras, ahora), meta = 3, rango = rangoSemana(ahora),
                    reto = EstadisticasPerfil.retoMes(carreras, ahora), amigos = emptyList(), nivel = "Gacela Intermedia", clima = clima,
                )
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$dir/inicio-$nombre.png")
    }

    @Config(qualifiers = "w360dp-h640dp-xhdpi") @Test fun pequena() = inicio("360x640")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun telefonoJhona() = inicio("393x873")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun fuenteGrande() { RuntimeEnvironment.setFontScale(1.3f); inicio("393x873-fuente130") }
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun climaSinDatos() {
        inicio("393x873-clima-sin-datos", clima = null)
        compose.onNodeWithTag("chip-clima").assertTextEquals("LOJA --")
    }
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun climaFueraDeLoja() {
        inicio("393x873-clima-quito", clima = ClimaActual(9, "llovizna", "Quito", 0L))
        compose.onNodeWithTag("chip-clima").assertTextEquals("QUITO 9 °C LLOVIZNA")
    }
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun climaReal() {
        inicio("393x873-clima-loja")
        compose.onNodeWithTag("chip-clima").assertTextEquals("LOJA 16 °C NUBLADO")
    }
    @Config(qualifiers = "w390dp-h2200dp-xxhdpi") @Test fun completa() = inicio("390x2200-completa")
}
