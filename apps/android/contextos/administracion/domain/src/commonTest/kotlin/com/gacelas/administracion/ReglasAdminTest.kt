package com.gacelas.administracion

import com.gacelas.administracion.domain.AccesoAdmin
import com.gacelas.administracion.domain.AccionModeracion
import com.gacelas.administracion.domain.EstadoReporte
import com.gacelas.administracion.domain.ReporteAdmin
import com.gacelas.administracion.domain.RolAdmin
import com.gacelas.administracion.domain.SeccionAdmin
import com.gacelas.administracion.domain.UsuarioAdmin
import com.gacelas.administracion.domain.accionesPermitidas
import com.gacelas.administracion.domain.buscar
import com.gacelas.administracion.domain.etiquetaAccion
import com.gacelas.administracion.domain.idCorto
import com.gacelas.administracion.domain.siguienteCaso
import com.gacelas.administracion.domain.versionValida
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReglasAdminTest {
    private fun caso(estado: EstadoReporte, tipo: String = "post") =
        ReporteAdmin("r", tipo, "x", "spam", "", estado, "", "", "", 1, emptyList())

    @Test
    fun sinRolNoHayEntradaNiSecciones() {
        assertFalse(AccesoAdmin.Ninguno.puedeEntrar)
        assertTrue(AccesoAdmin.Ninguno.secciones.isEmpty())
    }

    @Test
    fun moderadorSoloVeModeracion() {
        assertEquals(listOf(SeccionAdmin.Moderacion), AccesoAdmin(RolAdmin.Moderador).secciones)
        assertEquals(SeccionAdmin.entries, AccesoAdmin(RolAdmin.Admin).secciones)
    }

    @Test
    fun accionesComoEnLaWeb() {
        assertEquals(
            listOf(AccionModeracion.EnRevision, AccionModeracion.OcultarContenido, AccionModeracion.Descartado),
            accionesPermitidas(caso(EstadoReporte.Pendiente), hayNota = false),
        )
        assertEquals(
            listOf(AccionModeracion.SuspenderUsuario, AccionModeracion.Descartado, AccionModeracion.Nota),
            accionesPermitidas(caso(EstadoReporte.EnRevision, "usuario"), hayNota = true),
        )
        assertEquals(listOf(AccionModeracion.Reabierto), accionesPermitidas(caso(EstadoReporte.Resuelto), false))
        assertEquals("Ocultar comentario", etiquetaAccion(AccionModeracion.OcultarContenido, "comentario"))
    }

    @Test
    fun buscaSinTildesNiMayusculas() {
        val u = listOf(
            UsuarioAdmin("1", "luiso", "Luis Ordóñez", "", null, null, 0, 0.0),
            UsuarioAdmin("2", "ana", "Ana", "", null, null, 0, 0.0),
        )
        assertEquals(listOf("1"), u.buscar("ORDONEZ").map { it.id })
        assertEquals(2, u.buscar("  ").size)
    }

    @Test
    fun validaVersion() {
        assertTrue(versionValida("1.2.3"))
        assertFalse(versionValida("1.2"))
        assertFalse(versionValida("v1.2.3"))
    }

    @Test
    fun siguienteCasoEsElPendienteMasAntiguo() {
        fun r(id: String, e: EstadoReporte, f: String) = ReporteAdmin(id, "post", "o", "spam", "", e, f, "", "", 1, emptyList())
        val lista = listOf(
            r("a", EstadoReporte.Pendiente, "2026-10-30T10:00:00Z"),
            r("b", EstadoReporte.EnRevision, "2026-10-01T10:00:00Z"),
            r("c", EstadoReporte.Pendiente, "2026-10-20T10:00:00Z"),
        )
        assertEquals("c", lista.siguienteCaso()?.id)
        assertEquals(null, lista.filter { it.estado != EstadoReporte.Pendiente }.siguienteCaso())
        assertEquals("R-AB12", idCorto("0000-ab12"))
    }
}
