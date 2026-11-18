package com.gacelas.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.gacelas.ui.theme.GColors

/** Foto remota con cache de memoria y disco (Coil). Mientras carga queda el fondo de superficie. */
@Composable
fun FotoRemotaC2(url: String, modifier: Modifier = Modifier, descripcion: String? = null) =
    AsyncImage(url, descripcion, modifier.background(GColors.SurfaceHigh), contentScale = ContentScale.Crop)
