package com.gacelas.core.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gacelas.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service for tracking GPS location during activities.
 * 
 * This service:
 * - Requests location updates at high frequency (3-5 seconds)
 * - Buffers track points locally
 * - Broadcasts live position via Supabase Realtime
 * - Batch inserts track points to database every 30 seconds
 * - Handles offline scenarios with local caching
 * 
 * See SYSTEM.md for detailed architecture.
 */
@AndroidEntryPoint
class LocationService : Service() {

    companion object {
        const val ACTION_START = "com.gacelas.location.START"
        const val ACTION_STOP = "com.gacelas.location.STOP"
        const val EXTRA_ACTIVITY_ID = "activity_id"
        
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "location_service"
        private const val CHANNEL_NAME = "Seguimiento de Actividad"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val activityId = intent.getStringExtra(EXTRA_ACTIVITY_ID)
                startForeground(NOTIFICATION_ID, createNotification())
                // TODO: Start location updates
            }
            ACTION_STOP -> {
                // TODO: Stop location updates, flush remaining points
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el estado de grabación de tu actividad"
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Grabando actividad")
            .setContentText("Tu actividad se está grabando...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
}
