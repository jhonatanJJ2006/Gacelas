package com.gacelas.ui.intro

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntroArranqueTest {
    @BeforeTest fun limpiar() = IntroArranque.reiniciarParaPruebas()

    @Test fun seMuestraEnArranqueEnFrioConAnimaciones() = assertTrue(IntroArranque.debeMostrar(true, 1f))

    @Test fun noSeMuestraSiLaActividadSeRecrea() = assertFalse(IntroArranque.debeMostrar(false, 1f))

    @Test fun noSeMuestraConAnimacionesDesactivadas() = assertFalse(IntroArranque.debeMostrar(true, 0f))

    @Test fun soloUnaVezPorProceso() {
        assertTrue(IntroArranque.debeMostrar(true, 1f))
        IntroArranque.marcarMostrada()
        assertFalse(IntroArranque.debeMostrar(true, 1f))
    }

    @Test fun saltarTerminaUnaSolaVez() {
        val e = EstadoIntro()
        assertTrue(e.terminar())
        assertFalse(e.terminar())
        assertTrue(e.terminada)
    }

    @Test fun duraComoMaximoTresSegundos() = assertTrue(IntroArranque.DURACION_S <= 3f)

    @Test fun curvasIgualesAlPrototipo() {
        assertEquals(0f, CurvasIntro.e(0f))
        assertEquals(1f, CurvasIntro.e(1f))
        assertEquals(.5f, CurvasIntro.io(.5f))
        assertEquals(201, CurvasIntro.ruta.size)
        assertEquals(140f, CurvasIntro.ruta.first().x)
    }
}
