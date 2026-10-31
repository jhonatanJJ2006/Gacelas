package com.gacelas.administracion.infrastructure

import com.gacelas.administracion.domain.AccionModeracion
import com.gacelas.administracion.domain.AccionReporte
import com.gacelas.administracion.domain.AdminRepositorio
import com.gacelas.administracion.domain.AjusteApp
import com.gacelas.administracion.domain.CLAVE_VERSION_MINIMA
import com.gacelas.administracion.domain.ClubAdmin
import com.gacelas.administracion.domain.EstadisticasAdmin
import com.gacelas.administracion.domain.EstadoReporte
import com.gacelas.administracion.domain.EventoAdmin
import com.gacelas.administracion.domain.MetricasAdmin
import com.gacelas.administracion.domain.ReporteAdmin
import com.gacelas.administracion.domain.RolAdmin
import com.gacelas.administracion.domain.RutaAdmin
import com.gacelas.administracion.domain.UsuarioAdmin
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Panel en memoria con datos de ejemplo (los mismos que fuenteDemo de la web). Sirve para el modo
 * sin servidor, las pruebas y las capturas. [rol] simula la respuesta de is_admin/es_moderador.
 */
class AdminRepositorioMemoria(var rol: RolAdmin? = RolAdmin.Admin) : AdminRepositorio {
    var fallar: Boolean = false
    val usuarios = mutableListOf(
        UsuarioAdmin("u0", "andre.mont", "Andrea Montaño", "2026-10-20T10:00:00Z", null, null, 36, 214.0),
        UsuarioAdmin("u1", "kevin.jaramillo", "Kevin Jaramillo", "2026-10-17T10:00:00Z", null, null, 11, 66.0),
        UsuarioAdmin("u2", "dani.cueva", "Daniela Cueva", "2026-10-14T10:00:00Z", null, RolAdmin.Moderador, 69, 412.0),
        UsuarioAdmin("u3", "luiso.trail", "Luis Ordóñez", "2026-10-11T10:00:00Z", "2026-10-28T09:00:00Z", null, 170, 1020.0),
        UsuarioAdmin("u4", "jhonatan", "Jhonatan Jara", "2026-10-08T10:00:00Z", null, RolAdmin.Admin, 147, 880.0),
    )
    val reportes = mutableListOf(
        ReporteAdmin("r1", "post", "p1", "spam", "Publica enlaces de apuestas en cada carrera.", EstadoReporte.Pendiente,
            "2026-10-30T18:20:00Z", "Andrea Montaño", "Carrera matutina en Jipiro", 3, emptyList()),
        ReporteAdmin("r2", "usuario", "u9", "suplantacion", "Usa la foto y el nombre de otro corredor.", EstadoReporte.EnRevision,
            "2026-10-29T12:05:00Z", "Kevin Jaramillo", "@runner.falso", 1,
            listOf(AccionReporte("en_revision", "Lo reviso hoy", "2026-10-29T15:00:00Z", "Daniela Cueva"))),
        ReporteAdmin("r3", "comentario", "c4", "ofensivo", "", EstadoReporte.Pendiente,
            "2026-10-28T08:40:00Z", "", "Comentario en 10K Ciudad de Loja", 1, emptyList()),
        ReporteAdmin("r4", "evento", "e7", "falso", "Cobra inscripción y no existe.", EstadoReporte.Resuelto,
            "2026-10-25T20:10:00Z", "Luis Ordóñez", "Maratón Express Loja", 2,
            listOf(AccionReporte("ocultar_contenido", "Evento falso", "2026-10-26T09:00:00Z", "Jhonatan Jara"))),
    )
    val eventos = mutableListOf(
        EventoAdmin("e1", "10K Ciudad de Loja", "Carrera urbana por el centro histórico.", "2026-11-15T07:00:00-05:00", "Parque Central", 10.0, true),
        EventoAdmin("e2", "Trail Podocarpus", "Salida grupal de montaña.", "2026-11-29T06:30:00-05:00", "Cajanuma", 18.0, false),
    )
    val rutas = mutableListOf(RutaAdmin("r1", "Parque Jipiro", 3.2, "Fácil"), RutaAdmin("r2", "Mirador Las Pavas", 7.8, "Difícil"))
    val clubes = mutableListOf(ClubAdmin("c1", "Club Gacelas Loja", "Club principal."), ClubAdmin("c2", "Runners UNL", "Universitarios."))
    val ajustes = mutableListOf(
        AjusteApp("mantenimiento", JsonPrimitive(false)),
        AjusteApp("registro_abierto", JsonPrimitive(true)),
        AjusteApp(CLAVE_VERSION_MINIMA, JsonPrimitive("1.0.0")),
    )
    val moderaciones = mutableListOf<Triple<String, AccionModeracion, String>>()

    private fun revisar() {
        if (fallar) throw IllegalStateException("Sin conexión con el servidor")
    }

    private fun soloAdmin() {
        revisar()
        if (rol != RolAdmin.Admin) throw IllegalStateException("solo administradores")
    }

    override suspend fun rolActual(): RolAdmin? { revisar(); return rol }

    override suspend fun metricas(): MetricasAdmin {
        soloAdmin()
        return MetricasAdmin(2418, 126, 1094, 6120.4, reportes.count { it.estado.abierto }.toLong(), 6, clubes.size.toLong(), rutas.size.toLong())
    }

    override suspend fun usuarios(): List<UsuarioAdmin> { soloAdmin(); return usuarios.toList() }

    override suspend fun suspender(id: String, suspender: Boolean) {
        soloAdmin()
        val i = usuarios.indexOfFirst { it.id == id }
        if (i >= 0) usuarios[i] = usuarios[i].copy(suspendidoEn = if (suspender) "2026-10-31T12:00:00Z" else null)
    }

    override suspend fun cambiarRol(id: String, rol: RolAdmin?) {
        soloAdmin()
        val i = usuarios.indexOfFirst { it.id == id }
        if (i >= 0) usuarios[i] = usuarios[i].copy(rol = rol)
    }

    override suspend fun reportes(estado: EstadoReporte?, tipo: String?): List<ReporteAdmin> {
        revisar()
        if (rol == null) throw IllegalStateException("solo moderadores")
        return reportes.filter { (estado == null || it.estado == estado) && (tipo == null || it.tipo == tipo) }
    }

    override suspend fun moderar(reporteId: String, accion: AccionModeracion, nota: String) {
        revisar()
        if (rol == null) throw IllegalStateException("solo moderadores")
        moderaciones += Triple(reporteId, accion, nota)
        val i = reportes.indexOfFirst { it.id == reporteId }
        if (i < 0) return
        val nuevo = when (accion) {
            AccionModeracion.EnRevision -> EstadoReporte.EnRevision
            AccionModeracion.Resuelto, AccionModeracion.OcultarContenido, AccionModeracion.SuspenderUsuario -> EstadoReporte.Resuelto
            AccionModeracion.Descartado -> EstadoReporte.Descartado
            AccionModeracion.Reabierto -> EstadoReporte.Pendiente
            AccionModeracion.Nota -> reportes[i].estado
        }
        reportes[i] = reportes[i].copy(estado = nuevo, acciones = reportes[i].acciones + AccionReporte(accion.clave, nota, "2026-10-31T12:00:00Z", "Tú"))
    }

    override suspend fun estadisticas(desde: String, hasta: String): EstadisticasAdmin {
        soloAdmin()
        return EstadisticasAdmin(desde, hasta, mapOf(
            "usuarios_nuevos" to 126.0, "usuarios_activos" to 312.0, "carreras" to 1094.0, "km" to 6120.4,
            "eventos" to 4.0, "inscripciones" to 88.0, "reportes" to 7.0, "usuarios_total" to 2418.0,
        ))
    }

    override suspend fun eventos(): List<EventoAdmin> { soloAdmin(); return eventos.toList() }

    override suspend fun guardarEvento(e: EventoAdmin) {
        soloAdmin()
        val i = eventos.indexOfFirst { it.id == e.id && e.id != null }
        if (i >= 0) eventos[i] = e else eventos.add(0, e.copy(id = "e${eventos.size + 1}"))
    }

    override suspend fun rutas(): List<RutaAdmin> { soloAdmin(); return rutas.toList() }
    override suspend fun borrarRuta(id: String) { soloAdmin(); rutas.removeAll { it.id == id } }
    override suspend fun clubes(): List<ClubAdmin> { soloAdmin(); return clubes.toList() }
    override suspend fun borrarClub(id: String) { soloAdmin(); clubes.removeAll { it.id == id } }
    override suspend fun ajustes(): List<AjusteApp> { soloAdmin(); return ajustes.toList() }

    override suspend fun guardarAjuste(clave: String, valor: JsonElement) {
        soloAdmin()
        val i = ajustes.indexOfFirst { it.clave == clave }
        if (i >= 0) ajustes[i] = AjusteApp(clave, valor) else ajustes += AjusteApp(clave, valor)
    }
}
