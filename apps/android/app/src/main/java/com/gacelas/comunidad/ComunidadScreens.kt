package com.gacelas.comunidad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.gacelas.data.mock.Evento
import com.gacelas.data.mock.MockData
import com.gacelas.ui.components.AvatarCirculo
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

@Composable
fun FeedScreen(onEventos: () -> Unit) {
    Pantalla {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Comunidad", style = GType.headlineLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Filled.FilterList, "Filtrar", tint = GColors.TextSecondary) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pastilla("Feed", true)
            Pastilla("Eventos", false, onEventos)
        }
        MockData.actividades.forEachIndexed { i, a ->
            val usuario = MockData.usuario(a.usuarioId)
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarCirculo(40.dp, i)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(usuario?.nombre ?: "", style = GType.labelLg, color = GColors.TextPrimary)
                        Text(a.fecha, style = GType.bodySm, color = GColors.TextSecondary)
                    }
                    Icon(Icons.Filled.MoreVert, null, tint = GColors.TextSecondary)
                }
                Text(a.titulo, style = GType.titleMd, color = GColors.TextPrimary)
                Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(GColors.MapParks, GColors.SurfaceLow))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Route, null, tint = GColors.Primary500, modifier = Modifier.size(56.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Metrica("${a.distanciaKm}", "km", color = GColors.Primary500)
                    Metrica(MockData.duracionTexto(a.duracionSegundos), "tiempo")
                    Metrica(a.ritmo, "/km", color = GColors.Secondary500)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FavoriteBorder, "Me gusta", tint = GColors.TextSecondary)
                    Text(" ${a.likes}    ", style = GType.labelLg, color = GColors.TextSecondary)
                    Icon(Icons.AutoMirrored.Outlined.Chat, "Comentarios", tint = GColors.TextSecondary)
                    Text(" ${a.comentarios}", style = GType.labelLg, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.Share, "Compartir", tint = GColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun EventosScreen(onEvento: (String) -> Unit) {
    var pestana by rememberSaveable { mutableStateOf("Próximos") }
    Pantalla {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Eventos", style = GType.headlineLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = {}, modifier = Modifier.clip(CircleShape).background(GColors.Primary)) {
                Icon(Icons.Filled.Add, "Crear evento", tint = GColors.TextPrimary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Próximos", "Mis eventos", "Pasados").forEach { Pastilla(it, it == pestana, { pestana = it }) }
        }
        MockData.eventos.forEach { TarjetaEvento(it) { onEvento(it.id) } }
    }
}

@Composable
private fun TarjetaEvento(e: Evento, onClick: () -> Unit) {
    Tarjeta(padding = 0.dp, onClick = onClick) {
        Box(Modifier.fillMaxWidth().height(110.dp).background(Brush.linearGradient(listOf(GColors.Primary700, GColors.SurfaceLow)))) {
            Icon(Icons.Filled.Event, null, tint = GColors.TextPrimary.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Center).size(48.dp))
            Text("${e.distanciaKm} km", style = GType.labelMd, color = GColors.TextPrimary,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 4.dp))
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(e.titulo, style = GType.titleMd, color = GColors.TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarToday, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
                Text(" ${e.fecha}, ${e.hora}", style = GType.bodySm, color = GColors.TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
                Text(" ${e.ubicacion}", style = GType.bodySm, color = GColors.TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(64.dp)) { (0 until minOf(3, e.participantes)).forEach { i -> Box(Modifier.offset(x = (i * 16).dp)) { AvatarCirculo(28.dp, i) } } }
                Text(if (e.participantes > 3) "+${e.participantes - 3} participantes" else "${e.participantes} participantes",
                    style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                Text("Inscribirse", style = GType.labelMd, color = GColors.TextPrimary,
                    modifier = Modifier.clip(CircleShape).background(GColors.Primary).padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetalleEventoScreen(id: String, onAtras: () -> Unit) {
    val e = MockData.evento(id) ?: MockData.eventos.first()
    var inscrito by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(220.dp).background(Brush.verticalGradient(listOf(GColors.Primary700, GColors.BgPrimary)))) {
                IconButton(onClick = onAtras, modifier = Modifier.statusBarsPadding().padding(12.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.6f))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = GColors.TextPrimary)
                }
                Icon(Icons.Filled.Event, null, tint = GColors.TextPrimary.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center).size(72.dp))
                Row(Modifier.align(Alignment.BottomStart).padding(16.dp).clip(CircleShape).background(GColors.Primary).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = GColors.TextPrimary, modifier = Modifier.size(14.dp))
                    Text(" ${e.distanciaKm} km", style = GType.labelMd, color = GColors.TextPrimary)
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(e.titulo, style = GType.headlineLg, color = GColors.TextPrimary)
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarToday, null, tint = GColors.Primary500)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("${e.fecha}, 2026", style = GType.labelLg, color = GColors.TextPrimary)
                            Text("${e.hora} - 8:00 AM", style = GType.bodySm, color = GColors.TextSecondary)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = GColors.Primary500)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(e.ubicacion, style = GType.labelLg, color = GColors.TextPrimary)
                            Text("Entrada principal, Loja, Ecuador", style = GType.bodySm, color = GColors.TextSecondary)
                        }
                    }
                }
                Text("Descripción", style = GType.titleMd, color = GColors.TextPrimary)
                Text("${e.descripcion} Habrá hidratación en los km 3, 5 y 7.", style = GType.bodyMd, color = GColors.TextSecondary)
                Text("Participantes (${e.participantes})", style = GType.titleMd, color = GColors.TextPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(e.participantes) { AvatarCirculo(36.dp, it) }
                }
                Tarjeta {
                    Text("Detalles de la ruta", style = GType.titleMd, color = GColors.TextPrimary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Metrica("${e.distanciaKm}", "km", color = GColors.Primary500)
                        Metrica("45m", "elevación")
                        Metrica("Fácil", "dificultad", color = GColors.Secondary500)
                    }
                }
            }
        }
        Box(Modifier.background(GColors.BgSecondary).navigationBarsPadding().padding(16.dp)) {
            BotonPrimario(if (inscrito) "Inscrito" else "Inscribirme al evento", { inscrito = !inscrito }, icono = Icons.Filled.CheckCircle,
                color = if (inscrito) GColors.Secondary else GColors.Primary)
        }
    }
}
