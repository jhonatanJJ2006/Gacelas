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
import platform.CoreLocation.kCLLocationAccuracyBest
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

/** CoreLocation con precision Best; entrega como maximo una lectura por intervalo (1 s). */
@OptIn(ExperimentalForeignApi::class)
class UbicacionIos : FuenteUbicacion {
    private companion object { const val SIN_LECTURAS_MS = 1_500L }

    private var gestor = CLLocationManager()
    private var alLeer: ((LecturaGps) -> Unit)? = null
    private var intervaloMs = INTERVALO_GPS_MS
    private var ultimaMs = 0L
    private var fondo = false
    private var ultimoDelegadoMs = 0L
    private var temporizador: NSTimer? = null
    private var ultimaClave: String? = null

    override fun iniciar(intervaloMs: Long, enSegundoPlano: Boolean, alLeer: (LecturaGps) -> Unit) {
        this.alLeer = alLeer; this.intervaloMs = intervaloMs; fondo = enSegundoPlano
        configurar()
        when (gestor.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined ->
                if (enSegundoPlano) gestor.requestAlwaysAuthorization() else gestor.requestWhenInUseAuthorization()
            else -> {}
        }
        gestor.startUpdatingLocation()
        // Vigilante: si CoreLocation deja de entregar (locationd del simulador de CI solo envia la
        // primera posicion a cada gestor), se crea un gestor nuevo. En un iPhone llegan lecturas cada
        // segundo y esto no se activa.
        ultimoDelegadoMs = com.gacelas.shared.relojMonotonoMs()
        temporizador?.invalidate()
        temporizador = NSTimer.scheduledTimerWithTimeInterval(intervaloMs / 1000.0, repeats = true) { _ ->
            if (com.gacelas.shared.relojMonotonoMs() - ultimoDelegadoMs > SIN_LECTURAS_MS) {
                ultimoDelegadoMs = com.gacelas.shared.relojMonotonoMs()
                gestor.stopUpdatingLocation()
                gestor.delegate = null
                gestor = CLLocationManager()
                configurar()
                gestor.startUpdatingLocation()
            }
        }
    }

    private fun configurar() {
        gestor.delegate = delegado
        gestor.desiredAccuracy = kCLLocationAccuracyBest
        gestor.distanceFilter = kCLDistanceFilterNone
        gestor.activityType = CLActivityTypeFitness
        gestor.pausesLocationUpdatesAutomatically = false
        if (fondo && fondoDeclarado()) {
            gestor.allowsBackgroundLocationUpdates = true
            gestor.showsBackgroundLocationIndicator = true
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
        // kCLErrorLocationUnknown es transitorio: CoreLocation sigue intentando.
        override fun locationManager(manager: CLLocationManager, didFailWithError: platform.Foundation.NSError) = Unit
    }

    private fun autorizacion(manager: CLLocationManager) {
        val s = manager.authorizationStatus
        if (s == kCLAuthorizationStatusAuthorizedWhenInUse || s == kCLAuthorizationStatusAuthorizedAlways) manager.startUpdatingLocation()
    }

    private fun recibir(didUpdateLocations: List<*>) {
        ultimoDelegadoMs = com.gacelas.shared.relojMonotonoMs()
        (didUpdateLocations.lastOrNull() as? CLLocation)?.let { procesar(it) }
    }

    private fun procesar(l: CLLocation) {
        if (l.horizontalAccuracy < 0) return
        val ms = (l.timestamp.timeIntervalSince1970 * 1000).toLong()
        val clave = l.coordinate.useContents { "$latitude,$longitude" } + "@$ms"
        if (clave == ultimaClave) return
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
