package com.gacelas.administracion.domain

import kotlinx.serialization.json.JsonElement

/**
 * Puerto del panel de administracion. La implementacion usa las mismas funciones y tablas que la
 * web (apps/web/src/contexts/admin/infrastructure/fuentes.ts); el servidor vuelve a comprobar el rol.
 */
interface AdminRepositorio {
    /** Rol de la sesion actual, preguntado al servidor cada vez (sin cache). */
    suspend fun rolActual(): RolAdmin?
    suspend fun metricas(): MetricasAdmin
    suspend fun usuarios(): List<UsuarioAdmin>
    suspend fun suspender(id: String, suspender: Boolean)
    suspend fun cambiarRol(id: String, rol: RolAdmin?)
    suspend fun reportes(estado: EstadoReporte?, tipo: String?): List<ReporteAdmin>
    suspend fun moderar(reporteId: String, accion: AccionModeracion, nota: String)
    suspend fun estadisticas(desde: String, hasta: String): EstadisticasAdmin
    suspend fun eventos(): List<EventoAdmin>
    suspend fun guardarEvento(e: EventoAdmin)
    suspend fun rutas(): List<RutaAdmin>
    suspend fun borrarRuta(id: String)
    suspend fun clubes(): List<ClubAdmin>
    suspend fun borrarClub(id: String)
    suspend fun ajustes(): List<AjusteApp>
    suspend fun guardarAjuste(clave: String, valor: JsonElement)

    // Migracion 050 (docs/ADMIN.md, "Partes que faltaban").
    suspend fun cambiarPrioridad(reporteId: String, prioridad: Prioridad)
    suspend fun respuestasRapidas(): List<RespuestaRapida>
    suspend fun guardarRespuestaRapida(r: RespuestaRapida)
    suspend fun borrarRespuestaRapida(id: String)

    /** Corredores visibles, incidentes sin verificar y alertas SOS de las ultimas 24 h. */
    suspend fun escenarioVivo(): EscenarioVivo
    suspend fun versionesPublicadas(): List<VersionPublicada>
    suspend fun versionesEnUso(): List<VersionEnUso>
    suspend fun importacionesEcu(): List<ImportacionEcu>
}
