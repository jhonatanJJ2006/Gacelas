package com.gacelas.incidentes

import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.FechaIso
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

private fun JsonObject.texto(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
private fun JsonObject.numero(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.doubleOrNull
private fun JsonObject.entero(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.intOrNull

/**
 * Incidentes: ECU 911 agregado (seguridad_zona_ruta) y reportes de corredores
 * (incidentes_usuario, vista anonima incidentes_publicos). Ver docs/INCIDENTES.md.
 */
class IncidentesRepository(private val backend: Backend) {
    suspend fun seguridadZona(rutaId: String): SeguridadZona? = runCatching {
        Incidentes.leerZona(backend.rpc("seguridad_zona_ruta", JsonObject(mapOf("p_ruta_id" to JsonPrimitive(rutaId)))))
    }.getOrNull()

    suspend fun recientes(): List<IncidentePublico> = runCatching {
        backend.select("incidentes_publicos", emptyMap(), "ocurrido_hora", descendente = true).mapNotNull { f ->
            IncidentePublico(
                f.texto("id") ?: return@mapNotNull null, TipoIncidente.deId(f.texto("tipo")),
                f.numero("lat") ?: return@mapNotNull null, f.numero("lng") ?: return@mapNotNull null,
                f.texto("ocurrido_hora")?.let(FechaIso::leer) ?: 0L, f.entero("confirmaciones") ?: 0,
                f.texto("estado") == "verificado",
            )
        }
    }.getOrDefault(emptyList())

    /** Sube fotos (ya sin EXIF) con nombres aleatorios y crea el reporte. */
    suspend fun reportar(
        tipo: TipoIncidente, lat: Double, lng: Double, direccion: String, ocurridoMs: Long,
        descripcion: String?, fotosJpeg: List<ByteArray>, carpeta: String,
    ) {
        require(fotosJpeg.size <= Incidentes.MAX_FOTOS) { "Máximo ${Incidentes.MAX_FOTOS} fotos" }
        val rutas = fotosJpeg.mapIndexed { i, b ->
            require(b.size <= Incidentes.MAX_BYTES_FOTO) { "La foto es demasiado grande" }
            val ruta = "$carpeta/$i.jpg"
            backend.subirArchivo("incidentes", ruta, b, "image/jpeg")
            ruta
        }
        backend.insert("incidentes_usuario", listOf(JsonObject(buildMap {
            put("tipo", JsonPrimitive(tipo.id))
            descripcion?.trim()?.takeIf { it.isNotEmpty() }?.let { put("descripcion", JsonPrimitive(it.take(280))) }
            put("geom", JsonPrimitive(Incidentes.ewkt(lat, lng)))
            put("direccion", JsonPrimitive(direccion.take(200)))
            put("ocurrido_at", JsonPrimitive(FechaIso.formatear(ocurridoMs)))
            put("fotos", JsonArray(rutas.map(::JsonPrimitive)))
        })))
    }

    suspend fun confirmar(id: String) = runCatching {
        backend.insert("incidentes_confirmaciones", listOf(JsonObject(mapOf("incidente_id" to JsonPrimitive(id)))))
    }.isSuccess
}
