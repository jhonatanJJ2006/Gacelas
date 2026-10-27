package com.gacelas.core.notificaciones

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gacelas.core.location.LocationService
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/**
 * Estado REAL de las notificaciones de Runcity en Android (no el ajuste guardado en la app):
 * permiso/bloqueo del sistema y el canal de la carrera. Antes el switch de Preferencias mostraba
 * "activadas" aunque Android las tuviera bloqueadas y la notificacion de la carrera no salia.
 */
object PermisoNotificaciones {
    /** Notificaciones de la app permitidas por el sistema (POST_NOTIFICATIONS en 13+ y bloqueo manual). */
    fun appPermitidas(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    /** El canal de la carrera no esta desactivado (si aun no existe, cuenta como activo). */
    fun canalCarreraActivo(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 26) return true
        val canal = ctx.getSystemService(NotificationManager::class.java)?.getNotificationChannel(LocationService.CHANNEL_ID) ?: return true
        return canal.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun permitidas(ctx: Context): Boolean = appPermitidas(ctx) && canalCarreraActivo(ctx)

    /** Android 13+ y el permiso aun se puede pedir con el dialogo del sistema. */
    fun sePuedePedir(ctx: Context): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    /** Abre los ajustes de notificaciones de Runcity (o el canal de la carrera si solo este esta apagado). */
    fun intentAjustes(ctx: Context): Intent = when {
        Build.VERSION.SDK_INT >= 26 && appPermitidas(ctx) && !canalCarreraActivo(ctx) ->
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, LocationService.CHANNEL_ID)
        Build.VERSION.SDK_INT >= 26 ->
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun abrirAjustes(ctx: Context) {
        runCatching { ctx.startActivity(intentAjustes(ctx)) }
            .onFailure { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

private fun Context.actividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}

/** Estado real que se vuelve a leer al volver a la app (ON_RESUME), p. ej. desde Ajustes de Android. */
@Composable
fun notificacionesPermitidasState(): androidx.compose.runtime.State<Boolean> {
    val ctx = LocalContext.current
    val estado = remember { mutableStateOf(PermisoNotificaciones.permitidas(ctx)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) estado.value = PermisoNotificaciones.permitidas(ctx) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return estado
}

/**
 * Accion para activar: pide POST_NOTIFICATIONS si Android lo permite; si ya se nego (o en < 13,
 * o el canal esta apagado) abre los ajustes de notificaciones del sistema.
 */
@Composable
fun rememberActivarNotificaciones(alTerminar: (Boolean) -> Unit = {}): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        // Rechazo permanente (Android ya no muestra el dialogo): solo quedan los ajustes del sistema.
        val act = ctx.actividad()
        if (!ok && act != null && !ActivityCompat.shouldShowRequestPermissionRationale(act, Manifest.permission.POST_NOTIFICATIONS)) {
            PermisoNotificaciones.abrirAjustes(ctx)
        } else if (ok && !PermisoNotificaciones.canalCarreraActivo(ctx)) PermisoNotificaciones.abrirAjustes(ctx)
        alTerminar(PermisoNotificaciones.permitidas(ctx))
    }
    return remember(ctx) {
        {
            if (PermisoNotificaciones.sePuedePedir(ctx)) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (!PermisoNotificaciones.permitidas(ctx)) {
                PermisoNotificaciones.abrirAjustes(ctx)
            }
        }
    }
}

/** Aviso con boton cuando Android bloquea las notificaciones de Gacelas. */
@Composable
fun AvisoNotificacionesBloqueadas(texto: String, onActivar: () -> Unit, modifier: Modifier = Modifier) {
    Tarjeta(modifier.testTag("aviso-notificaciones"), fondo = GColors.Danger.copy(alpha = 0.12f), borde = GColors.Danger.copy(alpha = 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsOff, null, tint = GColors.Danger)
            Text("  Notificaciones bloqueadas en Android", style = GType.labelLg, color = GColors.Danger)
        }
        Text(texto, style = GType.bodyMd, color = GColors.TextSecondary)
        BotonPrimario("Abrir ajustes de notificaciones", onActivar, icono = null, color = GColors.Danger)
    }
}
