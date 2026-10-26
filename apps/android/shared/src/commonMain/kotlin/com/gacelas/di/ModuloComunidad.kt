package com.gacelas.di

import com.gacelas.comunidad.infrastructure.LocalEventosRepository
import com.gacelas.comunidad.infrastructure.SupabaseClubesRepository
import com.gacelas.comunidad.infrastructure.SupabaseEnVivoRepository
import com.gacelas.comunidad.infrastructure.SupabaseEventosRepository
import com.gacelas.comunidad.infrastructure.SupabaseNotificacionesRepository
import com.gacelas.comunidad.domain.repositorio.ClubesRepository
import com.gacelas.comunidad.domain.repositorio.EnVivoRepository
import com.gacelas.comunidad.domain.repositorio.EventosRepository
import com.gacelas.comunidad.domain.repositorio.NotificacionesRepository
import com.gacelas.data.remoto.Backend
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/** Ensamblado del bounded context comunidad (repositorios Supabase o local). */
internal object ModuloComunidad {
    fun eventosLocal(store: DataStore<Preferences>): EventosRepository = LocalEventosRepository(store)

    fun eventosRemoto(backend: Backend, store: DataStore<Preferences>, uid: suspend () -> String?): SupabaseEventosRepository =
        SupabaseEventosRepository(backend, store, uid)

    fun clubes(backend: Backend, uid: suspend () -> String?): ClubesRepository = SupabaseClubesRepository(backend, uid)

    fun enVivo(backend: Backend, uid: suspend () -> String?): EnVivoRepository = SupabaseEnVivoRepository(backend, uid)

    fun notificaciones(backend: Backend, uid: suspend () -> String?): NotificacionesRepository =
        SupabaseNotificacionesRepository(backend, uid)
}
