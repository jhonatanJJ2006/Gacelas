package com.gacelas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Texto del aviso: "1 carrera sin subir" / "3 carreras sin subir". */
fun textoPendientes(n: Int) = if (n == 1) "1 carrera sin subir" else "$n carreras sin subir"

/**
 * Aviso C2 de carreras grabadas sin conexion que aun no estan en el servidor (ticket 86e3u5y01).
 * Con 0 no se dibuja; al sincronizar desaparece solo. Tocar "Subir ahora" reintenta la subida.
 */
@Composable
fun AvisoSincronizacionC2(pendientes: Int, subiendo: Boolean, onSubir: () -> Unit, modifier: Modifier = Modifier) {
    if (pendientes <= 0) return
    Row(
        modifier.fillMaxWidth().clip(FormaC2).background(Color(0xFF2A1F0A)).border(1.dp, Color(0xFFB7791F), FormaC2)
            .padding(horizontal = 14.dp, vertical = 12.dp).testTag("aviso-pendientes"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.CloudUpload, null, tint = Color(0xFFF6AD55), modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(textoPendientes(pendientes), style = GType.labelLg, color = GColors.TextPrimary)
            Text(
                if (subiendo) "Subiendo..." else "Se guardaron en el celular. Se suben solas cuando haya conexión.",
                style = GType.bodySm, color = GColors.C2Muted,
            )
        }
        Text(
            "SUBIR AHORA",
            style = GType.labelMd, color = if (subiendo) GColors.C2Muted else Color(0xFFF6AD55),
            modifier = Modifier.clip(FormaBotonC2).border(1.dp, Color(0xFFB7791F), FormaBotonC2)
                .clickable(enabled = !subiendo, role = Role.Button, onClickLabel = "Subir carreras pendientes", onClick = onSubir)
                .padding(horizontal = 10.dp, vertical = 8.dp).testTag("subir-pendientes"),
        )
    }
}
