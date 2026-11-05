package com.gacelas

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.click
import androidx.compose.ui.test.center
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.clickable
import androidx.compose.ui.test.performClick

/** QA total: cada modal C2 (dialogo y hoja) cierra por boton, toque fuera y boton atras. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h873dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ModalesCierreQaTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
    private var abierto by mutableStateOf(true)

    private fun montar(modal: @Composable (cerrar: () -> Unit) -> Unit) {
        compose.setContent {
            GacelasTheme {
                val hojas = remember { com.gacelas.ui.components.HojasC2() }
                CompositionLocalProvider(com.gacelas.ui.components.LocalHojasC2 provides hojas) {
                    Box(Modifier.fillMaxSize().background(GColors.C2Bg)) {
                        Text("Pantalla de fondo")
                        if (abierto) modal { abierto = false }
                        com.gacelas.ui.components.AnfitrionHojasC2(hojas)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Composable private fun dialogo(cerrar: () -> Unit) = DialogoC2(
        onDismissRequest = cerrar,
        confirmButton = { BotonModalC2("Cerrar", cerrar) },
        title = { Text("Título") },
        text = { Text("Cuerpo") },
    )

    private fun cerrado() {
        compose.waitForIdle()
        assertTrue("el modal debe cerrarse", !abierto)
        compose.onNodeWithText("Pantalla de fondo").assertExists()
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test fun dialogoCierraConBoton() {
        montar { dialogo(it) }
        captureScreenRoboImage("$dir/qa-total/dialogo-abierto.png")
        compose.onNodeWithText("Cerrar").performClick(); cerrado()
    }

    @Test fun botonDelDialogoEjecutaSuAccionYNoElDescarte() {
        var via = ""
        montar { DialogoC2(onDismissRequest = { via += "F" }, confirmButton = { BotonModalC2("Confirmar", { via += "B" }) }, text = { Text("Cuerpo") }) }
        compose.onNodeWithText("Confirmar").performClick()
        compose.waitForIdle(); assertTrue("via=$via", via == "B")
    }

    @Test fun dialogoCierraTocandoFuera() {
        montar { dialogo(it) }
        compose.onNodeWithTag("dialogo-c2-ventana", useUnmergedTree = true).performTouchInput { click(Offset(10f, 10f)) }
        cerrado()
    }

    @Test fun dialogoCierraConAtras() {
        montar { dialogo(it) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        cerrado()
    }

    @Test fun dialogoNoSeCierraTocandoDentro() {
        montar { dialogo(it) }
        compose.onNodeWithText("Cuerpo", useUnmergedTree = true).performTouchInput { click(center) }
        compose.onNodeWithText("Título", useUnmergedTree = true).performTouchInput { click(center) }
        compose.onNodeWithTag("dialogo-c2-hoja", useUnmergedTree = true).performTouchInput { click(center) }
        compose.waitForIdle(); assertTrue("tocar dentro no debe cerrar", abierto)
    }

    @Test fun hojaNoSeCierraTocandoDentro() {
        montar { c -> com.gacelas.ui.components.HojaC2(onCerrar = c) { Text("Contenido de la hoja"); BotonModalC2("Listo", c) } }
        compose.onNodeWithText("Contenido de la hoja", useUnmergedTree = true).performTouchInput { click(center) }
        compose.waitForIdle(); assertTrue(abierto)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test fun hojaCierraPorLasTresVias() {
        val hoja: @Composable (() -> Unit) -> Unit = { c -> com.gacelas.ui.components.HojaC2(onCerrar = c) { BotonModalC2("Listo", c) } }
        montar(hoja)
        captureScreenRoboImage("$dir/qa-total/hoja-abierta.png")
        compose.onNodeWithText("Listo").performClick(); cerrado()
        compose.runOnIdle { abierto = true }
        compose.onNodeWithTag("hoja-c2-ventana", useUnmergedTree = true).performTouchInput { click(Offset(10f, 10f)) }; cerrado()
        compose.runOnIdle { abierto = true }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }; cerrado()
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test fun descartarCarreraCierraConAtras() {
        montar { c -> com.gacelas.correr.DialogoDescartarCarrera(onConfirmar = c, onCancelar = c) }
        captureScreenRoboImage("$dir/qa-total/descartar-carrera.png")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }; cerrado()
    }
}
