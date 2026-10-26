package com.gacelas.legal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gacelas.data.remoto.Backend
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Historial de consentimientos: se guarda en el dispositivo y, con servidor, en la tabla
 * consents (solo insercion; RLS: cada usuario ve y escribe lo suyo).
 */
class ConsentimientosRepository(
    private val store: DataStore<Preferences>,
    private val backend: Backend? = null,
    private val uid: suspend () -> String? = { null },
    private val reloj: () -> Long = { com.gacelas.shared.ahoraMs() },
) {
    private val clave = stringPreferencesKey("consentimientos")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun historial(): List<Consentimiento> =
        store.data.first()[clave]?.let { runCatching { json.decodeFromString<List<Consentimiento>>(it) }.getOrNull() }.orEmpty()

    suspend fun registrar(tipo: TipoConsentimiento, otorgado: Boolean) {
        val c = Consentimiento(tipo.clave, VersionesLegales.de(tipo), otorgado, reloj())
        val nuevo = historial() + c
        store.edit { it[clave] = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Consentimiento.serializer()), nuevo) }
        val id = uid() ?: return
        runCatching {
            backend?.insert("consents", listOf(JsonObject(mapOf(
                "user_id" to JsonPrimitive(id), "kind" to JsonPrimitive(c.tipo),
                "version" to JsonPrimitive(c.version), "granted" to JsonPrimitive(c.otorgado),
            ))))
        }
    }

    suspend fun otorgado(tipo: TipoConsentimiento) = otorgado(historial(), tipo)

    /** Borra la cuenta y sus datos en el servidor (funcion eliminar_mi_cuenta). */
    suspend fun eliminarCuenta(): Boolean {
        val b = backend ?: return false
        val ok = runCatching { b.rpc("eliminar_mi_cuenta", JsonObject(emptyMap())); true }.getOrDefault(false)
        if (ok) store.edit { it.remove(clave) }
        return ok
    }
}
