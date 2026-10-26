package com.gacelas.seguridad.application

import com.gacelas.seguridad.domain.DiaSeguridad
import com.gacelas.seguridad.domain.IncidentePublico
import com.gacelas.seguridad.domain.Incidentes
import com.gacelas.seguridad.domain.IncidentesRepositorio
import com.gacelas.seguridad.domain.NivelSeguridad
import com.gacelas.seguridad.domain.SeguridadZona
import com.gacelas.seguridad.domain.TipoIncidente
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class RepoFalso(var zona: SeguridadZona? = null) : IncidentesRepositorio {
    val reportes = mutableListOf<Pair<TipoIncidente, String>>()
    val confirmados = mutableListOf<String>()
    override suspend fun seguridadZona(rutaId: String) = zona
    override suspend fun recientes() = listOf(IncidentePublico("i1", TipoIncidente.Acoso, 0.0, 0.0, 1L, 0, false))
    override suspend fun reportar(
        tipo: TipoIncidente, lat: Double, lng: Double, direccion: String, ocurridoMs: Long,
        descripcion: String?, fotosJpeg: List<ByteArray>, carpeta: String,
    ) { reportes += tipo to carpeta }
    override suspend fun confirmar(id: String): Boolean { confirmados += id; return true }
}

class CasosSeguridadTest {
    @Test fun sinServidorUsaElEjemplo() = runTest {
        assertSame(Incidentes.EJEMPLO, ConsultarSeguridadDeRuta(null)("r1"))
        assertEquals(3, VerIncidentesRecientes(null)(1_000_000L).size)
    }

    @Test fun conServidorSinDatosDevuelveNull() = runTest {
        assertNull(ConsultarSeguridadDeRuta(RepoFalso())("r1"))
    }

    @Test fun recientesYConfirmarUsanElRepositorio() = runTest {
        val repo = RepoFalso()
        assertEquals(listOf("i1"), VerIncidentesRecientes(repo)(0L).map { it.id })
        assertTrue(ConfirmarIncidente(repo)("i1"))
        assertEquals(listOf("i1"), repo.confirmados)
    }

    @Test fun reportarPasaLosDatos() = runTest {
        val repo = RepoFalso()
        ReportarIncidente(repo)(NuevoIncidente(TipoIncidente.Trafico, 1.0, 2.0, "Calle", 5L, null, emptyList()), "u/x")
        assertEquals(listOf(TipoIncidente.Trafico to "u/x"), repo.reportes)
    }

    @Test fun sugiereElDiaConMenorIndice() {
        val zona = SeguridadZona(listOf(DiaSeguridad(0, NivelSeguridad.Medio, 1.0), DiaSeguridad(3, NivelSeguridad.Bajo, 0.4)), emptyList(), null, "x")
        assertEquals(3, SugerirDiaSeguro(zona))
        assertNull(SugerirDiaSeguro(null))
    }
}
