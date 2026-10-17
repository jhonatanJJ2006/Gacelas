package com.gacelas.shared

import com.gacelas.core.sensores.Rumbo
import kotlin.test.Test
import kotlin.test.assertEquals

/** Corre en JVM (Android) y en el simulador iOS (iosSimulatorArm64Test). */
class ComunTest {
    @Test fun rumbos() {
        assertEquals(270f, Rumbo.normalizar(-90f))
        assertEquals(-90f, Rumbo.diferencia(0f, 270f))
        assertEquals(270f, Rumbo.combinar(0f, 270f, 1.85)!!)
    }

    @Test fun deMatrizTelefonoVerticalAlOeste() {
        // Dispositivo vertical mirando al oeste: eje -Z = oeste (-1,0,0), eje Y = arriba.
        val r = floatArrayOf(0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
        assertEquals(270f, Rumbo.deMatriz(r), 0.5f)
    }

    @Test fun versionCodigo() = assertEquals(609, VersionGacelas.codigo("0.6.9"))
}
