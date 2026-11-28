package com.gacelas.admin

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Guarda un archivo del panel (CSV de Reportes) en la cache y abre el selector de compartir. */
fun compartirArchivoAdmin(context: Context, nombre: String, tipo: String, contenido: String) {
    val dir = File(context.cacheDir, "compartir").apply { mkdirs() }
    val archivo = File(dir, nombre.replace(Regex("[^A-Za-z0-9._-]"), "_"))
    archivo.writeText(contenido, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = tipo
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, nombre)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(envio, "Exportar reporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
