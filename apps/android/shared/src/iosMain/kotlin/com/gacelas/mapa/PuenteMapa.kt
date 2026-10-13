package com.gacelas.mapa

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.stringWithContentsOfFile
import platform.UIKit.UIView

/** Avisos de la vista nativa hacia Compose. */
interface OyenteMapa {
    /** El usuario movio la camara con un gesto (deja de seguir). */
    fun gesto()
    /** La camara se movio: rumbo actual en grados. */
    fun camara(rumbo: Double)
    /** Toco un circulo con id (salidas de rutas). */
    fun tocoCirculo(id: String)
}

/**
 * MapLibre Native iOS lo implementa en Swift (iosApp/MapaMapLibre.swift, paquete SPM) y se registra al
 * arrancar con [registrarMapaIos]. Kotlin no enlaza MapLibre: solo pasa GeoJSON y la camara.
 */
interface PuenteMapa {
    /** Vista MLNMapView con el estilo Gacelas. [estiloJson] (sin red) sustituye al archivo del bundle. */
    fun crear(oscuro: Boolean, lat: Double, lng: Double, zoom: Double, estiloJson: String?, oyente: OyenteMapa): UIView
    fun trazos(vista: UIView, geojson: String)
    fun circulos(vista: UIView, geojson: String)
    /** Camara: [arribaPx] es el padding superior (foco en el tercio inferior); [lineal] para el avance continuo. */
    fun camara(vista: UIView, lat: Double, lng: Double, zoom: Double, rumbo: Double, inclinacion: Double, arribaPx: Double, ms: Int, lineal: Boolean)
    fun encuadrar(vista: UIView, sur: Double, oeste: Double, norte: Double, este: Double, paddingDp: Double)
    /** Rumbo 0 (y sin inclinacion si [plano]). */
    fun norte(vista: UIView, plano: Boolean)
    fun zoom(vista: UIView, delta: Double)
}

/** Descarga offline de Loja (MLNOfflineStorage). Swift informa el estado con [MapaOfflineIos.informar]. */
interface PuenteMapaOffline {
    /** Al abrir la app: cache ambiental, reanuda o descarga sola si hay internet. */
    fun asegurar()
    fun descargar()
    fun eliminar()
    fun conRed(): Boolean
}

object MapaIos {
    var puente: PuenteMapa? = null
        internal set
    var offline: PuenteMapaOffline? = null
        internal set
    fun disponible() = puente != null
}

/** Llamado desde Swift antes de crear MainViewController. */
fun registrarMapaIos(puente: PuenteMapa, offline: PuenteMapaOffline) {
    MapaIos.puente = puente
    MapaIos.offline = offline
    offline.asegurar()
}

/** Estado de la descarga (lo actualiza Swift) y teselas guardadas (igual que MapaOffline en Android). */
object MapaOfflineIos {
    private val _estado = MutableStateFlow<EstadoMapaOffline>(EstadoMapaOffline.SinDescargar)
    val estado: StateFlow<EstadoMapaOffline> = _estado
    private const val CLAVE_TESELAS = "teselas_loja"

    /** tipo: 0 sin descargar, 1 descargando, 2 lista, 3 error. */
    fun informar(tipo: Int, porcentaje: Int, megas: Double, mensaje: String?) {
        _estado.value = when (tipo) {
            1 -> EstadoMapaOffline.Descargando(porcentaje.coerceIn(0, 99), megas)
            2 -> EstadoMapaOffline.Lista(megas)
            3 -> EstadoMapaOffline.Error(mensaje ?: "Error")
            else -> EstadoMapaOffline.SinDescargar
        }
    }

    fun estadoActual(): Int = when (_estado.value) {
        is EstadoMapaOffline.Descargando -> 1
        is EstadoMapaOffline.Lista -> 2
        is EstadoMapaOffline.Error -> 3
        else -> 0
    }

    /** Guarda la version exacta de teselas a partir de la TileJSON de OpenFreeMap (Swift la descarga). */
    fun guardarTileJson(tilejson: String) {
        ReglasTeselas.plantillaTeselas(tilejson)?.let { NSUserDefaults.standardUserDefaults.setObject(it, CLAVE_TESELAS) }
    }

    fun teselasGuardadas(): String? = NSUserDefaults.standardUserDefaults.stringForKey(CLAVE_TESELAS)

    /** Estilo Gacelas del bundle apuntando a las teselas fijas de la region descargada. */
    fun estiloSinRed(oscuro: Boolean): String? {
        val teselas = teselasGuardadas() ?: return null
        val ruta = NSBundle.mainBundle.pathForResource(if (oscuro) "gacelas-oscuro" else "gacelas-claro", "json") ?: return null
        @Suppress("CAST_NEVER_SUCCEEDS")
        val json = NSString.stringWithContentsOfFile(ruta, NSUTF8StringEncoding, null) ?: return null
        return ReglasTeselas.fijarTeselas(json, teselas)
    }
}
