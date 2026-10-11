package com.gacelas.ajustes

import com.gacelas.ui.components.EstadoDescarga
import org.junit.Assert.assertEquals
import org.junit.Test

class MapaOfflineTextoTest {
    @Test fun textosNoRompenElFormato() {
        assertEquals("Descargando 5% (1.3 MB)", textoEstadoMapa(EstadoDescarga.Descargando(5, 1.26)))
        assertEquals("Descargando 100% (12.0 MB)", textoEstadoMapa(EstadoDescarga.Descargando(100, 12.0)))
        assertEquals("Listo para usar sin red (8.5 MB)", textoEstadoMapa(EstadoDescarga.Lista(8.46)))
    }
}
