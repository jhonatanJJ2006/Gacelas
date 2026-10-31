package com.gacelas

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import com.gacelas.administracion.domain.AccesoAdmin
import com.gacelas.administracion.domain.RolAdmin
import com.gacelas.administracion.domain.SeccionAdmin
import com.gacelas.administracion.infrastructure.AdminRepositorioMemoria
import com.gacelas.administracion.presentation.PanelAdminContenido
import com.gacelas.ajustes.AjustesContenido
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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

    private fun panel(acceso: AccesoAdmin?, repo: AdminRepositorioMemoria = AdminRepositorioMemoria(acceso?.rol), inicial: SeccionAdmin? = null) =
        compose.setContent { GacelasTheme { PanelAdminContenido(acceso, repo, onAtras = {}, inicial = inicial) } }

    @Test fun rutaSinRolMuestraAccesoDenegado() {
        panel(AccesoAdmin.Ninguno)
        compose.onNodeWithTag("admin-denegado").assertIsDisplayed()
        compose.onNodeWithTag("admin-seccion-Usuarios").assertDoesNotExist()
        captura("acceso-denegado")
    }

    @Test fun seccionNoPermitidaVuelveAlMenu() {
        panel(AccesoAdmin(RolAdmin.Moderador), inicial = SeccionAdmin.Usuarios)
        compose.onNodeWithTag("admin-seccion-Moderacion").assertIsDisplayed()
        compose.onNodeWithTag("admin-seccion-Usuarios").assertDoesNotExist()
        captura("menu-moderador")
    }

    @Test fun menuAdmin() {
        panel(AccesoAdmin(RolAdmin.Admin))
        compose.onNodeWithTag("admin-seccion-Ajustes").assertIsDisplayed()
        captura("menu-admin")
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
    @Test fun ajustesAdmin() { panel(AccesoAdmin(RolAdmin.Admin), inicial = SeccionAdmin.Ajustes); captura("versiones-ajustes") }
}
