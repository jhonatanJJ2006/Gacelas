package com.gacelas.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.core.sensores.Rumbo

/** Colores de la identidad C4 (mismos hex que acceso/AccesoVisual.kt de Android). */
object ColoresC4 {
    val Fondo = Color(0xFF0A0E16)
    val Tarjeta = Color(0xFF131926)
    val Secondary = Color(0xFFBCC7DE)
    val Naranja = Color(0xFF7B1FA2)
    val Magenta = Color(0xFFD6338A)
    val MagentaOscuro = Color(0xFFAB0660)
}

/**
 * Pantalla de arranque compartida (etapa 1 de iOS): fondo C4, marca Gacelas y tarjeta glass.
 * Usa logica comun real (Rumbo) para comprobar que el modulo compartido funciona en cada plataforma.
 */
@Composable
fun InicioComun(version: String = VersionGacelas.NOMBRE, onEmpezar: () -> Unit = {}) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ColoresC4.Fondo, Color(0xFF111827), ColoresC4.Fondo))).testTag("inicio-comun"),
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(ColoresC4.Naranja), contentAlignment = Alignment.Center) {
                    Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("GACELAS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("LOJA RUNNING CO.", color = ColoresC4.Secondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(ColoresC4.Tarjeta.copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(ColoresC4.Magenta))
                    Spacer(Modifier.width(6.dp))
                    Text("RUNNING EN LOJA, ECUADOR", color = ColoresC4.Magenta, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Corre. Comparte.\nSupera.", color = Color.White, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                Text("Gacelas para ${Plataforma.nombre}: versión en preparación.", color = ColoresC4.Secondary, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(ColoresC4.Naranja).clickable(onClick = onEmpezar).testTag("empezar"), contentAlignment = Alignment.Center) {
                    Text("Empezar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text(
                    "Versión $version (${VersionGacelas.codigo(version)}) · lógica compartida OK (rumbo ${Rumbo.normalizar(-90f).toInt()}°, ${com.gacelas.tracking.formatearHms(3723)}, ${com.gacelas.data.mock.MockData.rutas.size} rutas)",
                    color = ColoresC4.Secondary.copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.testTag("version-comun"),
                )
            }
        }
    }
}
