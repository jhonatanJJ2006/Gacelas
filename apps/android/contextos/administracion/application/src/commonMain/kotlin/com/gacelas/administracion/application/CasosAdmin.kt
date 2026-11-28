package com.gacelas.administracion.application

import com.gacelas.administracion.domain.AccesoAdmin
import com.gacelas.administracion.domain.AccionModeracion
import com.gacelas.administracion.domain.AdminRepositorio
import com.gacelas.administracion.domain.CLAVE_VERSION_MINIMA
import com.gacelas.administracion.domain.EventoAdmin
import com.gacelas.administracion.domain.ReporteAdmin
import com.gacelas.administracion.domain.versionValida
import com.gacelas.administracion.domain.CLAVE_SLA
import com.gacelas.administracion.domain.RespuestaRapida
import com.gacelas.administracion.domain.SlaHoras
import com.gacelas.administracion.domain.respuestaValida
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonPrimitive

/**
 * Pregunta al servidor el rol de la sesion. Falla cerrado: ante cualquier error no hay acceso.
 * No guarda nada en disco; se recalcula al entrar a la app o al iniciar sesion.
 */
class ConsultarAccesoAdmin(private val repo: AdminRepositorio) {
    suspend operator fun invoke(): AccesoAdmin = try {
        AccesoAdmin(repo.rolActual())
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        AccesoAdmin.Ninguno
    }
}

/** Aplica una accion de moderacion con la nota recortada. "Solo nota" exige texto. */
class ModerarReporte(private val repo: AdminRepositorio) {
    suspend operator fun invoke(r: ReporteAdmin, accion: AccionModeracion, nota: String) {
        val limpia = nota.trim()
        require(accion != AccionModeracion.Nota || limpia.isNotEmpty()) { "Escribe una nota" }
        repo.moderar(r.id, accion, limpia)
    }
}

/** Guarda la version minima de Android si tiene el formato 1.2.3. */
class GuardarVersionMinima(private val repo: AdminRepositorio) {
    suspend operator fun invoke(version: String) {
        require(versionValida(version)) { "Usa el formato 1.2.3" }
        repo.guardarAjuste(CLAVE_VERSION_MINIMA, JsonPrimitive(version.trim()))
    }
}

/** Crea o edita un evento; exige titulo. */
class GuardarEvento(private val repo: AdminRepositorio) {
    suspend operator fun invoke(e: EventoAdmin) {
        require(e.titulo.isNotBlank()) { "El evento necesita un título" }
        require(e.distanciaKm >= 0) { "La distancia no puede ser negativa" }
        repo.guardarEvento(e.copy(titulo = e.titulo.trim()))
    }
}

/** Guarda las horas objetivo por prioridad (entre 1 y 720 h; la alta no puede superar a la media ni esta a la baja). */
class GuardarSla(private val repo: AdminRepositorio) {
    suspend operator fun invoke(sla: SlaHoras) {
        require(sla.valido) { "Usa entre 1 y 720 horas" }
        require(sla.alta <= sla.media && sla.media <= sla.baja) { "La prioridad alta debe tener el plazo más corto" }
        repo.guardarAjuste(CLAVE_SLA, sla.json())
    }
}

/** Crea o edita una respuesta rapida con titulo de hasta 60 y texto de hasta 500 caracteres. */
class GuardarRespuestaRapida(private val repo: AdminRepositorio) {
    suspend operator fun invoke(r: RespuestaRapida) {
        respuestaValida(r.titulo, r.texto)?.let { throw IllegalArgumentException(it) }
        repo.guardarRespuestaRapida(r.copy(titulo = r.titulo.trim(), texto = r.texto.trim()))
    }
}
