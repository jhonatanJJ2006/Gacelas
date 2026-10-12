package com.gacelas.data.eventos

import com.gacelas.shared.ahoraMs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.data.mock.Evento
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer

/** Eventos creados por el usuario e inscripciones. Hoy local; preparado para Supabase. */
interface EventosRepository {
    val creados: Flow<List<Evento>>
    val inscritos: Flow<Set<String>>
    suspend fun crear(evento: Evento)
    suspend fun alternarInscripcion(id: String)
}

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

@Serializable
enum class PestanaEventos(val etiqueta: String) { Proximos("Próximos"), MisEventos("Mis eventos"), Pasados("Pasados") }

/**
 * Si el evento ya empezo, con la hora de ahora. `pasado` venia fijo (datos de ejemplo) o calculado al
 * descargar, asi que el evento Jipiro del sabado 11 de octubre seguia en "Proximos". inicioMs es un
 * instante absoluto (America/Guayaquil = UTC-5 en el servidor), no depende de la zona del telefono.
 */
fun Evento.yaPaso(ahoraMs: Long = ahoraMs()): Boolean = inicioMs?.let { it < ahoraMs } ?: pasado

fun filtrarEventos(eventos: List<Evento>, pestana: PestanaEventos, inscritos: Set<String>, ahoraMs: Long = ahoraMs()): List<Evento> = when (pestana) {
    PestanaEventos.Proximos -> eventos.filter { !it.yaPaso(ahoraMs) }
    PestanaEventos.MisEventos -> eventos.filter { it.id in inscritos || it.creadoPorUsuario }
    PestanaEventos.Pasados -> eventos.filter { it.yaPaso(ahoraMs) }
}
