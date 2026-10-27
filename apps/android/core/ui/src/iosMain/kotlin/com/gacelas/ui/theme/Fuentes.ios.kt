package com.gacelas.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private fun bytesDelBundle(archivo: String): ByteArray {
    val punto = archivo.lastIndexOf('.')
    val ruta = NSBundle.mainBundle.pathForResource(archivo.substring(0, punto), archivo.substring(punto + 1))
        ?: error("Falta $archivo en el bundle de la app")
    val data = NSData.dataWithContentsOfFile(ruta) ?: error("No se pudo leer $archivo")
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return bytes
}

/*
 * Fuentes C2 leidas del bundle de la app (apps/ios/iosApp/iosApp/Fuentes), igual que Android:
 * Archivo (variable) para el cuerpo y Anton para los titulares.
 */
private fun familia(archivo: String, vararg pesos: FontWeight) = FontFamily(
    pesos.flatMap { w ->
        listOf(FontStyle.Normal, FontStyle.Italic).map { s ->
            Font(identity = "${archivo}_${w.weight}_${s}", getData = { bytesDelBundle("$archivo.ttf") }, weight = w, style = s)
        }
    },
)

private val pesosCuerpo = arrayOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black)

actual val Inter: FontFamily = familia("archivo_400", *pesosCuerpo)

actual val RobotoFlex: FontFamily = familia("archivo_400", *pesosCuerpo)

actual val SpaceGrotesk: FontFamily = familia("anton_400", FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
