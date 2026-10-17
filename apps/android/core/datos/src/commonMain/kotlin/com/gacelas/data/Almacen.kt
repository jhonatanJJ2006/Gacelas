package com.gacelas.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** JSON compartido para guardar objetos en DataStore. */
val jsonGacelas = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** Lee y escribe un valor serializable en una clave de DataStore. */
class ValorJson<T>(
    private val store: DataStore<Preferences>,
    nombre: String,
    private val serializer: KSerializer<T>,
    private val porDefecto: T,
) {
    private val clave = stringPreferencesKey(nombre)

    val flujo: Flow<T> = store.data.map { prefs ->
        prefs[clave]?.let { runCatching { jsonGacelas.decodeFromString(serializer, it) }.getOrNull() } ?: porDefecto
    }

    suspend fun actualizar(transformar: (T) -> T) {
        store.edit { prefs ->
            val actual = prefs[clave]?.let { runCatching { jsonGacelas.decodeFromString(serializer, it) }.getOrNull() } ?: porDefecto
            prefs[clave] = jsonGacelas.encodeToString(serializer, transformar(actual))
        }
    }
}
