package com.gacelas.actividad.domain.repositorio

import com.gacelas.actividad.domain.modelo.Carrera
import kotlinx.coroutines.flow.Flow

/** Historial de carreras (puerto del dominio). Lo implementan LocalCarrerasRepository y SupabaseCarrerasRepository. */
interface CarrerasRepository {
    val carreras: Flow<List<Carrera>>
    suspend fun guardar(carrera: Carrera)
    suspend fun eliminar(id: String)
}
