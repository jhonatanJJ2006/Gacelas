package com.gacelas.ui.intro

/**
 * Reglas de cuando se muestra la intro de marca (video res/raw/intro.mp4) (comunes a Android e iOS, ver docs/MARCA.md).
 *
 * Se muestra cada vez que la app pasa a primer plano (arranque en frio, volver tras salir con
 * atras o volver desde inicio/recientes con el proceso vivo), salvo que la app haya estado fuera
 * de primer plano menos de [UMBRAL_AUSENCIA_MS] (cambio rapido de app o rotacion de pantalla).
 * Nunca con las animaciones del sistema desactivadas.
 */
object IntroArranque {
    /** Duracion del video de la intro "Ruta y destello v2b", en segundos. */
    const val DURACION_S = 2.2f

    /** Si el video no muestra su primer cuadro en este tiempo, la intro se cierra (fallback). */
    const val ESPERA_PRIMER_CUADRO_MS = 1_000L

    /** Ausencia minima para repetir la intro al volver: 30 s. */
    const val UMBRAL_AUSENCIA_MS = 30_000L

    /** Ultima vez (reloj monotono, ms) que la app salio de primer plano o mostro la intro; null si nunca. */
    var ultimaSalidaMs: Long? = null
        private set

    /** Regla pura: [ultimaSalida] null es arranque en frio del proceso. */
    fun debeMostrar(ahoraMs: Long, ultimaSalida: Long?, escalaAnimaciones: Float): Boolean =
        escalaAnimaciones > 0f && (ultimaSalida == null || ahoraMs - ultimaSalida >= UMBRAL_AUSENCIA_MS)

    /** Decide al pasar a primer plano (onStart) y, si toca, la deja marcada. */
    fun alEntrar(ahoraMs: Long, escalaAnimaciones: Float): Boolean =
        debeMostrar(ahoraMs, ultimaSalidaMs, escalaAnimaciones).also { if (it) ultimaSalidaMs = ahoraMs }

    /** Se llama al salir de primer plano (onStop). */
    fun alSalir(ahoraMs: Long) { ultimaSalidaMs = ahoraMs }

    /** Solo para pruebas. */
    fun reiniciarParaPruebas() { ultimaSalidaMs = null }
}

/** Estado de una reproduccion: terminar (por tiempo o por toque) solo cuenta una vez. */
class EstadoIntro {
    var terminada = false
        private set

    /** Devuelve true solo la primera vez. */
    fun terminar(): Boolean = if (terminada) false else { terminada = true; true }
}
