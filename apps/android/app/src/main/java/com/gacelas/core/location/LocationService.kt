package com.gacelas.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gacelas.GacelasApp
import com.gacelas.MainActivity
import com.gacelas.R
import com.gacelas.data.carreras.PuntoGps
import com.gacelas.tracking.formatearHms
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Servicio en primer plano que recibe la ubicacion con FusedLocationProvider durante una carrera
 * y entrega cada punto a RunTracker. Muestra una notificacion con distancia y tiempo.
 * TODO: enviar posicion en vivo y lotes de puntos a Supabase cuando exista el backend.
 */
class LocationService : Service() {

    companion object {
        const val ACTION_START = "com.gacelas.location.START"
        const val ACTION_STOP = "com.gacelas.location.STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "location_service"
        private const val CHANNEL_NAME = "Seguimiento de Actividad"

        fun tienePermiso(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        fun iniciar(context: Context) {
            if (!tienePermiso(context)) return
            ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java).setAction(ACTION_START))
        }

        fun detener(context: Context) {
            context.startService(Intent(context, LocationService::class.java).setAction(ACTION_STOP))
        }
    }

    private val cliente by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val tracker get() = (application as GacelasApp).contenedor.tracker
    private var escuchando = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { l ->
                tracker.registrarPunto(PuntoGps(l.latitude, l.longitude, l.time, l.accuracy, if (l.hasAltitude()) l.altitude else null))
            }
            actualizarNotificacion()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val tipo = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
                ServiceCompat.startForeground(this, NOTIFICATION_ID, createNotification(), tipo)
                empezarUbicacion()
            }
            ACTION_STOP -> {
                detenerUbicacion()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun empezarUbicacion() {
        if (escuchando || !tienePermiso(this)) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(0f)
            .build()
        cliente.requestLocationUpdates(request, callback, Looper.getMainLooper())
        escuchando = true
    }

    private fun detenerUbicacion() {
        if (escuchando) cliente.removeLocationUpdates(callback)
        escuchando = false
    }

    override fun onDestroy() {
        detenerUbicacion()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                description = "Muestra el estado de grabación de tu actividad"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val e = tracker.estado.value
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (e.pausada) "Carrera en pausa" else "Grabando carrera")
            .setContentText("%.2f km  ·  %s".format(java.util.Locale.US, e.distanciaKm, formatearHms(e.segundos)))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(abrir)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun actualizarNotificacion() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, createNotification())
    }
}
