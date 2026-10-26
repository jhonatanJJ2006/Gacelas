package com.gacelas.identidad.domain.repositorio

import com.gacelas.identidad.domain.modelo.Ajustes
import kotlinx.coroutines.flow.Flow

interface AjustesRepository {
    val ajustes: Flow<Ajustes>
    suspend fun actualizar(transformar: (Ajustes) -> Ajustes)
}

