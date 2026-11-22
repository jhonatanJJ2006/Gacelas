package com.gacelas.comunidad

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.comunidad.domain.modelo.MiembroClub
import com.gacelas.comunidad.domain.modelo.Publicacion
import com.gacelas.comunidad.domain.modelo.RolClub
import com.gacelas.comunidad.domain.reglas.Ranking
import kotlin.test.Test
import kotlin.test.assertEquals

class RankingTest {
    private val ahora = 1_761_040_800_000L
    private fun pub(id: String, autor: String, km: Double) = Publicacion(
        id, autor, false, 0, "", "t", km, 100, 300, 0, 0, autorId = id, inicioMs = ahora,
    )

    @Test fun clubSoloCuentaMiembros() {
        val propias = listOf(Carrera("c", "Mia", inicioMs = ahora, segundos = 1000, distanciaKm = 5.0))
        val miembros = listOf(
            MiembroClub("u1", "Yo", RolClub.member, true),
            MiembroClub("u2", "Ana", RolClub.member, true),
        )
        val filas = Ranking.club(propias, "Yo", "u1", listOf(pub("u2", "Ana", 8.0), pub("u3", "Luis", 12.0)), miembros, ahora)
        assertEquals(listOf("Ana", "Yo"), filas.map { it.nombre })
        val ajeno = Ranking.club(propias, "Yo", "u9", listOf(pub("u2", "Ana", 8.0)), miembros, ahora)
        assertEquals(listOf("Ana"), ajeno.map { it.nombre })
    }
}
