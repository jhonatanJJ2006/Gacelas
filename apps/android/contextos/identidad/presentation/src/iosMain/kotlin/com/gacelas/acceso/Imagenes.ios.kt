package com.gacelas.acceso

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal fun bytesDelBundle(archivo: String): ByteArray {
    val punto = archivo.lastIndexOf('.')
    val ruta = NSBundle.mainBundle.pathForResource(archivo.substring(0, punto), archivo.substring(punto + 1))
        ?: error("Falta $archivo en el bundle de la app")
    val data = NSData.dataWithContentsOfFile(ruta) ?: error("No se pudo leer $archivo")
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return bytes
}

/** Vector drawable de Android (solo <path> con fillColor/pathData), leido del mismo XML. */
private fun vectorAndroid(xml: String): ImageVector {
    fun attr(tag: String, nombre: String) = Regex("android:$nombre=\"([^\"]*)\"").find(tag)?.groupValues?.get(1)
    val raiz = Regex("<vector[^>]*>", RegexOption.DOT_MATCHES_ALL).find(xml)!!.value
    val b = ImageVector.Builder(
        defaultWidth = attr(raiz, "width")!!.removeSuffix("dp").toFloat().dp,
        defaultHeight = attr(raiz, "height")!!.removeSuffix("dp").toFloat().dp,
        viewportWidth = attr(raiz, "viewportWidth")!!.toFloat(),
        viewportHeight = attr(raiz, "viewportHeight")!!.toFloat(),
    )
    Regex("<path[^>]*/>", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { m ->
        val color = attr(m.value, "fillColor")!!.removePrefix("#").toLong(16)
        b.addPath(
            pathData = PathParser().parsePathString(attr(m.value, "pathData")!!).toNodes(),
            fill = SolidColor(Color(0xFF000000L or color)),
        )
    }
    return b.build()
}

@Composable
actual fun imagenGacelas(imagen: ImagenGacelas): Painter =
    if (imagen.archivo.endsWith(".xml")) {
        rememberVectorPainter(remember(imagen) { vectorAndroid(bytesDelBundle(imagen.archivo).decodeToString()) })
    } else {
        remember(imagen) { BitmapPainter(org.jetbrains.skia.Image.makeFromEncoded(bytesDelBundle(imagen.archivo)).toComposeImageBitmap()) }
    }
