package com.gacelas.seguridad.data

import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.FechaIso
import com.gacelas.data.remoto.entero
import com.gacelas.data.remoto.numero
import com.gacelas.data.remoto.texto
import com.gacelas.seguridad.domain.IncidentePublico
import com.gacelas.seguridad.domain.Incidentes
import com.gacelas.seguridad.domain.IncidentesRepositorio
import com.gacelas.seguridad.domain.SeguridadZona
import com.gacelas.seguridad.domain.TipoIncidente
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Incidentes: ECU 911 agregado (seguridad_zona_ruta) y reportes de corredores
 * (incidentes_usuario, vista anonima incidentes_publicos). Ver docs/INCIDENTES.md.
 */
class SupabaseIncidentesRepositorio(private val backend: Backend) : IncidentesRepositorio {
    override suspend fun seguridadZona(rutaId: String): SeguridadZona? = runCatching {
        Incidentes.leerZona(backend.rpc("seguridad_zona_ruta", JsonObject(mapOf("p_ruta_id" to JsonPrimitive(rutaId)))))
    }.getOrNull()

    override suspend fun recientes(): List<IncidentePublico> = runCatching {
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
    override suspend fun reportar(
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

    override suspend fun confirmar(id: String): Boolean = runCatching {
        backend.insert("incidentes_confirmaciones", listOf(JsonObject(mapOf("incidente_id" to JsonPrimitive(id)))))
    }.isSuccess
}
