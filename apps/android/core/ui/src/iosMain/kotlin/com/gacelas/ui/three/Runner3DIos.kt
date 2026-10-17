@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.gacelas.ui.three

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo
import platform.Foundation.NSData
import platform.posix.memcpy

/**
 * Corredor 3D en iOS: el mismo GLB que Android (assets/models/gacelas-runner-animated.glb), los mismos
 * clips (Idle/Jog/Run), escala (1.8 unidades), altura por clip y camara que Runner3D (SceneView).
 * Swift lo renderiza con SceneKit + GLTFKit2 fuera de pantalla (iosApp/Corredor3D.swift) y aqui se
 * dibuja cada cuadro como imagen de Compose, asi el fondo de la tarjeta se ve detras como en Android
 * (una vista UIKit quedaria debajo del lienzo de Compose y taparia el degradado).
 */
interface Corredor3DNativo {
    /** Cuadro RGBA (premultiplicado, ancho*alto*4 bytes) en el [tiempo] en segundos, girado [rotacionY] grados. */
    fun cuadro(tiempo: Double, rotacionY: Double, ancho: Int, alto: Int): NSData?
}

/** Fabrica nativa (Swift). Devuelve null si el modelo no carga. */
interface PuenteCorredor3D {
    fun crear(clip: String, alturaY: Double, camaraX: Double, camaraY: Double, camaraZ: Double): Corredor3DNativo?
}

object Corredor3DIos {
    var puente: PuenteCorredor3D? = null
        internal set
}

/** Llamado desde Swift al arrancar (como registrarMapaIos). */
fun registrarCorredor3DIos(puente: PuenteCorredor3D) { Corredor3DIos.puente = puente }

/** Giro de autoRotar en Android: 0.3 grados por cuadro de Filament (60 fps). */
private const val GRADOS_POR_SEGUNDO = 0.3 * 60

/** Equivalente de Runner3D (Android). Sin puente o si el modelo falla, [Runner3DFallback] como Android sin 3D. */
@Composable
fun Runner3DIos(
    clip: ClipCorredorIos,
    modifier: Modifier = Modifier,
    autoRotar: Boolean = false,
    camara: Triple<Double, Double, Double> = Triple(0.0, 1.2, 3.5),
    rumboRelativo: Float = 0f,
) {
    val nativo = remember(clip) { Corredor3DIos.puente?.crear(clip.nombre, clip.alturaY, camara.first, camara.second, camara.third) }
    if (nativo == null) { Runner3DFallback(modifier); return }
    var ancho by remember { mutableIntStateOf(0) }
    var alto by remember { mutableIntStateOf(0) }
    var imagen by remember { mutableStateOf<ImageBitmap?>(null) }
    val rumbo by rememberUpdatedState(rumboRelativo)
    LaunchedEffect(nativo, ancho, alto) {
        if (ancho <= 0 || alto <= 0) return@LaunchedEffect
        var inicio = -1L
        while (true) {
            withFrameNanos { t ->
                if (inicio < 0) inicio = t
                val s = (t - inicio) / 1e9
                val giro = if (autoRotar) (s * GRADOS_POR_SEGUNDO) % 360.0 else -rumbo.toDouble()
                nativo.cuadro(s, giro, ancho, alto)?.let { imagen = aImagen(it, ancho, alto) }
            }
        }
    }
    Box(modifier.fillMaxSize().onSizeChanged { ancho = it.width; alto = it.height }) {
        imagen?.let { Image(it, contentDescription = "Corredor Gacelas", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
    }
}

/** Mismos clips y alturas que ClipCorredor (Android). */
enum class ClipCorredorIos(val nombre: String, val alturaY: Double) { Idle("Idle", -1.8), Jog("Jog", -0.9), Run("Run", -0.9) }

private fun aImagen(datos: NSData, ancho: Int, alto: Int): ImageBitmap? {
    val n = ancho * alto * 4
    if (datos.length.toInt() < n) return null
    val bytes = ByteArray(n)
    bytes.usePinned { memcpy(it.addressOf(0), datos.bytes, n.toULong()) }
    val info = ImageInfo(ancho, alto, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
    return SkiaImage.makeRaster(info, bytes, ancho * 4).toComposeImageBitmap()
}
