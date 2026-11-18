package com.gacelas.inicio

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.clima.domain.DiaClima
import com.gacelas.clima.domain.ReglasClima
import com.gacelas.rutas.colorDificultad
import com.gacelas.rutas.domain.modelo.RutasLoja
import com.gacelas.rutas.domain.reglas.CondicionClima
import com.gacelas.rutas.domain.reglas.RutasClima
import com.gacelas.tracking.Unidades
import com.gacelas.ui.ajustesState
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.EstadoVacioC2
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.components.TituloSeccion
import com.gacelas.ui.contenedor
import com.gacelas.ui.movimiento.Mov
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

private fun icono(codigo: Int): ImageVector = when (ReglasClima.tipo(codigo)) {
    "sol" -> Icons.Filled.WbSunny
    "nube" -> Icons.Filled.Cloud
    "lluvia" -> Icons.Filled.Umbrella
    else -> Icons.Filled.Thunderstorm
}

private val SEMANA = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/** "Hoy", "Ayer", "Mañana" o el dia de la semana con su numero. */
private fun nombreDia(fecha: String, hoy: kotlinx.datetime.LocalDate): String {
    val d = runCatching { kotlinx.datetime.LocalDate.parse(fecha) }.getOrNull() ?: return fecha
    return when (d.toEpochDays() - hoy.toEpochDays()) {
        0 -> "Hoy"
        -1 -> "Ayer"
        1 -> "Mañana"
        else -> "${SEMANA[d.dayOfWeek.ordinal]} ${d.dayOfMonth}"
    }
}

/**
 * Clima por dias como la app del tiempo: ahora, las proximas horas y 7 dias atras y adelante.
 * Al elegir un dia de hoy en adelante recomienda rutas: con lluvia nada de montana ni senderos.
 */
@Composable
fun ClimaScreen(onAtras: () -> Unit, onRuta: (String) -> Unit) {
    val c = contenedor()
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { c.climaInicioViewModel { System.currentTimeMillis() } }
    val clima by vm.clima.collectAsState()
    LaunchedEffect(Unit) { vm.refrescar() }
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val remotas by (c.explorar?.rutas ?: remember { kotlinx.coroutines.flow.MutableStateFlow(null) }).collectAsState()
    val rutas = remember(remotas) { RutasLoja.conOficiales(remotas.orEmpty()) }
    val hoy = remember { com.gacelas.shared.Fechas.fecha(System.currentTimeMillis()).let { kotlinx.datetime.LocalDate(it.year, it.monthNumber, it.dayOfMonth) } }
    val dias = clima?.dias.orEmpty()
    var elegido by rememberSaveable { mutableStateOf(hoy.toString()) }
    val dia = dias.firstOrNull { it.fecha == elegido } ?: dias.firstOrNull { it.fecha == hoy.toString() }

    androidx.compose.foundation.layout.Column(Modifier.background(com.gacelas.ui.components.Tablero.Fondo)) {
        EncabezadoAtras("Clima", onAtras)
        Pantalla(padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            val actual = clima
            if (actual == null) {
                EstadoVacioC2(Icons.Filled.CloudOff, "Sin datos del clima", "Conéctate a internet para ver el pronóstico de tu zona.", tag = "clima-vacio")
                return@Pantalla
            }
            // Ahora: temperatura grande, descripcion y lugar.
            Tarjeta(modifier = Modifier.testTag("clima-ahora"), fondo = GColors.C2Card) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(actual.lugar, style = GType.labelLg, color = GColors.C2Muted)
                        Text("${actual.temperatura}°", style = GType.metricHuge.copy(fontSize = androidx.compose.ui.unit.TextUnit(64f, androidx.compose.ui.unit.TextUnitType.Sp)), color = GColors.TextPrimary)
                        Text(actual.descripcion.replaceFirstChar { it.uppercase() }, style = GType.bodyLg, color = GColors.TextPrimary)
                        dias.firstOrNull { it.fecha == hoy.toString() }?.let {
                            Text("Máx. ${it.maxima}° · Mín. ${it.minima}°", style = GType.bodySm, color = GColors.C2Muted)
                        }
                    }
                    Icon(icono(actual.horas.firstOrNull()?.codigoWmo ?: dia?.codigoWmo ?: 3), null, tint = GColors.C2Cielo, modifier = Modifier.size(84.dp))
                }
                if (actual.horas.isNotEmpty()) Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("clima-horas"),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    val mejor = ReglasClima.mejorHora(actual.horas)
                    actual.horas.forEach { h ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${h.hora}h", style = GType.labelMd, color = GColors.C2Muted)
                            Icon(icono(h.codigoWmo), null, tint = if (h == mejor) androidx.compose.ui.graphics.Color(0xFFFBBF24) else GColors.C2Cielo, modifier = Modifier.size(20.dp))
                            Text("${h.temperatura}°", style = GType.labelLg, color = GColors.TextPrimary)
                        }
                    }
                }
            }
            if (dias.isEmpty()) {
                Text("El pronóstico por días aparece en la próxima actualización.", style = GType.bodySm, color = GColors.C2Muted)
                return@Pantalla
            }
            // Recomendacion para el dia elegido (hoy en adelante).
            val futuro = dia != null && dia.fecha >= hoy.toString()
            if (dia != null && futuro) {
                val cond = CondicionClima(dia.lluvioso, dia.caluroso, dia.ventoso)
                val recomendadas = remember(dia, rutas) { RutasClima.recomendar(cond, rutas) }
                Tarjeta(modifier = Modifier.testTag("clima-recomendacion"), borde = if (dia.lluvioso) GColors.C2Cielo else GColors.C2Menta) {
                    Text("Rutas para ${nombreDia(dia.fecha, hoy).lowercase()}", style = GType.titleMd, color = GColors.TextPrimary)
                    Text(RutasClima.motivo(cond), style = GType.bodySm, color = GColors.C2Muted)
                    recomendadas.forEach { r ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onRuta(r.id) }.padding(vertical = 8.dp).testTag("clima-ruta-${r.id}"),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(colorDificultad(r.dificultad)))
                            Text(r.nombre, style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${Unidades.texto(r.distanciaKm, m, 1).replace('.', ',')} ${Unidades.etiquetaDistancia(m)} · +${r.elevacionM} m", style = GType.labelMd, color = GColors.C2Muted)
                        }
                    }
                }
            } else if (dia != null) Text("Así estuvo el clima ese día. Elige hoy o un día próximo para ver rutas recomendadas.", style = GType.bodySm, color = GColors.C2Muted)

            val minimo = dias.minOf { it.minima }
            val maximo = dias.maxOf { it.maxima }
            val (pasados, proximos) = dias.partition { it.fecha < hoy.toString() }
            TituloSeccion("Próximos días")
            Tarjeta(padding = 6.dp) { proximos.forEach { d -> FilaDiaClima(d, nombreDia(d.fecha, hoy), d.fecha == dia?.fecha, minimo, maximo) { elegido = d.fecha } } }
            if (pasados.isNotEmpty()) {
                TituloSeccion("Días anteriores")
                Tarjeta(padding = 6.dp) { pasados.reversed().forEach { d -> FilaDiaClima(d, nombreDia(d.fecha, hoy), d.fecha == dia?.fecha, minimo, maximo, pasado = true) { elegido = d.fecha } } }
            }
            Text("Datos de Open-Meteo (CC BY 4.0).", style = GType.labelMd, color = GColors.C2Muted)
        }
    }
}

/** Fila de un dia: nombre, icono, lluvia, minima, barra de rango y maxima (como la app del tiempo). */
@Composable
private fun FilaDiaClima(d: DiaClima, nombre: String, elegido: Boolean, minimo: Int, maximo: Int, pasado: Boolean = false, onElegir: () -> Unit) {
    val fondo by animateColorAsState(if (elegido) GColors.C2Violeta.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent, Mov.corto(), label = "dia-clima")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(fondo).clickable(onClick = onElegir)
            .then(if (pasado && !elegido) Modifier.alpha(0.7f) else Modifier)
            .padding(horizontal = 10.dp, vertical = 10.dp).testTag("clima-dia-${d.fecha}"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(nombre, style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.width(96.dp), maxLines = 1)
        Icon(icono(d.codigoWmo), null, tint = if (d.lluvioso) GColors.C2Cielo else androidx.compose.ui.graphics.Color(0xFFFBBF24), modifier = Modifier.size(22.dp))
        Row(Modifier.width(48.dp), verticalAlignment = Alignment.CenterVertically) {
            if (d.probLluvia > 0) {
                Icon(Icons.Filled.WaterDrop, null, tint = GColors.C2Cielo, modifier = Modifier.size(12.dp))
                Text("${d.probLluvia}%", style = GType.labelMd, color = GColors.C2Cielo)
            }
        }
        Text("${d.minima}°", style = GType.labelLg, color = GColors.C2Muted)
        // Barra del rango del dia sobre el rango de toda la lista.
        val total = (maximo - minimo).coerceAtLeast(1).toFloat()
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(GColors.C2S2)) {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
                val ini = (d.minima - minimo) / total
                val ancho = ((d.maxima - d.minima) / total).coerceAtLeast(0.05f)
                if (ini > 0f) Box(Modifier.weight(ini))
                Box(Modifier.weight(ancho).height(6.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(GColors.C2Cielo, androidx.compose.ui.graphics.Color(0xFFFBBF24)))))
                val resto = 1f - ini - ancho
                if (resto > 0f) Box(Modifier.weight(resto))
            }
        }
        Text("${d.maxima}°", style = GType.labelLg, color = GColors.TextPrimary)
        if (d.ventoso) Icon(Icons.Filled.Air, "Viento fuerte", tint = GColors.C2Muted, modifier = Modifier.size(16.dp))
    }
}
