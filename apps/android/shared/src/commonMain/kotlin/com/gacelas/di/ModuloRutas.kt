package com.gacelas.di

import com.gacelas.data.remoto.Backend
import com.gacelas.rutas.data.RutasOffline
import com.gacelas.rutas.data.SupabaseExplorarRepository
import com.gacelas.rutas.domain.repositorio.ExplorarRepository

/** Ensamblado del bounded context rutas (exploracion social). */
internal object ModuloRutas {
    fun explorar(
        backend: Backend,
        offline: RutasOffline,
        alEncolar: () -> Unit,
        uid: suspend () -> String?,
    ): ExplorarRepository = SupabaseExplorarRepository(backend, offline, alEncolar, uid)
}
