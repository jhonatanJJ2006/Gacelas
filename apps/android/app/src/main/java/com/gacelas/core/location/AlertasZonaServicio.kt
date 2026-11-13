package com.gacelas.core.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gacelas.MainActivity
import com.gacelas.R
import com.gacelas.navigation.Destinos
import com.gacelas.seguridad.domain.AlertasZona
import com.gacelas.seguridad.domain.IncidentePublico
import com.gacelas.seguridad.domain.MonitorAlertasZona
import com.gacelas.tracking.DestinoPendiente
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Avisos de zona para la pantalla de la carrera (banner arriba) cuando la app esta abierta. */
object EventosAlertaZona {
    private val _flujo = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val flujo: SharedFlow<String> = _flujo.asSharedFlow()
    internal fun emitir(texto: String) { _flujo.tryEmit(texto) }
}

/**
 * Alertas de zona dentro del servicio de ubicacion en primer plano (86e3uf09b, 0.9.15): funcionan con la
 * pantalla apagada o la app en segundo plano. No pide ubicacion extra: usa los puntos que ya llegan al
 * servicio, relee los reportes cada 5 min y solo recalcula al moverse 25 m o cada 15 s.
 * Aviso: notificacion de alta prioridad con vibracion (canal "Alertas de zona"); si las notificaciones
 * estan apagadas, vibra igual. Respeta Ajustes, Privacidad y ubicacion, "Alertas de zona".
 */
class AlertasZonaServicio(
    private val context: Context,
    private val alcance: CoroutineScope,
    /** Lectura de reportes recientes; null sin servidor (no hay alertas). */
    private val fuente: () -> (suspend () -> List<IncidentePublico>)?,
    /** Ajuste "Alertas de zona" de Privacidad y ubicacion. */
    private val activadas: () -> Flow<Boolean>,
) {
    companion object {
        const val CANAL = "alertas_zona"
        private const val ID_BASE = 2_000
        val VIBRACION = longArrayOf(0, 350, 180, 350)
    }

    private val monitor = MonitorAlertasZona()
    private var activa = true
    private var trabajo: Job? = null
    private var avisos: com.gacelas.tracking.Avisos? = null

    fun iniciar() {
        if (trabajo != null) return
        monitor.reiniciar()
        crearCanal()
        val leer = fuente() ?: return
        trabajo = alcance.launch {
            launch { activadas().collect { activa = it } }
            while (isActive) {
                if (activa) runCatching { leer() }.onSuccess { monitor.reportes = it }
                delay(AlertasZona.REFRESCO_MS)
            }
        }
    }

    fun detener() {
        trabajo?.cancel()
        trabajo = null
        avisos?.liberar()
        avisos = null
    }

    /** Cada punto GPS del servicio. */
    fun punto(lat: Double, lng: Double, ahoraMs: Long = System.currentTimeMillis()) {
        if (!activa || trabajo == null) return
        val (i, m) = monitor.evaluar(lat, lng, ahoraMs) ?: return
        avisar(i.id, AlertasZona.mensaje(i, m))
    }

    private fun avisar(id: String, texto: String) {
        EventosAlertaZona.emitir(texto)
        val gestor = NotificationManagerCompat.from(context)
        val permitido = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val canalActivo = Build.VERSION.SDK_INT < 26 ||
            (gestor.getNotificationChannel(CANAL)?.importance ?: NotificationManager.IMPORTANCE_NONE) != NotificationManager.IMPORTANCE_NONE
        if (permitido && gestor.areNotificationsEnabled() && canalActivo) {
            val abrir = PendingIntent.getActivity(
                context, 3,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(DestinoPendiente.EXTRA, Destinos.CARRERA_EN_CURSO),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle("Alerta de zona")
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(VIBRACION)
                .setContentIntent(abrir)
                .setAutoCancel(true)
                .setTimeoutAfter(10 * 60 * 1000L)
                .build()
            runCatching { gestor.notify(ID_BASE + (id.hashCode() and 0xFFFF), n) }
                .onFailure { vibrar() }
        } else vibrar()
    }

    private fun vibrar() {
        val a = avisos ?: com.gacelas.tracking.Avisos(context).also { avisos = it }
        a.alertaZona()
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT < 26) return
        val canal = NotificationChannel(CANAL, "Alertas de zona", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Aviso al acercarte a menos de 200 m de un reporte de inseguridad reciente durante la carrera"
            enableVibration(true)
            vibrationPattern = VIBRACION
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(canal)
    }
}
