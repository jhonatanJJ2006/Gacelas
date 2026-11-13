package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.gacelas.correr.DialogoDescartarCarrera
import com.gacelas.correr.ModalCompartirEnVivo
import com.gacelas.identidad.domain.modelo.ContactoEmergencia
import com.gacelas.seguridad.EstadoSos
import com.gacelas.seguridad.HojaSOS
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** QA del rediseno movil: modales y hojas completos en 393x873, 360x800 y fuente 130 %. Solo con -Pgacelas.shots. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RedisenoModalesQaTest(private val modal: String, private val tamano: String) {
    @get:Rule val compose = createComposeRule()

    @Test fun captura() {
        val dir = System.getProperty("gacelas.shots")
        assumeTrue("Solo con -Pgacelas.shots", dir != null)
        File(dir!!).mkdirs()
        when (tamano) {
            "393x873" -> RuntimeEnvironment.setQualifiers("w393dp-h873dp-xxhdpi")
            "360x800" -> RuntimeEnvironment.setQualifiers("w360dp-h800dp-xxhdpi")
            else -> { RuntimeEnvironment.setQualifiers("w393dp-h873dp-xxhdpi"); RuntimeEnvironment.setFontScale(1.3f) }
        }
        val app = RuntimeEnvironment.getApplication() as GacelasApp
        runBlocking {
            app.contenedor.ajustes.actualizar {
                it.copy(contactosEmergencia = listOf(ContactoEmergencia("Mamá", "+593991234567"), ContactoEmergencia("Andrés", "+593987654321")))
            }
        }
        if (modal == "hoja-sos") { EstadoSos.alertaId.value = "qa"; EstadoSos.avisados["+593991234567"] = "SMS" to System.currentTimeMillis() }
        compose.setContent {
            GacelasTheme {
                Box(Modifier.fillMaxSize().background(GColors.C2Bg)) {
                    when (modal) {
                        "compartir-en-vivo" -> ModalCompartirEnVivo({}, {})
                        "descartar" -> DialogoDescartarCarrera({}, {})
                        "hoja-sos" -> HojaSOS({})
                        "reportar" -> com.gacelas.seguridad.ui.HojaReportarIncidente(-3.99 to -79.2, {})
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
        captureScreenRoboImage("$dir/modal-$modal-$tamano.png")
        EstadoSos.alertaId.value = null
        EstadoSos.avisados.clear()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun casos(): List<Array<Any>> = listOf("compartir-en-vivo", "descartar", "hoja-sos", "reportar")
            .flatMap { n -> listOf("393x873", "360x800", "393x873-fuente130").map { arrayOf<Any>(n, it) } }
    }
}
