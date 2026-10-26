package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.rutas.ListaRutasContenido
import com.gacelas.rutas.domain.modelo.CategoriaRuta
import com.gacelas.rutas.domain.modelo.FiltrosRuta
import com.gacelas.rutas.domain.modelo.PestanaRutas
import com.gacelas.rutas.domain.modelo.RutasLoja
import com.gacelas.rutas.domain.reglas.aplicarFiltros
import com.gacelas.shared.Fechas
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Rutas oficiales de Loja (insignia y filtro por categoria), reporte personal y reporte de evento. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class RutasReportesTest {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    private fun lista(f: FiltrosRuta) {
        compose.setContent {
            GacelasTheme {
                ListaRutasContenido(
                    aplicarFiltros(RutasLoja.todas, f), PestanaRutas.Explorar, {}, emptySet(), "", {}, f, {}, null, {}, true, true, {}, {}, {}, { _, _ -> },
                    mapaRutas = {},
                )
            }
        }
    }

    @Test fun rutasOficiales() {
        lista(FiltrosRuta())
        compose.onAllNodesWithTag("insignia-oficial", useUnmergedTree = true).fetchSemanticsNodes().let { assert(it.isNotEmpty()) }
        compose.onNodeWithTag("filtro-categoria").assertExists()
        captureScreenRoboImage("$dir/rutas-loja/lista-oficiales.png")
    }

    @Test fun rutasMontana() {
        lista(FiltrosRuta(categoria = CategoriaRuta.Montana))
        captureScreenRoboImage("$dir/rutas-loja/filtro-montana.png")
    }

    private fun ms(mes: Int, dia: Int) = LocalDateTime(2026, mes, dia, 7, 0).toInstant(Fechas.zona()).toEpochMilliseconds()

    @Test fun misReportes() {
        val r = RutasLoja.todas.first()
        val ini = r.puntos.first()
        val carreras = listOf(
            Carrera("1", "Mañana", inicioMs = ms(10, 19), segundos = 1500, distanciaKm = 5.0, desnivelM = 40, puntos = listOf(PuntoGps(ini.first, ini.second, ms(10, 19)))),
            Carrera("2", "Fondo", inicioMs = ms(10, 20), segundos = 3300, distanciaKm = 10.0, desnivelM = 120, puntos = listOf(PuntoGps(ini.first, ini.second, ms(10, 20)))),
            Carrera("3", "Suave", inicioMs = ms(10, 21), segundos = 960, distanciaKm = 3.0),
            Carrera("4", "Previa", inicioMs = ms(10, 14), segundos = 1900, distanciaKm = 6.0),
        )
        compose.setContent { GacelasTheme { com.gacelas.perfil.MisReportesContenido(carreras, ms(10, 21) + 18_000_000, {}, {}) } }
        compose.onNodeWithText("+200.0% vs. anterior").assertExists()
        compose.onNodeWithText(r.nombre).assertExists()
        captureScreenRoboImage("$dir/reportes/mis-reportes-semana.png")
        compose.onNodeWithText("AÑO").performClick()
        captureScreenRoboImage("$dir/reportes/mis-reportes-anio.png")
    }

    @Test fun reportarEvento() {
        compose.setContent {
            GacelasTheme {
                Box(Modifier.fillMaxSize().background(GColors.C2Bg))
                com.gacelas.social.ModalReporte("e-1", com.gacelas.ui.contenedor().social, com.gacelas.social.TipoContenido.Evento) {}
            }
        }
        compose.onNodeWithText("Reportar evento").assertExists()
        compose.onNodeWithText("Ruta o evento peligroso").assertExists()
        captureScreenRoboImage("$dir/reportes/reportar-evento.png")
    }
}
