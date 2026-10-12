package com.gacelas.shared

import com.gacelas.data.actualizacion.Aviso
import com.gacelas.data.actualizacion.InfoVersion
import com.gacelas.data.actualizacion.ReglasActualizacion
import com.gacelas.data.clubes.calendarioMes
import com.gacelas.data.remoto.FechaIso
import com.gacelas.data.remoto.traducirError
import com.gacelas.tracking.FaseCarrera
import com.gacelas.tracking.RunTracker
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearHms
import com.gacelas.tracking.formatearRitmo
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Se ejecuta en JVM (Android) y en el simulador iOS: misma logica en ambas plataformas. */
class LogicaComunTest {
    @Test fun formatos() {
        assertEquals("01:02:03", formatearHms(3723))
        assertEquals("5:07", formatearRitmo(307))
        assertEquals("3.14", "%.2f".fmt(3.14159))
        assertEquals("-0.50 km", "%.2f km".fmt(-0.5))
        assertEquals("0.05", Unidades.texto(0.05, true, 1))
        assertEquals("0f", "%02x".fmt(15))
    }

    @Test fun sha256Conocido() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex("abc"))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256Hex(""))
    }

    @Test fun fechasIso() {
        val ms = FechaIso.leer("2026-10-10 10:00:00.123+00")!!
        assertEquals("2026-10-10T10:00:00Z", FechaIso.formatear(ms))
        assertEquals(ms, FechaIso.leer("2026-10-10T05:00:00-05"))
    }

    @Test fun calendarioLunesADomingo() {
        val tz = TimeZone.of("America/Guayaquil")
        val semanas = calendarioMes(2026, 9, emptyList(), tz)
        assertEquals(31, semanas.flatten().count { it.delMes })
        assertEquals(0, Fechas.diaSemana(semanas[0][0].inicioMs, tz))
    }

    @Test fun cronometroConPausa() {
        var t = 0L
        val r = RunTracker(reloj = { t }, ahora = { 1_000L })
        r.iniciar(); t = 10_000; r.pausar(); t = 50_000; r.reanudar(); t = 65_000; r.actualizarTiempo()
        assertEquals(25, r.estado.value.segundos)
        assertEquals(FaseCarrera.Corriendo, r.estado.value.fase)
    }

    @Test fun actualizacionYErrores() {
        val info = InfoVersion(versionCode = 700, versionName = "0.7.0", apkUrl = "https://x/a.apk")
        assertIs<Aviso.Nueva>(ReglasActualizacion.aviso(609, info, null))
        assertTrue(ReglasActualizacion.sinCache("https://a/b.json", 5).contains("t=5"))
        assertEquals("Sin conexión con el servidor", traducirError("timeout"))
    }
}
