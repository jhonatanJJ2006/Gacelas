package com.gacelas.administracion

import com.gacelas.administracion.domain.AjusteApp
import com.gacelas.administracion.domain.CLAVE_SLA
import com.gacelas.administracion.domain.DiaEstadistica
import com.gacelas.administracion.domain.EstadisticasAdmin
import com.gacelas.administracion.domain.EstadoReporte
import com.gacelas.administracion.domain.EstadoSla
import com.gacelas.administracion.domain.Prioridad
import com.gacelas.administracion.domain.PuntoGeo
import com.gacelas.administracion.domain.RecuadroLoja
import com.gacelas.administracion.domain.ReporteAdmin
import com.gacelas.administracion.domain.SlaHoras
import com.gacelas.administracion.domain.csvEstadisticas
import com.gacelas.administracion.domain.estadoSla
import com.gacelas.administracion.domain.megas
import com.gacelas.administracion.domain.nombreCsvEstadisticas
import com.gacelas.administracion.domain.ordenarPorSla
import com.gacelas.administracion.domain.prioridadPorMotivo
import com.gacelas.administracion.domain.respuestaValida
import com.gacelas.administracion.domain.texto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OperacionAdminTest {
    private val hora = 3_600_000L

    @Test
    fun prioridadSegunMotivoIgualQueElServidor() {
        assertEquals(Prioridad.Alta, prioridadPorMotivo("acoso"))
        assertEquals(Prioridad.Alta, prioridadPorMotivo("peligroso"))
        assertEquals(Prioridad.Alta, prioridadPorMotivo("suplantacion"))
        assertEquals(Prioridad.Baja, prioridadPorMotivo("spam"))
        assertEquals(Prioridad.Media, prioridadPorMotivo("ofensivo"))
        assertEquals(Prioridad.Media, Prioridad.desde("desconocida"))
    }

    @Test
    fun plazoVencidoPorVencerYEnPlazo() {
        val creado = 0L
        val vence = 24 * hora
        assertIs<EstadoSla.EnPlazo>(estadoSla(creado, vence, 2 * hora, abierto = true))
        // Queda menos de una cuarta parte del plazo (6 h de 24).
        val porVencer = estadoSla(creado, vence, 19 * hora, abierto = true)
        assertIs<EstadoSla.PorVencer>(porVencer)
        assertEquals("Vence en 5 h", porVencer.texto())
        val vencido = estadoSla(creado, vence, 27 * hora, abierto = true)
        assertIs<EstadoSla.Vencido>(vencido)
        assertEquals("Vencido hace 3 h", vencido.texto())
        assertNull(estadoSla(creado, vence, 27 * hora, abierto = false))
        assertNull(estadoSla(creado, null, 0, abierto = true))
    }

    @Test
    fun plazoCortoAvisaConUnaHoraDeMargen() {
        assertIs<EstadoSla.PorVencer>(estadoSla(0, 4 * hora, 3 * hora + 1, abierto = true))
        assertEquals("Vence en 45 min", estadoSla(0, 4 * hora, 4 * hora - 45 * 60_000, abierto = true)!!.texto())
    }

    private fun caso(id: String, estado: EstadoReporte, vence: String?, p: Prioridad) =
        ReporteAdmin(id, "post", id, "otro", "", estado, "2026-10-01T00:00:00Z", "", "", 1, emptyList(), p, vence)

    @Test
    fun colaOrdenadaPorVencimientoYPrioridad() {
        val lista = listOf(
            caso("cerrado", EstadoReporte.Resuelto, "2026-10-01T01:00:00Z", Prioridad.Alta),
            caso("tarde", EstadoReporte.Pendiente, "2026-10-03T00:00:00Z", Prioridad.Baja),
            caso("pronto-media", EstadoReporte.Pendiente, "2026-10-01T04:00:00Z", Prioridad.Media),
            caso("pronto-alta", EstadoReporte.EnRevision, "2026-10-01T04:00:00Z", Prioridad.Alta),
            caso("sin-plazo", EstadoReporte.Pendiente, null, Prioridad.Alta),
        )
        assertEquals(listOf("pronto-alta", "pronto-media", "tarde", "sin-plazo", "cerrado"), lista.ordenarPorSla().map { it.id })
    }

    @Test
    fun plazosDesdeAjustes() {
        val ajuste = AjusteApp(CLAVE_SLA, JsonObject(mapOf("alta" to JsonPrimitive(2), "media" to JsonPrimitive(12), "baja" to JsonPrimitive(48))))
        assertEquals(SlaHoras(2, 12, 48), SlaHoras.desde(listOf(ajuste)))
        assertEquals(SlaHoras(4, 24, 72), SlaHoras.desde(emptyList()))
        assertEquals(SlaHoras(4, 24, 72), SlaHoras.desde(listOf(AjusteApp(CLAVE_SLA, JsonPrimitive("x")))))
        assertEquals(12, SlaHoras(2, 12, 48).horas(Prioridad.Media))
        assertEquals(false, SlaHoras(0, 12, 48).valido)
    }

    @Test
    fun proyeccionEnElRecuadroDeLoja() {
        val centro = assertNotNull(RecuadroLoja.proyectar(PuntoGeo(-3.99, -79.205)))
        assertEquals(0.5f, centro.first, 0.01f)
        assertEquals(0.5f, centro.second, 0.01f)
        assertNull(RecuadroLoja.proyectar(PuntoGeo(-0.18, -78.47)))
    }

    @Test
    fun csvIgualAlDeLaWeb() {
        val e = EstadisticasAdmin(
            "2026-10-30", "2026-10-31", emptyMap(),
            listOf(DiaEstadistica("2026-10-30", 4.0, 12.0, 15.0, 81.5), DiaEstadistica("2026-10-31", 0.0, 0.0, 0.0, 0.0)),
        )
        assertEquals(
            "\uFEFFDía;Usuarios nuevos;Corredores activos;Carreras;Km\r\n2026-10-30;4;12;15;81,5\r\n2026-10-31;0;0;0;0\r\n",
            csvEstadisticas(e),
        )
        assertEquals("gacelas-estadisticas-2026-10-30-a-2026-10-31.csv", nombreCsvEstadisticas(e))
    }

    @Test
    fun tamanoEnMegas() {
        assertEquals("39,9 MB", megas(39_876_927))
        assertEquals("52,4 MB", megas(52_400_000))
        assertEquals("-", megas(null))
    }

    @Test
    fun validacionDeRespuestasRapidas() {
        assertEquals("Escribe un título", respuestaValida(" ", "texto"))
        assertEquals("Escribe el texto de la respuesta", respuestaValida("Título", ""))
        assertEquals("El título admite hasta 60 caracteres", respuestaValida("x".repeat(61), "texto"))
        assertNull(respuestaValida("Contenido retirado", "Gracias por avisarnos."))
    }
}
