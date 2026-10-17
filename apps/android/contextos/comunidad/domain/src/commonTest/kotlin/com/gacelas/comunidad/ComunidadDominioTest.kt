package com.gacelas.comunidad

import com.gacelas.comunidad.domain.reglas.calendarioMes
import com.gacelas.shared.Fechas
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class ComunidadDominioTest {
    @Test fun calendarioLunesADomingo() {
        val tz = TimeZone.of("America/Guayaquil")
        val semanas = calendarioMes(2026, 9, emptyList(), tz)
        assertEquals(31, semanas.flatten().count { it.delMes })
        assertEquals(0, Fechas.diaSemana(semanas[0][0].inicioMs, tz))
    }
}
