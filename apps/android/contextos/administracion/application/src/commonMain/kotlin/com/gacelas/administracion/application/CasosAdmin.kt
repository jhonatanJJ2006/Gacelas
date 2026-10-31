package com.gacelas.administracion.application

import com.gacelas.administracion.domain.AccesoAdmin
import com.gacelas.administracion.domain.AccionModeracion
import com.gacelas.administracion.domain.AdminRepositorio
import com.gacelas.administracion.domain.CLAVE_VERSION_MINIMA
import com.gacelas.administracion.domain.EventoAdmin
import com.gacelas.administracion.domain.ReporteAdmin
import com.gacelas.administracion.domain.versionValida
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
