package com.gacelas.carreras

import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.carreras.domain.reglas.EstadisticasPerfil
import com.gacelas.shared.Fechas
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EstadisticasPerfilTest {
    private fun ms(mes: Int, dia: Int, hora: Int = 7) = LocalDateTime(2026, mes, dia, hora, 0).toInstant(Fechas.zona()).toEpochMilliseconds()
    private fun c(dia: Int, km: Double, seg: Int, mes: Int = 10) = Carrera("$mes-$dia", "x", inicioMs = ms(mes, dia), segundos = seg, distanciaKm = km, desnivelM = 10)
    private val ahora = ms(10, 21, 12)
    private val lista = listOf(c(19, 5.0, 1500), c(20, 10.5, 3150), c(21, 2.0, 500), c(5, 21.1, 7600, 8))

    @Test fun semanaEHistorico() {
        val t = EstadisticasPerfil.totalesSemana(lista, ahora)
        assertEquals(3, t.carreras)
        assertEquals(30, t.desnivelM)
        assertEquals(250, EstadisticasPerfil.historico(lista).mejorRitmoSegKm)
    }

    @Test fun kmMensualYRecords() {
        val m = EstadisticasPerfil.kmMensual(lista, ahora)
        assertEquals(6, m.size)
        assertEquals("Oct", m.last().etiqueta)
        assertEquals(17.5, m.last().km, 0.001)
        assertEquals(21.1, m[3].km, 0.001)
        val r = EstadisticasPerfil.records(lista)
        assertEquals(listOf(250, 1500, 3000), r.take(3).map { it.segundos })
        assertTrue(r[3].segundos != null)
    }

    @Test fun medallasYReto() {
        assertEquals(3, EstadisticasPerfil.mejorRacha(lista))
        val m = EstadisticasPerfil.medallas(lista).associate { it.id to it.lograda }
        assertEquals(true, m["21k"])
        assertEquals(false, m["racha7"])
        val r = EstadisticasPerfil.retoMes(lista, ahora)
        assertEquals("Reto Octubre Andino", r.titulo)
        assertEquals(10, r.diasRestantes)
        assertEquals("Hace 35 min", EstadisticasPerfil.haceTiempo(ahora - 35 * 60_000L, ahora))
    }
}
