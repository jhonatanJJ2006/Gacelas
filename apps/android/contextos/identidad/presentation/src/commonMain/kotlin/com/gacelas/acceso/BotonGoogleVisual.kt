package com.gacelas.acceso

import com.gacelas.ui.theme.GColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.ui.theme.GType
import com.gacelas.ui.theme.Inter

/**
 * Aspecto del boton "Continuar con Google" (comun a Android e iOS). La accion la pone cada plataforma.
 * Guias de marca de Google Sign-In, tema oscuro: fondo #131314, borde #8E918F, texto #E3E3E3,
 * forma de pildora y el logo "G" multicolor oficial sin modificar.
 */
@Composable
fun BotonGoogleVisual(
    onClick: () -> Unit,
    cargando: Boolean,
    /** Estilo de la portada C4 (Stitch): fondo #1C2028, borde blanco 10%, Inter 15 medium. */
    portada: Boolean = false,
    alto: Dp = 52.dp,
) {
    Button(
        onClick = onClick,
        enabled = !cargando,
        colors = if (portada) ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1C2028), contentColor = GColors.OnPrimary,
            disabledContainerColor = Color(0xFF1C2028), disabledContentColor = GColors.TextPrimary.copy(alpha = 0.6f),
        ) else ButtonDefaults.buttonColors(
            containerColor = if (GColors.claro) Color.White else Color(0xFF131314), contentColor = if (GColors.claro) Color(0xFF1F1F1F) else Color(0xFFE3E3E3),
            disabledContainerColor = if (GColors.claro) Color.White else Color(0xFF131314), disabledContentColor = (if (GColors.claro) Color(0xFF1F1F1F) else Color(0xFFE3E3E3)).copy(alpha = 0.6f),
        ),
        border = BorderStroke(1.dp, if (portada) GColors.TextPrimary.copy(alpha = 0.10f) else if (GColors.claro) Color(0xFF747775) else Color(0xFF8E918F)),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().height(alto).testTag("boton-google"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (cargando) CircularProgressIndicator(Modifier.size(18.dp), color = if (GColors.claro) Color(0xFF1F1F1F) else Color(0xFFE3E3E3), strokeWidth = 2.dp)
                else Image(imagenGacelas(ImagenGacelas.GoogleG), contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(
                if (cargando) "Abriendo Google..." else "Continuar con Google",
                fontFamily = if (portada) Inter else GType.labelLg.fontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                letterSpacing = if (portada) (-0.075).sp else 0.25.sp,
            )
        }
    }
}
