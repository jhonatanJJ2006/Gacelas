package com.gacelas.comunidad.domain.repositorio

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.shared.ahoraMs
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import com.gacelas.comunidad.domain.modelo.*
import com.gacelas.comunidad.domain.reglas.*

/** Eventos creados por el usuario e inscripciones. Hoy local; preparado para Supabase. */
interface EventosRepository {
    val creados: Flow<List<Evento>>
    val inscritos: Flow<Set<String>>
    suspend fun crear(evento: Evento)
    suspend fun alternarInscripcion(id: String)
}

/** Notificaciones de la cuenta (servidor). La implementa data.NotificacionesRepository. */
interface FuenteNotificaciones {
    val lista: kotlinx.coroutines.flow.StateFlow<List<Notificacion>>
    suspend fun refrescar()
    suspend fun marcarLeidas(ids: Collection<String>)
}

/** Clubes: lista con los mios. La implementa data.ClubesRepository. */
interface FuenteClubes {
    suspend fun listar(): Pair<List<Club>, Set<String>>
}
