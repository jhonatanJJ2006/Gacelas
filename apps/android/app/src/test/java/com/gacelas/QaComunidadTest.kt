package com.gacelas

import com.gacelas.data.eventos.PestanaEventos
import com.gacelas.data.eventos.filtrarEventos
import com.gacelas.data.eventos.yaPaso
import com.gacelas.data.mock.MockData
import com.gacelas.tracking.Unidades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QaComunidadTest {
    @Test fun distanciasCortasConDosDecimales() {
        assertEquals("0.05", Unidades.texto(0.0504, true, 1))
        assertEquals("0.10", Unidades.texto(0.102, true, 1))
        assertEquals("5.2", Unidades.texto(5.21, true, 1))
        assertEquals("12.30", Unidades.texto(12.3, true))
    }

    @Test fun eventoJipiroPasaAPasadosDespuesDeLas6Guayaquil() {
        val jipiro = MockData.eventos.first { it.id == "1" }
        val sabado559 = 1791716400000L - 60_000   // 11 oct 2026 05:59 en Guayaquil (UTC-5)
        val sabado1900 = 1791716400000L + 13 * 3_600_000L
        assertFalse(jipiro.yaPaso(sabado559))
        assertTrue(jipiro.yaPaso(sabado1900))
        assertTrue(filtrarEventos(MockData.eventos, PestanaEventos.Pasados, emptySet(), sabado1900).any { it.id == "1" })
        assertFalse(filtrarEventos(MockData.eventos, PestanaEventos.Proximos, emptySet(), sabado1900).any { it.id == "1" })
    }
}
