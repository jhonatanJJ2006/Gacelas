package com.gacelas.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.gacelas.acceso.bytesDelBundle

/* Lee <nombre>_<peso>.ttf del bundle de la app (apps/ios/iosApp/iosApp/Fuentes). */
private fun familia(nombre: String, vararg pesos: FontWeight) = FontFamily(
    pesos.map { w -> Font(identity = "${nombre}_${w.weight}", getData = { bytesDelBundle("${nombre}_${w.weight}.ttf") }, weight = w) },
)

actual val Inter: FontFamily = familia("inter", FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

actual val RobotoFlex: FontFamily =
    familia("roboto_flex", FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.Black)

actual val SpaceGrotesk: FontFamily = familia("space_grotesk", FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
