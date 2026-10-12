package com.gacelas.core.sensores

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBestForNavigation
import platform.CoreLocation.kCLDistanceFilterNone
import platform.CoreLocation.CLActivityTypeFitness
import platform.CoreMotion.CMAttitudeReferenceFrameXTrueNorthZVertical
import platform.CoreMotion.CMAttitudeReferenceFrameXMagneticNorthZVertical
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSBundle
import platform.Foundation.NSTimer
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObject

actual fun fuenteUbicacionNativa(): FuenteUbicacion? = UbicacionIos()
actual fun fuenteRumboNativa(): FuenteRumbo? = RumboIos()

/** Solo se pide segundo plano si el Info.plist declara UIBackgroundModes=location (si no, iOS cierra la app). */
private fun fondoDeclarado(): Boolean =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("UIBackgroundModes") as? List<*>)?.contains("location") == true

/** CoreLocation con BestForNavigation; entrega como maximo una lectura por intervalo (1 s). */
@OptIn(ExperimentalForeignApi::class)
class UbicacionIos : FuenteUbicacion {
    private val gestor = CLLocationManager()
    private var alLeer: ((LecturaGps) -> Unit)? = null
    private var intervaloMs = INTERVALO_GPS_MS
    private var ultimaMs = 0L
    private var fondo = false
    private var llamadas = 0
    private var sondeos = 0
    private var temporizador: NSTimer? = null
    private var ultimaClave: String? = null
    private var error: String? = null
    override val diagnostico: String
        get() = "permiso=${gestor.authorizationStatus} llamadas=$llamadas sondeos=$sondeos" + (error?.let { " error=$it" } ?: "")

    override fun iniciar(intervaloMs: Long, enSegundoPlano: Boolean, alLeer: (LecturaGps) -> Unit) {
        this.alLeer = alLeer; this.intervaloMs = intervaloMs; fondo = enSegundoPlano
        gestor.delegate = delegado
        gestor.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        gestor.distanceFilter = kCLDistanceFilterNone
        gestor.activityType = CLActivityTypeFitness
        gestor.pausesLocationUpdatesAutomatically = false
        if (enSegundoPlano && fondoDeclarado()) {
            gestor.allowsBackgroundLocationUpdates = true
            gestor.showsBackgroundLocationIndicator = true
        }
        when (gestor.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined ->
                if (enSegundoPlano) gestor.requestAlwaysAuthorization() else gestor.requestWhenInUseAuthorization()
            else -> {}
        }
        gestor.startUpdatingLocation()
        // Respaldo: cada intervalo se lee gestor.location por si CoreLocation no llama al delegado
        // (pasa en el simulador con ubicaciones inyectadas). Se ignoran lecturas repetidas.
        temporizador?.invalidate()
        temporizador = NSTimer.scheduledTimerWithTimeInterval(intervaloMs / 1000.0, repeats = true) { _ ->
            sondeos++
            gestor.location?.let { procesar(it, desdeSondeo = true) }
        }
    }

    override fun detener() {
        temporizador?.invalidate(); temporizador = null
        gestor.stopUpdatingLocation()
        if (fondoDeclarado()) gestor.allowsBackgroundLocationUpdates = false
        alLeer = null
    }

    // Kotlin/Native no permite mezclar supertipos Kotlin y Objective-C: el delegado es un objeto aparte.
    private val delegado = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) = autorizacion(manager)
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) = recibir(didUpdateLocations)
        override fun locationManager(manager: CLLocationManager, didFailWithError: platform.Foundation.NSError) {
            error = "${didFailWithError.domain}:${didFailWithError.code}"
        }
    }

    private fun autorizacion(manager: CLLocationManager) {
        val s = manager.authorizationStatus
        if (s == kCLAuthorizationStatusAuthorizedWhenInUse || s == kCLAuthorizationStatusAuthorizedAlways) manager.startUpdatingLocation()
    }

    private fun recibir(didUpdateLocations: List<*>) {
        llamadas++
        (didUpdateLocations.lastOrNull() as? CLLocation)?.let { procesar(it, desdeSondeo = false) }
    }

    private fun procesar(l: CLLocation, desdeSondeo: Boolean) {
        if (l.horizontalAccuracy < 0) return
        val ms = (l.timestamp.timeIntervalSince1970 * 1000).toLong()
        val clave = l.coordinate.useContents { "$latitude,$longitude" } + "@$ms"
        if (clave == ultimaClave) return
        if (desdeSondeo && com.gacelas.shared.relojMonotonoMs() - ultimaMs < intervaloMs / 2) return
        // Se limita con el reloj monotono: las ubicaciones simuladas pueden repetir el timestamp.
        val ahora = com.gacelas.shared.relojMonotonoMs()
        if (ahora - ultimaMs < intervaloMs - 100) return
        ultimaMs = ahora
        ultimaClave = clave
        val (lat, lng) = l.coordinate.useContents { latitude to longitude }
        alLeer?.invoke(
            LecturaGps(
                lat = lat, lng = lng, precisionM = l.horizontalAccuracy.toFloat(), tiempoMs = ms,
                altitudM = if (l.verticalAccuracy >= 0) l.altitude else null,
                velocidadMs = if (l.speed >= 0) l.speed else null,
                cursoGrados = if (l.course >= 0) l.course.toFloat() else null,
            ),
        )
    }
}

/**
 * Rumbo del borde superior del telefono: CLHeading (iOS ya compensa la inclinacion) y, si no hay
 * magnetometro de heading, CoreMotion deviceMotion. El simulador no tiene ninguno: el rumbo sale
 * entonces del curso GPS con Rumbo.combinar.
 */
@OptIn(ExperimentalForeignApi::class)
class RumboIos : FuenteRumbo {
    private val movimiento = CMMotionManager()
    private val gestor = CLLocationManager()
    private var alCambiar: ((Float) -> Unit)? = null

    override fun iniciar(alCambiar: (Float) -> Unit) {
        this.alCambiar = alCambiar
        if (CLLocationManager.headingAvailable()) {
            gestor.delegate = delegado
            gestor.headingFilter = 1.0
            gestor.startUpdatingHeading()
            return
        }
        val marcos = CMMotionManager.availableAttitudeReferenceFrames()
        val marco = when {
            marcos and CMAttitudeReferenceFrameXTrueNorthZVertical != 0uL -> CMAttitudeReferenceFrameXTrueNorthZVertical
            marcos and CMAttitudeReferenceFrameXMagneticNorthZVertical != 0uL -> CMAttitudeReferenceFrameXMagneticNorthZVertical
            else -> return
        }
        if (!movimiento.deviceMotionAvailable) return
        movimiento.deviceMotionUpdateInterval = 0.1
        movimiento.startDeviceMotionUpdatesUsingReferenceFrame(marco, NSOperationQueue.mainQueue) { m, _ ->
            // Marco de referencia: X = norte, Z = arriba, Y = oeste. Con yaw 0 el borde superior (Y del
            // dispositivo) mira al oeste (270°); yaw positivo es giro antihorario → rumbo = 270 - yaw.
            m?.attitude?.yaw?.let { alCambiar(Rumbo.normalizar((270.0 - it * 180.0 / kotlin.math.PI).toFloat())) }
        }
    }

    override fun detener() {
        movimiento.stopDeviceMotionUpdates()
        gestor.stopUpdatingHeading()
        alCambiar = null
    }

    private val delegado = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) = rumbo(didUpdateHeading)
    }

    private fun rumbo(didUpdateHeading: CLHeading) {
        val g = if (didUpdateHeading.trueHeading >= 0) didUpdateHeading.trueHeading else didUpdateHeading.magneticHeading
        alCambiar?.invoke(g.toFloat())
    }
}
