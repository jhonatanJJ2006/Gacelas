package com.gacelas

import com.gacelas.incidentes.Incidentes
import com.gacelas.incidentes.NivelSeguridad
import com.gacelas.incidentes.TipoIncidente
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Datos de prueba ficticios (EJEMPLO). */
class IncidentesTest {
    @Test fun nivelesPorIndice() {
        assertEquals(NivelSeguridad.Bajo, NivelSeguridad.deIndice(0.5))
        assertEquals(NivelSeguridad.Medio, NivelSeguridad.deIndice(1.0))
        assertEquals(NivelSeguridad.Alto, NivelSeguridad.deIndice(1.5))
    }

    @Test fun tiposCoincidenConLaMigracion() {
        assertEquals(
            listOf("robo_asalto", "acoso", "perros_agresivos", "iluminacion_deficiente", "trafico_peligroso", "via_mal_estado", "accidente", "otro"),
            TipoIncidente.entries.map { it.id },
        )
        assertEquals(TipoIncidente.Otro, TipoIncidente.deId("desconocido"))
    }

    @Test fun leeSeguridadZona() {
        val json = Json.parseToJsonElement(
            """{"dias":[{"dia":1,"nivel":"bajo","indice":0.4},{"dia":5,"nivel":"alto","indice":2.1}],
               "tipos":[{"servicio":"EJEMPLO","subtipo":"Ficticio","conteo":3}],"actualizado_hasta":"2026-03","fuente":"EJEMPLO"}""",
        )
        val z = Incidentes.leerZona(json)!!
        assertEquals(2, z.dias.size)
        assertEquals(NivelSeguridad.Alto, z.dias[1].nivel)
        assertEquals(3, z.tipos.first().conteo)
        assertEquals("2026-03", z.actualizadoHasta)
    }

    @Test fun puntoEwktEnOrdenLngLat() {
        assertEquals("SRID=4326;POINT(-79.2 -3.99)", Incidentes.ewkt(-3.99, -79.2))
    }

    @Test fun ejemplosSonFicticiosYRecientes() {
        val ahora = 1_000_000_000_000L
        val e = Incidentes.ejemplos(ahora)
        assertTrue(e.all { it.id.startsWith("vp-") && it.ocurridoMs < ahora })
        assertEquals("hace 2 h", Incidentes.haceTexto(e.first().ocurridoMs, ahora))
    }
}
