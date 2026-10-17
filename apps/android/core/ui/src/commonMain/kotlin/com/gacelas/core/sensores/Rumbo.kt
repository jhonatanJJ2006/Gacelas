package com.gacelas.core.sensores

import kotlin.math.abs

/** Matematicas de rumbos (grados 0..360), sin dependencias de Android para poder probarlas. */
object Rumbo {
    /**
     * Por debajo de esta velocidad (m/s, ~2 km/h) manda el sensor. Antes era 1,1-2,5 m/s: trotando a
     * 9 min/km (1,85 m/s) el rumbo era mitad brujula y mitad GPS, y la camara miraba hacia donde
     * apuntaba el telefono (p. ej. norte) en vez de hacia donde se corre.
     */
    const val VEL_SENSOR_MS = 0.6
    /** Desde esta velocidad (m/s, ~3,6 km/h, caminar rapido) manda solo el curso GPS. */
    const val VEL_GPS_MS = 1.0

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

    /**
     * Rumbo (grados, norte magnetico) hacia donde apunta el telefono a partir de la matriz de rotacion
     * (dispositivo → mundo, ENU). Suma la proyeccion horizontal del eje Y (arriba de la pantalla; vale
     * con el telefono plano) y la del eje -Z (la camara trasera; vale con el telefono vertical), asi
     * funciona en cualquier inclinacion sin el bloqueo de getOrientation().
     */
    fun deMatriz(r: FloatArray): Float {
        val este = r[1] - r[2]
        val norte = r[4] - r[5]
        return normalizar((kotlin.math.atan2(este.toDouble(), norte.toDouble()) * 180.0 / kotlin.math.PI).toFloat())
    }

    /** Giro del corredor respecto a la camara (grados en -180..180); 0 si no hay rumbo de movimiento. */
    fun relativo(movimiento: Float?, camara: Float): Float = movimiento?.let { diferencia(camara, it) } ?: 0f

    /** Evita mover la camara por cambios minimos del sensor. */
    fun cambioRelevante(previo: Float?, nuevo: Float?, umbral: Float = 4f): Boolean =
        nuevo != null && (previo == null || abs(diferencia(previo, nuevo)) >= umbral)
}

