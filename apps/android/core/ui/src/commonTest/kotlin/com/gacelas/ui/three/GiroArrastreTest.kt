package com.gacelas.ui.three

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GiroArrastreTest {
    @Test fun giroAutomatico() {
        val g = GiroArrastre(gradosAuto = 18f)
        assertEquals(18f, g.avanzar(1f), 0.01f)
    }

    @Test fun arrastrarPausaElGiroYSuma() {
        val g = GiroArrastre(gradosAuto = 18f, gradosPorPx = 0.5f)
        g.empezar()
        g.arrastrar(100f)
        assertEquals(50f, g.avanzar(1f), 0.01f)
        g.arrastrar(-200f)
        assertEquals(310f, g.angulo, 0.01f)
    }

    @Test fun inerciaSeFrenaYVuelveElAutomatico() {
        val g = GiroArrastre(gradosAuto = 18f, gradosPorPx = 0.5f)
        g.empezar(); g.soltar(400f)
        assertEquals(200f, g.inerciaActual, 0.01f)
        repeat(300) { g.avanzar(1f / 60f) }
        assertEquals(0f, g.inerciaActual, 0.01f)
        val a = g.angulo
        assertEquals((a + 18f) % 360f, g.avanzar(1f), 0.01f)
    }

    @Test fun inerciaLimitada() {
        val g = GiroArrastre(inerciaMax = 720f)
        g.empezar(); g.soltar(100_000f)
        assertTrue(g.inerciaActual <= 720f)
    }

    @Test
    fun enMovimientoSoloConArrastreOInerciaYReiniciarVuelveACero() {
        val g = GiroArrastre(gradosAuto = 0f)
        kotlin.test.assertFalse(g.enMovimiento)
        g.empezar(); g.arrastrar(50f)
        kotlin.test.assertTrue(g.enMovimiento)
        g.soltar(300f)
        repeat(600) { g.avanzar(1f / 60f) }
        kotlin.test.assertFalse(g.enMovimiento)
        g.reiniciar()
        kotlin.test.assertEquals(0f, g.angulo)
    }
}
