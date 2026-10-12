package com.gacelas

import com.gacelas.data.mock.MockData
import com.gacelas.data.rutas.Guia
import com.gacelas.rutas.rutasNavegables
import com.gacelas.ui.components.Mapas
import org.junit.Assert.assertEquals
import org.junit.Test

class QaNavegacionTest {
    /** La distancia de la ficha coincide con lo que dice la guia ("faltan ... km"). */
    @Test fun distanciaDeRutaIgualAlTrazo() {
        MockData.rutas.forEach { r ->
            val t = Mapas.trazos[r.id] ?: return@forEach
            assertEquals(r.nombre, Guia.longitudM(t.map { it.latitude to it.longitude }) / 1000.0, r.distanciaKm, 0.1)
        }
        // Como llega del servidor: distance_km 5.2 (dato viejo) y el trazo de 1.8 km.
        val servidor = MockData.rutas[0].copy(distanciaKm = 5.2, puntos = Mapas.trazos.getValue("1").map { it.latitude to it.longitude })
        val jipiro = rutasNavegables(listOf(servidor), emptyList(), emptySet()).first { it.id == "1" }
        assertEquals(1.8, jipiro.distanciaKm, 0.05)
    }
}
