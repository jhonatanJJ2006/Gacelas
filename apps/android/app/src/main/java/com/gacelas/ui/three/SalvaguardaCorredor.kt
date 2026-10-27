package com.gacelas.ui.three

import android.content.Context

/**
 * Salvaguarda contra cierres nativos de Filament (un PreconditionPanic aborta el proceso y no se puede
 * capturar). Antes de cada paso delicado (crear el motor, cambiar la superficie, los primeros cuadros,
 * destruir) se guarda una marca con commit() y se borra al terminar el paso. Si la app arranca y la
 * marca sigue puesta, el proceso murio en medio de ese paso: el 3D queda desactivado para esta version
 * y se muestra el respaldo, asi nunca hay un bucle de cierres. Una version nueva vuelve a intentarlo.
 */
class SalvaguardaCorredor(context: Context, private val version: Long) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Se llama una vez por proceso antes de decidir el motor. */
    fun puedeUsar3D(): Boolean {
        synchronized(Companion) {
            if (!revisado) {
                revisado = true
                if (prefs.getBoolean(EN_CURSO, false)) {
                    prefs.edit().putBoolean(EN_CURSO, false).putLong(BLOQUEADO, version).commit()
                }
            }
        }
        return prefs.getLong(BLOQUEADO, -1L) != version
    }

    fun empezarPaso() { prefs.edit().putBoolean(EN_CURSO, true).commit() }

    fun terminarPaso() { prefs.edit().putBoolean(EN_CURSO, false).apply() }

    companion object {
        const val PREFS = "corredor3d"
        const val EN_CURSO = "filament_en_curso"
        const val BLOQUEADO = "filament_bloqueado_version"
        /** Cuadros que se vigilan al arrancar antes de dar el 3D por estable. */
        const val CUADROS_VIGILADOS = 120
        private var revisado = false

        /** Solo tests: simula un proceso nuevo. */
        internal fun nuevoProceso() { synchronized(this) { revisado = false } }
    }
}
