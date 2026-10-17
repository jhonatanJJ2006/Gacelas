package com.gacelas.shared

import kotlin.test.Test
import kotlin.test.assertEquals

/** Utilidades puras de core:comun (JVM / simulador iOS). */
class ComunTest {
    @Test fun versionCodigo() = assertEquals(700, VersionGacelas.codigo("0.7.0"))
}
