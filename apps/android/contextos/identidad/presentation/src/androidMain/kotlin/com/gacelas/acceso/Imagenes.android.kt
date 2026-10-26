package com.gacelas.acceso

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.gacelas.identidad.presentation.R

@Composable
actual fun imagenGacelas(imagen: ImagenGacelas): Painter = painterResource(
    when (imagen) {
        ImagenGacelas.MapaBienvenida -> R.drawable.mapa_stitch_bienvenida
        ImagenGacelas.MapaLogin -> R.drawable.mapa_stitch_login
        ImagenGacelas.GoogleG -> R.drawable.ic_google_g
    },
)
