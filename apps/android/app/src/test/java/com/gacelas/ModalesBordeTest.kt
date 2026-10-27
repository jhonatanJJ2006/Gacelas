package com.gacelas

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
 * Regresion v0.8.9 / v0.9.0: en Android 15 (edge-to-edge obligatorio) la ventana propia de los Dialog
 * quedaba desplazada hacia abajo en algunos telefonos y el ultimo boton salia tapado por la barra de
 * navegacion ("Seguir corriendo" en Descartar carrera, "Cerrar" en Acerca de).
 *
 * Desde v0.9.1 todas las hojas y modales se dibujan en la ventana de la actividad (AnfitrionHojasC2).
 * Se comprueba, para cada modal de la app, que no se abre una ventana aparte, que la hoja queda
 * pegada al borde inferior de la pantalla y que todo su contenido, botones incluidos, esta a la vista.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ModalesBordeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    /** Monta [modal] como en MainActivity: anfitrion de hojas al final del contenedor raiz. */
    private fun montar(modal: @Composable () -> Unit) {
        compose.activity.enableEdgeToEdge()
        compose.setContent {
            GacelasTheme {
                val hojas = remember { com.gacelas.ui.components.HojasC2() }
                CompositionLocalProvider(com.gacelas.ui.components.LocalHojasC2 provides hojas) {
                    Box(Modifier.fillMaxSize().background(GColors.C2Bg).testTag("raiz-app")) {
                        modal()
                        com.gacelas.ui.components.AnfitrionHojasC2(hojas)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun comprobar(nombre: String, ultimoBoton: String, hoja: String = "dialogo-c2-hoja", acciones: String? = "dialogo-c2-acciones") {
        captureScreenRoboImage("$dir/modal-$nombre.png")
        org.junit.Assert.assertTrue("sin ventana aparte de Dialog", compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText(ultimoBoton).assertIsDisplayed()
        val raiz = compose.onNodeWithTag("raiz-app", useUnmergedTree = true).getBoundsInRoot()
        val h = compose.onNodeWithTag(hoja, useUnmergedTree = true).getBoundsInRoot()
        val boton = compose.onNodeWithText(ultimoBoton, useUnmergedTree = true).getBoundsInRoot()
        assertTrue("$nombre: hoja pegada abajo (hoja ${h.bottom}, pantalla ${raiz.bottom})", (raiz.bottom - h.bottom).value in -1f..1f)
        assertTrue("$nombre: la hoja empieza dentro de la pantalla", h.top >= raiz.top)
        assertTrue("$nombre: '$ultimoBoton' entero dentro de la hoja (${boton.bottom} <= ${h.bottom})", boton.bottom <= h.bottom && boton.top >= h.top)
        if (acciones != null) {
            val a = compose.onNodeWithTag(acciones, useUnmergedTree = true).getBoundsInRoot()
            assertTrue("$nombre: acciones dentro de la hoja", a.bottom <= h.bottom)
        }
    }

    private fun acercaDe(nombre: String) {
        montar {
            DialogoC2(
                onDismissRequest = {},
                confirmButton = { BotonModalC2("Cerrar", {}) },
                title = { MarcaGacelasA(40.dp, "LOJA, ECUADOR", tamTexto = 18f) },
                text = { Text("Versión 0.9.1 (901). App del grupo de running Runcity de Loja, Ecuador. Graba tus carreras con GPS, descubre rutas y entrena con la comunidad.") },
            )
        }
        comprobar("acerca-$nombre", "Cerrar")
    }

    private fun descartar(nombre: String) {
        montar { com.gacelas.correr.DialogoDescartarCarrera(onConfirmar = {}, onCancelar = {}) }
        comprobar("descartar-$nombre", "Seguir corriendo", hoja = "modal-descartar")
    }

    private fun compartirEnVivo(nombre: String) {
        montar { com.gacelas.correr.ModalCompartirEnVivo(onSi = {}, onNo = {}) }
        comprobar("compartir-en-vivo-$nombre", "No, gracias", hoja = "modal-compartir-en-vivo")
    }

    /** Hoja generica (Reportar incidente y similares) con contenido mas alto que la pantalla. */
    private fun hojaLarga(nombre: String) {
        montar {
            com.gacelas.ui.components.HojaC2(onCerrar = {}) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
                    repeat(30) { Text("Fila $it del formulario", Modifier.padding(vertical = 8.dp)) }
                    BotonModalC2("Enviar reporte", {})
                }
            }
        }
        compose.onNodeWithText("Enviar reporte").performScrollTo()
        comprobar("hoja-larga-$nombre", "Enviar reporte", hoja = "hoja-c2", acciones = null)
    }

    /** Fallback sin anfitrion (previews): la ventana del dialogo va a pantalla completa y sin ajustarse sola. */
    @Test fun ventanaSinAjusteDelSistema() {
        val p = com.gacelas.ui.components.propiedadesHojaC2()
        org.junit.Assert.assertFalse(p.decorFitsSystemWindows)
        org.junit.Assert.assertFalse(p.usePlatformDefaultWidth)
    }

    @Config(qualifiers = "w360dp-h640dp-xhdpi") @Test fun acercaPequena() = acercaDe("360x640")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun acercaTelefonoJhona() = acercaDe("393x873")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun acercaFuenteGrande() { RuntimeEnvironment.setFontScale(1.3f); acercaDe("393x873-fuente130") }

    @Config(qualifiers = "w360dp-h640dp-xhdpi") @Test fun descartarPequena() = descartar("360x640")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun descartarTelefonoJhona() = descartar("393x873")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun descartarFuenteGrande() { RuntimeEnvironment.setFontScale(1.3f); descartar("393x873-fuente130") }

    @Config(qualifiers = "w360dp-h640dp-xhdpi") @Test fun compartirPequena() = compartirEnVivo("360x640")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun compartirTelefonoJhona() = compartirEnVivo("393x873")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun compartirFuenteGrande() { RuntimeEnvironment.setFontScale(1.3f); compartirEnVivo("393x873-fuente130") }

    @Config(qualifiers = "w360dp-h640dp-xhdpi") @Test fun hojaLargaPequena() = hojaLarga("360x640")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun hojaLargaTelefonoJhona() = hojaLarga("393x873")
    @Config(qualifiers = "w393dp-h873dp-xxhdpi") @Test fun hojaLargaFuenteGrande() { RuntimeEnvironment.setFontScale(1.3f); hojaLarga("393x873-fuente130") }
}

