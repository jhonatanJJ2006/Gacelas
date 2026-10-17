package com.gacelas

import com.gacelas.rutas.data.Elevacion
import com.gacelas.rutas.data.SupabaseExplorarRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevacionTest {
    private val recta = listOf(-4.0 to -79.2, -3.99 to -79.2)

    @Test fun muestreaEquidistante() {
        val m = Elevacion.muestrear(recta, 11)
        assertEquals(11, m.size)
        assertEquals(-3.995, m[5].first, 1e-6)
    }

    @Test fun desnivelSoloSubidas() = assertEquals(25, Elevacion.desnivelPositivo(listOf(100, 101, 100, 110, 105, 120)))

    @Test fun leeRespuestaOpenMeteo() {
        assertEquals(listOf(2100, 2101), Elevacion.leer("""{"elevation":[2100.4,2100.6]}"""))
        assertTrue(Elevacion.url(recta).startsWith("https://api.open-meteo.com/v1/elevation?latitude=-4.00000,-3.99000"))
    }

    @Test fun subeFotoEnMiCarpeta() = runBlocking {
        val b = FakeBackend().apply { actual = com.gacelas.data.remoto.UsuarioRemoto("u-1", "a@b.c") }
        val repo = SupabaseExplorarRepository(b) { "u-1" }
        repo.subirFoto("ruta-1", ByteArray(10), "image/png")
        val clave = b.archivos.keys.single()
        val yo = "u-1"
        assertTrue(clave.startsWith("route-photos/$yo/ruta-1/") && clave.endsWith(".png"))
        assertEquals(1, repo.fotos("ruta-1").size)
    }
}
