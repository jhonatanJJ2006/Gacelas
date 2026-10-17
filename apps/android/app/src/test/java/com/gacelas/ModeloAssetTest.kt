package com.gacelas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** El modelo 3D debe ser un GLB real, no un puntero de Git LFS (si no, Filament no lo carga). */
class ModeloAssetTest {
    @Test fun modeloEsGlbReal() {
        val f = listOf("src/main/assets/models/gacelas-runner-animated.glb", "app/src/main/assets/models/gacelas-runner-animated.glb")
            .map(::File).first { it.exists() }
        val cabecera = f.readBytes().copyOfRange(0, 4).toString(Charsets.US_ASCII)
        assertEquals("glTF", cabecera)
        assertTrue(f.length() > 100_000)
    }
}
