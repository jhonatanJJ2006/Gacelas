package com.gacelas.shared

import com.gacelas.core.sensores.LecturaGps
import com.gacelas.core.sensores.Rumbo
import com.gacelas.core.sensores.SeguimientoSensores
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SensoresTest {
    @Test fun rumbos() {
        assertEquals(270f, Rumbo.normalizar(-90f))
        assertEquals(-90f, Rumbo.diferencia(0f, 270f))
        assertEquals(270f, Rumbo.combinar(0f, 270f, 1.85)!!)
    }

    @Test fun deMatrizTelefonoVerticalAlOeste() {
        val r = floatArrayOf(0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
        assertEquals(270f, Rumbo.deMatriz(r), 0.5f)
    }

    @Test fun quietoMandaBrujulaYCorriendoMandaGps() {
        var t = 0L
        val s = SeguimientoSensores(null, null, com.gacelas.carreras.domain.SesionCarrera(reloj = { t }, ahora = { 0L }))
        s.alRumbo(90f)
        s.alLeer(LecturaGps(-3.99, -79.20, 5f, 0, velocidadMs = 0.2, cursoGrados = 180f))
        assertEquals(90f, s.estado.value.rumbo)
        s.alLeer(LecturaGps(-3.99, -79.20, 5f, 1000, velocidadMs = 2.5, cursoGrados = 180f))
        assertEquals(180f, s.estado.value.rumbo)
        assertEquals(2, s.estado.value.lecturas)
    }

    @Test fun lecturasAlimentanElCronometro() {
        var t = 0L
        val s = SeguimientoSensores(null, null, com.gacelas.carreras.domain.SesionCarrera(reloj = { t }, ahora = { 0L }))
        s.tracker.iniciar()
        for (i in 0..30) { t = i * 1000L; s.alLeer(LecturaGps(-3.99 - i * 0.00003, -79.20, 5f, t, velocidadMs = 3.3)) }
        assertTrue(s.estado.value.carrera.distanciaKm > 0.08, "km=${s.estado.value.carrera.distanciaKm}")
        assertEquals(30, s.estado.value.carrera.segundos)
        val puntos = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        assertEquals("SO", puntos[((Rumbo.normalizar(225f) + 22.5f) / 45f).toInt() % 8])
    }
}
