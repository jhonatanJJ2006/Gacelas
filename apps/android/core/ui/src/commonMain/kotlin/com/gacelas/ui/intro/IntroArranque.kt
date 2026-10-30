package com.gacelas.ui.intro

/**
 * Reglas de cuando se muestra la intro de marca (Android: vector animado res/drawable/intro_marca.xml) (comunes a Android e iOS, ver docs/MARCA.md).
 *
 * Se muestra SIEMPRE que se abre la app de nuevo: arranque en frio y cualquier actividad nueva
 * (volver tras salir con atras o tras cerrarla desde recientes, aunque el proceso siga vivo y
 * hayan pasado pocos segundos). Al traerla de segundo plano con la actividad viva (inicio,
 * recientes, notificacion) solo se repite si estuvo fuera al menos [UMBRAL_AUSENCIA_MS]; una
 * rotacion o un cambio rapido de app no la relanzan. Nunca con las animaciones del sistema
 * desactivadas.
 */
object IntroArranque {
    /** Duracion de la intro "Ruta y destello v2b", en segundos. */
    const val DURACION_S = 2.2f

    /** Tope de seguridad: si la animacion no avisa su fin, la intro se retira a los 3,7 s. */
    const val TOPE_MS = 3_700L

    /** Ausencia minima para repetir la intro al volver: 30 s. */
    const val UMBRAL_AUSENCIA_MS = 30_000L

    /** Ultima vez (reloj monotono, ms) que la app salio de primer plano o mostro la intro; null si nunca. */
    var ultimaSalidaMs: Long? = null
        private set

    /** Regla pura: [ultimaSalida] null es arranque en frio del proceso. */
    /** [aperturaNueva]: actividad creada desde cero (no rotacion ni restauracion): siempre intro. */
    fun debeMostrar(ahoraMs: Long, ultimaSalida: Long?, escalaAnimaciones: Float, aperturaNueva: Boolean = false): Boolean =
        escalaAnimaciones > 0f && (aperturaNueva || ultimaSalida == null || ahoraMs - ultimaSalida >= UMBRAL_AUSENCIA_MS)

    /** Decide al pasar a primer plano (onCreate/onStart) y, si toca, la deja marcada. */
    fun alEntrar(ahoraMs: Long, escalaAnimaciones: Float, aperturaNueva: Boolean = false): Boolean =
        debeMostrar(ahoraMs, ultimaSalidaMs, escalaAnimaciones, aperturaNueva).also { if (it) ultimaSalidaMs = ahoraMs }

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
