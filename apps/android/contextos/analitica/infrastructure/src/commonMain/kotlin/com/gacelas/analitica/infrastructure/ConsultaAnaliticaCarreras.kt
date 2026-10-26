package com.gacelas.analitica.infrastructure

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.repositorio.CarrerasRepository
import com.gacelas.analitica.domain.ConsultaAnalitica
import com.gacelas.analitica.domain.InicioRuta
import kotlinx.coroutines.flow.Flow

/**
 * Adaptador de lectura sobre el historial de carreras (local o Supabase, segun el repositorio que
 * se inyecte). Los inicios de rutas los aporta quien cablea, para no depender del contexto rutas.
 */
class ConsultaAnaliticaCarreras(
    private val repo: CarrerasRepository,
    private val inicios: () -> List<InicioRuta> = { emptyList() },
) : ConsultaAnalitica {
    override val carreras: Flow<List<Carrera>> get() = repo.carreras
    override fun iniciosRutas(): List<InicioRuta> = inicios()
}
