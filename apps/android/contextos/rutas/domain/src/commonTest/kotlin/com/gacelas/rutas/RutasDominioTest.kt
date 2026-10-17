package com.gacelas.rutas

import com.gacelas.rutas.domain.casosuso.ConsultaRutas
import com.gacelas.rutas.domain.casosuso.rutasVisibles
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.FiltroDistancia
import com.gacelas.rutas.domain.modelo.FiltrosRuta
import com.gacelas.rutas.domain.modelo.PestanaRutas
import com.gacelas.rutas.domain.modelo.Ruta
import kotlin.test.Test
import kotlin.test.assertEquals

private val ejemplo = listOf(
    Ruta("1", "Parque Jipiro", 1.8, Dificultad.Facil, 45, "Panorámica"),
    Ruta("3", "Cerro Villonaco", 4.4, Dificultad.Dificil, 380, "Montaña"),
    Ruta("4", "Campus UTPL", 6.1, Dificultad.Moderado, 150, "Mía", autorId = "yo"),
)

class RutasDominioTest {
    @Test fun casoDeUsoCombinaPestanaFiltrosYCercania() {
        assertEquals(listOf("4"), rutasVisibles(ejemplo, emptySet(), "yo", ConsultaRutas(PestanaRutas.MisRutas)).map { it.id })
        assertEquals(listOf("3"), rutasVisibles(ejemplo, emptySet(), "yo", ConsultaRutas(filtros = FiltrosRuta(Dificultad.Dificil))).map { it.id })
        assertEquals(listOf("4"), rutasVisibles(ejemplo, emptySet(), "yo", ConsultaRutas(filtros = FiltrosRuta(distancia = FiltroDistancia.De5a10))).map { it.id })
        assertEquals(listOf("4", "1", "3"), rutasVisibles(ejemplo, emptySet(), "yo", ConsultaRutas(cerca = mapOf("4" to 0.5, "1" to 2.0))).map { it.id })
        assertEquals(listOf("1"), rutasVisibles(ejemplo, emptySet(), "yo", ConsultaRutas(texto = "jipiro")).map { it.id })
    }
}
