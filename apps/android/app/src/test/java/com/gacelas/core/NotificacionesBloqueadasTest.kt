package com.gacelas.core

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.gacelas.ajustes.ConfiguracionNotificacionesScreen
import com.gacelas.core.location.LocationService
import com.gacelas.core.notificaciones.PermisoNotificaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Bug: Android bloqueaba las notificaciones pero Preferencias mostraba "activadas". */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificacionesBloqueadasTest {
    @get:Rule val compose = createComposeRule()
    private val ctx: Context = ApplicationProvider.getApplicationContext<Application>()
    private val nm get() = ctx.getSystemService(NotificationManager::class.java)

    @Test fun bloqueoDelSistemaSeDetecta() {
        shadowOf(nm).setNotificationsEnabled(true)
        assertTrue(PermisoNotificaciones.permitidas(ctx))
        shadowOf(nm).setNotificationsEnabled(false)
        assertFalse(PermisoNotificaciones.permitidas(ctx))
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, PermisoNotificaciones.intentAjustes(ctx).action)
    }

    @Test fun canalDeCarreraApagadoSeDetectaYAbreElCanal() {
        shadowOf(nm).setNotificationsEnabled(true)
        nm.createNotificationChannel(NotificationChannel(LocationService.CHANNEL_ID, "x", NotificationManager.IMPORTANCE_NONE))
        assertFalse(PermisoNotificaciones.permitidas(ctx))
        val i = PermisoNotificaciones.intentAjustes(ctx)
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, i.action)
        assertEquals(LocationService.CHANNEL_ID, i.getStringExtra(Settings.EXTRA_CHANNEL_ID))
    }

    @Test fun switchMuestraBloqueadasYAviso() {
        shadowOf(nm).setNotificationsEnabled(false)
        compose.setContent { ConfiguracionNotificacionesScreen(onAtras = {}) }
        compose.onNodeWithText("Bloqueadas en Android: no verás la carrera en la barra").assertExists()
        compose.onNodeWithTag("aviso-notificaciones").assertExists()
    }

    @Test fun switchMuestraActivadas() {
        shadowOf(nm).setNotificationsEnabled(true)
        compose.setContent { ConfiguracionNotificacionesScreen(onAtras = {}) }
        compose.onNodeWithText("Activadas en Android").assertExists()
        compose.onNodeWithTag("aviso-notificaciones").assertDoesNotExist()
    }
}
