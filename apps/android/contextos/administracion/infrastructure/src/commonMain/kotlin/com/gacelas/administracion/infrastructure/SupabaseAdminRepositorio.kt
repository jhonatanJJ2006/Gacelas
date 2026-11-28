package com.gacelas.administracion.infrastructure

import com.gacelas.administracion.domain.AccesoUsuario
import com.gacelas.administracion.domain.AccionModeracion
import com.gacelas.administracion.domain.AlertaSos
import com.gacelas.administracion.domain.EscenarioVivo
import com.gacelas.administracion.domain.DiaEstadistica
import com.gacelas.administracion.domain.ImportacionEcu
import com.gacelas.administracion.domain.Prioridad
import com.gacelas.administracion.domain.PuntoGeo
import com.gacelas.administracion.domain.RespuestaRapida
import com.gacelas.administracion.domain.VersionEnUso
import com.gacelas.administracion.domain.VersionPublicada
import com.gacelas.administracion.domain.AccionReporte
import com.gacelas.administracion.domain.AdminRepositorio
import com.gacelas.administracion.domain.AjusteApp
import com.gacelas.administracion.domain.ClubAdmin
import com.gacelas.administracion.domain.EstadisticasAdmin
import com.gacelas.administracion.domain.EstadoReporte
import com.gacelas.administracion.domain.EventoAdmin
import com.gacelas.administracion.domain.MetricasAdmin
import com.gacelas.administracion.domain.ReporteAdmin
import com.gacelas.administracion.domain.RolAdmin
import com.gacelas.administracion.domain.RutaAdmin
import com.gacelas.administracion.domain.UsuarioAdmin
import com.gacelas.data.remoto.Backend
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Panel de administracion sobre Supabase. Usa exactamente las funciones y tablas de la web
 * (is_admin, es_moderador, admin_metricas, admin_usuarios, admin_suspender, admin_cola_reportes,
 * moderar_reporte, admin_estadisticas; tablas user_roles, events, routes, clubs y app_settings) y
 * las de la migracion 050 (admin_cola_reportes_sla, admin_prioridad_reporte, respuestas rapidas,
 * admin_sos_alertas, admin_ultimo_acceso, versiones e importaciones_ecu911).
 * Todas comprueban el rol en el servidor (SECURITY DEFINER o RLS con public.is_admin()).
 */
class SupabaseAdminRepositorio(private val backend: Backend) : AdminRepositorio {
    private val vacio = JsonObject(emptyMap())

    override suspend fun rolActual(): RolAdmin? {
        if (backend.usuarioActual() == null) return null
        if (backend.rpc("is_admin", vacio).esVerdadero()) return RolAdmin.Admin
        if (backend.rpc("es_moderador", vacio).esVerdadero()) return RolAdmin.Moderador
        return null
    }

    override suspend fun metricas(): MetricasAdmin {
        val o = backend.rpc("admin_metricas", vacio) as? JsonObject ?: JsonObject(emptyMap())
        return MetricasAdmin(
            usuarios = o.entero("usuarios"), usuarios7d = o.entero("usuarios_7d"), carreras7d = o.entero("carreras_7d"),
            km7d = o.decimal("km_7d"), reportesPendientes = o.entero("reportes_pendientes"),
            eventosProximos = o.entero("eventos_proximos"), clubes = o.entero("clubes"), rutas = o.entero("rutas"),
        )
    }

    override suspend fun usuarios(): List<UsuarioAdmin> {
        val lista = backend.rpc("admin_usuarios", vacio).filas().map(::usuario)
        // Ultimo acceso (050). Si falla, la lista sale igual y el dato queda sin mostrar.
        val accesos = opcional { backend.rpc("admin_ultimo_acceso", vacio).filas().map(::acceso) }.orEmpty().associateBy { it.id }
        return lista.map { u ->
            val a = accesos[u.id] ?: return@map u
            u.copy(ultimoAcceso = a.ultimoAcceso, plataforma = a.plataforma, versionApp = a.version)
        }
    }

    override suspend fun suspender(id: String, suspender: Boolean) {
        backend.rpc("admin_suspender", JsonObject(mapOf("uid" to JsonPrimitive(id), "suspender" to JsonPrimitive(suspender))))
    }

    override suspend fun cambiarRol(id: String, rol: RolAdmin?) {
        if (rol == null) backend.delete("user_roles", mapOf("user_id" to id))
        else backend.upsert("user_roles", listOf(JsonObject(mapOf("user_id" to JsonPrimitive(id), "role" to JsonPrimitive(rol.clave)))), "user_id")
    }

    override suspend fun reportes(estado: EstadoReporte?, tipo: String?): List<ReporteAdmin> {
        val args = JsonObject(mapOf("estado" to (estado?.clave?.let(::JsonPrimitive) ?: JsonNull), "tipo" to (tipo?.let(::JsonPrimitive) ?: JsonNull)))
        return backend.rpc("admin_cola_reportes_sla", args).filas().map(::reporte)
    }

    override suspend fun moderar(reporteId: String, accion: AccionModeracion, nota: String) {
        backend.rpc("moderar_reporte", JsonObject(mapOf("reporte" to JsonPrimitive(reporteId), "accion" to JsonPrimitive(accion.clave), "nota" to JsonPrimitive(nota))))
    }

    override suspend fun estadisticas(desde: String, hasta: String): EstadisticasAdmin {
        val o = backend.rpc("admin_estadisticas", JsonObject(mapOf("desde" to JsonPrimitive(desde), "hasta" to JsonPrimitive(hasta)))) as? JsonObject
        val totales = (o?.get("totales") as? JsonObject).orEmpty().mapValues { (_, v) -> (v as? JsonPrimitive)?.numero() ?: 0.0 }
        val serie = (o?.get("serie") as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map {
            DiaEstadistica(it.texto("dia").orEmpty(), it.decimal("nuevos"), it.decimal("activos"), it.decimal("carreras"), it.decimal("km"))
        }
        return EstadisticasAdmin(desde, hasta, totales, serie)
    }

    override suspend fun eventos(): List<EventoAdmin> =
        backend.select("events", emptyMap(), ordenarPor = "starts_at", descendente = true).map {
            EventoAdmin(
                id = it.texto("id"), titulo = it.texto("title").orEmpty(), descripcion = it.texto("description").orEmpty(),
                iniciaEn = it.texto("starts_at"), lugar = it.texto("location").orEmpty(), distanciaKm = it.decimal("distance_km"),
                publicado = (it["published"] as? JsonPrimitive)?.booleanOrNull ?: false,
            )
        }

    override suspend fun guardarEvento(e: EventoAdmin) {
        val fila = JsonObject(
            mapOf(
                "title" to JsonPrimitive(e.titulo), "description" to JsonPrimitive(e.descripcion),
                "starts_at" to (e.iniciaEn?.let(::JsonPrimitive) ?: JsonNull), "location" to JsonPrimitive(e.lugar),
                "distance_km" to JsonPrimitive(e.distanciaKm), "published" to JsonPrimitive(e.publicado),
                "date_label" to JsonPrimitive(etiquetaFecha(e.iniciaEn)), "time_label" to JsonPrimitive(etiquetaHora(e.iniciaEn)),
            ),
        )
        if (e.id != null) backend.update("events", fila, mapOf("id" to e.id!!)) else backend.insert("events", listOf(fila))
    }

    override suspend fun rutas(): List<RutaAdmin> = backend.select("routes", emptyMap(), ordenarPor = "name").map {
        RutaAdmin(it.texto("id").orEmpty(), it.texto("name").orEmpty(), it.decimal("distance_km"), it.texto("difficulty").orEmpty())
    }

    override suspend fun borrarRuta(id: String) = backend.delete("routes", mapOf("id" to id))

    override suspend fun clubes(): List<ClubAdmin> = backend.select("clubs", emptyMap(), ordenarPor = "name").map {
        ClubAdmin(it.texto("id").orEmpty(), it.texto("name").orEmpty(), it.texto("description").orEmpty())
    }

    override suspend fun borrarClub(id: String) = backend.delete("clubs", mapOf("id" to id))

    override suspend fun ajustes(): List<AjusteApp> = backend.select("app_settings", emptyMap(), ordenarPor = "key").map {
        AjusteApp(it.texto("key").orEmpty(), it["value"] ?: JsonNull)
    }

    override suspend fun guardarAjuste(clave: String, valor: JsonElement) {
        backend.upsert("app_settings", listOf(JsonObject(mapOf("key" to JsonPrimitive(clave), "value" to valor))), "key")
    }

    override suspend fun cambiarPrioridad(reporteId: String, prioridad: Prioridad) {
        backend.rpc("admin_prioridad_reporte", JsonObject(mapOf("reporte" to JsonPrimitive(reporteId), "prioridad" to JsonPrimitive(prioridad.clave))))
    }

    override suspend fun respuestasRapidas(): List<RespuestaRapida> = backend.rpc("admin_respuestas_rapidas", vacio).filas().map {
        RespuestaRapida(it.texto("id"), it.texto("titulo").orEmpty(), it.texto("texto").orEmpty())
    }

    override suspend fun guardarRespuestaRapida(r: RespuestaRapida) {
        backend.rpc(
            "admin_guardar_respuesta_rapida",
            JsonObject(mapOf("p_id" to (r.id?.let(::JsonPrimitive) ?: JsonNull), "p_titulo" to JsonPrimitive(r.titulo.trim()), "p_texto" to JsonPrimitive(r.texto.trim()))),
        )
    }

    override suspend fun borrarRespuestaRapida(id: String) {
        backend.rpc("admin_borrar_respuesta_rapida", JsonObject(mapOf("p_id" to JsonPrimitive(id))))
    }

    /**
     * Mismo escenario que la web (fuente.ts, vivoSupabase): corredores con posicion de los ultimos
     * 2 minutos que estan corriendo, incidentes de corredores sin verificar y alertas SOS. Cada parte
     * falla por separado y queda vacia, como en la web.
     */
    override suspend fun escenarioVivo(): EscenarioVivo {
        val ahora = Clock.System.now()
        val corredores = opcional { backend.select("runner_positions", emptyMap(), ordenarPor = "updated_at", descendente = true) }.orEmpty()
            .filter { (it["running"] as? JsonPrimitive)?.booleanOrNull == true && reciente(it.texto("updated_at"), ahora) }
            .mapNotNull { punto(it) }
        val incidentes = opcional { backend.select("incidentes_publicos", mapOf("estado" to "pendiente")) }.orEmpty().mapNotNull { punto(it) }
        val sos = backend.rpc("admin_sos_alertas", JsonObject(mapOf("horas" to JsonPrimitive(24)))).filas().map {
            AlertaSos(
                id = it.texto("id").orEmpty(), nombre = it.texto("nombre").orEmpty(), activa = (it["activa"] as? JsonPrimitive)?.booleanOrNull == true,
                iniciadaEn = it.texto("started_at").orEmpty(), terminadaEn = it.texto("ended_at"), ultimaEn = it.texto("last_at"),
                posicion = punto(it), puntos = it.entero("puntos").toInt(),
            )
        }
        return EscenarioVivo(corredores, incidentes, sos)
    }

    override suspend fun versionesPublicadas(): List<VersionPublicada> = backend.rpc("admin_versiones_publicadas", vacio).filas().map {
        VersionPublicada(
            it.texto("version_name").orEmpty(), it.entero("version_code").toInt(), it.texto("canal").orEmpty(),
            it.texto("publicada_en").orEmpty(), it.texto("apk_bytes")?.toLongOrNull(),
        )
    }

    override suspend fun versionesEnUso(): List<VersionEnUso> = backend.rpc("admin_versiones_en_uso", JsonObject(mapOf("dias" to JsonPrimitive(30)))).filas().map {
        VersionEnUso(it.texto("plataforma").orEmpty(), it.texto("version_name").orEmpty(), it.texto("version_code")?.toIntOrNull(), it.entero("usuarios"))
    }

    override suspend fun importacionesEcu(): List<ImportacionEcu> =
        backend.select("importaciones_ecu911", emptyMap(), ordenarPor = "fecha", descendente = true).take(12).map {
            ImportacionEcu(
                it.texto("periodo").orEmpty(), it.texto("archivo").orEmpty(), it.entero("filas"), it.entero("filas_loja"),
                it.texto("estado") == "ok", it.texto("mensaje").orEmpty(), it.texto("fecha").orEmpty(),
            )
        }

    private fun acceso(o: JsonObject) = AccesoUsuario(o.texto("id").orEmpty(), o.texto("ultimo_acceso"), o.texto("plataforma"), o.texto("version_name"))

    private fun usuario(o: JsonObject) = UsuarioAdmin(
        id = o.texto("id").orEmpty(), username = o.texto("username").orEmpty(), nombre = o.texto("full_name"),
        creadoEn = o.texto("created_at").orEmpty(), suspendidoEn = o.texto("suspended_at"), rol = RolAdmin.desde(o.texto("role")),
        carreras = o.entero("carreras"), km = o.decimal("km"),
    )

    private fun reporte(o: JsonObject) = ReporteAdmin(
        id = o.texto("id").orEmpty(), tipo = o.texto("target_type").orEmpty(), objetivoId = o.texto("target_id").orEmpty(),
        motivo = o.texto("reason").orEmpty(), detalle = o.texto("detail").orEmpty(), estado = EstadoReporte.desde(o.texto("status").orEmpty()),
        creadoEn = o.texto("created_at").orEmpty(), reportante = o.texto("reportante").orEmpty(), resumen = o.texto("resumen").orEmpty(),
        totalReportes = o.entero("total_reportes").coerceAtLeast(1),
        acciones = (o["acciones"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map {
            AccionReporte(it.texto("accion").orEmpty(), it.texto("nota").orEmpty(), it.texto("fecha").orEmpty(), it.texto("moderador").orEmpty())
        },
        prioridad = Prioridad.desde(o.texto("prioridad")), venceEn = o.texto("vence_en"),
    )
}

/** Ejecuta un bloque cuyo fallo no debe tumbar la seccion; null si falla. */
private suspend fun <T> opcional(bloque: suspend () -> T): T? = try {
    bloque()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}

/** Posicion de una fila con lat y lng; null si falta alguna. */
private fun punto(o: JsonObject): PuntoGeo? {
    val lat = (o["lat"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.numero() ?: return null
    val lng = (o["lng"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.numero() ?: return null
    return PuntoGeo(lat, lng)
}

/** Posicion de menos de 2 minutos (mismo criterio que el mapa en vivo, CADUCIDAD_MS). */
internal fun reciente(iso: String?, ahora: Instant, maxMs: Long = 2 * 60_000): Boolean {
    val t = try { Instant.parse(iso ?: return false) } catch (_: IllegalArgumentException) { return false }
    return (ahora - t).inWholeMilliseconds in 0..maxMs
}

private fun JsonElement.esVerdadero() = (this as? JsonPrimitive)?.booleanOrNull == true
private fun JsonElement.filas(): List<JsonObject> = (this as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
private fun JsonPrimitive.numero(): Double? = doubleOrNull ?: contentOrNull?.toDoubleOrNull()
internal fun JsonObject.texto(k: String): String? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
internal fun JsonObject.decimal(k: String): Double = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.numero() ?: 0.0
internal fun JsonObject.entero(k: String): Long = decimal(k).toLong()

/** "2026-11-15T07:00:00-05:00" -> "15/11/2026" (como date_label de la web). */
internal fun etiquetaFecha(iso: String?): String {
    val f = iso?.take(10)?.split('-') ?: return ""
    return if (f.size == 3) "${f[2]}/${f[1]}/${f[0]}" else ""
}

/** "2026-11-15T07:00:00-05:00" -> "07:00". */
internal fun etiquetaHora(iso: String?): String = iso?.substringAfter('T', "")?.take(5).orEmpty()

