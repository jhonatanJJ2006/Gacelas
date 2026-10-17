package com.gacelas.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Tarjeta "Mapa sin conexión" de Ajustes (compartida). [porcentaje] != null mientras descarga. */
@Composable
fun TarjetaMapaOffline(
    nombre: String,
    texto: String,
    lista: Boolean,
    porcentaje: Int?,
    disponible: Boolean,
    onDescargar: () -> Unit,
    onEliminar: () -> Unit,
) {
    Tarjeta(Modifier.testTag("mapa-offline")) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mapa sin conexión: $nombre", style = GType.bodyLg, color = GColors.TextPrimary)
                    Text(
                        texto,
                        style = GType.bodySm, color = GColors.TextSecondary,
                    )
                }
                when {
                    lista -> TextButton(onClick = onEliminar) { Text("Eliminar", color = GColors.Danger) }
                    porcentaje != null -> Unit
                    else -> TextButton(onClick = onDescargar, enabled = disponible) { Text("Descargar", color = GColors.Primary400) }
                }
            }
            porcentaje?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth()) }
        }
    }
}
