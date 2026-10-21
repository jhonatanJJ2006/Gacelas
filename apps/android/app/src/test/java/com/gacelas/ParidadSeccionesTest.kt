package com.gacelas

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.gacelas.carreras.domain.reglas.Estadisticas
import com.gacelas.carreras.domain.reglas.EstadisticasPerfil
import com.gacelas.carreras.domain.reglas.Racha
import com.gacelas.correr.CarreraMuestra
import com.gacelas.data.mock.MockData
import com.gacelas.inicio.InicioContenido
import com.gacelas.inicio.rangoSemana
import com.gacelas.perfil.Contadores
import com.gacelas.perfil.EstadisticasHistoricas
import com.gacelas.perfil.KilometrajeMensual
import com.gacelas.perfil.Medallero
import com.gacelas.perfil.Records
import com.gacelas.rutas.ListaRutasContenido
import com.gacelas.rutas.domain.modelo.FiltrosRuta
import com.gacelas.rutas.domain.modelo.PestanaRutas
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas de las secciones de Stitch (Inicio, Perfil, Rutas) para comparar con la web movil. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h2400dp-xxhdpi")
class ParidadSeccionesTest {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
    private val ahora = CarreraMuestra.guardada.inicioMs + 3_600_000L
    private val carreras = listOf(CarreraMuestra.guardada)

    @Test fun inicio() {
        compose.setContent {
            GacelasTheme {
                InicioContenido(
                    saludo = "Buenos días", nombre = "Jhona", semana = Estadisticas.semana(carreras, ahora), hoy = Estadisticas.diaSemana(ahora), m = true,
                    racha = Racha(), evento = MockData.eventos.first(), onCorrer = {}, onEventos = {}, onEvento = {}, corredor3D = { Box(Modifier) },
                    totales = EstadisticasPerfil.totalesSemana(carreras, ahora), meta = 3, rango = rangoSemana(ahora),
                    reto = EstadisticasPerfil.retoMes(carreras, ahora), amigos = emptyList(),
                )
            }
        }
        compose.onRoot().captureRoboImage("$dir/paridad/inicio-secciones.png")
    }

    @Test fun perfil() {
        compose.setContent {
            GacelasTheme {
                Pantalla {
                    Contadores(carreras.size, 0, 0)
                    EstadisticasHistoricas(EstadisticasPerfil.historico(carreras), true)
                    KilometrajeMensual(EstadisticasPerfil.kmMensual(carreras, ahora), true)
                    Records(EstadisticasPerfil.records(carreras))
                    Medallero(EstadisticasPerfil.medallas(carreras))
                }
            }
        }
        compose.onRoot().captureRoboImage("$dir/paridad/perfil-secciones.png")
    }

    @Test fun rutas() {
        compose.setContent {
            GacelasTheme {
                ListaRutasContenido(
                    MockData.rutas, PestanaRutas.Explorar, {}, emptySet(), "", {}, FiltrosRuta(), {}, null, {}, true, true, {}, {}, {}, { _, _ -> },
                    mapaRutas = {},
                )
            }
        }
        compose.onRoot().captureRoboImage("$dir/paridad/rutas-lista.png")
    }
}
