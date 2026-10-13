package com.gacelas

import com.gacelas.core.sensores.Rumbo
import com.gacelas.carreras.domain.modelo.PuntoGps
import com.gacelas.carreras.domain.SesionCarrera
import com.gacelas.ui.components.ReglasDescarga
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class BrujulaOfflineTest {
    /** Matriz dispositivo→mundo (ENU) de un telefono con rumbo h y cabeceo pitch (0 plano, 90 vertical). */
    private fun matriz(h: Double, pitch: Double): FloatArray {
        val a = Math.toRadians(h); val p = Math.toRadians(pitch)
        // Ejes del dispositivo en el mundo: X = derecha, Y = arriba de pantalla, Z = hacia el usuario.
        val f = doubleArrayOf(sin(a), cos(a), 0.0)          // frente horizontal
        val x = doubleArrayOf(cos(a), -sin(a), 0.0)
        val y = DoubleArray(3) { f[it] * cos(p) + (if (it == 2) 1.0 else 0.0) * sin(p) }
        val z = DoubleArray(3) { -f[it] * sin(p) + (if (it == 2) 1.0 else 0.0) * cos(p) }
        return floatArrayOf(x[0].toFloat(), y[0].toFloat(), z[0].toFloat(), x[1].toFloat(), y[1].toFloat(), z[1].toFloat(), x[2].toFloat(), y[2].toFloat(), z[2].toFloat())
    }

    @Test fun rumboCorrectoPlanoYVertical() {
        for (pitch in listOf(0.0, 30.0, 60.0, 85.0, 90.0)) for (h in listOf(0.0, 90.0, 180.0, 270.0, 45.0)) {
            val r = Rumbo.deMatriz(matriz(h, pitch))
            assertEquals("h=$h pitch=$pitch", 0f, Rumbo.diferencia(h.toFloat(), r), 0.5f)
        }
    }

    @Test fun norteASurGiraEntero() {
        assertEquals(180f, Math.abs(Rumbo.diferencia(Rumbo.deMatriz(matriz(0.0, 80.0)), Rumbo.deMatriz(matriz(180.0, 80.0)))), 0.5f)
    }

    @Test fun quietoManDaLaBrujula() {
        // Quieto (ritmo null → 0 m/s) el GPS no pesa nada.
        assertEquals(180f, Rumbo.combinar(180f, 0f, 0.0)!!, 0.01f)
    }

    @Test fun alPararseSeBorraElRitmo() {
        var t = 0L
        val tr = SesionCarrera(reloj = { t }, ahora = { 1_000L })
        tr.iniciar()
        for (i in 0..10) { t = i * 4_000L; tr.registrarPunto(PuntoGps(-4.0 + i * 0.0001, -79.2, t, 5f)) }
        assertNotNull(tr.estado.value.ritmoActualSegKm)
        // Quieto: puntos con menos de 2 m de avance durante mas de PARADO_MS.
        t = 44_000; tr.registrarPunto(PuntoGps(-4.0 + 10 * 0.0001, -79.2, t, 5f))
        assertNotNull(tr.estado.value.ritmoActualSegKm)
        t = 40_000 + SesionCarrera.PARADO_MS + 1_000; tr.registrarPunto(PuntoGps(-4.0 + 10 * 0.0001 + 0.000005, -79.2, t, 5f))
        assertNull(tr.estado.value.ritmoActualSegKm)
    }

    @Test fun descargaAutomaticaSoloConWifiYSiFalta() {
        assertTrue(ReglasDescarga.descargarSola(existe = false, completa = false, wifi = true))
        assertTrue(ReglasDescarga.descargarSola(existe = true, completa = false, wifi = true))
        assertFalse(ReglasDescarga.descargarSola(existe = true, completa = true, wifi = true))
        assertFalse(ReglasDescarga.descargarSola(existe = false, completa = false, wifi = false))
    }
}
