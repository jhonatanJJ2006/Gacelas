package com.gacelas.data.rutas

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.data.mock.Ruta
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer

/** Rutas guardadas por el usuario. Hoy local; preparado para Supabase. */
interface RutasRepository {
    val guardadas: Flow<Set<String>>
    suspend fun alternarGuardada(id: String)
}

class LocalRutasRepository(store: DataStore<Preferences>) : RutasRepository {
    private val valor = ValorJson(store, "rutas_guardadas", SetSerializer(String.serializer()), emptySet())
    override val guardadas: Flow<Set<String>> = valor.flujo
    override suspend fun alternarGuardada(id: String) = valor.actualizar { if (id in it) it - id else it + id }
}

enum class PestanaRutas(val etiqueta: String) { Explorar("Explorar"), MisRutas("Mis Rutas"), Guardadas("Guardadas") }

/** Filtra rutas segun pestana, guardadas y texto de busqueda. */
fun filtrarRutas(rutas: List<Ruta>, pestana: PestanaRutas, guardadas: Set<String>, usuarioId: String, consulta: String = ""): List<Ruta> {
    val base = when (pestana) {
        PestanaRutas.Explorar -> rutas
        PestanaRutas.MisRutas -> rutas.filter { it.autorId == usuarioId || it.propia }
        PestanaRutas.Guardadas -> rutas.filter { it.id in guardadas }
    }
    val q = consulta.trim().lowercase()
    return if (q.isEmpty()) base else base.filter { it.nombre.lowercase().contains(q) || it.descripcion.lowercase().contains(q) }
}
