package com.gacelas.shared

import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearHms
import com.gacelas.tracking.formatearRitmo
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
