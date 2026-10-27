package com.gacelas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors

/** Flecha de retroceso de las pantallas de mapa (arriba a la izquierda, el que llama pone los insets). */
@Composable
fun BotonVolverMapa(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(42.dp).clip(CircleShape).background(GColors.C2Card.copy(alpha = 0.95f)).border(1.dp, GColors.C2Linea, CircleShape)
            .clickable(onClickLabel = "Volver", role = Role.Button, onClick = onClick).testTag("boton-volver-mapa"),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White, modifier = Modifier.size(22.dp)) }
}

/** Boton lateral de reportar incidente: color de alerta C2 e icono de advertencia, para el borde derecho del mapa. */
@Composable
fun BotonReportarMapa(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(6.dp)
    Box(
        modifier.size(42.dp).clip(forma).background(GColors.C2Peligro).border(1.dp, Color(0xFFF472B6).copy(alpha = 0.6f), forma)
            .clickable(onClickLabel = "Reportar incidente", role = Role.Button, onClick = onClick).testTag("boton-reportar-incidente"),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.Warning, "Reportar incidente", tint = Color.White, modifier = Modifier.size(22.dp)) }
}
