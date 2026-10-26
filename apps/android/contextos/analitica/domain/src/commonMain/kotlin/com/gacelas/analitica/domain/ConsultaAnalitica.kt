package com.gacelas.analitica.domain

import com.gacelas.actividad.domain.modelo.Carrera
import kotlinx.coroutines.flow.Flow

/** Puerto de lectura de Analitica: carreras del corredor e inicios de rutas para las favoritas. */
interface ConsultaAnalitica {
    val carreras: Flow<List<Carrera>>
    fun iniciosRutas(): List<InicioRuta>
}
