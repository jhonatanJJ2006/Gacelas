package com.gacelas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.unit.dp
import com.gacelas.acceso.BienvenidaScreen
import com.gacelas.acceso.InicioSesionScreen
import com.gacelas.ui.theme.GacelasTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Las pantallas de acceso no usan scroll y caben en 360x640 dp, incluso con poco alto (teclado). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class AccesoSinScrollTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bienvenidaCabeEnPantallaPequena() {
        compose.setContent { GacelasTheme { BienvenidaScreen({}, {}, {}) } }
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        listOf("Empezar", "Continuar con Google", "Inicia sesión", "Saltar", "Jipiro · 1.8 KM").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test @Config(qualifiers = "w360dp-h800dp")
    fun loginCompletoEnPantallaNormal() {
        compose.setContent { GacelasTheme { InicioSesionScreen({}, {}, {}, {}) } }
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        listOf("Bienvenido de vuelta", "Iniciar sesión", "Continuar con Google", "Crear cuenta").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test fun loginEn640dpCabeSinScroll() {
        compose.setContent { GacelasTheme { InicioSesionScreen({}, {}, {}, {}) } }
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        listOf("Bienvenido de vuelta", "CORREO ELECTRÓNICO", "Iniciar sesión", "¿Olvidaste tu contraseña?").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test fun tocarFueraCierraElTeclado() {
        compose.setContent { GacelasTheme { InicioSesionScreen({}, {}, {}, {}) } }
        compose.onNodeWithTag("campo-Correo electrónico").performClick()
        compose.onNodeWithTag("campo-Correo electrónico").assertIsFocused()
        compose.onNodeWithText("Bienvenido de vuelta").performTouchInput { click() }
        compose.onNodeWithTag("campo-Correo electrónico").assertIsNotFocused()
    }

    @Test fun loginConTecladoSeCompactaSinPerderElBoton() {
        // ~300 dp de alto: lo que queda con el teclado abierto en un telefono de 640 dp.
        compose.setContent { GacelasTheme { Box(Modifier.size(360.dp, 300.dp)) { InicioSesionScreen({}, {}, {}, {}) } } }
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        compose.onNodeWithText("Iniciar sesión").assertIsDisplayed()
        compose.onNodeWithText("¿Olvidaste tu contraseña?").assertIsDisplayed()
        compose.onAllNodes(androidx.compose.ui.test.hasText("Continuar con Google")).assertCountEquals(0)
    }
}
