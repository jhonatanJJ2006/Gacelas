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

    /** Decision vigente de un tipo: true, false o null si nunca se pregunto. */
    suspend fun decision(tipo: TipoConsentimiento): Boolean? = vigente(historial(), tipo)?.otorgado

    /**
     * Trae los consentimientos del servidor y los une al historial local. Asi lo que ya se acepto en
     * otro dispositivo, en la web o antes de reinstalar la app no se vuelve a preguntar.
     */
    suspend fun sincronizar(): Boolean {
        val id = uid() ?: return false
        val b = backend ?: return false
        val filas = runCatching { b.select("consents", mapOf("user_id" to id), "created_at") }.getOrNull() ?: return false
        val remotos = filas.mapNotNull { f ->
            val tipo = (f["kind"] as? JsonPrimitive)?.contentOrNullSeguro() ?: return@mapNotNull null
            val version = (f["version"] as? JsonPrimitive)?.contentOrNullSeguro() ?: ""
            val ok = (f["granted"] as? JsonPrimitive)?.contentOrNullSeguro() == "true"
            val fecha = (f["created_at"] as? JsonPrimitive)?.contentOrNullSeguro()
                ?.let { runCatching { kotlinx.datetime.Instant.parse(it).toEpochMilliseconds() }.getOrNull() } ?: 0L
            Consentimiento(tipo, version, ok, fecha)
        }
        val unidos = unirHistoriales(historial(), remotos)
        store.edit { it[clave] = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Consentimiento.serializer()), unidos) }
        return true
    }

    /** Borra la cuenta y sus datos en el servidor (funcion eliminar_mi_cuenta). */
    suspend fun eliminarCuenta(): Boolean {
        val b = backend ?: return false
        val ok = runCatching { b.rpc("eliminar_mi_cuenta", JsonObject(emptyMap())); true }.getOrDefault(false)
        if (ok) store.edit { it.remove(clave) }
        return ok
    }
}

private fun JsonPrimitive.contentOrNullSeguro(): String? = if (this is kotlinx.serialization.json.JsonNull) null else content

/** Une dos historiales sin duplicar (mismo tipo y decision con menos de 5 s de diferencia). */
fun unirHistoriales(local: List<Consentimiento>, remoto: List<Consentimiento>): List<Consentimiento> {
    val todos = (local + remoto).sortedBy { it.fechaMs }
    val salida = mutableListOf<Consentimiento>()
    for (c in todos) {
        if (salida.none { it.tipo == c.tipo && it.otorgado == c.otorgado && kotlin.math.abs(it.fechaMs - c.fechaMs) < 5_000 }) salida += c
    }
    return salida
}
