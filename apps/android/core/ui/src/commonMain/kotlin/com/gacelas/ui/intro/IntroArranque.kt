package com.gacelas.ui.intro

/** Reglas de cuando se muestra la intro de marca (comunes a Android e iOS). */
object IntroArranque {
    /** Duracion de la animacion A "Ruta y destello", en segundos. */
    const val DURACION_S = 2.8f

    /** Ya se mostro en este proceso: un arranque en caliente (actividad recreada) no la repite. */
    var mostradaEnProceso = false
        private set

    /**
     * Solo en arranque en frio ([arranqueNuevo] y sin haberse mostrado en el proceso) y con
     * animaciones del sistema activas ([escalaAnimaciones] mayor que 0).
     */
    fun debeMostrar(arranqueNuevo: Boolean, escalaAnimaciones: Float): Boolean =
        arranqueNuevo && !mostradaEnProceso && escalaAnimaciones > 0f

    /** Marca la intro como mostrada; se llama al decidir mostrarla. */
    fun marcarMostrada() { mostradaEnProceso = true }

    /** Solo para pruebas. */
    fun reiniciarParaPruebas() { mostradaEnProceso = false }
}

/** Estado de una reproduccion: terminar (por tiempo o por toque) solo cuenta una vez. */
class EstadoIntro {
    var terminada = false
        private set

    /** Devuelve true solo la primera vez. */
    fun terminar(): Boolean = if (terminada) false else { terminada = true; true }
}
