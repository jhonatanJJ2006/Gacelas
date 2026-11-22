package com.gacelas.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.reglas.AnalisisCarrera
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.ajustesState
import com.gacelas.ui.carrerasState
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Dos carreras del mismo usuario: distancia, tiempo, ritmo y desnivel. */
@Composable
fun CompararCarrerasScreen(onAtras: () -> Unit) {
    val carreras = carrerasState().value.sortedByDescending { it.inicioMs }
    val m = ajustesState().value.unidadesMetricas
    var ida by remember(carreras) { mutableStateOf(carreras.getOrNull(0)?.id.orEmpty()) }
    var idb by remember(carreras) { mutableStateOf(carreras.getOrNull(1)?.id ?: carreras.getOrNull(0)?.id.orEmpty()) }
    Column(Modifier.fillMaxSize().testTag("comparar-carreras")) {
        EncabezadoAtras("Comparar carreras", onAtras)
        if (carreras.size < 2) {
            Text("Guarda al menos dos carreras para compararlas.", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.padding(16.dp))
            return
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectorCarrera("Carrera A", carreras, ida) { ida = it }
            SelectorCarrera("Carrera B", carreras, idb) { idb = it }
            val a = carreras.find { it.id == ida }
            val b = carreras.find { it.id == idb }
            if (a != null && b != null) TablaComparacion(a, b, m)
        }
    }
}

@Composable
private fun SelectorCarrera(etiqueta: String, carreras: List<Carrera>, id: String, onElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val actual = carreras.find { it.id == id }
    Tarjeta(Modifier.fillMaxWidth().testTag("selector-${etiqueta.last()}")) {
        Text(etiqueta, style = GType.labelMd, color = GColors.TextSecondary)
        Text(actual?.titulo ?: "Elige una carrera", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.fillMaxWidth())
        androidx.compose.material3.TextButton(onClick = { abierto = true }) { Text("Cambiar") }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            carreras.forEach { c ->
                DropdownMenuItem(text = { Text(c.titulo) }, onClick = { onElegir(c.id); abierto = false })
            }
        }
    }
}

@Composable
private fun TablaComparacion(a: Carrera, b: Carrera, m: Boolean) {
    val la = AnalisisCarrera.lado(a.id, a.titulo, a.distanciaKm, a.segundos, a.desnivelM)
    val lb = AnalisisCarrera.lado(b.id, b.titulo, b.distanciaKm, b.segundos, b.desnivelM)
    val filas = listOf(
        "Distancia" to ("${Unidades.texto(la.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}" to "${Unidades.texto(lb.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}"),
        "Tiempo" to (formatearMs(la.segundos) to formatearMs(lb.segundos)),
        "Ritmo" to ("${formatearRitmo(Unidades.ritmo(la.ritmoSegKm, m))} ${Unidades.etiquetaRitmo(m)}" to "${formatearRitmo(Unidades.ritmo(lb.ritmoSegKm, m))} ${Unidades.etiquetaRitmo(m)}"),
        "Desnivel" to ("+${la.desnivelM} m" to "+${lb.desnivelM} m"),
    )
    Tarjeta(Modifier.fillMaxWidth().testTag("tabla-comparacion")) {
        filas.forEach { (nombre, par) ->
            Text(nombre, style = GType.labelMd, color = GColors.TextSecondary)
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(par.first, style = GType.bodyMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                Text(par.second, style = GType.bodyMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            }
        }
    }
}
