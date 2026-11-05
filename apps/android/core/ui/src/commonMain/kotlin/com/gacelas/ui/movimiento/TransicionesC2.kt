package com.gacelas.ui.movimiento

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * Transiciones de pantalla C2 (compartidas por Android y la app iOS de Compose).
 *
 * - Avanzar a una pantalla secundaria: entra desde la derecha un 8 % del ancho con fundido
 *   (MEDIO, Entrar); la anterior se funde (CORTO). Volver es el espejo.
 * - Cambio de pestana: fundido cruzado sin desplazamiento ni zoom (cada pestana dibuja su barra
 *   inferior, que asi no se mueve).
 * - Escala 0 (quitar animaciones): sin transicion.
 *
 * Duraciones cortas a proposito: la pantalla nueva atiende toques cuando termina su entrada
 * (Navigation la pasa a RESUMED al acabar), asi que no se alarga mas alla de 250 ms.
 */
object TransicionesC2 {
    private const val FRACCION = 0.08f

    fun entrar(escala: Float, pestana: Boolean): EnterTransition {
        if (escala <= 0f) return EnterTransition.None
        val fundido = fadeIn(MovimientoC2.spec(MovimientoC2.MEDIO, escala, MovimientoC2.Entrar))
        return if (pestana) fundido
        else fundido + slideInHorizontally(MovimientoC2.spec(MovimientoC2.MEDIO, escala, MovimientoC2.Entrar)) { (it * FRACCION).toInt() }
    }

    fun salir(escala: Float): ExitTransition {
        if (escala <= 0f) return ExitTransition.None
        return fadeOut(MovimientoC2.spec(MovimientoC2.CORTO, escala, MovimientoC2.Salir))
    }

    fun volverEntrar(escala: Float): EnterTransition {
        if (escala <= 0f) return EnterTransition.None
        return fadeIn(MovimientoC2.spec(MovimientoC2.MEDIO, escala, MovimientoC2.Entrar))
    }

    fun volverSalir(escala: Float, pestana: Boolean): ExitTransition {
        if (escala <= 0f) return ExitTransition.None
        val fundido = fadeOut(MovimientoC2.spec(MovimientoC2.CORTO, escala, MovimientoC2.Salir))
        return if (pestana) fundido
        else fundido + slideOutHorizontally(MovimientoC2.spec(MovimientoC2.MEDIO, escala, MovimientoC2.Salir)) { (it * FRACCION).toInt() }
    }
}
