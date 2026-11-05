package com.gacelas.ui.movimiento

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Especificaciones de movimiento C2 (docs/MARCA.md, seccion Movimiento). Todas las animaciones de
 * la app salen de aqui: nada de duraciones o curvas sueltas en las pantallas.
 *
 * - Duraciones entre 150 y 350 ms (las cifras que cuentan y el destello son la unica excepcion).
 * - Curvas: entrar desacelera (llega suave), salir acelera (se va rapido), estandar para cambios de estado.
 * - [escala] viene del sistema (Android: "Escala de duracion de animacion"; 0 = quitar animaciones).
 *   Con escala 0 todas las especificaciones son instantaneas (snap) y los escalonados no esperan.
 */
object MovimientoC2 {
    /** Pulsacion, cambios de color, chips, interruptores. */
    const val CORTO = 150

    /** Hojas, dialogos, salidas de pantalla, barras. */
    const val MEDIO = 250

    /** Entrada de pantallas y de elementos destacados. */
    const val LARGO = 350

    /** Retraso entre elementos consecutivos de una lista. */
    const val ESCALON = 45

    /** A partir de este indice ya no se suma retraso: listas largas no esperan. */
    const val ESCALONES_MAX = 6

    /** Cifras del resumen que cuentan hacia arriba. */
    const val CONTEO = 900

    /** Escala de un boton pulsado (graphicsLayer, sin relayout). */
    const val ESCALA_PULSADO = 0.96f

    /** Desplazamiento de entrada de elementos (dp). */
    const val DESPLAZAMIENTO_DP = 16

    val Entrar: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Salir: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Estandar: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Duracion efectiva segun la escala del sistema (0 si se quitaron las animaciones). */
    fun duracion(base: Int, escala: Float): Int = if (escala <= 0f) 0 else (base * escala.coerceAtMost(10f)).toInt()

    /** Retraso del elemento [indice] de una lista escalonada. */
    fun retraso(indice: Int, escala: Float): Int = duracion(ESCALON * indice.coerceIn(0, ESCALONES_MAX), escala)

    fun <T> spec(base: Int, escala: Float, curva: Easing = Estandar, retraso: Int = 0): FiniteAnimationSpec<T> {
        val d = duracion(base, escala)
        return if (d == 0) snap() else tween(d, delayMillis = retraso, easing = curva)
    }
}

/**
 * Escala de movimiento del sistema. Android la lee de Settings.Global.ANIMATOR_DURATION_SCALE en
 * MainActivity; iOS/escritorio usan 1 (iOS respeta "Reducir movimiento" en su capa nativa).
 */
val LocalEscalaMovimiento = staticCompositionLocalOf { 1f }

/** Atajos para usar en composables: respetan la escala del sistema. */
object Mov {
    val escala: Float
        @Composable @ReadOnlyComposable
        get() = LocalEscalaMovimiento.current

    val activo: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalEscalaMovimiento.current > 0f

    @Composable @ReadOnlyComposable
    fun <T> corto(curva: Easing = MovimientoC2.Estandar): AnimationSpec<T> = MovimientoC2.spec(MovimientoC2.CORTO, escala, curva)

    @Composable @ReadOnlyComposable
    fun <T> medio(curva: Easing = MovimientoC2.Estandar): AnimationSpec<T> = MovimientoC2.spec(MovimientoC2.MEDIO, escala, curva)

    @Composable @ReadOnlyComposable
    fun <T> largo(curva: Easing = MovimientoC2.Entrar): AnimationSpec<T> = MovimientoC2.spec(MovimientoC2.LARGO, escala, curva)
}
