package com.gacelas

import com.gacelas.identidad.domain.modelo.ConfigIntervalos
import com.gacelas.identidad.domain.reglas.ValidacionAuth
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.reglas.Estadisticas
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.comunidad.domain.modelo.PestanaEventos
import com.gacelas.comunidad.domain.reglas.filtrarEventos
import com.gacelas.comunidad.domain.reglas.yaPaso
import com.gacelas.data.mock.MockData.ID_USUARIO
import com.gacelas.data.mock.MockData
import com.gacelas.rutas.domain.modelo.PestanaRutas
import com.gacelas.rutas.domain.reglas.filtrarRutas
import com.gacelas.actividad.domain.FaseCarrera
import com.gacelas.tracking.FaseIntervalo
import com.gacelas.tracking.PlanIntervalos
import com.gacelas.actividad.domain.SesionCarrera
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearHms
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.tracking.haversineM
import com.gacelas.tracking.intervaloMetronomoMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GeoTest {
    @Test fun haversine_unGradoDeLatitud() = assertEquals(111_195.0, haversineM(0.0, 0.0, 1.0, 0.0), 50.0)
    @Test fun haversine_mismoPuntoEsCero() = assertEquals(0.0, haversineM(-4.0, -79.2, -4.0, -79.2), 1e-9)
    @Test fun formatos() {
        assertEquals("5:30", formatearRitmo(330))
        assertEquals("--:--", formatearRitmo(null))
        assertEquals("--:--", formatearRitmo(0))
        assertEquals("01:01:05", formatearHms(3665))
        assertEquals("2:05", formatearMs(125))
    }
    @Test fun unidadesImperiales() {
        assertEquals("1.00", Unidades.texto(1.609344, false))
        assertEquals("mi", Unidades.etiquetaDistancia(false))
        assertEquals(Unidades.ritmo(300, true), 300)
        assertEquals(482, Unidades.ritmo(300, false))
        assertEquals("/mi", Unidades.etiquetaRitmo(false))
    }
}

class SesionCarreraTest {
    private var t = 0L
    private val tracker = SesionCarrera(reloj = { t }, ahora = { 1_000L })
    /** 0.0001 grados de latitud son ~11.1 m. */
    private fun punto(i: Int, precision: Float = 5f, alt: Double? = null) = PuntoGps(-4.0 + i * 0.0001, -79.2, i * 4_000L, precision, alt)

    @Test fun acumulaDistanciaYTiempo() {
        tracker.iniciar()
        for (i in 0..10) { t = i * 4_000L; tracker.registrarPunto(punto(i)) }
        val e = tracker.estado.value
        assertEquals(FaseCarrera.Corriendo, e.fase)
        assertEquals(0.111, e.distanciaKm, 0.002)
        assertEquals(40, e.segundos)
        assertNotNull(e.ritmoActualSegKm)
    }

    @Test fun descartaPuntosImprecisosYSaltos() {
        tracker.iniciar()
        tracker.registrarPunto(punto(0))
        t = 4_000; tracker.registrarPunto(punto(1, precision = 80f))
        assertEquals(80f, tracker.estado.value.ultimaPrecisionM)
        t = 5_000; tracker.registrarPunto(PuntoGps(-3.99, -79.2, 5_000, 5f)) // salto de 1 km en 5 s
        assertEquals(0.0, tracker.estado.value.distanciaKm, 1e-9)
        assertEquals(1, tracker.estado.value.puntos.size)
    }

    @Test fun pausaNoCuentaTiempoNiDistancia() {
        tracker.iniciar()
        tracker.registrarPunto(punto(0))
        t = 10_000; tracker.pausar()
        t = 60_000; tracker.registrarPunto(punto(5)); tracker.actualizarTiempo()
        assertEquals(10, tracker.estado.value.segundos)
        assertEquals(0.0, tracker.estado.value.distanciaKm, 1e-9)
        tracker.reanudar()
        t = 65_000; tracker.actualizarTiempo()
        assertEquals(15, tracker.estado.value.segundos)
    }

    @Test fun parcialesAutomaticosPorKilometro() {
        tracker.iniciar()
        for (i in 0..100) { t = i * 4_000L; tracker.registrarPunto(punto(i)) }
        val p = tracker.estado.value.parciales
        assertEquals(1, p.size)
        assertFalse(p.first().manual)
    }

    @Test fun vueltaManualYDesnivel() {
        tracker.iniciar()
        tracker.registrarPunto(punto(0, alt = 2000.0))
        t = 4_000; tracker.registrarPunto(punto(1, alt = 2003.0))
        t = 8_000; tracker.registrarPunto(punto(2, alt = 2001.0))
        tracker.vuelta()
        val e = tracker.estado.value
        assertEquals(3.0, e.desnivelM, 1e-6)
        assertTrue(e.parciales.any { it.manual })
    }

    @Test fun finalizarYGuardarYReiniciar() {
        tracker.iniciar()
        tracker.registrarPunto(punto(0))
        t = 4_000; tracker.registrarPunto(punto(1))
        assertEquals(FaseCarrera.Terminada, tracker.finalizar().fase)
        val c = tracker.aCarrera("Prueba", "nota", publica = false, id = "x")
        assertEquals("x", c.id); assertFalse(c.publica); assertEquals(2, c.puntos.size)
        tracker.reiniciar()
        assertEquals(FaseCarrera.Inactiva, tracker.estado.value.fase)
    }

    @Test fun ignoraPuntosSinCarrera() {
        tracker.registrarPunto(punto(0))
        assertTrue(tracker.estado.value.puntos.isEmpty())
    }
}

class PlanIntervalosTest {
    private val plan = PlanIntervalos(ConfigIntervalos(trabajoSeg = 60, descansoSeg = 30, rondas = 3))
    @Test fun duracionSinUltimoDescanso() = assertEquals(240, plan.duracionTotalSeg)
    @Test fun fases() {
        assertEquals(FaseIntervalo.Trabajo, plan.estado(0).fase)
        assertEquals(60, plan.estado(0).restanteSeg)
        assertEquals(FaseIntervalo.Descanso, plan.estado(60).fase)
        assertEquals(30, plan.estado(60).restanteSeg)
        assertEquals(2, plan.estado(90).ronda)
        assertEquals(FaseIntervalo.Terminado, plan.estado(240).fase)
    }
    @Test fun metronomo() {
        assertEquals(500L, intervaloMetronomoMs(120))
        assertEquals(250L, intervaloMetronomoMs(1000))
    }
}

class ValidacionAuthTest {
    @Test fun email() {
        assertNull(ValidacionAuth.email("jhona@utpl.edu.ec"))
        assertNotNull(ValidacionAuth.email(""))
        assertNotNull(ValidacionAuth.email("sin-arroba"))
    }
    @Test fun password() {
        assertNull(ValidacionAuth.password("gacela123"))
        assertNotNull(ValidacionAuth.password("corta1"))
        assertNotNull(ValidacionAuth.password("soloLetras"))
    }
    @Test fun nombreYUsername() {
        assertNotNull(ValidacionAuth.nombre(" a "))
        assertEquals("jhona_perez", ValidacionAuth.usernameDesde("Jhona Pérez!".replace("é", "e"), "x@y.com"))
        assertEquals("x", ValidacionAuth.usernameDesde("!!", "x@y.com"))
    }
}

class FiltrosTest {
    @Test fun rutas() {
        val todas = MockData.rutas
        assertEquals(todas.size, filtrarRutas(todas, PestanaRutas.Explorar, emptySet(), ID_USUARIO).size)
        assertTrue(filtrarRutas(todas, PestanaRutas.MisRutas, emptySet(), ID_USUARIO).all { it.autorId == ID_USUARIO })
        assertTrue(filtrarRutas(todas, PestanaRutas.MisRutas, emptySet(), ID_USUARIO).isNotEmpty())
        val id = todas.first().id
        assertEquals(listOf(id), filtrarRutas(todas, PestanaRutas.Guardadas, setOf(id), ID_USUARIO).map { it.id })
        assertTrue(filtrarRutas(todas, PestanaRutas.Explorar, emptySet(), ID_USUARIO, "zzzz").isEmpty())
    }
    @Test fun eventos() {
        val ev = MockData.eventos
        assertTrue(filtrarEventos(ev, PestanaEventos.Proximos, emptySet()).none { it.yaPaso() })
        assertTrue(filtrarEventos(ev, PestanaEventos.Pasados, emptySet()).all { it.yaPaso() })
        assertTrue(filtrarEventos(ev, PestanaEventos.Pasados, emptySet()).isNotEmpty())
        val id = ev.first().id
        assertTrue(filtrarEventos(ev, PestanaEventos.MisEventos, setOf(id)).any { it.id == id })
    }
}

class EstadisticasTest {
    private fun ms(y: Int, m: Int, d: Int, h: Int = 7) = Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, 0) }.timeInMillis
    private fun carrera(ms: Long, km: Double = 5.0) = Carrera(id = "$ms", titulo = "c", inicioMs = ms, segundos = 1500, distanciaKm = km, desnivelM = 10)

    @Test fun semanaYRacha() {
        // Viernes 9 de octubre de 2026
        val hoy = ms(2026, 10, 9, 20)
        val lista = listOf(carrera(ms(2026, 10, 9)), carrera(ms(2026, 10, 8)), carrera(ms(2026, 10, 7)), carrera(ms(2026, 10, 1)))
        val s = Estadisticas.semana(lista, hoy)
        assertEquals(3, s.carreras)
        assertEquals(15.0, s.km, 1e-9)
        assertEquals(setOf(2, 3, 4), s.diasCorridos)
        assertEquals(300, s.ritmoSegKm)
        assertEquals(3, s.racha)
    }
    @Test fun rachaSiHoyNoCorrio() = assertEquals(1, Estadisticas.racha(listOf(carrera(ms(2026, 10, 8))), ms(2026, 10, 9)))
    @Test fun mes() {
        val r = Estadisticas.mes(listOf(carrera(ms(2026, 10, 2)), carrera(ms(2026, 9, 30))), ms(2026, 10, 9))
        assertEquals(1, r.carreras); assertEquals(10, r.desnivelM)
    }
}
