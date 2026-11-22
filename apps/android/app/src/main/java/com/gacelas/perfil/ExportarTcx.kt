package com.gacelas.perfil

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.actividad.domain.reglas.AnalisisCarrera
import java.io.File

/** Escribe el TCX en la cache y abre el selector para compartirlo. */
fun compartirTcx(context: Context, titulo: String, inicioMs: Long, segundos: Int, distanciaKm: Double, puntos: List<PuntoGps>) {
    val nombre = AnalisisCarrera.nombreTcx(titulo, inicioMs)
    val xml = AnalisisCarrera.carreraATcx(titulo, inicioMs, segundos, distanciaKm, puntos)
    val dir = File(context.cacheDir, "compartir").apply { mkdirs() }
    val archivo = File(dir, nombre)
    archivo.writeText(xml)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.garmin.tcx+xml"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(envio, "Exportar TCX"))
}

fun compartirTcx(context: Context, c: Carrera) =
    compartirTcx(context, c.titulo, c.inicioMs, c.segundos, c.distanciaKm, c.puntos)
