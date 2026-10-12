package com.gacelas.actualizacion

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.security.MessageDigest

sealed interface Descarga {
    data class Progreso(val porcentaje: Int, val hechos: Long = 0, val total: Long = 0) : Descarga
    data class Lista(val archivo: File) : Descarga
    data class Error(val mensaje: String) : Descarga
}

/** Descarga el APK con DownloadManager e inicia la instalacion con FileProvider. */
object Instalador {
    private fun carpeta(context: Context) = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "actualizaciones").apply { mkdirs() }

    fun descargar(context: Context, url: String, versionName: String, sha256: String?): Flow<Descarga> = flow {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val archivo = File(carpeta(context), "gacelas-$versionName.apk")
        carpeta(context).listFiles()?.forEach { it.delete() }
        val id = dm.enqueue(
            DownloadManager.Request(Uri.parse(url))
                .setTitle("Gacelas $versionName")
                .setDescription("Descargando actualización")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(archivo)),
        )
        while (true) {
            val estado = dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                if (!c.moveToFirst()) return@use null
                Triple(
                    c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                    c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                    c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                )
            }
            if (estado == null) { emit(Descarga.Error("Se canceló la descarga")); return@flow }
            val (s, hechos, total) = estado
            when (s) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    if (sha256 != null && !sha256.equals(hash(archivo), ignoreCase = true)) {
                        archivo.delete(); emit(Descarga.Error("El archivo descargado no coincide. Intenta de nuevo.")); return@flow
                    }
                    emit(Descarga.Lista(archivo)); return@flow
                }
                DownloadManager.STATUS_FAILED -> { emit(Descarga.Error("No se pudo descargar. Revisa tu conexión.")); return@flow }
                else -> emit(Descarga.Progreso(if (total > 0) (hechos * 100 / total).toInt() else 0, hechos, maxOf(total, 0)))
            }
            delay(400)
        }
    }.flowOn(Dispatchers.IO)

    private fun hash(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { i -> val b = ByteArray(1 shl 16); while (true) { val n = i.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Tamano del APK con una peticion HEAD (para manifiestos sin apkBytes). */
    suspend fun tamano(url: String): Long? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            val con = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            try { con.requestMethod = "HEAD"; con.connectTimeout = 6000; con.readTimeout = 6000; con.contentLengthLong.takeIf { it > 0 } } finally { con.disconnect() }
        }.getOrNull()
    }

    /** En Android 8+ cada app necesita permiso del usuario para instalar APKs. */
    fun puedeInstalar(context: Context) = Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun pedirPermiso(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun instalar(context: Context, archivo: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
