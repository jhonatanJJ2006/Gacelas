package com.gacelas.social

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.carreras.domain.reglas.Estadisticas
import com.gacelas.data.remoto.Backend
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class Logro(val codigo: String, val titulo: String, val descripcion: String) {
    PrimeraCarrera("primera_carrera", "Primera carrera", "Grabaste tu primera carrera."),
    Km5("km_5", "5 km", "Corriste 5 km en una carrera."),
    Km10("km_10", "10 km", "Corriste 10 km en una carrera."),
    Racha7("racha_7", "Racha de 7 días", "Corriste 7 días seguidos."),
}

/** Reglas simples de logros a partir del historial de carreras. */
fun logrosCumplidos(carreras: List<Carrera>, ahoraMs: Long): List<Logro> = buildList {
    if (carreras.isNotEmpty()) add(Logro.PrimeraCarrera)
    val max = carreras.maxOfOrNull { it.distanciaKm } ?: 0.0
    if (max >= 5) add(Logro.Km5)
    if (max >= 10) add(Logro.Km10)
    if (Estadisticas.racha(carreras, ahoraMs) >= 7) add(Logro.Racha7)
}

val MOTIVOS_REPORTE = listOf(
    "spam" to "Spam o publicidad", "ofensivo" to "Contenido ofensivo", "acoso" to "Acoso o intimidación",
    "falso" to "Carrera falsa o trampa", "otro" to "Otro motivo",
)

/** Que se reporta: post usa post_reports (compatibilidad); el resto, content_reports (migracion 023). */
enum class TipoContenido(val clave: String, val titulo: String) {
    Post("post", "Reportar publicación"), Usuario("usuario", "Reportar corredor"),
    Comentario("comentario", "Reportar comentario"), Evento("evento", "Reportar evento"),
}

/** Motivos validos por tipo, igual que la web (features/social/moderacion.ts). */
fun motivosReporte(tipo: TipoContenido): List<Pair<String, String>> = when (tipo) {
    TipoContenido.Post -> MOTIVOS_REPORTE
    TipoContenido.Usuario -> listOf(
        "suplantacion" to "Suplantación de identidad", "acoso" to "Acoso o intimidación", "spam" to "Spam o cuenta falsa",
        "ofensivo" to "Perfil ofensivo", "otro" to "Otro motivo",
    )
    TipoContenido.Comentario -> listOf(
        "ofensivo" to "Contenido ofensivo", "acoso" to "Acoso o intimidación", "spam" to "Spam o publicidad", "otro" to "Otro motivo",
    )
    TipoContenido.Evento -> listOf(
        "peligroso" to "Ruta o evento peligroso", "falso" to "Evento falso o engañoso", "spam" to "Spam o publicidad",
        "ofensivo" to "Contenido ofensivo", "otro" to "Otro motivo",
    )
}

/** Logros desbloqueados (dispositivo y tabla user_achievements) y reportes (tabla post_reports). */
class SocialRepository(
    private val store: DataStore<Preferences>,
    private val backend: Backend? = null,
    private val uid: suspend () -> String? = { null },
    private val reloj: () -> Long = { com.gacelas.shared.ahoraMs() },
) {
    private val clave = stringPreferencesKey("logros")

    /** Codigo a fecha (ms) de cada logro desbloqueado. */
    suspend fun desbloqueados(): Map<String, Long> =
        store.data.first()[clave].orEmpty().split(';').mapNotNull { e ->
            val p = e.split('='); if (p.size == 2) p[0] to (p[1].toLongOrNull() ?: 0L) else null
        }.toMap()

    /**
     * Guarda los logros nuevos y devuelve los que se acaban de ganar. La primera vez que se
     * llama (sin registro previo) los guarda sin devolverlos, para no saturar con modales.
     */
    suspend fun desbloquearNuevos(carreras: List<Carrera>): List<Logro> {
        val previo = store.data.first()[clave]
        val ya = desbloqueados()
        val ahora = reloj()
        val nuevos = logrosCumplidos(carreras, ahora).filter { it.codigo !in ya }
        if (nuevos.isEmpty() && previo != null) return emptyList()
        val todo = ya + nuevos.associate { it.codigo to ahora }
        store.edit { it[clave] = todo.entries.joinToString(";") { (k, v) -> "$k=$v" } }
        val id = uid()
        if (id != null && nuevos.isNotEmpty()) runCatching {
            backend?.upsert("user_achievements", nuevos.map { l ->
                JsonObject(mapOf("user_id" to JsonPrimitive(id), "code" to JsonPrimitive(l.codigo)))
            }, "user_id,code")
        }
        return if (previo == null) emptyList() else nuevos
    }

    /** Envia el reporte; false sin servidor o sin sesion. */
    suspend fun reportar(postId: String, motivo: String, detalle: String, tipo: TipoContenido = TipoContenido.Post): Boolean {
        val b = backend ?: return false
        val id = uid() ?: return false
        return runCatching {
            if (tipo == TipoContenido.Post) b.insert("post_reports", listOf(JsonObject(mapOf(
                "post_id" to JsonPrimitive(postId), "reporter_id" to JsonPrimitive(id),
                "reason" to JsonPrimitive(motivo), "detail" to JsonPrimitive(detalle.trim()),
            ))))
            else b.insert("content_reports", listOf(JsonObject(mapOf(
                "target_type" to JsonPrimitive(tipo.clave), "target_id" to JsonPrimitive(postId), "reporter_id" to JsonPrimitive(id),
                "reason" to JsonPrimitive(motivo), "detail" to JsonPrimitive(detalle.trim()),
            ))))
            true
        }.getOrElse { it.message?.contains("23505") == true }
    }
}
