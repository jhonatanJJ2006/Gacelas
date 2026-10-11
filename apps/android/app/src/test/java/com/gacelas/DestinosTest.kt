package com.gacelas

import com.gacelas.navigation.Destinos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinosTest {
    @Test
    fun hay26PantallasUnicas() {
        assertEquals(26, Destinos.todas.size)
        assertEquals(Destinos.todas.size, Destinos.todas.toSet().size)
    }

    @Test
    fun rutasCoincidenConLaWeb() {
        val web = listOf(
            "bienvenida", "inicio-sesion", "registro", "recuperar-contrasena", "permisos-ubicacion",
            "inicio", "rutas", "comunidad", "comunidad/eventos", "perfil",
            "correr", "correr/en-curso", "correr/cronometro", "correr/resumen",
            "rutas/buscar", "rutas/navegacion", "comunidad/eventos/{id}", "comunidad/en-vivo", "comunidad/en-vivo/{id}", "rutas/detalle/{id}",
            "perfil/editar", "perfil/avatar", "ajustes", "ajustes/privacidad", "ajustes/notificaciones", "notificaciones",
        )
        assertEquals(web.toSet(), Destinos.todas.toSet())
    }

    @Test
    fun accesoQuedaFueraDeLaBarraInferior() {
        assertTrue(Destinos.acceso.none { it in Destinos.conBarraInferior })
        assertEquals("comunidad/eventos/7", Destinos.detalleEvento("7"))
    }
}
