package com.gacelas.cuenta.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.cuenta.domain.modelo.Ajustes
import com.gacelas.cuenta.domain.repositorio.AjustesRepository
import kotlinx.coroutines.flow.Flow

class LocalAjustesRepository(store: DataStore<Preferences>) : AjustesRepository {
    private val valor = ValorJson(store, "ajustes", Ajustes.serializer(), Ajustes())
    override val ajustes: Flow<Ajustes> = valor.flujo
    override suspend fun actualizar(transformar: (Ajustes) -> Ajustes) = valor.actualizar(transformar)
}
