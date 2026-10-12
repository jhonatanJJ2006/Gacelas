package com.gacelas.shared

import com.gacelas.core.sensores.LecturaGps
import com.gacelas.core.sensores.SeguimientoSensores
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SensoresTest {
    @Test fun quietoMandaBrujulaYCorriendoMandaGps() {
        var t = 0L
        val s = SeguimientoSensores(null, null, com.gacelas.tracking.RunTracker(reloj = { t }, ahora = { 0L }))
        s.alRumbo(90f)
        s.alLeer(LecturaGps(-3.99, -79.20, 5f, 0, velocidadMs = 0.2, cursoGrados = 180f))
        assertEquals(90f, s.estado.value.rumbo)
        s.alLeer(LecturaGps(-3.99, -79.20, 5f, 1000, velocidadMs = 2.5, cursoGrados = 180f))
        assertEquals(180f, s.estado.value.rumbo)
        assertEquals(2, s.estado.value.lecturas)
    }

    @Test fun lecturasAlimentanElCronometro() {
        var t = 0L
        val s = SeguimientoSensores(null, null, com.gacelas.tracking.RunTracker(reloj = { t }, ahora = { 0L }))
        s.tracker.iniciar()
        for (i in 0..30) { t = i * 1000L; s.alLeer(LecturaGps(-3.99 - i * 0.00003, -79.20, 5f, t, velocidadMs = 3.3)) }
        assertTrue(s.estado.value.carrera.distanciaKm > 0.08, "km=${s.estado.value.carrera.distanciaKm}")
        assertEquals(30, s.estado.value.carrera.segundos)
        assertEquals("SO", cardinal(225f))
    }
}
