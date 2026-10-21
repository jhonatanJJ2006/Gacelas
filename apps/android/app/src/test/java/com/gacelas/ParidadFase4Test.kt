package com.gacelas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas de las pantallas de la fase 4 de Stitch (resumen, buscador, editar perfil y avatar). */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h1700dp-xxhdpi")
class ParidadFase4Test(private val ruta: String) {
    @get:Rule val compose = createComposeRule()

    @Test fun captura() {
        if (ruta == Destinos.EDITAR_PERFIL) kotlinx.coroutines.runBlocking {
            val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<GacelasApp>()
            app.contenedor.auth.registrar("Jhona Runner", "jhona@utpl.edu.ec", "gacela123")
        }
        compose.setContent { GacelasTheme { GacelasNavHost(inicio = Destinos.BIENVENIDA, rutaInicial = ruta) } }
        compose.mainClock.advanceTimeBy(800)
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        compose.onRoot().captureRoboImage("$dir/paridad/fase4-${ruta.replace("/", "_")}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun rutas(): List<Array<Any>> = listOf(Destinos.RESUMEN, Destinos.BUSCADOR_RUTAS, Destinos.EDITAR_PERFIL, Destinos.AVATAR).map { arrayOf(it) }
    }
}
