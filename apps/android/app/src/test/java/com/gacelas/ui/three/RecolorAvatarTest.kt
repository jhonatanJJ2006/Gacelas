package com.gacelas.ui.three

import com.gacelas.identidad.domain.modelo.ConfigAvatar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecolorAvatarTest {
    @Test fun soloTineLasZonasElegidasYConservaLaSombra() {
        val gris = 0xFF808080.toInt(); val oscuro = 0xFF404040.toInt()
        val px = intArrayOf(gris, oscuro, gris, gris)
        val mascara = byteArrayOf(RecolorAvatar.CAMISETA.toByte(), RecolorAvatar.CAMISETA.toByte(), RecolorAvatar.PIEL.toByte(), 0)
        RecolorAvatar.tenir(px, mascara, mapOf(RecolorAvatar.CAMISETA to 0xFF00FF00.toInt()))
        // Camiseta en verde: el texel claro queda mas claro que el oscuro.
        assertEquals(0, (px[0] shr 16) and 0xFF)
        assertTrue(((px[0] shr 8) and 0xFF) > ((px[1] shr 8) and 0xFF))
        // Piel y zonas sin mascara no cambian.
        assertEquals(gris, px[2]); assertEquals(gris, px[3])
    }

    @Test fun configPorDefectoEsElModeloOriginal() {
        assertEquals(emptyMap<Int, Int>(), RecolorAvatar.colores(ConfigAvatar()))
        val c = RecolorAvatar.colores(ConfigAvatar(equipacion = 2, tonoPiel = 3, colorShorts = 0, colorZapatillas = 2))
        assertEquals(setOf(RecolorAvatar.CAMISETA, RecolorAvatar.PIEL, RecolorAvatar.SHORTS, RecolorAvatar.ZAPATILLAS), c.keys)
        assertEquals(ClipCorredor.Run, RecolorAvatar.clip(ConfigAvatar(estilo = "Veloz"), ClipCorredor.Idle))
        assertEquals(ClipCorredor.Idle, RecolorAvatar.clip(null, ClipCorredor.Idle))
    }
}
