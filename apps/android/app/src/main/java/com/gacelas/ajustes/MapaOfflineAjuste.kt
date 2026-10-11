package com.gacelas.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.ui.components.EstadoDescarga
import com.gacelas.ui.components.MapaOffline
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Tarjeta de Ajustes para bajar el mapa de Loja y usarlo sin conexion. */
@Composable
fun MapaOfflineAjuste() {
    val context = LocalContext.current
    val estado by MapaOffline.estado.collectAsState()
    val disponible = Mapas.configurado()
    LaunchedEffect(disponible) { if (disponible) MapaOffline.consultar(context) }
    Tarjeta(Modifier.testTag("mapa-offline")) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mapa sin conexión: ${MapaOffline.LOJA.nombre}", style = GType.bodyLg, color = GColors.TextPrimary)
                    Text(
                        when (val e = estado) {
                            EstadoDescarga.SinDescargar -> "Descarga las calles de Loja (OpenStreetMap) para correr sin datos"
                            is EstadoDescarga.Descargando -> "Descargando ${e.porcentaje}% (%.1f MB)".format(e.megas)
                            is EstadoDescarga.Lista -> "Listo para usar sin red (%.1f MB)".format(e.megas)
                            is EstadoDescarga.Error -> e.mensaje
                        },
                        style = GType.bodySm, color = GColors.TextSecondary,
                    )
                }
                when (estado) {
                    is EstadoDescarga.Lista -> TextButton(onClick = { MapaOffline.eliminar(context) }) { Text("Eliminar", color = GColors.Danger) }
                    is EstadoDescarga.Descargando -> Unit
                    else -> TextButton(onClick = { MapaOffline.descargar(context) }, enabled = disponible) { Text("Descargar", color = GColors.Primary400) }
                }
            }
            (estado as? EstadoDescarga.Descargando)?.let { LinearProgressIndicator(progress = { it.porcentaje / 100f }, modifier = Modifier.fillMaxWidth()) }
        }
    }
}
