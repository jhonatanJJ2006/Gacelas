package com.gacelas

import com.gacelas.ui.components.urlFotoOptimizada
import org.junit.Assert.assertEquals
import org.junit.Test

/** 0.9.21: las fotos de Supabase Storage pasan por Vercel Image Optimization; el resto no se toca. */
class FotoOptimizadaTest {
    private val supa = "https://abc.supabase.co"
    private val sitio = "https://gacelas.vercel.app"
    private val foto = "$supa/storage/v1/object/public/post-images/u1/a b.jpg"

    private fun opt(url: String, ancho: Int) = urlFotoOptimizada(url, ancho, supa, sitio)

    @Test fun fotoDeSupabaseVaAVercelConAnchoPermitido() {
        assertEquals("$sitio/_vercel/image?url=https%3A%2F%2Fabc.supabase.co%2Fstorage%2Fv1%2Fobject%2Fpublic%2Fpost-images%2Fu1%2Fa+b.jpg&w=640&q=70", opt(foto, 640))
        assertEquals(true, opt(foto, 100).contains("&w=256&"))
        assertEquals(true, opt(foto, 700).contains("&w=1080&"))
        assertEquals(true, opt(foto, 4000).contains("&w=1080&"))
    }

    @Test fun otrasUrlsYSinServidorQuedanIgual() {
        assertEquals("https://otra.com/x.jpg", opt("https://otra.com/x.jpg", 640))
        assertEquals("$supa/storage/v1/object/sign/privado.jpg", opt("$supa/storage/v1/object/sign/privado.jpg", 640))
        assertEquals(foto, urlFotoOptimizada(foto, 640, supabase = "", sitio = sitio))
    }
}
