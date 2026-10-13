package com.gacelas

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.gacelas.correr.CarreraMuestra
import com.gacelas.correr.TrazadoRuta
import com.gacelas.perfil.DetalleCarreraContenido
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Detalle de carrera con CarreraMuestra.guardada al tamaño del iPhone 16 (iOS: --pantalla=detalle). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ParidadDetalleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun detalle() {
        val c = CarreraMuestra.guardada
        val fecha = SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", Locale("es", "EC")).apply { timeZone = TimeZone.getTimeZone("America/Guayaquil") }.format(Date(c.inicioMs))
        compose.setContent {
            GacelasTheme {
                DetalleCarreraContenido(c, fecha, true, {}, {}) { TrazadoRuta(it.puntos, Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp))) }
            }
        }
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        compose.onRoot().captureRoboImage("$dir/paridad/detalle-carrera.png")
    }
}
