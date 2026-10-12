package com.gacelas.ui.components

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

/** Zona descargable del mapa (limites en grados y zooms de teselas vectoriales). */
data class RegionMapa(
    val id: String,
    val nombre: String,
    val norte: Double,
    val este: Double,
    val sur: Double,
    val oeste: Double,
    val zoomMin: Double = 10.0,
    /** OpenFreeMap sirve teselas hasta z14; MapLibre las amplia para zooms mayores. */
    val zoomMax: Double = 16.0,
) {
    fun contiene(lat: Double, lng: Double) = lat in sur..norte && lng in oeste..este
}

sealed interface EstadoDescarga {
    data object SinDescargar : EstadoDescarga
    data class Descargando(val porcentaje: Int, val megas: Double) : EstadoDescarga
    data class Lista(val megas: Double) : EstadoDescarga
    data class Error(val mensaje: String) : EstadoDescarga
}

/**
 * Descarga de teselas OpenStreetMap (OpenFreeMap) para usar el mapa sin red con MapLibre.
 * Se baja la zona con los dos estilos Gacelas; las teselas son las mismas, asi que la segunda
 * descarga solo agrega el estilo. Las regiones quedan en la base de datos de MapLibre.
 */
object MapaOffline {
    /** Ciudad de Loja y alrededores (Jipiro, Zamora Huayco, Punzara, Carigan). */
    val LOJA = RegionMapa("loja", "Loja, Ecuador", norte = -3.92, este = -79.15, sur = -4.06, oeste = -79.28)

    /**
     * El descargador offline de MapLibre solo baja estilos por http(s): con asset://mapas/... se quedaba en 0%.
     * Este estilo publico de OpenFreeMap usa la misma fuente (tiles.openfreemap.org/planet) y las mismas
     * fuentes (Noto Sans) que los estilos Gacelas, y la cache de teselas es compartida por URL, asi que
     * el mapa Gacelas (oscuro o claro) las encuentra sin red.
     */
    const val ESTILO_DESCARGA = "https://tiles.openfreemap.org/styles/dark"

    private val _estado = MutableStateFlow<EstadoDescarga>(EstadoDescarga.SinDescargar)
    val estado: StateFlow<EstadoDescarga> = _estado

    private fun gestor(context: Context): OfflineManager {
        MapLibre.getInstance(context.applicationContext)
        return OfflineManager.getInstance(context.applicationContext)
    }

    private fun metadatos(r: RegionMapa, estilo: String) = "${r.id}|$estilo".toByteArray()
    private fun OfflineRegion.clave() = String(metadata)

    private fun regiones(context: Context, alListar: (List<OfflineRegion>) -> Unit) {
        gestor(context).listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) = alListar(offlineRegions?.toList().orEmpty())
            override fun onError(error: String) { _estado.value = EstadoDescarga.Error(error) }
        })
    }

    /** Lee si la zona ya esta descargada (y cuanto pesa). */
    fun consultar(context: Context, r: RegionMapa = LOJA) {
        regiones(context) { lista ->
            // Regiones de v0.6.0-0.6.2 con estilo asset:// nunca terminan: se borran.
            lista.filter { it.clave().startsWith("${r.id}|") && it.clave() != String(metadatos(r, ESTILO_DESCARGA)) }
                .forEach { vieja -> vieja.setDownloadState(OfflineRegion.STATE_INACTIVE); vieja.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                    override fun onDelete() = Unit
                    override fun onError(error: String) = Unit
                }) }
            val propias = lista.filter { it.clave() == String(metadatos(r, ESTILO_DESCARGA)) }
            if (propias.isEmpty()) { _estado.value = EstadoDescarga.SinDescargar; return@regiones }
            var bytes = 0L; var completas = 0
            propias.forEach { reg ->
                reg.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                    override fun onStatus(status: OfflineRegionStatus?) {
                        bytes += status?.completedResourceSize ?: 0L
                        if (status?.isComplete == true) completas++
                        if (completas == propias.size) _estado.value = EstadoDescarga.Lista(bytes / 1e6)
                        else if (_estado.value !is EstadoDescarga.Descargando) _estado.value = EstadoDescarga.SinDescargar
                    }
                    override fun onError(error: String?) = Unit
                })
            }
        }
    }

    /** Cache ambiental de MapLibre (zonas vistas con red), aparte de la region descargada. */
    const val CACHE_AMBIENTAL_BYTES = 150L * 1024 * 1024

    /**
     * Al abrir la app: amplia la cache ambiental, reanuda una descarga que quedo a medias y, si Loja
     * no esta descargada y hay wifi (red no medida), la descarga sola. Antes solo se bajaba a mano
     * desde Ajustes y una descarga interrumpida no se reanudaba nunca.
     */
    fun asegurar(context: Context, r: RegionMapa = LOJA, conDatos: Boolean = false) {
        val app = context.applicationContext
        runCatching { gestor(app).setMaximumAmbientCacheSize(CACHE_AMBIENTAL_BYTES, null) }
        regiones(app) { lista ->
            val propia = lista.firstOrNull { it.clave() == String(metadatos(r, ESTILO_DESCARGA)) }
            val wifi = conDatos || ReglasDescarga.redNoMedida(app)
            if (propia == null) {
                if (ReglasDescarga.descargarSola(existe = false, completa = false, wifi = wifi)) descargar(app, r) else consultar(app, r)
                return@regiones
            }
            propia.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                override fun onStatus(status: OfflineRegionStatus?) {
                    val completa = status?.isComplete == true
                    if (ReglasDescarga.descargarSola(existe = true, completa = completa, wifi = wifi)) reanudar(propia) else consultar(app, r)
                }
                override fun onError(error: String?) = consultar(app, r)
            })
        }
    }

    private fun reanudar(region: OfflineRegion) {
        if (teselasGuardadas(MapLibre.getApplicationContext()) == null) guardarTeselas(MapLibre.getApplicationContext())
        _estado.value = EstadoDescarga.Descargando(0, 0.0)
        region.setObserver(observador(region, 0, DoubleArray(1), DoubleArray(1), BooleanArray(1)))
        region.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

    private fun observador(offlineRegion: OfflineRegion, i: Int, progreso: DoubleArray, megas: DoubleArray, listas: BooleanArray) =
        object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                val req = status.requiredResourceCount.coerceAtLeast(1)
                progreso[i] = status.completedResourceCount.toDouble() / req
                megas[i] = status.completedResourceSize / 1e6
                if (status.isComplete) {
                    listas[i] = true
                    offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
                }
                _estado.value = if (listas.all { it }) EstadoDescarga.Lista(megas.sum())
                else EstadoDescarga.Descargando((progreso.average() * 100).toInt().coerceIn(0, 99), megas.sum())
            }
            override fun onError(error: OfflineRegionError) {
                if (error.reason == OfflineRegionError.REASON_CONNECTION) _estado.value = EstadoDescarga.Error("Sin conexión: la descarga sigue al volver la red")
            }
            override fun mapboxTileCountLimitExceeded(limit: Long) {
                _estado.value = EstadoDescarga.Error("La zona supera el límite de $limit teselas")
            }
        }

    /**
     * CAUSA del mapa vacio sin red: los estilos usan la TileJSON https://tiles.openfreemap.org/planet,
     * cuyas teselas llevan version (planet/20261004_113936_pt/{z}/{x}/{y}.pbf) y OpenFreeMap la cambia
     * cada semana. Al abrir el mapa con red despues de descargar, MapLibre refrescaba la TileJSON con la
     * version nueva y, sin red, pedia teselas que no estaban en la region. Se guarda la plantilla de la
     * version descargada y sin red el estilo se arma con ella (ver [estiloSinRed]).
     */
    private const val PREFS = "mapa_offline"
    private const val CLAVE_TESELAS = "teselas_loja"

    fun teselasGuardadas(context: Context): String? =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE_TESELAS, null)

    private fun guardarTeselas(context: Context) {
        Thread {
            runCatching {
                val con = java.net.URL(TILEJSON).openConnection() as java.net.HttpURLConnection
                con.connectTimeout = 8000; con.readTimeout = 8000
                val txt = con.inputStream.bufferedReader().use { it.readText() }
                ReglasDescarga.plantillaTeselas(txt)?.let {
                    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLAVE_TESELAS, it).apply()
                }
            }
        }.start()
    }

    const val TILEJSON = "https://tiles.openfreemap.org/planet"

    /** Estilo Gacelas (JSON del asset) apuntando a las teselas fijas de la region descargada. */
    fun estiloSinRed(context: Context, asset: String): String? {
        val teselas = teselasGuardadas(context) ?: return null
        val json = runCatching { context.assets.open(asset).bufferedReader().use { it.readText() } }.getOrNull() ?: return null
        return ReglasDescarga.fijarTeselas(json, teselas)
    }

    fun descargar(context: Context, r: RegionMapa = LOJA) {
        guardarTeselas(context)
        val estilos = listOf(ESTILO_DESCARGA)
        val densidad = context.resources.displayMetrics.density
        val limites = LatLngBounds.from(r.norte, r.este, r.sur, r.oeste)
        _estado.value = EstadoDescarga.Descargando(0, 0.0)
        val progreso = DoubleArray(estilos.size)
        val megas = DoubleArray(estilos.size)
        val listas = BooleanArray(estilos.size)
        estilos.forEachIndexed { i, estilo ->
            val def = OfflineTilePyramidRegionDefinition(estilo, limites, r.zoomMin, r.zoomMax, densidad)
            gestor(context).createOfflineRegion(def, metadatos(r, estilo), object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    offlineRegion.setObserver(observador(offlineRegion, i, progreso, megas, listas))
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                }
                override fun onError(error: String) { _estado.value = EstadoDescarga.Error(error) }
            })
        }
    }

    fun eliminar(context: Context, r: RegionMapa = LOJA) {
        regiones(context) { lista ->
            lista.filter { it.clave().startsWith("${r.id}|") }.forEach { reg ->
                reg.setDownloadState(OfflineRegion.STATE_INACTIVE)
                reg.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                    override fun onDelete() { _estado.value = EstadoDescarga.SinDescargar }
                    override fun onError(error: String) { _estado.value = EstadoDescarga.Error(error) }
                })
            }
        }
    }
}

/** Reglas puras de la descarga automatica (probadas en JVM). */
object ReglasDescarga {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /** Primera plantilla "tiles" de una TileJSON. */
    fun plantillaTeselas(tilejson: String): String? = runCatching {
        json.parseToJsonElement(tilejson).let { it as kotlinx.serialization.json.JsonObject }["tiles"]
            .let { it as kotlinx.serialization.json.JsonArray }.first().let { (it as kotlinx.serialization.json.JsonPrimitive).content }
    }.getOrNull()

    /** Cambia la fuente openmaptiles {url: TileJSON} por {tiles:[plantilla], maxzoom 14}. */
    fun fijarTeselas(estilo: String, plantilla: String): String? = runCatching {
        val e = json.parseToJsonElement(estilo) as kotlinx.serialization.json.JsonObject
        val fuentes = e["sources"] as kotlinx.serialization.json.JsonObject
        val nuevas = kotlinx.serialization.json.JsonObject(fuentes.mapValues { (k, v) ->
            if (k != "openmaptiles") v else kotlinx.serialization.json.buildJsonObject {
                (v as kotlinx.serialization.json.JsonObject).forEach { (kk, vv) -> if (kk != "url") put(kk, vv) }
                put("tiles", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive(plantilla))))
                put("minzoom", kotlinx.serialization.json.JsonPrimitive(0))
                put("maxzoom", kotlinx.serialization.json.JsonPrimitive(14))
            }
        })
        kotlinx.serialization.json.JsonObject(e + ("sources" to nuevas)).toString()
    }.getOrNull()

    /** Se descarga sola si falta o quedo incompleta, y solo con wifi (o red no medida). */
    fun descargarSola(existe: Boolean, completa: Boolean, wifi: Boolean): Boolean = wifi && (!existe || !completa)

    fun redNoMedida(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    fun conRed(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return true
        return cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}

/** Descarga (o reanuda) Loja en segundo plano en cuanto hay internet; reintenta con espera exponencial. */
class MapaOfflineWorker(context: Context, params: androidx.work.WorkerParameters) : androidx.work.CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { MapaOffline.asegurar(applicationContext, conDatos = true) }
        val fin = kotlinx.coroutines.withTimeoutOrNull(20 * 60_000L) {
            MapaOffline.estado.first { it is EstadoDescarga.Lista || it is EstadoDescarga.Error }
        }
        return if (fin is EstadoDescarga.Lista) Result.success() else Result.retry()
    }

    companion object {
        const val NOMBRE = "gacelas-mapa-offline-loja"
        fun programar(context: Context) {
            runCatching {
                val req = androidx.work.OneTimeWorkRequestBuilder<MapaOfflineWorker>()
                    .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                    .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE, androidx.work.ExistingWorkPolicy.KEEP, req)
            }
        }
    }
}
