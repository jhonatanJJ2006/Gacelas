package com.gacelas.data

import com.gacelas.data.actualizacion.Aviso
import com.gacelas.data.actualizacion.InfoVersion
import com.gacelas.data.actualizacion.ReglasActualizacion
import com.gacelas.data.remoto.FechaIso
import com.gacelas.data.remoto.traducirError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DatosRemotoTest {
    @Test fun fechasIso() {
        val ms = FechaIso.leer("2026-10-10 10:00:00.123+00")!!
        assertEquals("2026-10-10T10:00:00Z", FechaIso.formatear(ms))
        assertEquals(ms, FechaIso.leer("2026-10-10T05:00:00-05"))
    }

    @Test fun actualizacionYErrores() {
        val info = InfoVersion(versionCode = 700, versionName = "0.7.0", apkUrl = "https://x/a.apk")
        assertIs<Aviso.Nueva>(ReglasActualizacion.aviso(609, info, null))
        assertTrue(ReglasActualizacion.sinCache("https://a/b.json", 5).contains("t=5"))
        assertEquals("Sin conexión con el servidor", traducirError("timeout"))
    }
}
