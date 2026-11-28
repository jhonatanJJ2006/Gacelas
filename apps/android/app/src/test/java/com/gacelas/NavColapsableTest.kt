package com.gacelas

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onRoot
import com.gacelas.ui.navegacion.BarraNavColapsable
import com.gacelas.ui.navegacion.ItemNavColapsable
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.Assert.assertEquals

/** Barra del navbar B colapsable generico (core/ui/navegacion): seleccion, insignias reales y captura. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class NavColapsableTest {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    private fun items(cuenta: Int?) = listOf(
        ItemNavColapsable("a", "Inicio", Icons.Filled.Home),
        ItemNavColapsable("b", "Casos", Icons.Filled.Flag, insignia = cuenta),
        ItemNavColapsable("c", "Mapa", Icons.Filled.Map),
    )

    @Test fun marcaLaSeccionActivaYAvisaAlElegir() {
        var elegido by mutableStateOf("a")
        val pulsados = mutableListOf<String>()
        compose.setContent {
            GacelasTheme { BarraNavColapsable(items(null), elegido, { pulsados += it; elegido = it }) }
        }
        compose.onNodeWithTag("navc-a").assertIsSelected()
        compose.onNodeWithTag("navc-c").performClick()
        compose.waitForIdle()
        assertEquals(listOf("c"), pulsados)
        compose.onNodeWithTag("navc-c").assertIsSelected()
        compose.onRoot().captureRoboImage("$dir/nav-colapsable/barra.png")
    }

    @Test fun lasInsigniasSoloMuestranConteosReales() {
        compose.setContent { GacelasTheme { BarraNavColapsable(items(0), "a", {}) } }
        compose.onAllNodesWithTag("navc-insignia", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun conUnConteoMuestraInsigniaYLoDiceAlLector() {
        compose.setContent { GacelasTheme { BarraNavColapsable(items(7), "a", {}) } }
        compose.onAllNodesWithTag("navc-insignia", useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithTag("navc-b").assertContentDescriptionEquals("Casos, 7 pendientes")
    }
}
