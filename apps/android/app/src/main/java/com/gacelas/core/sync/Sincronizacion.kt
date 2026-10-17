package com.gacelas.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gacelas.GacelasApp
import java.util.concurrent.TimeUnit

/**
 * Sube lo que quedo pendiente sin red (carreras y rutas). WorkManager solo lo ejecuta cuando hay
 * conexion (constraint CONNECTED), sobrevive a reinicios y reintenta con espera exponencial.
 */
class SincronizacionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? GacelasApp ?: return Result.success()
        val quedan = runCatching { app.contenedor.subirPendientes() }.getOrElse { return Result.retry() }
        return if (quedan > 0) Result.retry() else Result.success()
    }
}

object Sincronizacion {
    const val NOMBRE = "gacelas-sincronizar-pendientes"

    fun solicitud() = OneTimeWorkRequestBuilder<SincronizacionWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()

    /** Programa la subida; si ya hay una esperando red se conserva (no se duplica). */
    fun programar(context: Context) {
        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.KEEP, solicitud())
        }
    }
}
