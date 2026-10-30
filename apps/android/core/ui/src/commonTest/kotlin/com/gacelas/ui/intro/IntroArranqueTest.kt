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

    @Test fun aperturaNuevaSiempreMuestraAunqueHayaSalidoHacePoco() {
        IntroArranque.alSalir(10_000)                          // cerro con atras o desde recientes
        assertTrue(IntroArranque.alEntrar(12_000, 1f, aperturaNueva = true)) // reabre a los 2 s
        assertFalse(IntroArranque.alEntrar(12_500, 1f))       // su onStart (o una rotacion) no la repite
    }

    @Test fun aperturaNuevaRespetaAnimacionesDesactivadas() = assertFalse(IntroArranque.debeMostrar(0, 0, 0f, aperturaNueva = true))

    @Test fun saltarSoloCuentaUnaVez() {
        val e = EstadoIntro()
        assertTrue(e.terminar())
        assertFalse(e.terminar())
    }

    @Test fun duraComoMaximoTresSegundos() = assertTrue(IntroArranque.DURACION_S <= 3f)
    @Test fun elTopeNoCortaLaIntro() = assertTrue(IntroArranque.TOPE_MS >= (IntroArranque.DURACION_S * 1000).toLong() + 1_000)
}
