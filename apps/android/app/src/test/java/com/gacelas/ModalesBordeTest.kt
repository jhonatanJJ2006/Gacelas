package com.gacelas

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.gacelas.ui.components.BotonModalC2
import com.gacelas.ui.components.DialogoC2
import com.gacelas.ui.components.MarcaGacelasA
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regresion v0.8.9: en Android 15 (edge-to-edge obligatorio) la hoja "Acerca de" quedaba desplazada
 * hacia abajo y el boton Cerrar salia tapado por la barra de navegacion. La hoja debe quedar pegada
 * al borde inferior de la ventana y todo su contenido, botones incluidos, a la vista.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ModalesBordeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @OptIn(ExperimentalRoborazziApi::class)
    private fun acercaDe(nombre: String) {
        compose.activity.enableEdgeToEdge()
        compose.setContent {
            GacelasTheme {
                Box(Modifier.fillMaxSize().background(GColors.C2Bg))
                DialogoC2(
                    onDismissRequest = {},
                    confirmButton = { BotonModalC2("Cerrar", {}) },
                    title = { MarcaGacelasA(40.dp, "LOJA, ECUADOR", tamTexto = 18f) },
                    text = { Text("Versión 0.9.0 (900). App del grupo de running Runcity de Loja, Ecuador. Graba tus carreras con GPS, descubre rutas y entrena con la comunidad.") },
                )
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("$dir/modal-acerca-$nombre.png")
        compose.onNodeWithText("Cerrar").assertIsDisplayed()
        // La hoja (cuerpo + acciones) termina en el borde inferior de su ventana, menos la barra de navegacion.
        val acciones = compose.onNodeWithTag("dialogo-c2-acciones", useUnmergedTree = true).getBoundsInRoot()
        val raiz = compose.onNodeWithTag("dialogo-c2-ventana", useUnmergedTree = true).getBoundsInRoot()
        val nav = compose.onNodeWithTag("dialogo-c2-hoja", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("hoja pegada abajo: hoja ${nav.bottom} ventana ${raiz.bottom}", raiz.bottom - nav.bottom <= 1.dp)
        assertTrue("acciones dentro de la hoja: ${acciones.bottom} <= ${nav.bottom}", acciones.bottom <= nav.bottom)
        assertTrue("la hoja empieza dentro de la pantalla", nav.top >= raiz.top)
    }

    /** La causa del fallo: la ventana del dialogo debe ir a pantalla completa y sin ajustarse sola a las barras. */
    @Test fun ventanaSinAjusteDelSistema() {
        val p = com.gacelas.ui.components.propiedadesHojaC2()
        org.junit.Assert.assertFalse(p.decorFitsSystemWindows)
        org.junit.Assert.assertFalse(p.usePlatformDefaultWidth)
    }

    @Config(qualifiers = "w360dp-h640dp-xhdpi") @Test fun pequena() = acercaDe("360x640")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun telefonoJhona() = acercaDe("393x873")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun fuenteGrande() {
        RuntimeEnvironment.setFontScale(1.3f)
        acercaDe("393x873-fuente130")
    }
}
