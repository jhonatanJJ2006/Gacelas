package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.gacelas.inicio.RutaSugeridaC2
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Tarjeta "Ruta sugerida para hoy" con ruta segura y con estado vacio (docs/RUTA-SUGERIDA.md). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h400dp-xxhdpi")
class RutaSugeridaCapturasTest {
    @get:Rule val compose = createComposeRule()

    private fun dir() = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    private fun captura(nombre: String, ruta: Ruta?, nota: String) {
        compose.setContent {
            GacelasTheme { Box(Modifier.background(GColors.C2Bg).padding(16.dp)) { RutaSugeridaC2(ruta, true, {}, nota) } }
        }
        compose.onNodeWithTag(if (ruta == null) "ruta-sugerida-vacia" else "ruta-sugerida-segura", useUnmergedTree = true).assertExists()
        compose.onRoot().captureRoboImage("${dir()}/ruta-sugerida/android-$nombre.png")
    }

    @Test fun conRuta() = captura(
        "ruta",
        Ruta("loja-la-tebaida", "Parque Lineal La Tebaida", 2.98, Dificultad.Facil, 44, "Ida y vuelta junto al río Malacatos"),
        "Zona segura a esta hora. No la corriste en 3 días",
    )

    @Test fun sinRuta() = captura("vacia", null, "Ya corriste o te sugerimos todas las rutas seguras en los últimos 3 días.")
}
