package com.gacelas

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.gacelas.ui.three.RecolorAvatar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 0.9.21: la textura del avatar tenida se guarda en el telefono para no repetir el tenido en cada arranque. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RecolorAvatarCacheTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun texturaTenidaQuedaGuardadaEnDiscoYBorraVersionesViejas() {
        val colores = mapOf(RecolorAvatar.CAMISETA to 0xFF2563EB.toInt(), RecolorAvatar.PIEL to 0xFF8D5A3B.toInt())
        val archivo = RecolorAvatar.archivoCache(ctx, colores)
        val viejo = java.io.File(archivo.parentFile, "v1-2-ff000000.jpg").apply { parentFile!!.mkdirs(); writeText("x") }
        assertNotNull(RecolorAvatar.textura(ctx, colores))
        val limite = System.currentTimeMillis() + 10_000
        while (!archivo.exists() && System.currentTimeMillis() < limite) Thread.sleep(50)
        assertTrue("no se guardo ${archivo.name}", archivo.exists())
        assertTrue("debe borrar la cache de otra version", !viejo.exists())
        val leida = BitmapFactory.decodeFile(archivo.path)
        assertEquals(1024, leida.width)
        assertEquals(1024, leida.height)
        assertEquals(archivo, RecolorAvatar.archivoCache(ctx, colores.entries.reversed().associate { it.key to it.value }))
    }
}
