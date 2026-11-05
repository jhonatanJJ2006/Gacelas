package com.gacelas

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.navigation.atras
import com.gacelas.navigation.ir
import com.gacelas.ui.theme.GacelasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * QA total (docs/QA-TOTAL.md): recorre todas las rutas del grafo, comprueba que cada pantalla
 * abre sin crash y que "atras" vuelve a la anterior, y cubre doble toque y recreacion.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h873dp-xxhdpi")
class QaTotalTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: NavHostController

    private fun montar(inicio: String = Destinos.INICIO) {
        compose.setContent {
            nav = rememberNavController()
            GacelasTheme { GacelasNavHost(nav, inicio = inicio, sesionActiva = true) }
        }
        compose.waitForIdle()
    }

    private fun concreta(ruta: String) = ruta.replace("{id}", "1")

    private fun recorrerTodo() {
        montar()
        val rutas = Destinos.todas.filter { it !in Destinos.acceso && it != Destinos.INICIO }
        rutas.forEach { ruta ->
            compose.runOnIdle { nav.ir(concreta(ruta)) }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals("No se abrio $ruta", ruta, nav.currentDestination?.route) }
            compose.runOnIdle { nav.atras() }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals("Atras desde $ruta no volvio a Inicio", Destinos.INICIO, nav.currentDestination?.route) }
        }
    }

    @Test fun humoTodasLasPantallasAbrenYVuelven() = recorrerTodo()

    @Test fun humoConFuenteGrande() {
        RuntimeEnvironment.setFontScale(1.5f)
        recorrerTodo()
    }

    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun humoEnPantallaPequena() = recorrerTodo()

    @Test fun flujoDeAccesoAbreYVuelve() {
        montar(Destinos.BIENVENIDA)
        listOf(Destinos.INICIO_SESION, Destinos.REGISTRO, Destinos.RECUPERAR_CONTRASENA, Destinos.PERMISOS_UBICACION).forEach { ruta ->
            compose.runOnIdle { nav.ir(ruta) }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(ruta, nav.currentDestination?.route) }
            compose.runOnIdle { nav.atras() }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(Destinos.BIENVENIDA, nav.currentDestination?.route) }
        }
    }

    @Test fun dobleToqueNoApilaDosVecesLaMismaPantalla() {
        montar()
        compose.runOnIdle {
            nav.ir(Destinos.AJUSTES)
            nav.ir(Destinos.AJUSTES)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, nav.currentBackStack.value.count { it.destination.route == Destinos.AJUSTES })
            nav.atras()
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(Destinos.INICIO, nav.currentDestination?.route) }
    }

    @Test fun dobleAtrasNoSacaDosPantallas() {
        montar()
        compose.runOnIdle { nav.ir(Destinos.AJUSTES) }
        compose.waitForIdle()
        compose.runOnIdle { nav.ir(Destinos.PRIVACIDAD) }
        compose.waitForIdle()
        compose.runOnIdle {
            nav.atras()
            assertFalse("El segundo atras debe ignorarse", nav.atras())
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(Destinos.AJUSTES, nav.currentDestination?.route) }
    }

    @Test fun recreacionConservaLaPantallaActual() {
        val tester = StateRestorationTester(compose)
        tester.setContent {
            nav = rememberNavController()
            GacelasTheme { GacelasNavHost(nav, inicio = Destinos.INICIO, sesionActiva = true) }
        }
        compose.runOnIdle { nav.ir(Destinos.AJUSTES) }
        compose.waitForIdle()
        tester.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertEquals(Destinos.AJUSTES, nav.currentDestination?.route) }
    }
}
