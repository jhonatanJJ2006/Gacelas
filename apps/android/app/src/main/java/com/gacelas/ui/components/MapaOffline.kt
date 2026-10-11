package com.gacelas.ui.components

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val zoomMax: Double = 14.0,
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
            val propias = lista.filter { it.clave().startsWith("${r.id}|") }
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

    fun descargar(context: Context, r: RegionMapa = LOJA) {
        val estilos = listOf(Mapas.ESTILO_OSCURO, Mapas.ESTILO_CLARO)
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
                    offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
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
                            // Errores sueltos de una tesela no detienen la descarga; se informan al final.
                            if (error.reason == OfflineRegionError.REASON_CONNECTION) _estado.value = EstadoDescarga.Error("Sin conexión: la descarga sigue al volver la red")
                        }
                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            _estado.value = EstadoDescarga.Error("La zona supera el límite de $limit teselas")
                        }
                    })
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
