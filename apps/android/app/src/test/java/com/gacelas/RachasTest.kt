package com.gacelas

import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.carreras.domain.reglas.Rachas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RachasTest {
    private val ahora = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 10, 12, 0, 0) }.timeInMillis
    private fun hace(dias: Int) = Carrera(id = "$dias", titulo = "c", inicioMs = Rachas.sumarDias(ahora, -dias), segundos = 1500, distanciaKm = 5.0)

    @Test fun igualQueLaFuncionSql() {
        val r = Rachas.calcular(listOf(0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11).map(::hace), ahora, 3)
        assertEquals(3, r.diaria)
        assertEquals(8, r.mejorDiaria)
        assertEquals(2, r.semanal)
        assertEquals(2, r.mejorSemanal)
        assertEquals(5, r.carrerasSemana)
    }

    @Test fun sigueVivaAyerYSeRompeTrasUnDia() {
        val viva = Rachas.calcular(listOf(hace(1), hace(2)), ahora)
        assertEquals(2, viva.diaria)
        assertTrue(Rachas.enRiesgo(viva, ahora))
        assertEquals(0, Rachas.calcular(listOf(hace(2), hace(3)), ahora).diaria)
        assertEquals(0, Rachas.calcular(emptyList(), ahora).semanal)
    }

    @Test fun semanaEnCursoSinMetaNoRompe() =
        assertEquals(1, Rachas.calcular(listOf(0, 6, 7, 8).map(::hace), ahora, 3).semanal)

    @Test fun mapaCalorDoceSemanas() {
        val m = Rachas.mapaCalor(listOf(hace(0), hace(0), hace(3)), ahora)
        assertEquals(12, m.size)
        assertEquals(listOf(5.0, 10.0), m.flatten().filter { it.km > 0 }.map { it.km })
        assertEquals(1, m.flatten().count { it.futuro })
    }
}
