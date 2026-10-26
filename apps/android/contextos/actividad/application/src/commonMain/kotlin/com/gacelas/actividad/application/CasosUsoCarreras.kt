package com.gacelas.actividad.application

import com.gacelas.actividad.domain.EstadoCarrera
import com.gacelas.actividad.domain.SesionCarrera
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.actividad.domain.reglas.Estadisticas
import com.gacelas.actividad.domain.reglas.Racha
import com.gacelas.actividad.domain.reglas.Rachas
import com.gacelas.actividad.domain.reglas.ResumenSemana
import com.gacelas.actividad.domain.repositorio.CarrerasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/* Casos de uso del contexto carreras: una accion de negocio cada uno, sin UI ni plataforma. */

class IniciarCarrera(private val sesion: SesionCarrera) {
    operator fun invoke() = sesion.iniciar()
}

class AlternarPausa(private val sesion: SesionCarrera) {
    operator fun invoke() = sesion.alternarPausa()
}

class RegistrarPunto(private val sesion: SesionCarrera) {
    operator fun invoke(p: PuntoGps) = sesion.registrarPunto(p)
}

class MarcarVuelta(private val sesion: SesionCarrera) {
    operator fun invoke() = sesion.vuelta()
}

class FinalizarCarrera(private val sesion: SesionCarrera) {
    operator fun invoke(): EstadoCarrera = sesion.finalizar()
}

/** Convierte la carrera terminada en [Carrera] (titulo por hora si falta) y la guarda. */
class GuardarCarrera(private val sesion: SesionCarrera, private val repo: CarrerasRepository) {
    suspend operator fun invoke(titulo: String, nota: String, publica: Boolean): Carrera {
        val c = sesion.aCarrera(titulo, nota, publica)
        repo.guardar(c)
        sesion.reiniciar()
        return c
    }
}

class EliminarCarrera(private val repo: CarrerasRepository) {
    suspend operator fun invoke(id: String) = repo.eliminar(id)
}

class ObservarCarreras(private val repo: CarrerasRepository) {
    operator fun invoke(): Flow<List<Carrera>> = repo.carreras
}

class ObtenerCarrera(private val repo: CarrerasRepository) {
    operator fun invoke(id: String): Flow<Carrera?> = repo.carreras.map { l -> l.firstOrNull { it.id == id } }
}

class CalcularResumenSemana {
    operator fun invoke(carreras: List<Carrera>, ahoraMs: Long): ResumenSemana = Estadisticas.semana(carreras, ahoraMs)
}

class CalcularRacha {
    operator fun invoke(carreras: List<Carrera>, ahoraMs: Long, metaSemanal: Int = 3): Racha = Rachas.calcular(carreras, ahoraMs, metaSemanal)
}
