package com.gacelas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.unit.dp
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.theme.GacelasTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CampoContrasenaTest {
    @get:Rule val regla = createComposeRule()

    @Test fun ojoMuestraYOcultaYListoEnvia() {
        var enviado = false
        regla.setContent {
            GacelasTheme {
                var v by remember { mutableStateOf("secreta1") }
                CampoTexto("Contraseña", v, { v = it }, password = true, onListo = { enviado = true })
            }
        }
        regla.onNodeWithText("secreta1").assertDoesNotExist()
        regla.onNodeWithContentDescription("Mostrar contraseña").assertHeightIsAtLeast(44.dp).performClick()
        regla.onNodeWithText("secreta1").assertExists()
        regla.onNodeWithContentDescription("Ocultar contraseña").performClick()
        regla.onNodeWithText("secreta1").assertDoesNotExist()
        regla.onNodeWithText("Contraseña").assertExists()
        regla.onNode(androidx.compose.ui.test.hasSetTextAction()).performImeAction()
        assertTrue(enviado)
    }
}
