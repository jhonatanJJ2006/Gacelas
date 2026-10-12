package com.gacelas.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.data.carreras.CarreraGuardada
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.ajustesState
import com.gacelas.ui.carrerasState
import com.gacelas.ui.components.BotonSecundario
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.MapaGacelas
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Tarjeta
import com.gacelas.correr.TrazadoRuta
import com.gacelas.ui.components.aLatLng
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Detalle de una carrera guardada: mapa del recorrido, metricas, parciales, notas y borrar. */
@Composable
fun DetalleCarreraScreen(id: String, onAtras: () -> Unit) {
    val carreras by carrerasState()
    val c: CarreraGuardada? = carreras.firstOrNull { it.id == id }
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val repo = contenedor().carreras
    val scope = rememberCoroutineScope()
    var confirmar by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary).testTag("detalle-carrera")) {
        EncabezadoAtras(c?.titulo ?: "Carrera", onAtras)
        if (c == null) {
            Text("Esta carrera ya no está.", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.padding(16.dp))
            return
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", Locale("es", "EC")).format(Date(c.inicioMs)), style = GType.bodyMd, color = GColors.TextSecondary)
            if (c.puntos.size >= 2) {
                MapaGacelas(c.puntos.aLatLng(), Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp))) {
                    TrazadoRuta(c.puntos, Modifier.fillMaxWidth().height(240.dp))
                }
            }
            Tarjeta {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Metrica(Unidades.texto(c.distanciaKm, m), Unidades.etiquetaDistancia(m), color = GColors.Primary500)
                    Metrica(formatearMs(c.segundos), "Tiempo")
                    Metrica(formatearRitmo(Unidades.ritmo(c.ritmoSegKm, m)), Unidades.etiquetaRitmo(m))
                    Metrica("${c.desnivelM} m", "Desnivel")
                }
            }
            if (c.parciales.isNotEmpty()) Tarjeta {
                Text("Parciales", style = GType.titleMd, color = GColors.TextPrimary)
                c.parciales.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("Km ${p.numero}", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                        Text(formatearMs(p.segundos), style = GType.labelLg, color = GColors.TextPrimary)
                    }
                }
            }
            if (c.nota.isNotBlank()) Tarjeta { Text(c.nota, style = GType.bodyMd, color = GColors.TextPrimary) }
            Text(if (c.publica) "Pública" else "Privada", style = GType.bodySm, color = GColors.TextSecondary)
            BotonSecundario("Eliminar carrera", { confirmar = true }, Modifier.fillMaxWidth(), color = GColors.Danger)
        }
    }
    if (confirmar) AlertDialog(
        onDismissRequest = { confirmar = false },
        title = { Text("¿Eliminar esta carrera?") },
        text = { Text("Se quita de tu historial y de tu cuenta. No se puede deshacer.") },
        confirmButton = { TextButton(onClick = { confirmar = false; scope.launch { repo.eliminar(id) }; onAtras() }) { Text("Eliminar", color = GColors.Danger) } },
        dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } },
    )
}
