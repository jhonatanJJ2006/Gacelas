package com.gacelas.rutas.domain.repositorio

import kotlinx.coroutines.flow.Flow

/** Rutas guardadas por el usuario. Hoy local; preparado para Supabase. */
interface RutasRepository {
    val guardadas: Flow<Set<String>>
    suspend fun alternarGuardada(id: String)
}
