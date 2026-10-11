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
)

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

    /** URL publica del manifiesto a partir de SUPABASE_URL; null sin servidor. */
    fun urlManifiesto(supabaseUrl: String): String? =
        supabaseUrl.trimEnd('/').takeIf { it.startsWith("https://") }?.let { "$it/storage/v1/object/public/app-releases/android/latest.json" }

    /** versionCode a partir de versionName (0.6.2 -> 602, 1.2.3 -> 10203); igual que build.gradle.kts. */
    fun codigoDe(versionName: String): Int {
        val p = versionName.split('.', '-').take(3).map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
        return p[0] * 10000 + p[1] * 100 + p[2]
    }
}

/** Revisa como maximo una vez al dia (o siempre con [forzar]) y recuerda "Mas tarde" por version. */
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
        if (url != null && (forzar || ReglasActualizacion.tocaRevisar(c, ahora()))) {
            runCatching { jsonGacelas.decodeFromString(InfoVersion.serializer(), descargar(url)) }.onSuccess { info ->
                c = c.copy(revisadoMs = ahora(), info = info)
                cache.actualizar { it.copy(revisadoMs = ahora(), info = info) }
            }
        }
        return ReglasActualizacion.aviso(versionActual, c.info, c.pospuesta)
    }

    suspend fun posponer(versionCode: Int) = cache.actualizar { it.copy(pospuesta = versionCode) }
}

private suspend fun descargarTexto(url: String): String = withContext(Dispatchers.IO) {
    val con = URL(url).openConnection() as HttpURLConnection
    try {
        con.connectTimeout = 8000; con.readTimeout = 8000
        con.setRequestProperty("Cache-Control", "no-cache")
        if (con.responseCode != 200) error("HTTP ${con.responseCode}")
        con.inputStream.bufferedReader().use { it.readText() }
    } finally { con.disconnect() }
}
