package com.gacelas.ui.movimiento

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovimientoC2Test {
    @Test
    fun duracionesDentroDelRangoDeMarca() {
        listOf(MovimientoC2.CORTO, MovimientoC2.MEDIO, MovimientoC2.LARGO).forEach { assertTrue(it in 150..350, "$it fuera de 150-350 ms") }
    }

    @Test
    fun animacionesQuitadasDanDuracionCero() {
        assertEquals(0, MovimientoC2.duracion(MovimientoC2.MEDIO, 0f))
        assertEquals(0, MovimientoC2.retraso(3, 0f))
        assertTrue(MovimientoC2.spec<Float>(MovimientoC2.MEDIO, 0f) is SnapSpec)
    }

    @Test
    fun escalaNormalUsaTweenConLaDuracionBase() {
        assertEquals(MovimientoC2.MEDIO, MovimientoC2.duracion(MovimientoC2.MEDIO, 1f))
        val s = MovimientoC2.spec<Float>(MovimientoC2.CORTO, 1f)
        assertTrue(s is TweenSpec && s.durationMillis == MovimientoC2.CORTO)
    }

    @Test
    fun escalonadoTieneTope() {
        assertEquals(MovimientoC2.ESCALON, MovimientoC2.retraso(1, 1f))
        assertEquals(MovimientoC2.retraso(MovimientoC2.ESCALONES_MAX, 1f), MovimientoC2.retraso(40, 1f))
        assertTrue(MovimientoC2.retraso(40, 1f) <= 300)
    }
}
