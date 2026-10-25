package com.gacelas.moderacion

import com.gacelas.moderacion.domain.MOTIVOS_GENERICOS
import com.gacelas.moderacion.domain.ObjetivoDenuncia
import com.gacelas.moderacion.domain.TipoContenido
import com.gacelas.moderacion.domain.motivosReporte
import com.gacelas.moderacion.domain.tablaDenuncia
import kotlin.test.Test
import kotlin.test.assertEquals

class DenunciaTest {
    @Test fun publicacionesSiguenEnPostReports() {
        assertEquals("post_reports", tablaDenuncia(TipoContenido.Post.objetivo("p1")))
        assertEquals("content_reports", tablaDenuncia(TipoContenido.Evento.objetivo("e1")))
    }

    @Test fun tipoNuevoUsaLaColaGenerica() {
        val o = ObjetivoDenuncia("ruta_cerrada", "r1")
        assertEquals("content_reports", tablaDenuncia(o))
        assertEquals(MOTIVOS_GENERICOS, motivosReporte(o.tipo))
        assertEquals("suplantacion", motivosReporte("usuario").first().first)
    }

    @Test fun incidentesEnLaCola() {
        val o = TipoContenido.Incidente.objetivo("i1")
        assertEquals("content_reports", tablaDenuncia(o))
        assertEquals("falso", motivosReporte("incidente").first().first)
    }
}
