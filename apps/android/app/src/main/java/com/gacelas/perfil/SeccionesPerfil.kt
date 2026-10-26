package com.gacelas.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gacelas.actividad.domain.reglas.Historico
import com.gacelas.actividad.domain.reglas.KmMes
import com.gacelas.actividad.domain.reglas.Medalla
import com.gacelas.actividad.domain.reglas.RecordPersonal
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearHms
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

@Composable
private fun Titulo(icono: ImageVector, texto: String, extra: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = GColors.Primary400, modifier = Modifier.size(20.dp))
        Text("  $texto", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
        extra?.let { Text(it, style = GType.labelMd, color = GColors.TextSecondary) }
    }
}

@Composable
fun Contadores(carreras: Int, seguidores: Int, siguiendo: Int) {
    Tarjeta(Modifier.testTag("contadores"), padding = 12.dp) {
        Row(Modifier.fillMaxWidth()) {
            listOf("Carreras" to carreras, "Seguidores" to seguidores, "Siguiendo" to siguiendo).forEach { (t, v) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$v", style = GType.headlineSm, color = GColors.TextPrimary)
                    Text(t, style = GType.labelMd, color = GColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun Celda(t: String, v: String, u: String, icono: ImageVector, modifier: Modifier) {
    Tarjeta(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t, style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
            Icon(icono, null, tint = GColors.Primary400, modifier = Modifier.size(18.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(v, style = GType.headlineSm, color = GColors.TextPrimary, maxLines = 1)
            if (u.isNotEmpty()) Text(" $u", style = GType.bodySm, color = GColors.Primary400)
        }
    }
}

@Composable
fun EstadisticasHistoricas(h: Historico, m: Boolean) {
    Column(Modifier.testTag("estadisticas-historicas"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Titulo(Icons.Filled.Route, "Estadísticas históricas", "${h.carreras} carreras")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Celda("Total distancia", Unidades.texto(h.km, m, 1), Unidades.etiquetaDistancia(m), Icons.Filled.Route, Modifier.weight(1f))
            Celda("Tiempo total", formatearHms(h.segundos), "", Icons.Filled.Timer, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Celda("Desnivel acum.", "+${h.desnivelM}", "m", Icons.Filled.Terrain, Modifier.weight(1f))
            Celda("Ritmo récord", formatearRitmo(Unidades.ritmo(h.mejorRitmoSegKm, m)), Unidades.etiquetaRitmo(m), Icons.Filled.Bolt, Modifier.weight(1f))
        }
    }
}

@Composable
fun KilometrajeMensual(meses: List<KmMes>, m: Boolean) {
    val max = (meses.maxOfOrNull { it.km } ?: 0.0).coerceAtLeast(1.0)
    val actual = meses.lastOrNull()
    Tarjeta(Modifier.testTag("km-mensual")) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Kilometraje mensual", style = GType.titleMd, color = GColors.TextPrimary)
                Text("${meses.firstOrNull()?.etiqueta} a ${actual?.etiqueta} ${actual?.anio}", style = GType.bodySm, color = GColors.TextSecondary)
            }
            actual?.let {
                Text(
                    "${it.etiqueta}: ${Unidades.texto(it.km, m, 1)} ${Unidades.etiquetaDistancia(m)}",
                    style = GType.labelMd, color = GColors.Primary400,
                    modifier = Modifier.clip(CircleShape).background(GColors.Primary.copy(alpha = 0.3f)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().height(128.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            meses.forEachIndexed { i, x ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    val f = (x.km / max).toFloat().coerceIn(0.04f, 1f)
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.fillMaxWidth().fillMaxHeight(f).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(if (i == meses.lastIndex) Brush.verticalGradient(listOf(GColors.Primary400, GColors.Primary700)) else Brush.verticalGradient(listOf(GColors.SurfaceHighest, GColors.SurfaceHighest))),
                        )
                    }
                    Text(x.etiqueta, style = GType.bodySm, color = GColors.TextSecondary)
                }
            }
        }
    }
}

private fun tiempoRecord(s: Int?) = when {
    s == null -> "--"
    s >= 3600 -> formatearHms(s)
    else -> formatearMs(s)
}

@Composable
fun Records(lista: List<RecordPersonal>) {
    Column(Modifier.testTag("records"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Titulo(Icons.Filled.EmojiEvents, "Récords personales")
        lista.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { r ->
                    Tarjeta(Modifier.weight(1f)) {
                        Text(r.etiqueta, style = GType.labelMd, color = GColors.Primary400, modifier = Modifier.clip(CircleShape).background(GColors.Primary.copy(alpha = 0.3f)).padding(horizontal = 8.dp, vertical = 2.dp))
                        Text(tiempoRecord(r.segundos), style = GType.headlineSm, color = GColors.TextPrimary)
                        Text(if (r.segundos == null) "Aún sin marca" else "Mejor tiempo", style = GType.bodySm, color = GColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun Medallero(lista: List<Medalla>) {
    Column(Modifier.testTag("medallero"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Titulo(Icons.Filled.MilitaryTech, "Medallero", "${lista.count { it.lograda }} de ${lista.size}")
        lista.chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { x ->
                    Tarjeta(Modifier.weight(1f).alpha(if (x.lograda) 1f else 0.4f), padding = 10.dp) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(if (x.lograda) GColors.Primary.copy(alpha = 0.3f) else GColors.SurfaceHigh)
                                    .border(2.dp, if (x.lograda) GColors.Primary400 else GColors.BorderSubtle, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(if (x.lograda) Icons.Filled.WorkspacePremium else Icons.Filled.Lock, null, tint = if (x.lograda) GColors.Primary400 else GColors.TextSecondary)
                            }
                            Text(x.titulo, style = GType.labelMd, color = GColors.TextPrimary, textAlign = TextAlign.Center)
                            Text(x.descripcion, style = GType.bodySm, color = GColors.TextSecondary, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
