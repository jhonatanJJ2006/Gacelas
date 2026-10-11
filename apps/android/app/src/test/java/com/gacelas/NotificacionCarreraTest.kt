package com.gacelas

import com.gacelas.data.carreras.PuntoGps
import com.gacelas.tracking.FaseCarrera
import com.gacelas.tracking.NotificacionCarrera
import com.gacelas.tracking.RunTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificacionCarreraTest {
    private var reloj = 0L
    private val t = RunTracker(reloj = { reloj }, ahora = { 1_000_000L + reloj })
    private fun punto(i: Int) = PuntoGps(-3.99 - i * 0.00003, -79.2, 1_000_000L + reloj, 5f, null)

    @Test fun maquinaDeEstadosYNotificacion() {
        t.iniciar()
        var c = NotificacionCarrera.contenido(t.estado.value)
        assertEquals("Carrera en curso", c.titulo); assertEquals("Pausar", c.accionPausa); assertTrue(c.cronometroCorre)

        for (i in 0..5) { reloj += 1000; t.registrarPunto(punto(i)) }
        val puntos = t.estado.value.puntos.size
        val km = t.estado.value.distanciaKm
        assertTrue(km > 0)

        t.alternarPausa()
        assertEquals(FaseCarrera.Pausada, t.estado.value.fase)
        val seg = t.estado.value.segundos
        c = NotificacionCarrera.contenido(t.estado.value)
        assertEquals("Carrera en pausa", c.titulo); assertEquals("Reanudar", c.accionPausa); assertFalse(c.cronometroCorre)

        // En pausa: ni puntos ni tiempo.
        for (i in 6..10) { reloj += 1000; t.registrarPunto(punto(i)) }
        t.actualizarTiempo()
        assertEquals(puntos, t.estado.value.puntos.size)
        assertEquals(seg, t.estado.value.segundos)
        assertEquals(km, t.estado.value.distanciaKm, 0.0)

        t.alternarPausa()
        reloj += 3000; t.actualizarTiempo()
        assertEquals(seg + 3, t.estado.value.segundos)
        assertTrue(NotificacionCarrera.contenido(t.estado.value).texto.contains("km"))

        t.finalizar()
        assertFalse(t.estado.value.activa)
        assertEquals(FaseCarrera.Terminada, t.estado.value.fase)
    }
}
