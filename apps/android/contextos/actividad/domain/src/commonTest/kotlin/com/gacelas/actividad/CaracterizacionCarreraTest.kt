package com.gacelas.actividad

import com.gacelas.actividad.domain.FaseCarrera
import com.gacelas.actividad.domain.SesionCarrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pruebas de caracterizacion del flujo de carrera (F2, docs/ARQUITECTURA-DDD.md): fijan el
 * comportamiento actual antes de mover codigo entre capas.
 */
class CaracterizacionCarreraTest {
    private var reloj = 0L
    private fun sesion() = SesionCarrera(reloj = { reloj }, ahora = { 1_791_811_800_000L })
    private fun punto(i: Int, precision: Float = 5f) = PuntoGps(-4.0 - i * 0.00003, -79.2, reloj, precision)

    @Test fun pausaNoSumaTiempoNiDistancia() {
        val s = sesion()
        s.iniciar()
        for (i in 1..60) { reloj = i * 1000L; s.registrarPunto(punto(i)); s.actualizarTiempo() }
        s.pausar()
        val enPausa = s.estado.value
        assertEquals(FaseCarrera.Pausada, enPausa.fase)
        for (i in 61..120) { reloj = i * 1000L; s.registrarPunto(punto(i)); s.actualizarTiempo() }
        assertEquals(enPausa.segundos, s.estado.value.segundos)
        assertEquals(enPausa.distanciaKm, s.estado.value.distanciaKm)
        reloj = 121_000L
        s.reanudar()
        // Reanudar no reinicia el ultimo punto valido: se sigue desde donde se pauso.
        for (i in 0..5) { reloj = 121_000L + i * 1000L; s.registrarPunto(punto(61 + i)) }
        s.actualizarTiempo()
        assertTrue(s.estado.value.distanciaKm > enPausa.distanciaKm)
        assertEquals(enPausa.segundos + 5, s.estado.value.segundos)
    }

    @Test fun puntosImprecisosNoCuentan() {
        val s = sesion()
        s.iniciar()
        reloj = 1000L; s.registrarPunto(punto(1))
        reloj = 2000L; s.registrarPunto(punto(2, precision = 200f))
        assertEquals(0.0, s.estado.value.distanciaKm)
        assertEquals(200f, s.estado.value.ultimaPrecisionM)
    }

    @Test fun finalizarYConvertirConservaLosDatos() {
        val s = sesion()
        s.iniciar()
        for (i in 1..30) { reloj = i * 1000L; s.registrarPunto(punto(i)) }
        val fin = s.finalizar()
        assertEquals(FaseCarrera.Terminada, fin.fase)
        val c = s.aCarrera("Mi carrera", "nota", publica = true, id = "id-1")
        assertEquals("id-1", c.id)
        assertEquals("Mi carrera", c.titulo)
        assertEquals(fin.segundos, c.segundos)
        assertEquals(fin.distanciaKm, c.distanciaKm)
        assertEquals(fin.puntos, c.puntos)
        assertEquals(fin.parciales, c.parciales)
        s.reiniciar()
        assertEquals(FaseCarrera.Inactiva, s.estado.value.fase)
        assertEquals(0, s.estado.value.puntos.size)
    }
}
