package com.gacelas.comunidad.data

import com.gacelas.carreras.domain.modelo.PuntoGps
import com.gacelas.comunidad.domain.modelo.ComentarioFeed
import com.gacelas.comunidad.domain.modelo.Publicacion
import com.gacelas.comunidad.domain.reglas.haceCuanto
import com.gacelas.comunidad.domain.repositorio.FeedRepository
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.ErrorRemoto
import com.gacelas.data.remoto.FechaIso
import com.gacelas.shared.ahoraMs
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put

private fun JsonObject.txt(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
private fun JsonObject.num(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }
private fun JsonObject.bool(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.booleanOrNull ?: false

/** Fila de la RPC feed_comunidad a publicacion del feed (mismo contrato que la web). */
fun publicacionDesdeFilaFeed(f: JsonObject, nombrePropio: String? = null, ahora: Long = ahoraMs(), url: (String) -> String = { it }): Publicacion {
    val inicio = f.txt("start_time")?.let { runCatching { FechaIso.leer(it) }.getOrNull() } ?: 0L
    val km = (f.num("distance_meters") ?: 0.0) / 1000.0
    val seg = f.num("duration_seconds")?.toInt() ?: 0
    val propia = f.bool("propia")
    val puntos = (f["public_points"] as? JsonArray).orEmpty().mapNotNull { e ->
        (e as? JsonObject)?.let { o ->
            val la = o.num("lat"); val lo = o.num("lng")
            if (la == null || lo == null) null else PuntoGps(la, lo, (o.num("tiempoMs") ?: o.num("t") ?: 0.0).toLong())
        }
    }
    return Publicacion(
        id = f.txt("id").orEmpty(),
        autor = if (propia && nombrePropio != null) nombrePropio else f.txt("autor")?.ifBlank { null } ?: "Corredor",
        propia = propia,
        indice = (f.txt("user_id")?.hashCode() ?: 0).let { if (it < 0) -it else it } % 6,
        cuando = haceCuanto(inicio.takeIf { it > 0 }, ahora),
        titulo = f.txt("title")?.ifBlank { null } ?: "Carrera",
        distanciaKm = km,
        segundos = seg,
        ritmoSegKm = if (km > 0.01) (seg / km).toInt() else null,
        likesBase = f.num("likes_count")?.toInt() ?: 0,
        comentariosBase = f.num("comments_count")?.toInt() ?: 0,
        puntos = puntos,
        autorId = if (propia) null else f.txt("user_id"),
        remota = true,
        meGusta = f.bool("me_gusta"),
        inicioMs = inicio,
        clienteId = f.txt("client_id"),
        desnivelM = f.num("elevation_gain")?.toInt() ?: 0,
        imagenes = (f["images"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.let(url) },
        compartidos = f.num("reposts_count")?.toInt() ?: 0,
        compartida = f.bool("reposteada"),
        autorKm = f.num("autor_km"),
        ultimoComentario = (f["ultimo_comentario"] as? JsonObject)?.let { c ->
            c.txt("texto")?.let { (c.txt("autor")?.ifBlank { null } ?: "Corredor") to it }
        },
    )
}

fun comentarioDesdeFila(f: JsonObject) = ComentarioFeed(
    f.txt("id").orEmpty(), f.txt("user_id").orEmpty(), f.txt("autor")?.ifBlank { null } ?: "Corredor", f.txt("body").orEmpty(),
)

class SupabaseFeedRepository(
    private val backend: Backend,
    private val uid: suspend () -> String?,
    private val nombrePropio: suspend () -> String? = { null },
) : FeedRepository {
    private fun filas(e: JsonElement) = (e as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }

    override suspend fun publicaciones(): List<Publicacion>? {
        uid() ?: return null
        val r = runCatching { backend.rpc("feed_comunidad", buildJsonObject { put("p_limite", 40) }) }.getOrNull() ?: return null
        val yo = nombrePropio()
        val url = { ruta: String ->
            if (ruta.startsWith("http")) ruta else backend.urlPublica(ruta.substringBefore('/'), ruta.substringAfter('/'))
        }
        return filas(r).map { publicacionDesdeFilaFeed(it, yo, url = url) }
    }

    override suspend fun alternarMeGusta(id: String, activo: Boolean) {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para aplaudir")
        if (activo) {
            runCatching { backend.insert("post_likes", listOf(buildJsonObject { put("user_id", yo); put("post_id", id) })) }
                .onFailure { if (it.message?.contains("duplicate", true) != true) throw it }
        } else {
            backend.delete("post_likes", mapOf("user_id" to yo, "post_id" to id))
        }
    }

    override suspend fun comentarios(id: String): List<ComentarioFeed> =
        runCatching { filas(backend.rpc("comentarios_publicacion", buildJsonObject { put("p_post", id) })).map(::comentarioDesdeFila) }
            .getOrDefault(emptyList())

    override suspend fun comentar(id: String, texto: String) {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para comentar")
        val t = texto.trim()
        if (t.isEmpty()) return
        backend.insert("post_comments", listOf(buildJsonObject { put("user_id", yo); put("post_id", id); put("body", t.take(1000)) }))
    }

    override suspend fun siguiendo(): Set<String> {
        val yo = uid() ?: return emptySet()
        return runCatching { backend.select("follows", mapOf("follower_id" to yo)).mapNotNull { it.txt("following_id") }.toSet() }
            .getOrDefault(emptySet())
    }

    override suspend fun alternarCompartida(id: String, activo: Boolean) {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para compartir")
        if (activo) {
            runCatching { backend.insert("post_reposts", listOf(buildJsonObject { put("user_id", yo); put("activity_id", id) })) }
                .onFailure { if (it.message?.contains("duplicate", true) != true) throw it }
        } else {
            backend.delete("post_reposts", mapOf("user_id" to yo, "activity_id" to id))
        }
    }

    override suspend fun subirFoto(id: String, bytes: ByteArray, tipo: String, posicion: Int): String {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para subir fotos")
        val ext = if ("png" in tipo) "png" else if ("webp" in tipo) "webp" else "jpg"
        val ruta = "$yo/$id/${ahoraMs()}-$posicion.$ext"
        val url = backend.subirArchivo("post-images", ruta, bytes, tipo)
        backend.insert("post_images", listOf(buildJsonObject {
            put("activity_id", id); put("user_id", yo); put("path", "post-images/$ruta"); put("position", posicion)
        }))
        return url
    }

    override suspend fun seguidores(): Int {
        val yo = uid() ?: return 0
        return runCatching { backend.select("follows", mapOf("following_id" to yo)).size }.getOrDefault(0)
    }

    override suspend fun alternarSeguir(id: String): Set<String> {
        val yo = uid() ?: return emptySet()
        if (id in siguiendo()) backend.delete("follows", mapOf("follower_id" to yo, "following_id" to id))
        else backend.insert("follows", listOf(buildJsonObject { put("follower_id", yo); put("following_id", id) }))
        return siguiendo()
    }
}
