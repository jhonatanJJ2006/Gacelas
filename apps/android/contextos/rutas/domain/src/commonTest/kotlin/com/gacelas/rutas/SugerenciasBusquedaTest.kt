package com.gacelas.rutas

import com.gacelas.rutas.domain.reglas.SugerenciasBusqueda
import kotlin.test.Test
import kotlin.test.assertEquals

class SugerenciasBusquedaTest {
    private val rutas = listOf("Parque Lineal La Tebaida", "Cerro Villonaco", "Parque Jipiro", "Mirador Las Peñas", "Orillas del Zamora")

    @Test fun sinTextoMuestraRecientes() {
        val s = SugerenciasBusqueda.sugerir("", rutas, listOf("jipiro", "Jipiro", "zamora"))
        assertEquals(listOf("jipiro", "zamora"), s.map { it.texto })
        assertEquals(true, s.all { it.reciente })
    }

    @Test fun recientesPrimeroLuegoPrefijoSinTildes() {
        val s = SugerenciasBusqueda.sugerir("pe", rutas, listOf("pena alta"))
        assertEquals(listOf("pena alta", "Mirador Las Peñas"), s.map { it.texto })
        // Resalta "Pe" de "Peñas" en la posicion real del texto.
        assertEquals(12, s[1].desde); assertEquals(2, s[1].largo)
    }

    @Test fun palabrasQueEmpiezanVanAntesQueLasQueContienen() {
        val s = SugerenciasBusqueda.sugerir("par", rutas, emptyList())
        assertEquals(listOf("Parque Jipiro", "Parque Lineal La Tebaida"), s.map { it.texto })
        assertEquals(emptyList(), SugerenciasBusqueda.sugerir("xyz", rutas, emptyList()))
    }
}
