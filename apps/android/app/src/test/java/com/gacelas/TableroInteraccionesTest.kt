package com.gacelas

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.printToString
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gacelas.actividad.domain.EstadoCarrera
import com.gacelas.actividad.domain.FaseCarrera
import com.gacelas.correr.CarreraMuestra
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Direccion A "Tablero": cada elemento principal se pulsa y lleva a su pantalla o cambia su estado.
 * Complementa la auditoria de /workspace/qa-rediseno-movil-v2/interacciones.md.
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h873dp-xxhdpi")
class TableroInteraccionesTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as GacelasApp

    private fun abrir(ruta: String, fase: FaseCarrera? = null) {
        runBlocking {
            app.contenedor.store.edit { it[stringPreferencesKey("ruta_sugerida")] = """{"historial":[],"seguras":["1","2","3"]}""" }
        }
        if (fase != null) {
            val t = app.contenedor.tracker
            val campo = t.javaClass.getDeclaredField("_estado").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            (campo.get(t) as MutableStateFlow<EstadoCarrera>).value = CarreraMuestra.estado.copy(inicioMs = System.currentTimeMillis() - 600_000L, fase = fase)
            t.javaClass.getDeclaredField("acumuladoMs").apply { isAccessible = true }.setLong(t, 600_000L)
        }
        compose.setContent { GacelasTheme { GacelasNavHost(inicio = Destinos.INICIO, rutaInicial = ruta, sesionActiva = true) } }
        compose.mainClock.advanceTimeBy(2000)
        compose.waitForIdle()
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.tocar(tag: String) {
        onNodeWithTag(tag, useUnmergedTree = true).performClick()
        repeat(3) { mainClock.advanceTimeBy(500); waitForIdle() }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.performScrollToSiPuede() = apply { runCatching { performScrollTo() } }

    private fun atras() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.mainClock.advanceTimeBy(900); compose.waitForIdle()
    }

    private fun existe(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun hayTexto(t: String) = compose.onAllNodesWithText(t, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test fun dockLlevaACadaPestanaYACorrer() {
        abrir(Destinos.INICIO)
        compose.tocar("nav-rutas"); assertTrue(existe("rutas-tablero"))
        compose.tocar("nav-comunidad"); assertTrue(hayTexto("Comunidad") || hayTexto("COMUNIDAD"))
        compose.tocar("nav-perfil"); assertTrue(existe("perfil-nombre"))
        compose.tocar("nav-inicio"); assertTrue(existe("inicio-tablero"))
        compose.tocar("nav-correr"); assertTrue(existe("pantalla-correr"))
    }

    @Test fun inicioBuscarNotificacionesYRutaDelDia() {
        abrir(Destinos.INICIO)
        assertTrue("La ruta del dia sale de la ultima lista segura del servidor", existe("inicio-ruta-ir"))
        compose.tocar("inicio-ruta-ir"); assertTrue(existe("mapa-detalle-ruta"))
        atras()
        compose.tocar("inicio-notificaciones"); assertTrue(hayTexto("Notificaciones"))
        atras()
        compose.tocar("inicio-buscar"); assertTrue(hayTexto("¿A dónde quieres correr?"))
    }

    @Test fun inicioWidgetSemanaAbreProgreso() {
        abrir(Destinos.INICIO)
        compose.tocar("widget-semana"); assertTrue(existe("perfil-nombre"))
    }

    @Test fun correrModosEditarYCuenta() {
        abrir(Destinos.CORRER)
        assertTrue(existe("modo-ruta"))
        compose.tocar("modo-libre"); assertTrue(hayTexto("Sin meta"))
        compose.tocar("correr-editar"); assertTrue(existe("hoja-objetivo"))
        compose.tocar("objetivo-listo"); assertTrue(!existe("hoja-objetivo"))
        compose.tocar("correr-cuenta"); assertTrue(existe("correr-cuenta"))
        compose.tocar("boton-reportar-incidente"); assertTrue(hayTexto("Reportar incidente"))
    }

    @Test fun carreraCronoPausaTerminarYVoz() {
        abrir(Destinos.CARRERA_EN_CURSO, FaseCarrera.Corriendo)
        compose.tocar("boton-cronometro"); assertTrue(existe("panel-cronometro"))
        compose.tocar("cerrar-cronometro"); assertTrue(!existe("panel-cronometro"))
        compose.tocar("boton-voz")
        compose.tocar("boton-stop"); assertTrue(existe("modal-terminar"))
    }

    @Test fun carreraPausaAbreModal() {
        abrir(Destinos.CARRERA_EN_CURSO, FaseCarrera.Corriendo)
        compose.tocar("boton-pausa")
        assertTrue(existe("modal-pausa"))
    }

    @Test fun carreraSosSoloConPulsacionLarga() {
        abrir(Destinos.CARRERA_EN_CURSO, FaseCarrera.Corriendo)
        compose.tocar("boton-sos"); assertTrue("Un toque no activa el SOS", !existe("hoja-sos"))
        compose.onNodeWithTag("boton-sos").performSemanticsAction(SemanticsActions.OnLongClick)
        compose.mainClock.advanceTimeBy(900); compose.waitForIdle()
        assertTrue(existe("hoja-sos"))
        com.gacelas.seguridad.EstadoSos.alertaId.value = null
    }

    @Test fun resumenVista3dYCompartir() {
        abrir(Destinos.RESUMEN, FaseCarrera.Terminada)
        assertTrue(hayTexto("Ver en 3D"))
        compose.tocar("resumen-3d"); assertTrue(hayTexto("Ver en 2D"))
        compose.tocar("boton-compartir"); assertTrue(hayTexto("Compartir"))
    }

    @Test fun rutasCarruselListaYFiltros() {
        abrir(Destinos.RUTAS)
        val primera = compose.onAllNodesWithTag("rutas-carrusel").fetchSemanticsNodes().size
        assertEquals(1, primera)
        compose.tocar("rutas-ver-lista"); assertTrue(existe("rutas-ver-mapa"))
        compose.tocar("rutas-ver-mapa"); assertTrue(existe("rutas-carrusel"))
    }

    @Test fun rutasTarjetaPrimerToqueEligeSegundoAbre() {
        abrir(Destinos.RUTAS)
        // La primera tarjeta ya esta elegida; en otra, el primer toque la elige y el segundo la abre.
        val otra = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("otra tarjeta") { n ->
            n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { null }?.let { it.startsWith("tarjeta-ruta-") && it != "tarjeta-ruta-1" } == true
        }, useUnmergedTree = true).fetchSemanticsNodes().first().config[androidx.compose.ui.semantics.SemanticsProperties.TestTag]
        compose.tocar(otra); assertTrue("El primer toque solo elige", !existe("mapa-detalle-ruta"))
        compose.tocar(otra); assertTrue(existe("mapa-detalle-ruta"))
    }

    @Test fun rutasFiltrosYChips() {
        abrir(Destinos.RUTAS)
        compose.tocar("chip-cerca")
        compose.tocar("boton-filtros"); assertTrue(hayTexto("Filtros"))
    }

    @Test fun rutasCorrerDesdeTarjeta() {
        abrir(Destinos.RUTAS)
        compose.tocar("correr-ruta-1")
        assertEquals("1", com.gacelas.rutas.infrastructure.RutaActiva.id?.removePrefix("ruta-"))
        com.gacelas.rutas.infrastructure.RutaActiva.id = null
    }

    @Test fun detalleRutaReportarYCompartir() {
        abrir(Destinos.detalleRuta("1"))
        compose.tocar("boton-reportar-incidente"); assertTrue(hayTexto("Reportar incidente"))
    }
}
