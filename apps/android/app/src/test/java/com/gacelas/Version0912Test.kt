package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.gacelas.actividad.domain.EstadoCarrera
import com.gacelas.actividad.domain.FaseCarrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.correr.ContenidoCarreraC4
import com.gacelas.correr.TrazadoRuta
import com.gacelas.correr.VistaCarrera
import com.gacelas.identidad.domain.modelo.ContactoEmergencia
import com.gacelas.identidad.domain.modelo.Visibilidad
import com.gacelas.identidad.infrastructure.ajustesTolerantes
import com.gacelas.legal.Consentimiento
import com.gacelas.legal.unirHistoriales
import com.gacelas.seguridad.BotonSOS
import com.gacelas.seguridad.ContactosEmergenciaC2
import com.gacelas.seguridad.EstadoSos
import com.gacelas.seguridad.HojaSOS
import com.gacelas.seguridad.domain.Sos
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 0.9.12: SOS, consentimiento unico, prefs de la web y controles de carrera con texto. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h884dp-xxhdpi")
class Version0912Test {
    @get:Rule val compose = createComposeRule()

    private fun captura(n: String) {
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        compose.onRoot().captureRoboImage("$dir/v0912-$n.png")
    }

    @Test fun telefonosYMensajeSos() {
        assertEquals("+593991234567", Sos.normalizarTelefono("099 123 4567"))
        assertEquals("+593991234567", Sos.normalizarTelefono("593991234567"))
        assertEquals("+14155550100", Sos.normalizarTelefono("+1 (415) 555-0100"))
        assertNull(Sos.validar("Mamá", "0991234567"))
        assertTrue(Sos.validar("", "0991234567")!!.contains("nombre"))
        val m = Sos.mensaje("Ana", "abc", -4.0, -79.2)
        assertTrue(m.contains("https://gacelas.vercel.app/sos/abc"))
        assertTrue(m.contains("911"))
    }

    @Test fun prefsEscritasPorLaWebNoBorranLaDecision() {
        // La web guarda visibilidad en minusculas y campos propios: antes se descartaba todo.
        val web = Json.parseToJsonElement(
            """{"visibilidad":"seguidores","compartirEnVivo":true,"metaSemanal":4,"campoDeLaWeb":{"x":1},
               "contactosEmergencia":[{"nombre":"Mamá","telefono":"+593991234567"}],"pesoKg":"no-numero"}""",
        ) as JsonElement
        val a = ajustesTolerantes(web)!!
        assertEquals(true, a.compartirEnVivo)
        assertEquals(Visibilidad.Seguidores, a.visibilidad)
        assertEquals(4, a.metaSemanal)
        assertEquals(listOf(ContactoEmergencia("Mamá", "+593991234567")), a.contactosEmergencia)
    }

    @Test fun historialesSeUnenSinDuplicar() {
        val local = listOf(Consentimiento("ubicacion_visible", "v", true, 1_000))
        val remoto = listOf(Consentimiento("ubicacion_visible", "v", true, 3_000), Consentimiento("gps_carrera", "v", true, 500))
        assertEquals(2, unirHistoriales(local, remoto).size)
    }

    private val estado = EstadoCarrera(
        fase = FaseCarrera.Corriendo, segundos = 892, distanciaKm = 2.34, desnivelM = 18.0, ultimaPrecisionM = 9f, ritmoActualSegKm = 381,
        puntos = (0..40).map { PuntoGps(-4.0 + it * 0.0005, -79.2 + Math.sin(it / 6.0) * 0.001, it * 1000L, 5f) },
    )

    @Test fun controlesConTextoYSelector2D3D() {
        var vista = true
        compose.setContent {
            GacelasTheme {
                ContenidoCarreraC4(VistaCarrera(estado), onPausa = {}, onStop = {}, onVuelta = {}, onAtras = {}, onVoz = {}, vozActiva = true,
                    extraCabecera = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            com.gacelas.seguridad.ui.BotonReportarIncidente(compacto = true) { null }
                            Box(Modifier.padding(top = 8.dp)) { BotonSOS(-4.0 to -79.2) }
                        }
                    },
                ) { v3d, controles, _ ->
                    vista = v3d
                    Box(Modifier.fillMaxSize().background(Color(0xFF0A0E16))) {
                        TrazadoRuta(estado.puntos, Modifier.fillMaxSize())
                        controles({}, {}, 0f, true)
                    }
                }
            }
        }
        compose.onNodeWithTag("vista-3d-on").assertIsSelected()
        compose.onNodeWithTag("vista-2d-on").assertIsNotSelected()
        compose.onNodeWithTag("boton-sos").assertIsDisplayed()
        captura("carrera-controles-3d")
        compose.onNodeWithTag("vista-2d-on").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("vista-2d-on").assertIsSelected()
        assertEquals(false, vista)
        captura("carrera-controles-2d")
    }

    @Test fun hojaSosActiva() {
        EstadoSos.alertaId.value = "5050aaaa-0000-4000-8000-000000000001"
        EstadoSos.ultimaPos = -3.9931 to -79.2042
        try {
            compose.setContent { GacelasTheme { Box(Modifier.fillMaxSize().background(Color(0xFF0C1322))) { HojaSOS(onCerrar = {}) } } }
            compose.waitForIdle()
            compose.onNodeWithTag("llamar-911").assertIsDisplayed()
            captura("hoja-sos")
        } finally { EstadoSos.alertaId.value = null }
    }

    @Test fun contactosDeEmergencia() {
        compose.setContent {
            GacelasTheme {
                Box(Modifier.fillMaxSize().background(Color(0xFF0C1322)).padding(16.dp)) {
                    ContactosEmergenciaC2(listOf(ContactoEmergencia("Mamá", "+593991234567"))) {}
                }
            }
        }
        compose.onNodeWithTag("agregar-contacto").assertIsDisplayed()
        captura("contactos-emergencia")
    }

    @Test fun inicioDeCarreraEstiloC2() {
        compose.setContent { GacelasTheme { com.gacelas.correr.CorrerScreen(onCerrar = {}, onEmpezar = {}) } }
        compose.waitForIdle()
        captura("correr-inicio")
    }
}
