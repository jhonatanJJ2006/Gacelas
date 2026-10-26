package com.gacelas.actividad.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.repositorio.CarrerasRepository
import com.gacelas.data.ValorJson
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer

/** Historial en el telefono (DataStore). */
class LocalCarrerasRepository(store: DataStore<Preferences>) : CarrerasRepository {
    private val valor = ValorJson(store, "carreras", ListSerializer(Carrera.serializer()), emptyList())
    override val carreras: Flow<List<Carrera>> = valor.flujo
    override suspend fun guardar(carrera: Carrera) = valor.actualizar { lista -> listOf(carrera) + lista.filterNot { it.id == carrera.id } }
    override suspend fun eliminar(id: String) = valor.actualizar { lista -> lista.filterNot { it.id == id } }
}
