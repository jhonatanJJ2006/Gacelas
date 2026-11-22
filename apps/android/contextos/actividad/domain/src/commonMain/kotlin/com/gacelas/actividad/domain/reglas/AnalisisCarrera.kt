package com.gacelas.actividad.domain.reglas

import com.gacelas.actividad.domain.modelo.Parcial
import com.gacelas.actividad.domain.modelo.PuntoGps
import kotlinx.datetime.Instant

data class PuntoElevacion(val distanciaKm: Double, val altitudM: Double)

data class LadoComparacion(
    val id: String,
    val titulo: String,
    val distanciaKm: Double,
    val segundos: Int,
    val ritmoSegKm: Int?,
    val desnivelM: Int,
)

/** Ritmo, elevacion contra distancia y TCX de una carrera ya registrada. */
object AnalisisCarrera {
    fun parcialesPorKm(parciales: List<Parcial>) = parciales.filter { !it.manual && it.distanciaKm > 0 }

    /** Altura contra distancia recorrida, remuestreada a pasos iguales. */
    fun serieElevacion(puntos: List<PuntoGps>, maxPuntos: Int = 80): List<PuntoElevacion> {
        val conAltura = puntos.filter { it.altitudM != null }
        if (conAltura.size < 2) return emptyList()
        var metros = 0.0
        val serie = ArrayList<PuntoElevacion>(conAltura.size)
        serie.add(PuntoElevacion(0.0, conAltura.first().altitudM!!))
        for (i in 1 until conAltura.size) {
            metros += Geo.haversineM(conAltura[i - 1], conAltura[i])
            serie.add(PuntoElevacion(metros / 1000.0, conAltura[i].altitudM!!))
        }
        val total = serie.last().distanciaKm
        if (serie.size <= maxPuntos || total <= 0.0) return serie
        return List(maxPuntos) { i ->
            val d = total * i / (maxPuntos - 1)
            var j = 1
            while (j < serie.size - 1 && serie[j].distanciaKm < d) j++
            val a = serie[j - 1]
            val b = serie[j]
            val tramo = b.distanciaKm - a.distanciaKm
            val t = if (tramo == 0.0) 0.0 else (d - a.distanciaKm) / tramo
            PuntoElevacion(d, a.altitudM + (b.altitudM - a.altitudM) * t)
        }
    }

    fun escaparXml(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    fun iso(ms: Long) = Instant.fromEpochMilliseconds(ms).toString().replace(Regex("\\.\\d+Z$"), "Z")

    fun nombreTcx(titulo: String, inicioMs: Long): String {
        val base = titulo.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "carrera" }
        val fecha = iso(inicioMs).take(10).replace("-", "")
        return "$base-$fecha.tcx"
    }

    fun carreraATcx(titulo: String, inicioMs: Long, segundos: Int, distanciaKm: Double, puntos: List<PuntoGps>): String {
        var metros = 0.0
        val track = puntos.mapIndexed { i, p ->
            if (i > 0) metros += Geo.haversineM(puntos[i - 1], p)
            val alt = p.altitudM?.let { "<AltitudeMeters>${unDecimal(it)}</AltitudeMeters>" }.orEmpty()
            val tiempo = iso(if (p.tiempoMs > 0) p.tiempoMs else inicioMs)
            "<Trackpoint><Time>$tiempo</Time><Position><LatitudeDegrees>${p.lat}</LatitudeDegrees><LongitudeDegrees>${p.lng}</LongitudeDegrees></Position>$alt<DistanceMeters>${unDecimal(metros)}</DistanceMeters></Trackpoint>"
        }.joinToString("")
        val id = iso(inicioMs)
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<TrainingCenterDatabase xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\">" +
            "<Activities><Activity Sport=\"Running\"><Id>$id</Id>" +
            "<Lap StartTime=\"$id\">" +
            "<TotalTimeSeconds>${segundos.coerceAtLeast(0)}</TotalTimeSeconds>" +
            "<DistanceMeters>${unDecimal(distanciaKm * 1000)}</DistanceMeters>" +
            "<Intensity>Active</Intensity><TriggerMethod>Manual</TriggerMethod>" +
            "<Track>$track</Track></Lap>" +
            "<Notes>${escaparXml(titulo)}</Notes>" +
            "</Activity></Activities></TrainingCenterDatabase>"
    }

    private fun unDecimal(n: Double): String {
        val v = kotlin.math.round(n * 10).toLong()
        val abs = kotlin.math.abs(v)
        return "${if (v < 0) "-" else ""}${abs / 10}.${abs % 10}"
    }

    fun lado(id: String, titulo: String, distanciaKm: Double, segundos: Int, desnivelM: Int) = LadoComparacion(
        id, titulo, distanciaKm, segundos,
        if (distanciaKm > 0.01) (segundos / distanciaKm).toInt() else null,
        desnivelM,
    )
}
