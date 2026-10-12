package com.gacelas.data.actualizacion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.data.jsonGacelas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manifiesto publico de la ultima version (Supabase Storage, bucket publico app-releases,
 * android/latest.json). El repo es privado: la app nunca lee GitHub ni lleva tokens.
 */
@Serializable
data class InfoVersion(
    val versionCode: Int,
    val versionName: String,
    val notes: String = "",
    val apkUrl: String,
    /** Versiones por debajo de esta deben actualizar (sin "Mas tarde"). */
    val minVersionCode: Int? = null,
    val sha256: String? = null,
    /** Novedades como lista (opcional, desde 0.6.6). Si falta se derivan de [notes]. */
    val items: List<String>? = null,
    /** Tamano del APK en bytes (opcional). Si falta, la app lo pide con HEAD. */
    val apkBytes: Long? = null,
) {
    /** Lista de novedades: [items] o, en manifiestos viejos, [notes] partido por lineas, ";" o frases. */
    fun novedades(): List<String> = items?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?: ReglasActualizacion.partirNotas(notes)
}

@Serializable
data class CacheActualizacion(val revisadoMs: Long = 0L, val info: InfoVersion? = null, val pospuesta: Int? = null)

sealed interface Aviso {
    data object Nada : Aviso
    data class Nueva(val info: InfoVersion, val obligatoria: Boolean) : Aviso
}

object ReglasActualizacion {
    const val UN_DIA_MS = 24 * 60 * 60 * 1000L

    fun aviso(actual: Int, info: InfoVersion?, pospuesta: Int? = null): Aviso {
        if (info == null || info.versionCode <= actual || !info.apkUrl.startsWith("https://")) return Aviso.Nada
        val obligatoria = info.minVersionCode != null && actual < info.minVersionCode
        if (!obligatoria && pospuesta != null && pospuesta >= info.versionCode) return Aviso.Nada
        return Aviso.Nueva(info, obligatoria)
    }

    fun tocaRevisar(cache: CacheActualizacion, ahoraMs: Long) = cache.revisadoMs == 0L || ahoraMs - cache.revisadoMs >= UN_DIA_MS || ahoraMs < cache.revisadoMs

    fun partirNotas(notes: String): List<String> {
        val t = notes.trim()
        if (t.isEmpty()) return emptyList()
        val partes = when {
            '\n' in t -> t.split('\n')
            ';' in t -> t.split(';')
            else -> t.split(Regex("(?<=[.!?])\\s+"))
        }
        val limpias = partes.map { it.trim().trimStart('-', '•', '*').trim().trimEnd('.') }.filter { it.isNotEmpty() }
        // Una sola frase larga con comas ("a, b y c") se lista por partes.
        val lista = if (limpias.size == 1 && limpias[0].length > 60 && ", " in limpias[0]) limpias[0].split(", ") else limpias
        return lista.map { it.trim().replaceFirstChar { c -> c.uppercase() } }.filter { it.isNotEmpty() }
    }

    /** "47 MB" a partir de bytes (MiB redondeados, igual que el resto de la app). */
    fun textoMb(bytes: Long): String = "${Math.round(bytes / 1048576.0)} MB"

    fun sinCache(url: String, t: Long): String = url + (if ('?' in url) "&" else "?") + "t=$t"

    /** URL publica del manifiesto a partir de SUPABASE_URL; null sin servidor. */
    fun urlManifiesto(supabaseUrl: String): String? =
        supabaseUrl.trimEnd('/').takeIf { it.startsWith("https://") }?.let { "$it/storage/v1/object/public/app-releases/android/latest.json" }

    /** versionCode a partir de versionName (0.6.2 -> 602, 1.2.3 -> 10203); igual que build.gradle.kts. */
    fun codigoDe(versionName: String): Int {
        val p = versionName.split('.', '-').take(3).map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
        return p[0] * 10000 + p[1] * 100 + p[2]
    }
}

/** Revisa en cada llamada (sin cache diaria; la cache solo cubre la falta de red) y recuerda "Mas tarde" solo en la sesion. */
class ActualizacionRepository(
    store: DataStore<Preferences>,
    private val urlManifiesto: String?,
    private val versionActual: Int,
    private val ahora: () -> Long = System::currentTimeMillis,
    private val descargar: suspend (String) -> String = ::descargarTexto,
) {
    private val cache = ValorJson(store, "actualizacion", CacheActualizacion.serializer(), CacheActualizacion())

    suspend fun revisar(forzar: Boolean = false): Aviso {
        var c = cache.flujo.first()
        val url = urlManifiesto
        if (url != null) {
            // Siempre se pide al servidor (al abrir y al volver a primer plano); la cache solo sirve sin red.
            runCatching { jsonGacelas.decodeFromString(InfoVersion.serializer(), descargar(url)) }.onSuccess { info ->
                c = c.copy(revisadoMs = ahora(), info = info)
                cache.actualizar { it.copy(revisadoMs = ahora(), info = info) }
            }
        }
        val aviso = ReglasActualizacion.aviso(versionActual, c.info, pospuestaSesion)
        runCatching { android.util.Log.i("Actualizacion", "actual=$versionActual servidor=${c.info?.versionCode} -> ${aviso.javaClass.simpleName}") }
        return aviso
    }

    /** "Mas tarde" solo vale para esta sesion (proceso): al volver a abrir la app se avisa otra vez. */
    @Volatile private var pospuestaSesion: Int? = null

    suspend fun posponer(versionCode: Int) { pospuestaSesion = versionCode }
}

private suspend fun descargarTexto(url: String): String = withContext(Dispatchers.IO) {
    // ?t= evita la cache del CDN de Storage (max-age=60) y de proxies.
    val con = URL(ReglasActualizacion.sinCache(url, System.currentTimeMillis())).openConnection() as HttpURLConnection
    try {
        con.connectTimeout = 8000; con.readTimeout = 8000
        con.useCaches = false
        con.setRequestProperty("Cache-Control", "no-cache")
        con.setRequestProperty("Pragma", "no-cache")
        if (con.responseCode != 200) error("HTTP ${con.responseCode}")
        con.inputStream.bufferedReader().use { it.readText() }
    } finally { con.disconnect() }
}
