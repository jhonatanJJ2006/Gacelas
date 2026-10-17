package com.gacelas.rutas.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.rutas.domain.repositorio.RutasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer

class LocalRutasRepository(store: DataStore<Preferences>) : RutasRepository {
    private val valor = ValorJson(store, "rutas_guardadas", SetSerializer(String.serializer()), emptySet())
    override val guardadas: Flow<Set<String>> = valor.flujo
    override suspend fun alternarGuardada(id: String) = valor.actualizar { if (id in it) it - id else it + id }
}
