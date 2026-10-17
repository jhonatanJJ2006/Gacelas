package com.gacelas.cuenta.domain.repositorio

import com.gacelas.cuenta.domain.modelo.Ajustes
import kotlinx.coroutines.flow.Flow

interface AjustesRepository {
    val ajustes: Flow<Ajustes>
    suspend fun actualizar(transformar: (Ajustes) -> Ajustes)
}

