package com.gacelas.core.sensores

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlin.math.abs

/**
 * Rumbo del telefono con el sensor de orientacion (TYPE_ROTATION_VECTOR, fusion de giroscopio,
 * acelerometro y magnetometro). Entrega grados respecto al norte verdadero (0..360), corregidos
 * por la rotacion de la pantalla y la declinacion magnetica del lugar.
 */
class Brujula(context: Context) : SensorEventListener {
    private val gestor = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor: Sensor? = gestor?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    @Suppress("DEPRECATION")
    private val ventana = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val matriz = FloatArray(9)
    private val remapeada = FloatArray(9)
    private val orientacion = FloatArray(3)
    private var suavizado: Float? = null
    private var alCambiar: ((Float) -> Unit)? = null

    /** Declinacion (grados) para pasar de norte magnetico a verdadero. */
    @Volatile var declinacion: Float = 0f

    val disponible: Boolean get() = sensor != null

    fun actualizarDeclinacion(lat: Double, lng: Double, altitudM: Double = 0.0) {
        declinacion = GeomagneticField(lat.toFloat(), lng.toFloat(), altitudM.toFloat(), System.currentTimeMillis()).declination
    }

    fun iniciar(alCambiar: (Float) -> Unit) {
        this.alCambiar = alCambiar
        sensor?.let { gestor?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun detener() {
        gestor?.unregisterListener(this)
        alCambiar = null
        suavizado = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(matriz, event.values)
        val (ejeX, ejeY) = when (rotacionPantalla()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(matriz, ejeX, ejeY, remapeada)
        SensorManager.getOrientation(remapeada, orientacion)
        val crudo = Rumbo.normalizar(Math.toDegrees(orientacion[0].toDouble()).toFloat() + declinacion)
        val nuevo = suavizado?.let { Rumbo.suavizar(it, crudo, 0.15f) } ?: crudo
        suavizado = nuevo
        alCambiar?.invoke(nuevo)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun rotacionPantalla(): Int =
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= 30) runCatching { ventana?.defaultDisplay?.rotation }.getOrNull() ?: Surface.ROTATION_0
        else ventana?.defaultDisplay?.rotation ?: Surface.ROTATION_0
}

/** Matematicas de rumbos (grados 0..360), sin dependencias de Android para poder probarlas. */
object Rumbo {
    /** Por debajo de esta velocidad (m/s, unos 4 km/h) manda el sensor. */
    const val VEL_SENSOR_MS = 1.1
    /** Por encima de esta velocidad (m/s, unos 9 km/h) manda el GPS. */
    const val VEL_GPS_MS = 2.5

    fun normalizar(g: Float): Float = ((g % 360f) + 360f) % 360f

    /** Diferencia con signo de a hacia b en (-180, 180]. */
    fun diferencia(a: Float, b: Float): Float {
        val d = normalizar(b - a)
        return if (d > 180f) d - 360f else d
    }

    /** Interpolacion circular: t = 0 devuelve a, t = 1 devuelve b, siempre por el camino corto. */
    fun mezclar(a: Float, b: Float, t: Float): Float = normalizar(a + diferencia(a, b) * t.coerceIn(0f, 1f))

    /** Filtro paso bajo circular (alfa = peso de la lectura nueva). */
    fun suavizar(previo: Float, nuevo: Float, alfa: Float): Float = mezclar(previo, nuevo, alfa)

    /** Peso del GPS segun la velocidad: 0 quieto o lento, 1 corriendo, lineal entre ambos. */
    fun pesoGps(velocidadMs: Double?): Float {
        val v = velocidadMs ?: return 0f
        return ((v - VEL_SENSOR_MS) / (VEL_GPS_MS - VEL_SENSOR_MS)).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Rumbo combinado: sensor cuando se esta quieto o lento y GPS cuando se corre, con una mezcla
     * gradual entre ambos. Si falta una de las fuentes se usa la otra.
     */
    fun combinar(sensor: Float?, gps: Float?, velocidadMs: Double?): Float? = when {
        sensor == null -> gps
        gps == null -> sensor
        else -> mezclar(sensor, gps, pesoGps(velocidadMs))
    }

    /** Evita mover la camara por cambios minimos del sensor. */
    fun cambioRelevante(previo: Float?, nuevo: Float?, umbral: Float = 4f): Boolean =
        nuevo != null && (previo == null || abs(diferencia(previo, nuevo)) >= umbral)
}

/** Rumbo del sensor como estado de Compose; null si el telefono no tiene el sensor o [activo] es falso. */
@Composable
fun rumboSensorState(activo: Boolean, lat: Double? = null, lng: Double? = null): State<Float?> {
    val estado = remember { mutableStateOf<Float?>(null) }
    if (LocalInspectionMode.current) return estado
    val context = LocalContext.current
    val brujula = remember { Brujula(context.applicationContext) }
    val conPosicion = lat != null && lng != null
    DisposableEffect(activo, conPosicion) {
        if (lat != null && lng != null) brujula.actualizarDeclinacion(lat, lng)
        if (activo && brujula.disponible) brujula.iniciar { estado.value = it } else estado.value = null
        onDispose { brujula.detener() }
    }
    return estado
}
