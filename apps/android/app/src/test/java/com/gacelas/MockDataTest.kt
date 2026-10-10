package com.gacelas

import com.gacelas.data.mock.MockData
import com.gacelas.data.mock.TipoNotificacion
import com.gacelas.notificaciones.filtrarNotificaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDataTest {
    @Test
    fun datosIgualesALaWeb() {
        assertEquals(3, MockData.rutas.size)
        assertEquals(listOf("Parque Jipiro", "Río Zamora", "Cerro Villonaco"), MockData.rutas.map { it.nombre })
        assertEquals(3, MockData.eventos.size)
        assertEquals(5, MockData.notificaciones.size)
        assertEquals(5, MockData.splits.size)
        assertEquals(1, MockData.splits.count { it.actual })
    }

    @Test
    fun actividadesApuntanAUsuariosExistentes() {
        MockData.actividades.forEach { assertNotNull(MockData.usuario(it.usuarioId)) }
        assertEquals("28:34", MockData.duracionTexto(1714))
    }

    @Test
    fun buscarEvento() {
        assertEquals("Trail Running Podocarpus", MockData.evento("2")?.titulo)
        assertEquals(null, MockData.evento("99"))
    }

    @Test
    fun filtrosDeNotificaciones() {
        val n = MockData.notificaciones
        assertEquals(5, filtrarNotificaciones(n, "Todas").size)
        assertEquals(2, filtrarNotificaciones(n, "Sin leer").size)
        assertTrue(filtrarNotificaciones(n, "En vivo").all { it.tipo == TipoNotificacion.EnVivo })
    }
}
