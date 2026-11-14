package com.gacelas.rutas.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.texto
import com.gacelas.data.sync.BorradoSync
import com.gacelas.data.sync.CambioClave
import com.gacelas.data.sync.ConsultaPagina
import com.gacelas.data.sync.bajarTodo
import com.gacelas.data.sync.borradosDe
import com.gacelas.data.sync.fusionarClaves
import com.gacelas.data.sync.marcaMs
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
    private val offline: com.gacelas.rutas.infrastructure.RutasOffline? = null,
    private val alEncolar: () -> Unit = {},
) : RutasRepository {
    private val valor = ValorJson(store, "rutas_guardadas", SetSerializer(String.serializer()), emptySet())
    override val guardadas: Flow<Set<String>> = valor.flujo

    /** Sin [desde], reemplaza el conjunto. Con marca, solo altas y [borrados]. */
    suspend fun descargar(desde: String? = null, borrados: List<BorradoSync> = emptyList()): Boolean {
        val uid = userId() ?: return false
        val filas = runCatching {
            bajarTodo(backend::selectPagina, ConsultaPagina("saved_routes", mapOf("user_id" to uid), marcaExclusiva = desde, columnaClave = "route_id"))
        }.getOrElse { return false }
        val altas = filas.mapNotNull { f -> f.texto("route_id")?.let { CambioClave(it, marcaMs(f)) } }
        valor.actualizar { fusionarClaves(it, altas, if (desde == null) emptyList() else borradosDe(borrados, "saved_routes"), desde == null) }
        return true
    }

    override suspend fun alternarGuardada(id: String) {
        val estaba = id in valor.flujo.first()
        valor.actualizar { if (estaba) it - id else it + id }
        val uid = userId() ?: return
        runCatching {
            if (estaba) backend.delete("saved_routes", mapOf("user_id" to uid, "route_id" to id))
            else backend.insert("saved_routes", listOf(buildJsonObject { put("user_id", uid); put("route_id", id) }))
        }.onFailure {
            offline?.let { it.encolar(com.gacelas.rutas.infrastructure.OperacionRuta(if (estaba) "quitar" else "guardar", id)); alEncolar() }
        }
    }
}
