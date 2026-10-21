package com.gacelas.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gacelas.carreras.domain.reglas.ResumenPeriodo
import com.gacelas.carreras.domain.reglas.RetoMes
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.Publicacion
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearHms
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.components.AvatarCirculo
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.components.TituloSeccion
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

private val DIAS = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
private fun Barra(pct: Float, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(GColors.SurfaceHighest)) {
        Box(Modifier.fillMaxWidth(pct.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(color))
    }
}

@Composable
private fun Mosaico(icono: ImageVector, valor: String, etiqueta: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(GColors.SurfaceContainer).border(1.dp, GColors.BorderSubtle, RoundedCornerShape(16.dp)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, null, tint = GColors.Primary400, modifier = Modifier.size(18.dp))
        Text(valor, style = GType.titleMd, color = GColors.TextPrimary, maxLines = 1)
        Text(etiqueta, style = GType.bodySm, color = GColors.TextSecondary, textAlign = TextAlign.Center)
    }
}

/** "Tu semana atletica": meta semanal, tres metricas y dias corridos (Stitch, igual que la web). */
@Composable
fun SemanaAtletica(totales: ResumenPeriodo, dias: Set<Int>, hoy: Int, meta: Int, rango: String, m: Boolean) {
    val pct = (totales.carreras.toFloat() / meta.coerceAtLeast(1)).coerceAtMost(1f)
    Column(Modifier.testTag("semana-atletica"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Tu semana atlética", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(rango, style = GType.labelMd, color = GColors.TextSecondary)
        }
        Tarjeta {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Flag, null, tint = GColors.Secondary500, modifier = Modifier.size(18.dp))
                Text("  Meta semanal: $meta ${if (meta == 1) "carrera" else "carreras"}", style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                Text("${(pct * 100).toInt()}%", style = GType.labelMd, color = GColors.Secondary500)
            }
            Barra(pct, GColors.Secondary500)
            Row {
                Text("${Unidades.texto(totales.km, m, 1)} ${Unidades.etiquetaDistancia(m)} recorridos", style = GType.bodySm, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                Text("${totales.carreras} de $meta", style = GType.bodySm, color = GColors.TextSecondary)
            }
        }
        val ritmo = if (totales.km > 0.01) (totales.segundos / totales.km).toInt() else null
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mosaico(Icons.Filled.Schedule, formatearHms(totales.segundos), "Tiempo total", Modifier.weight(1f))
            Mosaico(Icons.Filled.Speed, formatearRitmo(Unidades.ritmo(ritmo, m)), "Ritmo (${Unidades.etiquetaRitmo(m)})", Modifier.weight(1f))
            Mosaico(Icons.Filled.Terrain, "+${totales.desnivelM} m", "Desnivel", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DIAS.forEachIndexed { i, dia ->
                val hecho = i in dias
                Column(
                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (hecho) GColors.Secondary.copy(alpha = 0.2f) else GColors.SurfaceContainer)
                        .border(if (hecho || i == hoy) 2.dp else 1.dp, if (hecho) GColors.Secondary else if (i == hoy) GColors.Primary else GColors.BorderSubtle, RoundedCornerShape(12.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(dia, style = GType.labelMd, color = if (hecho) GColors.Secondary500 else GColors.TextSecondary)
                    if (hecho) Icon(Icons.Filled.CheckCircle, null, tint = GColors.Secondary500, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun Chip(icono: ImageVector, texto: String, tinte: androidx.compose.ui.graphics.Color) {
    Row(Modifier.clip(CircleShape).background(GColors.SurfaceHigh).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = tinte, modifier = Modifier.size(14.dp))
        Text(" $texto", style = GType.labelMd, color = GColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Tarjeta del proximo encuentro con bloque de fecha, chips de hora y lugar e inscritos reales. */
@Composable
fun TarjetaEvento(evento: Evento, inscrito: Boolean, onClick: () -> Unit) {
    Tarjeta(Modifier.testTag("tarjeta-evento"), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Text(evento.titulo, style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            evento.inicioMs?.let { ms ->
                val (dia, mes) = com.gacelas.carreras.domain.reglas.EstadisticasPerfil.diaYMes(ms)
                Column(
                    Modifier.width(56.dp).clip(RoundedCornerShape(12.dp)).background(GColors.Primary.copy(alpha = 0.2f)).padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("$dia", style = GType.titleMd, color = GColors.TextPrimary)
                    Text(mes.uppercase(), style = GType.labelMd, color = GColors.Primary400)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(Icons.Filled.Schedule, evento.hora, GColors.Primary400)
            if (evento.ubicacion.isNotBlank()) Chip(Icons.Filled.LocationOn, evento.ubicacion, GColors.Secondary500)
        }
        Text("${evento.participantes + if (inscrito) 1 else 0} inscritos${if (inscrito) " (tú incluido)" else ""}", style = GType.labelMd, color = GColors.TextSecondary)
    }
}

@Composable
fun TarjetaReto(reto: RetoMes, m: Boolean) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(GColors.Primary700.copy(alpha = 0.45f), GColors.SurfaceContainer)))
            .border(1.dp, GColors.BorderSubtle, RoundedCornerShape(16.dp)).padding(16.dp).testTag("reto-mes"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row {
            Text("RETO DEL MES", style = GType.labelMd, color = GColors.Primary400, modifier = Modifier.weight(1f))
            Text("Termina en ${reto.diasRestantes} ${if (reto.diasRestantes == 1) "día" else "días"}", style = GType.labelMd, color = GColors.TextSecondary)
        }
        Text(reto.titulo, style = GType.headlineSm, color = GColors.TextPrimary)
        Barra((reto.km / reto.metaKm).toFloat(), GColors.Primary400)
        Row {
            Text("${Unidades.texto(reto.km, m, 1)} ${Unidades.etiquetaDistancia(m)}", style = GType.bodySm, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("Meta ${Unidades.texto(reto.metaKm, m, 0)} ${Unidades.etiquetaDistancia(m)}", style = GType.bodySm, color = GColors.TextSecondary)
        }
    }
}

@Composable
fun AmigosEnRuta(lista: List<Publicacion>, m: Boolean, onVerTodos: () -> Unit) {
    Column(Modifier.testTag("amigos-en-ruta"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSeccion("Amigos en ruta", accion = "Ver todos", onAccion = onVerTodos)
        if (lista.isEmpty()) {
            Tarjeta { Text("Sigue a corredores en Comunidad para ver aquí su actividad.", style = GType.bodyMd, color = GColors.TextSecondary) }
        }
        lista.forEach { p ->
            Tarjeta(padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarCirculo(40.dp, p.indice)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.autor, style = GType.labelLg, color = GColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${Unidades.texto(p.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)} en ${p.titulo}", style = GType.bodySm, color = GColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(p.cuando, style = GType.bodySm, color = GColors.TextSecondary)
                    }
                    Row(Modifier.clip(CircleShape).background(GColors.SurfaceHigh).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Favorite, null, tint = GColors.Primary400, modifier = Modifier.size(14.dp))
                        Text(" ${p.likesBase}", style = GType.labelMd, color = GColors.TextSecondary)
                    }
                }
            }
        }
    }
}
