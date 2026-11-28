package com.gacelas

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.gacelas.administracion.domain.AccesoAdmin
import com.gacelas.administracion.domain.AccionModeracion
import com.gacelas.administracion.domain.RolAdmin
import com.gacelas.administracion.domain.SeccionAdmin
import com.gacelas.administracion.infrastructure.AdminRepositorioMemoria
import com.gacelas.administracion.presentation.LocalAhoraAdmin
import com.gacelas.administracion.presentation.LocalExportarAdmin
import com.gacelas.administracion.presentation.LocalRefrescoAdminMs
import com.gacelas.administracion.presentation.PanelAdminContenido
import com.gacelas.administracion.domain.Prioridad
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import com.gacelas.ajustes.AjustesContenido
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Panel de administracion movil: la entrada en Ajustes solo aparece con rol, la ruta directa sin rol
 * muestra "Acceso denegado" y cada seccion tiene su captura (docs/capturas/admin-movil).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class AdminMovilTest {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    private fun captura(nombre: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$dir/admin-movil/$nombre.png")
    }

    private fun ajustes(onAdmin: (() -> Unit)?) = compose.setContent {
        GacelasTheme {
            AjustesContenido(Ajustes(), "1.0.0", {}, {}, {}, {}, {}, {}, {}, mapaOffline = {}, cabecera = { Text("Jhonatan Jara") }, onAdmin = onAdmin)
        }
    }

    @Test fun ajustesSinRolNoMuestraEntrada() {
        ajustes(onAdmin = null)
        compose.onNodeWithTag("ajustes-admin").assertDoesNotExist()
        compose.onNodeWithText("Panel de administración").assertDoesNotExist()
    }

    @Test fun ajustesConRolMuestraEntrada() {
        ajustes(onAdmin = {})
        compose.onNodeWithTag("ajustes-admin").performScrollTo().assertIsDisplayed()
        captura("ajustes-entrada-admin")
    }

    /** Exportaciones pedidas por el panel (nombre, tipo, contenido). */
    private val exportados = mutableListOf<Triple<String, String, String>>()

    // Hora fija para que plazos y "hace X" no dependan del dia en que corre la prueba.
    private val ahora = kotlinx.datetime.Instant.parse("2026-10-31T12:00:00Z")

    private fun panel(acceso: AccesoAdmin?, repo: AdminRepositorioMemoria = AdminRepositorioMemoria(acceso?.rol), inicial: SeccionAdmin? = null) =
        compose.setContent {
            GacelasTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAhoraAdmin provides { ahora },
                    LocalRefrescoAdminMs provides 0L,
                    LocalExportarAdmin provides { n, t, c -> exportados += Triple(n, t, c) },
                ) { PanelAdminContenido(acceso, repo, onAtras = {}, inicial = inicial) }
            }
        }

    @Test fun rutaSinRolMuestraAccesoDenegado() {
        panel(AccesoAdmin.Ninguno)
        compose.onNodeWithTag("admin-denegado").assertIsDisplayed()
        compose.onNodeWithTag("admin-seccion-Usuarios").assertDoesNotExist()
        captura("acceso-denegado")
    }

    @Test fun seccionNoPermitidaAbreLaPrimeraPermitida() {
        panel(AccesoAdmin(RolAdmin.Moderador), inicial = SeccionAdmin.Usuarios)
        compose.onNodeWithTag("admin-caso-r1").assertExists()
        compose.onNodeWithTag("admin-usuario-u0").assertDoesNotExist()
        // Con un solo destino no hay barra a donde saltar.
        compose.onNodeWithTag("admin-nav-barra").assertDoesNotExist()
        captura("menu-moderador")
    }

    @Test fun barraDeAdminTieneLosSeisDestinos() {
        panel(AccesoAdmin(RolAdmin.Admin))
        listOf("resumen", "moderacion", "usuarios", "rutas", "reportes", "ajustes").forEach {
            compose.onNodeWithTag("admin-nav-$it").assertIsDisplayed()
        }
        captura("menu-admin")
    }

    @Test fun insigniaDeModeracionUsaElConteoRealDePendientes() {
        // La memoria tiene 2 casos pendientes (r1 y r3).
        panel(AccesoAdmin(RolAdmin.Admin))
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("navc-insignia", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("admin-nav-moderacion").assertContentDescriptionEquals("Moderación, 2 pendientes")
    }

    @Test fun sinPendientesNoHayInsignia() {
        val repo = AdminRepositorioMemoria().apply { reportes.clear() }
        panel(AccesoAdmin(RolAdmin.Admin), repo)
        compose.waitForIdle()
        compose.onAllNodesWithTag("navc-insignia", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun tocarUnDestinoCambiaDeSeccion() {
        panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Dashboard)
        compose.onNodeWithTag("admin-nav-usuarios").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("admin-usuario-u0").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("admin-usuario-u0").assertIsDisplayed()
    }

    @Test fun resumenMuestraElMapaEnVivoConSosYElSiguienteCaso() {
        panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Dashboard)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("atender-siguiente").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("atender-siguiente").assertIsDisplayed()
        compose.onNodeWithTag("mapa-en-vivo").assertIsDisplayed()
        compose.onNodeWithTag("mapa-sos-activos").assertIsDisplayed()
        captura("resumen")
    }

    @Test fun resumenSinDatosEnVivoLoDice() {
        val repo = AdminRepositorioMemoria().apply { escenarioFalla = true }
        panel(AccesoAdmin(RolAdmin.Admin), repo, SeccionAdmin.Dashboard)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("mapa-sin-fuente").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("sos-error").performScrollTo().assertIsDisplayed()
    }

    @Test fun resumenListaLasAlertasSos() {
        panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Dashboard)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("sos-s1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("sos-s1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Activa").assertExists()
        compose.onNodeWithTag("sos-s2").performScrollTo()
        captura("resumen-sos")
    }

    @Test fun moderacionMuestraPrioridadPlazoYRespuestasRapidas() {
        val repo = AdminRepositorioMemoria()
        panel(AccesoAdmin(RolAdmin.Admin), repo, SeccionAdmin.Moderacion)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("sla-r3", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        // r3 (media) vencio el 29/10; r1 (baja) vence el 02/11.
        compose.onNodeWithText("Vencido hace 2 d").assertExists()
        compose.onNodeWithTag("prioridad-r1", useUnmergedTree = true).assertExists()
        captura("moderacion-prioridad-sla")
        compose.onNodeWithText("Sin infracción").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("selector-prioridad").performScrollTo()
        captura("moderacion-respuesta-rapida")
        compose.onNodeWithText("Alta").performClick()
        compose.waitUntil(5_000) { repo.reportes.first { it.id == "r3" }.prioridad == Prioridad.Alta }
    }

    @Test fun usuariosMuestranUltimoAcceso() {
        panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Usuarios)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("acceso-u0", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Último acceso hace 3 h · Android 1.0.1").assertExists()
        compose.onNodeWithText("Sin registro de acceso").assertExists()
        captura("usuarios-ultimo-acceso")
    }

    @Test fun reportesExportaCsvYMuestraVersionesYEcu() {
        panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Estadisticas)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("exportar-csv").fetchSemanticsNodes().isNotEmpty() }
        captura("reportes-exportar")
        compose.onNodeWithTag("exportar-csv").performScrollTo().performClick()
        assertEquals(1, exportados.size)
        assertTrue(exportados.single().first.startsWith("gacelas-estadisticas-"))
        assertEquals("text/csv", exportados.single().second)
        assertTrue(exportados.single().third.contains("Día;Usuarios nuevos;Corredores activos;Carreras;Km"))
        compose.onNodeWithTag("versiones-publicadas").performScrollTo().assertIsDisplayed()
        captura("reportes-versiones")
        compose.onNodeWithText("Con error").performScrollTo().assertIsDisplayed()
        captura("reportes-ecu911")
    }

    @Test fun ajustesEditaPlazosYRespuestas() {
        val repo = AdminRepositorioMemoria()
        panel(AccesoAdmin(RolAdmin.Admin), repo, SeccionAdmin.Ajustes)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("ajustes-sla").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("ajustes-sla").performScrollTo()
        captura("ajustes-plazos")
        compose.onNodeWithTag("ajustes-respuestas").performScrollTo()
        captura("ajustes-respuestas")
        compose.onNodeWithText("Nueva respuesta").performScrollTo().performClick()
        compose.waitForIdle()
        captura("ajustes-respuesta-nueva")
    }

    @Test fun atenderSiguienteAbreModeracionConElCasoMasAntiguo() {
        panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Dashboard)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("atender-siguiente").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("atender-siguiente").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("admin-caso-r3").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("admin-caso-r3").assertIsDisplayed()
    }

    @Test fun asignarmeElSiguienteTomaElPendienteMasAntiguo() {
        val repo = AdminRepositorioMemoria()
        panel(AccesoAdmin(RolAdmin.Admin), repo, SeccionAdmin.Moderacion)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("asignarme-siguiente").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("asignarme-siguiente").performClick()
        compose.waitUntil(5_000) { repo.moderaciones.isNotEmpty() }
        assertEquals(Triple("r3", AccionModeracion.EnRevision, ""), repo.moderaciones.single())
    }

    @Test fun dashboard() { panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Dashboard); captura("dashboard") }

    @Test fun dashboardError() {
        panel(AccesoAdmin(RolAdmin.Admin), AdminRepositorioMemoria().apply { fallar = true }, SeccionAdmin.Dashboard)
        compose.onNodeWithTag("admin-error").assertIsDisplayed()
        captura("dashboard-error")
    }

    @Test fun usuarios() { panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Usuarios); captura("usuarios") }

    @Test fun moderacion() {
        panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Moderacion)
        compose.onNodeWithTag("admin-caso-r1").assertExists()
        captura("moderacion")
    }

    @Test fun moderacionVacia() {
        val repo = AdminRepositorioMemoria().apply { reportes.clear() }
        panel(AccesoAdmin(RolAdmin.Moderador), repo, SeccionAdmin.Moderacion)
        compose.onNodeWithTag("admin-vacio").assertIsDisplayed()
        captura("moderacion-vacia")
    }

    @Test fun estadisticas() { panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Estadisticas); captura("estadisticas") }
    @Test fun eventos() { panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Eventos); captura("eventos") }
    @Test fun rutasYClubes() { panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.RutasYClubes); captura("rutas-clubes") }
    @Test fun usuariosTocaFiltros() { panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Usuarios); compose.onNodeWithText("Suspendidos").performClick(); compose.waitForIdle(); compose.onNodeWithTag("admin-usuario-u3").assertIsDisplayed(); compose.onNodeWithTag("admin-usuario-u0").assertDoesNotExist() }
    @Test fun ajustesAdmin() { panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Ajustes); captura("versiones-ajustes") }
}
