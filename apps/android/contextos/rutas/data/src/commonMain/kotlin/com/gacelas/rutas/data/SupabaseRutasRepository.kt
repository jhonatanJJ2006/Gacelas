package com.gacelas.rutas.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.texto
import com.gacelas.rutas.domain.repositorio.RutasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Rutas guardadas sincronizadas con Supabase (saved_routes). */
class SupabaseRutasRepository(
    private val backend: Backend,
    store: DataStore<Preferences>,
    private val userId: suspend () -> String?,
    private val offline: com.gacelas.rutas.data.RutasOffline? = null,
    private val alEncolar: () -> Unit = {},
) : RutasRepository {
    private val valor = ValorJson(store, "rutas_guardadas", SetSerializer(String.serializer()), emptySet())
    override val guardadas: Flow<Set<String>> = valor.flujo

    suspend fun descargar() {
        val uid = userId() ?: return
        val filas = runCatching { backend.select("saved_routes", mapOf("user_id" to uid)) }.getOrNull() ?: return
        valor.actualizar { filas.mapNotNull { it.texto("route_id") }.toSet() }
    }

    override suspend fun alternarGuardada(id: String) {
        val estaba = id in valor.flujo.first()
        valor.actualizar { if (estaba) it - id else it + id }
        val uid = userId() ?: return
        runCatching {
            if (estaba) backend.delete("saved_routes", mapOf("user_id" to uid, "route_id" to id))
            else backend.insert("saved_routes", listOf(buildJsonObject { put("user_id", uid); put("route_id", id) }))
        }.onFailure {
            offline?.let { it.encolar(com.gacelas.rutas.data.OperacionRuta(if (estaba) "quitar" else "guardar", id)); alEncolar() }
        }
    }
}
