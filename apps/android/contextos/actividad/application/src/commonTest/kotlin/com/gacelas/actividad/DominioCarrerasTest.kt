package com.gacelas.actividad

import com.gacelas.actividad.domain.FaseCarrera
import com.gacelas.actividad.domain.SesionCarrera
import com.gacelas.actividad.application.EliminarCarrera
import com.gacelas.actividad.application.GuardarCarrera
import com.gacelas.actividad.application.ObtenerCarrera
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.Distancia
import com.gacelas.actividad.domain.modelo.Parcial
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.actividad.domain.modelo.Ritmo
import com.gacelas.actividad.domain.reglas.Geo
import com.gacelas.actividad.domain.reglas.ReglasGps
import com.gacelas.actividad.domain.reglas.ReglasParciales
import com.gacelas.actividad.domain.reglas.ReglasRitmo
import com.gacelas.actividad.domain.repositorio.CarrerasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepoMemoria : CarrerasRepository {
    override val carreras = MutableStateFlow<List<Carrera>>(emptyList())
    override suspend fun guardar(carrera: Carrera) { carreras.value = listOf(carrera) + carreras.value.filterNot { it.id == carrera.id } }
    override suspend fun eliminar(id: String) { carreras.value = carreras.value.filterNot { it.id == id } }
}

class DominioCarrerasTest {
    @Test fun cronometroConPausa() {
        var t = 0L
        val r = SesionCarrera(reloj = { t }, ahora = { 1_000L })
        r.iniciar(); t = 10_000; r.pausar(); t = 50_000; r.reanudar(); t = 65_000; r.actualizarTiempo()
        assertEquals(25, r.estado.value.segundos)
        assertEquals(FaseCarrera.Corriendo, r.estado.value.fase)
    }

    @Test fun valueObjects() {
        assertEquals(2340.0, Distancia(2.34).metros, 1e-9)
        assertEquals(2, (Distancia(1.5) + Distancia(0.6)).kmCompletos)
        assertEquals(381, Ritmo.promedio(892, Distancia(2.34))?.segPorKm)
        assertNull(Ritmo.promedio(100, Distancia(0.005)))
    }

    @Test fun etiquetaDeParciales() {
        assertEquals("Km 2", Parcial(2, 1.0, 380).etiqueta)
        assertEquals("Vuelta 1", Parcial(1, 0.34, 129, manual = true).etiqueta)
    }

    @Test fun parcialAutomaticoSoloAlCompletarKm() {
        assertNull(ReglasParciales.automatico(emptyList(), 0.99, 370, 0))
        assertEquals(Parcial(1, 1.0, 375), ReglasParciales.automatico(emptyList(), 1.01, 375, 0))
        // Las vueltas manuales no cuentan como km completos.
        assertEquals(Parcial(2, 1.0, 380), ReglasParciales.automatico(listOf(Parcial(1, 1.0, 375), Parcial(1, 0.5, 190, true)), 2.0, 755, 375))
    }

    @Test fun vueltaManualDesdeLaAnterior() {
        val previas = listOf(Parcial(1, 1.0, 378), Parcial(1, 0.5, 200, manual = true))
        assertEquals(Parcial(2, 0.84, 692, manual = true), ReglasParciales.vuelta(previas, 1.34, 892).let { it.copy(distanciaKm = (it.distanciaKm * 100).toInt() / 100.0) })
    }

    @Test fun filtroGps() {
        val a = PuntoGps(-4.0, -79.2, 0L, 5f)
        assertTrue(ReglasGps.impreciso(a.copy(precisionM = 40f)))
        assertTrue(ReglasGps.descartar(a, a.copy(lat = -4.000001, tiempoMs = 1000L))) // < 2 m: ruido
        assertTrue(ReglasGps.descartar(a, a.copy(lat = -4.001, tiempoMs = 1000L))) // 111 m en 1 s: imposible
        assertTrue(!ReglasGps.descartar(a, a.copy(lat = -4.00003, tiempoMs = 1000L))) // ~3.3 m/s
        assertEquals(3.0, ReglasGps.subida(a.copy(altitudM = 2000.0), a.copy(altitudM = 2003.0)))
        assertEquals(0.0, ReglasGps.subida(a.copy(altitudM = 2003.0), a.copy(altitudM = 2000.0)))
    }

    @Test fun ritmoVentana() {
        val puntos = (0..10).map { PuntoGps(-4.0 + it * 0.00003, -79.2, it * 1000L, 5f) }
        val r = ReglasRitmo.ventana(puntos)!!
        val esperado = (10.0 / (Geo.haversineM(puntos.first(), puntos.last()) / 1000.0)).toInt()
        assertEquals(esperado, r)
    }

    @Test fun sesionCompletaYGuardar() = runTest {
        var reloj = 0L
        val sesion = SesionCarrera(reloj = { reloj }, ahora = { 1_791_811_800_000L })
        sesion.iniciar()
        // ~3.34 m cada segundo hacia el sur durante 320 s (>1 km).
        for (i in 1..320) { reloj = i * 1000L; sesion.registrarPunto(PuntoGps(-4.0 - i * 0.00003, -79.2, i * 1000L, 5f)) }
        sesion.vuelta()
        val fin = sesion.finalizar()
        assertEquals(1, fin.parciales.count { !it.manual })
        assertEquals(1, fin.parciales.count { it.manual })
        val repo = RepoMemoria()
        val c = GuardarCarrera(sesion, repo)("", "nota", publica = false)
        assertEquals(com.gacelas.actividad.domain.reglas.tituloPorHora(1_791_811_800_000L), c.titulo) // segun la zona horaria del equipo
        assertEquals(c, ObtenerCarrera(repo)(c.id).first())
        EliminarCarrera(repo)(c.id)
        assertNull(ObtenerCarrera(repo)(c.id).first())
    }
}
