package com.gacelas.rutas

import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.rutas.domain.reglas.CarreraSugerida
import com.gacelas.rutas.domain.reglas.ResultadoSugerida
import com.gacelas.rutas.domain.reglas.SugerenciaPrevia
import com.gacelas.rutas.domain.reglas.diaEcuador
import com.gacelas.rutas.domain.reglas.distanciaObjetivo
import com.gacelas.rutas.domain.reglas.elegirRutaSugerida
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private fun ruta(id: String, km: Double, lat: Double = -4.0) =
    Ruta(id, id, km, Dificultad.Facil, 0, "", puntos = listOf(lat to -79.2, (lat + 0.01) to -79.2))

// 26 oct 2026, 08:00 en Ecuador (13:00 UTC).
private const val AHORA = 1_793_019_600_000L
private const val DIA = 86_400_000L
private val HOY = diaEcuador(AHORA)
private val rutas = listOf(ruta("a", 3.0), ruta("b", 3.2, -3.9), ruta("insegura", 3.0), ruta("c", 8.0, -3.8))
private val seguras = setOf("a", "b", "c")
private val historicas = (0 until 5).map { CarreraSugerida(AHORA - (10 + it) * DIA, 3.0, -3.5 to -79.2) }

private fun id(r: ResultadoSugerida) = (r as ResultadoSugerida.Ok).ruta.id

class RutaSugeridaTest {
    @Test fun diaDeEcuadorCambiaALas5Utc() {
        assertEquals(diaEcuador(AHORA), diaEcuador(AHORA + 15 * 3600 * 1000L)) // 23:00 Ecuador
        assertEquals(diaEcuador(AHORA) + 1, diaEcuador(AHORA + 16 * 3600 * 1000L))
    }

    @Test fun objetivoEsMediaDeUltimas5o5km() {
        assertEquals(5.0, distanciaObjetivo(emptyList()))
        assertEquals(3.0, distanciaObjetivo(historicas))
    }

    @Test fun nuncaInsegura() {
        assertEquals("a", id(elegirRutaSugerida(rutas, historicas, seguras, emptyList(), AHORA)))
        assertIs<ResultadoSugerida.SinRuta>(elegirRutaSugerida(rutas, historicas, emptySet(), emptyList(), AHORA))
    }

    @Test fun estableEnElDia() {
        assertEquals("c", id(elegirRutaSugerida(rutas, historicas, seguras, listOf(SugerenciaPrevia(HOY, "c")), AHORA)))
    }

    @Test fun noRepiteSugeridasNiCorridasEn3Dias() {
        val corrida = CarreraSugerida(AHORA - 2 * DIA, 3.1, -3.9 to -79.2)
        val r = elegirRutaSugerida(rutas, historicas + corrida, seguras, listOf(SugerenciaPrevia(HOY - 3, "a")), AHORA)
        assertEquals("c", id(r))
    }

    @Test fun hace4DiasYaNoCuenta() {
        assertEquals("a", id(elegirRutaSugerida(rutas, historicas, seguras, listOf(SugerenciaPrevia(HOY - 4, "a")), AHORA)))
    }

    @Test fun todasRepetidasLoDice() {
        val r = elegirRutaSugerida(rutas, historicas, setOf("a"), listOf(SugerenciaPrevia(HOY - 1, "a")), AHORA)
        assertIs<ResultadoSugerida.SinRuta>(r)
    }

    @Test fun aIgualDistanciaPrefiereLaCercana() {
        val r = elegirRutaSugerida(listOf(ruta("lejos", 3.0, -3.5), ruta("cerca", 3.0)), historicas, setOf("lejos", "cerca"), emptyList(), AHORA, -4.0 to -79.2)
        assertEquals("cerca", id(r))
    }
}
