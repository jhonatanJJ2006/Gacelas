package com.gacelas

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Navbar B colapsable en la app de usuario: la barra inferior es la del componente compartido
 * (burbuja que salta a la seccion activa), con las secciones de siempre y sin insignias inventadas.
 * Las capturas (sin emulador) solo se escriben con -Pgacelas.shots=dir.
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h873dp-xxhdpi")
class NavColapsableUsuarioTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as GacelasApp

    private fun abrir(ruta: String) {
        runBlocking {
            app.contenedor.store.edit { it[stringPreferencesKey("ruta_sugerida")] = """{"historial":[],"seguras":["1","2","3"]}""" }
        }
        compose.setContent { GacelasTheme { GacelasNavHost(inicio = Destinos.INICIO, rutaInicial = ruta, sesionActiva = true) } }
        compose.mainClock.advanceTimeBy(2000)
        compose.waitForIdle()
    }

    private fun tocar(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        repeat(3) { compose.mainClock.advanceTimeBy(500); compose.waitForIdle() }
    }

    private fun existe(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun seleccionada(tag: String): Boolean =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config[SemanticsProperties.Selected]

    private fun captura(nombre: String) {
        val dir = System.getProperty("gacelas.shots") ?: return
        File(dir).mkdirs()
        captureScreenRoboImage("$dir/$nombre.png")
    }

    @Test fun laBarraTraeLasSeccionesDeSiempreYLaBurbujaSigueALaActiva() {
        abrir(Destinos.INICIO)
        assertTrue(existe("nav-barra"))
        listOf("inicio", "rutas", "correr", "comunidad", "perfil").forEach { assertTrue("falta $it", existe("nav-$it")) }
        assertTrue(existe("nav-burbuja"))
        // Sin conteos reales por pestana no hay insignias.
        assertFalse(existe("navc-insignia"))
        assertTrue(seleccionada("nav-inicio"))
        captura("nav-usuario-inicio")
        tocar("nav-rutas")
        assertTrue(seleccionada("nav-rutas")); assertFalse(seleccionada("nav-inicio"))
        captura("nav-usuario-rutas")
        tocar("nav-comunidad")
        assertTrue(seleccionada("nav-comunidad"))
        captura("nav-usuario-comunidad")
        tocar("nav-perfil")
        assertTrue(seleccionada("nav-perfil"))
        captura("nav-usuario-perfil")
        tocar("nav-inicio")
        assertEquals(true, seleccionada("nav-inicio"))
        tocar("nav-correr")
        assertTrue(existe("listo-para-correr"))
        // En el flujo de carrera la barra no se muestra.
        assertFalse(existe("nav-barra"))
    }
}
