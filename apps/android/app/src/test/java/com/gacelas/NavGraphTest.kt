package com.gacelas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavGraphTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun elGrafoContieneTodasLasRutas() {
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            GacelasTheme { GacelasNavHost(nav) }
        }
        compose.runOnIdle {
            Destinos.todas.forEach { ruta ->
                assertNotNull("Falta la ruta $ruta", nav.graph.findNode(ruta))
            }
            assertEquals(Destinos.BIENVENIDA, nav.currentDestination?.route)
        }
    }

    @Test
    fun saltarLlevaAInicio() {
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            GacelasTheme { GacelasNavHost(nav) }
        }
        compose.onNodeWithText("Saltar").performClick()
        compose.runOnIdle { assertEquals(Destinos.INICIO, nav.currentDestination?.route) }
    }
}

