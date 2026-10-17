package com.gacelas.core.sensores

import com.gacelas.carreras.domain.modelo.PuntoGps
import com.gacelas.carreras.domain.EstadoCarrera
import com.gacelas.carreras.domain.SesionCarrera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Lectura de GPS independiente de la plataforma. [cursoGrados] y [velocidadMs] son null si no son validos. */
data class LecturaGps(
    val lat: Double,
    val lng: Double,
    val precisionM: Float,
    val tiempoMs: Long,
    val altitudM: Double? = null,
    val velocidadMs: Double? = null,
    val cursoGrados: Float? = null,
) {
    fun aPunto() = PuntoGps(lat, lng, tiempoMs, precisionM, altitudM)
}

/** GPS de alta precision cada [intervaloMs]. En iOS: CoreLocation. */
interface FuenteUbicacion {
    fun iniciar(intervaloMs: Long = INTERVALO_GPS_MS, enSegundoPlano: Boolean = false, alLeer: (LecturaGps) -> Unit)
    fun detener()
}

/** Rumbo del telefono en grados (norte verdadero si se conoce, si no magnetico). En iOS: CLHeading/CoreMotion. */
interface FuenteRumbo {
    fun iniciar(alCambiar: (Float) -> Unit)
    fun detener()
}

/** Igual que LocationService.INTERVALO_MS en Android. */
const val INTERVALO_GPS_MS = 1000L

/**
 * Fuentes nativas de la plataforma. En Android devuelve null: la app sigue usando LocationService
 * (servicio en primer plano) y Brujula (SensorManager), que necesitan Context.
 */
expect fun fuenteUbicacionNativa(): FuenteUbicacion?
expect fun fuenteRumboNativa(): FuenteRumbo?

data class EstadoSensores(
    val lectura: LecturaGps? = null,
    val lecturas: Int = 0,
    val rumboSensor: Float? = null,
    /** Rumbo final: sensor quieto/lento, curso GPS corriendo ([Rumbo.VEL_SENSOR_MS]..[Rumbo.VEL_GPS_MS]). */
    val rumbo: Float? = null,
    val carrera: EstadoCarrera = EstadoCarrera(),
)

/**
 * Une GPS + rumbo con la logica compartida: cada lectura va a [SesionCarrera.registrarPunto] y el rumbo
 * se combina con [Rumbo.combinar] (mismos umbrales que Android).
 */
class SeguimientoSensores(
    private val ubicacion: FuenteUbicacion?,
    private val brujula: FuenteRumbo?,
    val tracker: SesionCarrera = SesionCarrera(),
) {
    private val _estado = MutableStateFlow(EstadoSensores())
    val estado: StateFlow<EstadoSensores> = _estado.asStateFlow()

    fun alLeer(l: LecturaGps) {
        tracker.registrarPunto(l.aPunto())
        _estado.update { it.copy(lectura = l, lecturas = it.lecturas + 1, rumbo = combinado(it.rumboSensor, l), carrera = tracker.estado.value) }
    }

    fun alRumbo(g: Float) {
        val n = Rumbo.normalizar(g)
        _estado.update { e ->
            val suave = e.rumboSensor?.let { Rumbo.suavizar(it, n, 0.25f) } ?: n
            e.copy(rumboSensor = suave, rumbo = combinado(suave, e.lectura))
        }
    }

    private fun combinado(sensor: Float?, l: LecturaGps?): Float? = Rumbo.combinar(sensor, l?.cursoGrados, l?.velocidadMs)

    fun iniciar(carrera: Boolean = false) {
        if (carrera) tracker.iniciar()
        ubicacion?.iniciar(INTERVALO_GPS_MS, enSegundoPlano = carrera, alLeer = ::alLeer)
        brujula?.iniciar(::alRumbo)
    }

    fun tic() { tracker.actualizarTiempo(); _estado.update { it.copy(carrera = tracker.estado.value) } }

    fun detener() { ubicacion?.detener(); brujula?.detener() }
}
