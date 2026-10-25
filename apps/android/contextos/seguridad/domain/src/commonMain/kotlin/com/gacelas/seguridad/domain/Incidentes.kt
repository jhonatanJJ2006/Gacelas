package com.gacelas.seguridad.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/** Tipos de incidente que puede reportar un corredor (mismos id que la migracion 030). */
enum class TipoIncidente(val id: String, val nombre: String) {
    RoboAsalto("robo_asalto", "Robo o asalto"),
    Acoso("acoso", "Acoso"),
    PerrosAgresivos("perros_agresivos", "Perros agresivos"),
    Iluminacion("iluminacion_deficiente", "Iluminación deficiente"),
    Trafico("trafico_peligroso", "Tráfico peligroso"),
    ViaMalEstado("via_mal_estado", "Vía en mal estado"),
    Accidente("accidente", "Accidente"),
    Otro("otro", "Otro");

    companion object {
        fun deId(id: String?) = entries.firstOrNull { it.id == id } ?: Otro
    }
}

enum class NivelSeguridad { Bajo, Medio, Alto;
    companion object {
        fun deIndice(i: Double) = if (i < 0.75) Bajo else if (i < 1.5) Medio else Alto
        fun deTexto(t: String?) = when (t) { "bajo" -> Bajo; "alto" -> Alto; else -> Medio }
    }
}

data class DiaSeguridad(val dia: Int, val nivel: NivelSeguridad, val indice: Double)
data class TipoFrecuente(val servicio: String, val subtipo: String, val conteo: Int)
data class SeguridadZona(
    val dias: List<DiaSeguridad>,
    val tipos: List<TipoFrecuente>,
    val actualizadoHasta: String?,
    val fuente: String,
    val ejemplo: Boolean = false,
)

data class IncidentePublico(
    val id: String,
    val tipo: TipoIncidente,
    val lat: Double,
    val lng: Double,
    val ocurridoMs: Long,
    val confirmaciones: Int,
    val verificado: Boolean,
)

object Incidentes {
    const val MAX_FOTOS = 3
    const val MAX_BYTES_FOTO = 3 * 1024 * 1024
    val DIAS_CORTOS = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
    const val AVISO =
        "Indicador orientativo con datos agregados del ECU 911 por parroquia (sin hora exacta) y reportes de corredores. " +
            "Llega con meses de retraso y no garantiza tu seguridad: mantente atento y, ante una emergencia, llama al 911."

    /** Datos de ejemplo (ficticios) para la vista previa y la app sin servidor. */
    val EJEMPLO = SeguridadZona(
        dias = listOf(0.6, 0.9, 0.8, 0.9, 1.1, 1.6, 1.3).mapIndexed { d, i -> DiaSeguridad(d, NivelSeguridad.deIndice(i), i) },
        tipos = listOf(
            TipoFrecuente("Tránsito y Movilidad", "Ejemplo: siniestro de tránsito", 18),
            TipoFrecuente("Seguridad Ciudadana", "Ejemplo: alerta de seguridad", 11),
            TipoFrecuente("Gestión Sanitaria", "Ejemplo: atención prehospitalaria", 7),
        ),
        actualizadoHasta = "2026-03",
        fuente = "Ejemplo de vista previa. Fuente real: SIS ECU 911 (CC-BY) y reportes de corredores",
        ejemplo = true,
    )

    fun ejemplos(ahora: Long) = listOf(
        IncidentePublico("vp-i1", TipoIncidente.PerrosAgresivos, -3.994, -79.203, ahora - 2 * 3_600_000L, 3, true),
        IncidentePublico("vp-i2", TipoIncidente.Iluminacion, -3.986, -79.199, ahora - 26 * 3_600_000L, 1, false),
        IncidentePublico("vp-i3", TipoIncidente.Trafico, -4.001, -79.207, ahora - 50 * 3_600_000L, 5, true),
    )

    fun haceTexto(ms: Long, ahora: Long): String {
        val h = (ahora - ms) / 3_600_000L
        return when { h < 1 -> "hace menos de 1 h"; h < 24 -> "hace $h h"; else -> "hace ${h / 24} d" }
    }

    /** Punto en EWKT que PostGIS acepta en una columna geometry(Point, 4326). */
    fun ewkt(lat: Double, lng: Double) = "SRID=4326;POINT($lng $lat)"

    fun leerZona(json: JsonElement): SeguridadZona? {
        val o = json as? JsonObject ?: return null
        val dias = (o["dias"] as? JsonArray).orEmpty().mapNotNull { e ->
            val d = e as? JsonObject ?: return@mapNotNull null
            DiaSeguridad(d.entero("dia") ?: return@mapNotNull null, NivelSeguridad.deTexto(d.texto("nivel")), d.numero("indice") ?: 1.0)
        }
        val tipos = (o["tipos"] as? JsonArray).orEmpty().mapNotNull { e ->
            val t = e as? JsonObject ?: return@mapNotNull null
            TipoFrecuente(t.texto("servicio").orEmpty(), t.texto("subtipo").orEmpty(), t.entero("conteo") ?: 0)
        }
        return SeguridadZona(dias, tipos, o.texto("actualizado_hasta"), o.texto("fuente") ?: "SIS ECU 911 (CC-BY)")
    }
}

internal fun JsonObject.texto(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
internal fun JsonObject.numero(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.doubleOrNull
internal fun JsonObject.entero(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.intOrNull


/** Puerto del contexto Seguridad de rutas; la implementacion vive en la capa data. */
interface IncidentesRepositorio {
    suspend fun seguridadZona(rutaId: String): SeguridadZona?
    suspend fun recientes(): List<IncidentePublico>
    suspend fun reportar(
        tipo: TipoIncidente, lat: Double, lng: Double, direccion: String, ocurridoMs: Long,
        descripcion: String?, fotosJpeg: List<ByteArray>, carpeta: String,
    )
    suspend fun confirmar(id: String): Boolean
}
