package com.gacelas

import com.gacelas.actividad.domain.EstadoCarrera
import com.gacelas.actividad.domain.FaseCarrera
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.correr.ObjetivoCarrera
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.perfil.periodo
import com.gacelas.perfil.volumenSemanal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rediseno movil 0.9.16: objetivo de carrera, preferencias nuevas y cifras del perfil. */
class Version0916Test {
    private val e = EstadoCarrera(fase = FaseCarrera.Corriendo, segundos = 892, distanciaKm = 2.34)

    @Test fun objetivoDistancia() {
        val o = ObjetivoCarrera.de("distancia", 6000)
        assertTrue(o.activo)
        assertEquals("6 km", o.valorTexto(true))
        assertEquals(0.39f, o.progreso(e)!!, 0.01f)
        assertEquals("Faltan 3.66 km", o.estado(e, true).replace(',', '.'))
        assertFalse(o.cumplido(e))
        assertTrue(o.cumplido(e.copy(distanciaKm = 6.01)))
    }

    @Test fun objetivoTiempoYRitmo() {
        val t = ObjetivoCarrera.de("tiempo", 1800)
        assertEquals("30 min", t.valorTexto(true))
        assertEquals("Faltan 15:08", t.estado(e, true))
        val r = ObjetivoCarrera.de("ritmo", 330)
        assertNull(r.progreso(e))
        assertTrue(r.estado(e, true).startsWith("Vas 6:21"))
    }

    @Test fun libreYDesconocido() {
        assertFalse(ObjetivoCarrera.de("libre", 0).activo)
        assertEquals(ObjetivoCarrera.Tipo.Libre, ObjetivoCarrera.de("otro", 5).tipo)
        assertEquals(6000, ObjetivoCarrera.predeterminado(ObjetivoCarrera.Tipo.Distancia, null).valor)
        assertEquals(350, ObjetivoCarrera.predeterminado(ObjetivoCarrera.Tipo.Ritmo, 360).valor)
    }

    @Test fun preferenciasNuevasConValoresPorDefecto() {
        val a = Ajustes()
        assertTrue(a.vistaMapa3d)
        assertEquals("libre", a.objetivoTipo)
        assertEquals(0, a.objetivoValor)
    }

    @Test fun volumenYPeriodoSalenDelHistorial() {
        val ahora = 1_791_811_800_000L
        val c = listOf(
            Carrera("a", "", inicioMs = ahora - 3_600_000L, segundos = 1800, distanciaKm = 5.0, desnivelM = 30),
            Carrera("b", "", inicioMs = ahora - 9 * 86_400_000L, segundos = 1200, distanciaKm = 3.0, desnivelM = 10),
        )
        val v = volumenSemanal(c, ahora, 12)
        assertEquals(12, v.size)
        assertEquals(8.0, v.sum(), 0.001)
        val p = periodo(c, ahora - 28 * 86_400_000L, ahora + 1)
        assertEquals(2, p.carreras)
        assertEquals(375, p.ritmo)
    }
}
