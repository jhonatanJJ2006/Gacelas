package com.gacelas.ui.intro

import com.gacelas.ui.intro.LineaTiempoIntroE as E
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Valores clave de la linea de tiempo "Opcion E" (escena_E de render_opciones.py). */
class LineaTiempoIntroETest {
    private fun cerca(esperado: Float, real: Float, tol: Float = 1e-3f) =
        assertTrue(abs(esperado - real) <= tol, "esperado $esperado, real $real")

    @Test fun inicioEsFondo() {
        cerca(1f, E.velo(0f))
        cerca(0f, E.avanceAproximacion(0f))
        cerca(0f, E.fraccionContorno(0f))
        cerca(0f, E.alfaCabeza(0f))
        cerca(0f, E.alfaRelleno(0f))
        cerca(0f, E.alfaLetra(0f, 0))
    }

    @Test fun t032LlegaElPuntoYEmpiezaElContorno() {
        cerca(1f, E.avanceAproximacion(0.32f))
        cerca(0f, E.fraccionContorno(0.32f))
        cerca(1f, E.alfaCabeza(0.32f))
        cerca(1f, E.alfaContorno(0.32f))
        cerca(0f, E.velo(0.32f))
    }

    @Test fun t11ContornoCompletoYRellenoEntrando() {
        cerca(1f, E.fraccionContorno(1.1f))
        assertTrue(E.alfaRelleno(1.1f) in 0.15f..0.4f)
        assertTrue(E.alfaContorno(1.1f) > 0.99f)
        assertTrue(E.avanceLinea(1.1f, 0) in 0.6f..0.8f)
        assertTrue(E.alfaCabeza(1.1f) in 0.8f..0.9f)
    }

    @Test fun t185PulsoTerminadoYTextoSaliendo() {
        cerca(0f, E.alfaContorno(1.85f))
        cerca(1f, E.alfaRelleno(1.85f))
        cerca(1f, E.escalaLogo(1.85f), 2e-3f)
        assertNull(E.barrido(1.85f))
        assertTrue(E.alfaLetra(1.85f, 0) > 0.9f)
        assertTrue(E.alfaLetra(1.85f, 6) < E.alfaLetra(1.85f, 0))
        cerca(0f, E.alfaLema(1.85f))
    }

    @Test fun t24Reposo() {
        for (i in 0 until 7) cerca(1f, E.alfaLetra(2.4f, i))
        for (i in 0 until 7) cerca(0f, E.dyLetra(2.4f, i), 1e-2f)
        cerca(1f, E.alfaLema(2.4f))
        cerca(0f, E.espaciadoLema(2.4f))
        cerca(0f, E.velo(2.4f))
        cerca(0f, E.alfaAnillo(E.anillo(2.4f)))
    }

    @Test fun finEsFondo() {
        cerca(1f, E.velo(3.0f))
        cerca(1f, E.veloSalida(2.95f))
    }

    @Test fun pulsoMaximoCincoPorCiento() = cerca(1.05f, E.escalaLogo(1.22f + 0.21f))

    @Test fun contornoMonotono() {
        var prev = -1f
        var t = 0f
        while (t <= 3f) { val d = E.fraccionContorno(t); assertTrue(d >= prev); prev = d; t += 0.01f }
    }

    @Test fun duracion() = assertEquals(3.0f, E.DURACION_S)
}
