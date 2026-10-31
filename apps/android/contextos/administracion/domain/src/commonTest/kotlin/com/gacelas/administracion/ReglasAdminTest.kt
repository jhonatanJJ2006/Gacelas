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
}
