package com.gacelas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.gacelas.BuildConfig
import com.gacelas.compartir.SITIO
import com.gacelas.ui.theme.GColors
import java.net.URLEncoder

/** Anchos que acepta Vercel Image Optimization (images.sizes en apps/web/vercel.json). */
private val ANCHOS_VERCEL = listOf(256, 640, 1080)

/** Fotos de demostracion (migracion 047): https://picsum.photos/seed/<semilla>/<ancho>/<alto>. */
private val PICSUM = Regex("""^https://picsum\.photos/seed/([^/?#]+)/(\d+)/(\d+)$""")

/**
 * Las fotos publicas de Supabase Storage se piden a Vercel Image Optimization: WebP al ancho pedido y
 * cache de 30 dias en el borde, en vez del original de varios MB. Las de demostracion de picsum se
 * piden ya a ese ancho y en WebP (picsum lo admite en la URL). Otras URLs no se tocan.
 */
fun urlFotoOptimizada(url: String, ancho: Int, supabase: String = BuildConfig.SUPABASE_URL, sitio: String = SITIO): String {
    val w = ANCHOS_VERCEL.firstOrNull { it >= ancho } ?: ANCHOS_VERCEL.last()
    PICSUM.matchEntire(url)?.destructured?.let { (semilla, a, h) ->
        val original = a.toInt()
        if (w >= original || original <= 0) return url
        return "https://picsum.photos/seed/$semilla/$w/${((h.toLong() * w + original / 2) / original).coerceAtLeast(1)}.webp"
    }
    if (supabase.isBlank() || !url.startsWith("${supabase.trimEnd('/')}/storage/v1/object/public/")) return url
    return "$sitio/_vercel/image?url=${URLEncoder.encode(url, "UTF-8")}&w=$w&q=70"
}

/**
 * Foto remota con cache de memoria y disco (Coil). Mientras carga late un esqueleto; si no carga, un
 * icono. Si la version optimizada falla (cuota de Vercel, sin despliegue), se pide la original.
 */
@Composable
fun FotoRemotaC2(url: String, modifier: Modifier = Modifier, descripcion: String? = null, ancho: Int = 640) {
    var fuente by remember(url, ancho) { mutableStateOf(urlFotoOptimizada(url, ancho)) }
    var estado by remember(fuente) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    Box(modifier.background(GColors.SurfaceHigh), contentAlignment = Alignment.Center) {
        AsyncImage(
            fuente, descripcion, Modifier.matchParentSize(), contentScale = ContentScale.Crop,
            onState = { s -> if (s is AsyncImagePainter.State.Error && fuente != url) fuente = url else estado = s },
        )
        when (estado) {
            is AsyncImagePainter.State.Success -> Unit
            is AsyncImagePainter.State.Error -> Icon(Icons.Outlined.Image, null, tint = GColors.C2Muted, modifier = Modifier.size(28.dp))
            else -> {
                val alfa by pulsoEsqueleto()
                Box(Modifier.matchParentSize().graphicsLayer { alpha = alfa }.background(GColors.C2S2).testTag("foto-cargando"))
            }
        }
    }
}
