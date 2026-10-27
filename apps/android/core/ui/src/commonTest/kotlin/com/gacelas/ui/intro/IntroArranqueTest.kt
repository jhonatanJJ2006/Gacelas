package com.gacelas.ui.intro

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntroArranqueTest {
    @BeforeTest fun limpiar() = IntroArranque.reiniciarParaPruebas()

    private val u = IntroArranque.UMBRAL_AUSENCIA_MS

    @Test fun seMuestraEnArranqueEnFrio() = assertTrue(IntroArranque.debeMostrar(1_000, null, 1f))

    @Test fun noSeMuestraConAnimacionesDesactivadas() = assertFalse(IntroArranque.debeMostrar(1_000, null, 0f))

    @Test fun noSeRepiteEnCambioRapidoDeApp() = assertFalse(IntroArranque.debeMostrar(10_000 + u - 1, 10_000, 1f))

    @Test fun seRepiteTrasTreintaSegundosFuera() = assertTrue(IntroArranque.debeMostrar(10_000 + u, 10_000, 1f))

    @Test fun cicloCompleto() {
        assertTrue(IntroArranque.alEntrar(0, 1f))           // arranque en frio
        assertFalse(IntroArranque.alEntrar(500, 1f))        // rotacion: onStop/onStart seguidos
        IntroArranque.alSalir(5_000)                       // sale con atras o a inicio
        assertFalse(IntroArranque.alEntrar(5_000 + 10_000, 1f)) // vuelve a los 10 s
        IntroArranque.alSalir(20_000)
        assertTrue(IntroArranque.alEntrar(20_000 + u, 1f))  // vuelve a los 30 s
        assertEquals(20_000 + u, IntroArranque.ultimaSalidaMs)
    }

    @Test fun saltarSoloCuentaUnaVez() {
        val e = EstadoIntro()
        assertTrue(e.terminar())
        assertFalse(e.terminar())
    }

    @Test fun duraComoMaximoTresSegundos() = assertTrue(IntroArranque.DURACION_S <= 3f)
    @Test fun esperaElPrimerCuadroUnSegundo() = assertEquals(1_000L, IntroArranque.ESPERA_PRIMER_CUADRO_MS)
}
