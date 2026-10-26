package com.gacelas.seguridad.ui

import androidx.compose.ui.graphics.Color
import com.gacelas.seguridad.domain.IncidentePublico
import com.gacelas.seguridad.domain.NivelSeguridad
import com.gacelas.ui.theme.GColors

private val Ambar = Color(0xFFF5B54A)

fun colorNivel(n: NivelSeguridad) = when (n) {
    NivelSeguridad.Bajo -> GColors.C2Menta
    NivelSeguridad.Medio -> Ambar
    NivelSeguridad.Alto -> GColors.C2Peligro
}

fun textoNivel(n: NivelSeguridad) = when (n) {
    NivelSeguridad.Bajo -> "Bajo"
    NivelSeguridad.Medio -> "Medio"
    NivelSeguridad.Alto -> "Alto"
}

/** Color de un marcador de incidente en el mapa (verificados mas intensos). */
fun colorIncidente(i: IncidentePublico): Color = if (i.verificado) GColors.C2Peligro else Ambar
