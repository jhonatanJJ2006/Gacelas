package com.gacelas.comunidad.infrastructure

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
import com.gacelas.comunidad.domain.repositorio.*
import com.gacelas.data.remoto.*

class LocalEventosRepository(store: DataStore<Preferences>) : EventosRepository {
    private val creadosValor = ValorJson(store, "eventos_creados", ListSerializer(Evento.serializer()), emptyList())
    private val inscritosValor = ValorJson(store, "eventos_inscritos", SetSerializer(String.serializer()), emptySet())
    override val creados: Flow<List<Evento>> = creadosValor.flujo
    override val inscritos: Flow<Set<String>> = inscritosValor.flujo
    override suspend fun crear(evento: Evento) {
        creadosValor.actualizar { it + evento }
        inscritosValor.actualizar { it + evento.id }
    }
    override suspend fun alternarInscripcion(id: String) = inscritosValor.actualizar { if (id in it) it - id else it + id }
}
