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
    TarjetaMapaOffline(
        nombre = MapaOffline.LOJA.nombre, texto = textoEstadoMapa(estado), lista = estado is EstadoDescarga.Lista,
        porcentaje = (estado as? EstadoDescarga.Descargando)?.porcentaje, disponible = disponible,
        onDescargar = { MapaOffline.descargar(context) }, onEliminar = { MapaOffline.eliminar(context) },
    )
}

/** Texto del estado de la descarga. El porcentaje va fuera del patron de format: un "%" suelto lo rompia (crash en v0.6.x). */
internal fun textoEstadoMapa(e: EstadoDescarga): String {
    fun mb(m: Double) = String.format(java.util.Locale.US, "%.1f MB", m)
    return when (e) {
        EstadoDescarga.SinDescargar -> "Descarga las calles de Loja (OpenStreetMap) para correr sin datos"
        is EstadoDescarga.Descargando -> "Descargando ${e.porcentaje}% (${mb(e.megas)})"
        is EstadoDescarga.Lista -> "Mapa offline listo (${mb(e.megas)})"
        is EstadoDescarga.Error -> e.mensaje
    }
}
