package com.gacelas

import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.repositorio.ClubesRepository
import com.gacelas.comunidad.presentation.ClubesViewModel
import com.gacelas.comunidad.presentation.DetalleClubViewModel
import com.gacelas.comunidad.presentation.ui.ClubesContenido
import com.gacelas.comunidad.presentation.ui.DetalleClubContenido
import com.gacelas.core.location.AlertasZonaServicio
import com.gacelas.seguridad.domain.IncidentePublico
import com.gacelas.seguridad.domain.MonitorAlertasZona
import com.gacelas.seguridad.domain.TipoIncidente
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/** 0.9.15: esqueletos de grupos y comunidades, y alertas de zona en el servicio de ubicacion. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h884dp-xxhdpi")
class Version0915Test {
    @get:Rule val compose = createComposeRule()

    private fun captura(n: String) {
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        compose.onRoot().captureRoboImage("$dir/v0915-$n.png")
    }

    private val ahora = 1_800_000_000_000L
    private val lat = -3.9931
    private val lng = -79.2042
    private fun reporte(id: String, dLat: Double) = IncidentePublico(id, TipoIncidente.RoboAsalto, lat + dLat, lng, ahora - 3_600_000, 2, false)

    @Test fun monitorAvisaUnaVezYRecalculaSoloAlMoverse() {
        val m = MonitorAlertasZona()
        m.reportes = listOf(reporte("a", 0.0013), reporte("b", 0.02))
        val primera = m.evaluar(lat, lng, ahora)
        assertEquals("a", primera?.first?.id)
        // Mismo sitio: no repite el aviso.
        assertNull(m.evaluar(lat, lng, ahora + 20_000))
        // Se acerca a "b": la evaluacion espera a moverse 25 m o 15 s.
        m.reportes = m.reportes + reporte("c", 0.0001)
        assertEquals("c", m.evaluar(lat, lng, ahora + 21_000)?.first?.id)
        assertNull(m.evaluar(lat + 0.00005, lng, ahora + 22_000))
        assertEquals("b", m.evaluar(lat + 0.0195, lng, ahora + 23_000)?.first?.id)
        m.reiniciar()
        assertEquals(emptySet<String>(), m.yaAvisados)
    }

    private fun servicio(activas: MutableStateFlow<Boolean>, alcance: CoroutineScope): AlertasZonaServicio {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(ctx.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(true)
        shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        return AlertasZonaServicio(ctx, alcance, fuente = { suspend { listOf(reporte("a", 0.0013), reporte("b", 0.05)) } }, activadas = { activas })
    }

    @Test fun servicioNotificaConPantallaApagadaUnaVezPorReporte() {
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val s = servicio(MutableStateFlow(true), alcance)
        s.iniciar()
        ShadowLooper.idleMainLooper()
        s.punto(lat, lng, ahora)
        s.punto(lat + 0.0003, lng, ahora + 30_000)
        val nm = ApplicationProvider.getApplicationContext<Context>().getSystemService(NotificationManager::class.java)
        val avisos = shadowOf(nm).allNotifications.filter { it.channelId == AlertasZonaServicio.CANAL }
        assertEquals(1, avisos.size)
        org.junit.Assert.assertTrue(avisos.first().extras.getString("android.text")!!.startsWith("Atención: reporte de robo o asalto a "))
        assertEquals(NotificationManager.IMPORTANCE_HIGH, nm.getNotificationChannel(AlertasZonaServicio.CANAL).importance)
        s.detener(); alcance.cancel()
    }

    @Test fun servicioRespetaElAjusteApagado() {
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val s = servicio(MutableStateFlow(false), alcance)
        s.iniciar()
        ShadowLooper.idleMainLooper()
        s.punto(lat, lng, ahora)
        val nm = ApplicationProvider.getApplicationContext<Context>().getSystemService(NotificationManager::class.java)
        assertEquals(0, shadowOf(nm).allNotifications.count { it.channelId == AlertasZonaServicio.CANAL })
        s.detener(); alcance.cancel()
    }

    private val colgado = object : ClubesRepository {
        override suspend fun listar(): Pair<List<Club>, Set<String>> = awaitCancellation()
        override suspend fun detalle(id: String): Club? = awaitCancellation()
    }

    @Test fun esqueletoListaDeGrupos() {
        compose.setContent { GacelasTheme { ClubesContenido(ClubesViewModel(colgado), true, {}, {}, {}) } }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("cargando-clubes").assertIsDisplayed()
        captura("esqueleto-grupos")
    }

    @Test fun esqueletoDetalleDeGrupo() {
        compose.setContent { GacelasTheme { DetalleClubContenido("g1", DetalleClubViewModel("g1", colgado), true, {}, {}, {}, {}) } }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("cargando-club").assertIsDisplayed()
        captura("esqueleto-detalle-grupo")
    }

    @Test fun grupoInexistenteNoQuedaCargando() {
        val vacio = object : ClubesRepository {
            override suspend fun listar() = emptyList<Club>() to emptySet<String>()
            override suspend fun detalle(id: String): Club? = null
        }
        compose.setContent { GacelasTheme { DetalleClubContenido("x", DetalleClubViewModel("x", vacio), true, {}, {}, {}, {}) } }
        compose.waitForIdle()
        compose.onNodeWithTag("club-no-encontrado").assertIsDisplayed()
    }
}
