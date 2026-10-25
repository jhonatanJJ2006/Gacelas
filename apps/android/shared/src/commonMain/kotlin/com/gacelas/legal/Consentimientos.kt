package com.gacelas.legal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gacelas.data.remoto.Backend
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Tipos de consentimiento (columna consents.kind en Supabase). */
enum class TipoConsentimiento(val clave: String) {
    Terminos("terminos"), Privacidad("privacidad"), GpsCarrera("gps_carrera"), UbicacionVisible("ubicacion_visible"),
}

@Serializable
data class Consentimiento(val tipo: String, val version: String, val otorgado: Boolean, val fechaMs: Long)

/** Versiones vigentes de los documentos legales; si cambian se vuelven a pedir. */
object VersionesLegales {
    const val TERMINOS = "2026-10-25"
    const val PRIVACIDAD = "2026-10-25"
    const val UBICACION = "2026-10-25"

    fun de(t: TipoConsentimiento) = when (t) {
        TipoConsentimiento.Terminos -> TERMINOS
        TipoConsentimiento.Privacidad -> PRIVACIDAD
        else -> UBICACION
    }
}

/** Ultimo estado de cada tipo segun el historial (el mas reciente manda). */
fun vigente(historial: List<Consentimiento>, tipo: TipoConsentimiento): Consentimiento? =
    historial.filter { it.tipo == tipo.clave }.maxByOrNull { it.fechaMs }

/** True si hay que volver a pedir los terminos (nunca aceptados o version distinta). */
fun requiereAceptarTerminos(historial: List<Consentimiento>): Boolean {
    val t = vigente(historial, TipoConsentimiento.Terminos)
    return t == null || !t.otorgado || t.version != VersionesLegales.TERMINOS
}

fun otorgado(historial: List<Consentimiento>, tipo: TipoConsentimiento) = vigente(historial, tipo)?.otorgado == true

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
