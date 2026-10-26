package com.gacelas.acceso

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Imagenes de la app. Los archivos son los mismos en ambas plataformas
 * (shared/src/androidMain/res); iOS los recibe en su bundle desde project.yml.
 */
enum class ImagenGacelas(val archivo: String) {
    MapaBienvenida("mapa_stitch_bienvenida.webp"),
    MapaLogin("mapa_stitch_login.webp"),
    GoogleG("ic_google_g.xml"),
}

@Composable
expect fun imagenGacelas(imagen: ImagenGacelas): Painter
