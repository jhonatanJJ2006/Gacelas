package com.gacelas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
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
        modifier.size(48.dp).vidrioC2(RoundedCornerShape(16.dp))
            .clickable(onClickLabel = "Volver", role = Role.Button, onClick = onClick).testTag("boton-volver-mapa"),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White, modifier = Modifier.size(22.dp)) }
}

/** Boton lateral de reportar incidente (tablero): cuadro ambar con icono y la etiqueta REPORTAR debajo. */
@Composable
fun BotonReportarMapa(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(18.dp)
    androidx.compose.foundation.layout.Column(
        modifier.clickable(onClickLabel = "Reportar incidente", role = Role.Button, onClick = onClick).testTag("boton-reportar-incidente"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(58.dp).clip(forma).background(Tablero.AmbarFondo).border(2.dp, Color(0xFFFBBF24).copy(alpha = 0.7f), forma),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Warning, "Reportar incidente", tint = Color.White, modifier = Modifier.size(26.dp)) }
        androidx.compose.material3.Text("REPORTAR", style = com.gacelas.ui.theme.GType.labelMd.copy(fontSize = spAcotado(9.5f, 1.05f), fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
            color = Color(0xFFFBBF24), maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 3.dp))
    }
}
