package com.gacelas

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.gacelas.actualizacion.ContenidoModal
import com.gacelas.actualizacion.Fase
import com.gacelas.data.actualizacion.InfoVersion
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Modal de actualizacion V3 sobre el inicio, al tamaño del iPhone 16 (mismos datos que iOS --pantalla=modal). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ParidadModalTest {
    @get:Rule val compose = createComposeRule()

    private val info = InfoVersion(700, "0.7.0", "x", "https://example.com/a.apk",
        items = listOf("Nuevo diseño de bienvenida y login", "Mapa de Loja sin conexión en el acceso", "Tocar fuera cierra el teclado"), apkBytes = 49_283_072)

    private fun captura(fase: Fase, obligatoria: Boolean, nombre: String) {
        compose.setContent {
            GacelasTheme {
                Box {
                    GacelasNavHost(inicio = Destinos.BIENVENIDA, rutaInicial = Destinos.INICIO)
                    ContenidoModal("0.6.9", info, obligatoria, fase, info.apkBytes, {}, {}, {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        compose.onRoot().captureRoboImage("$dir/paridad/$nombre.png")
    }

    @Test fun modal() = captura(Fase.Info, false, "modal")
    @Test fun modalDescargando() = captura(Fase.Descargando(62, 30_408_704, 49_283_072), true, "modal-descargando")
}
