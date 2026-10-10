package com.gacelas.rutas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gacelas.data.mock.Dificultad
import com.gacelas.data.mock.MockData
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.IconoCirculo
import com.gacelas.ui.components.MapaPlaceholder
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.gacelas.ui.three.ClipCorredor
import com.gacelas.ui.three.Runner3D
import io.github.sceneview.math.Position

@Composable
fun ListaRutasScreen(onBuscar: () -> Unit, onRuta: (String) -> Unit) {
    var pestana by rememberSaveable { mutableStateOf("Explorar") }
    Pantalla {
        Text("Rutas de Loja", style = GType.headlineLg, color = GColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Explorar", "Mis Rutas", "Guardadas").forEach { Pastilla(it, it == pestana, { pestana = it }) }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceContainer)
                .border(1.dp, GColors.BorderSubtle, RoundedCornerShape(12.dp)).clickable(onClick = onBuscar).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = GColors.TextSecondary)
            Text("  Buscar rutas...", style = GType.bodyLg, color = GColors.TextSecondary)
        }
        MockData.rutas.forEach { ruta ->
            Tarjeta(padding = 0.dp, onClick = { onRuta(ruta.id) }) {
                Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.linearGradient(listOf(GColors.MapParks, GColors.SurfaceLow))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Route, null, tint = GColors.Primary500.copy(alpha = 0.7f), modifier = Modifier.size(56.dp))
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ruta.nombre, style = GType.titleMd, color = GColors.TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Straighten, null, tint = GColors.TextSecondary, modifier = Modifier.size(16.dp))
                        Text(" ${ruta.distanciaKm} km   ", style = GType.bodyMd, color = GColors.TextSecondary)
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = GColors.TextSecondary, modifier = Modifier.size(16.dp))
                        Text(" ${ruta.elevacionM}m", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                        EtiquetaDificultad(ruta.dificultad)
                    }
                }
            }
        }
    }
}

@Composable
fun EtiquetaDificultad(d: Dificultad) {
    val color = when (d) {
        Dificultad.Facil -> GColors.Secondary500
        Dificultad.Moderado -> GColors.Primary500
        Dificultad.Dificil -> GColors.Danger
    }
    Text(d.etiqueta, style = GType.labelMd, color = color, modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp))
}

@Composable
fun BuscadorRutasScreen(onAtras: () -> Unit, onNavegar: () -> Unit) {
    var consulta by rememberSaveable { mutableStateOf("") }
    var seleccion by rememberSaveable { mutableStateOf("primary") }
    Box(Modifier.fillMaxSize().background(Color(0xFF090F1A))) {
        MapaPlaceholder(rutaPrincipal = seleccion == "primary")
        IconButton(onClick = onAtras, modifier = Modifier.statusBarsPadding().padding(12.dp).clip(CircleShape).background(GColors.BgSecondary)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = GColors.TextPrimary)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF0F1626)).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(GColors.BorderSubtle))
            CampoTexto("Destino", consulta, { consulta = it }, "¿A dónde quieres correr?", trailing = { Icon(Icons.Filled.Search, null, tint = GColors.TextSecondary) })
            MockData.rutasSugeridas.forEach { r ->
                val activa = seleccion == r.id
                val color = if (r.principal) GColors.Primary else GColors.Secondary500
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (activa) color.copy(alpha = 0.1f) else Color(0xFF141C2E))
                        .border(2.dp, if (activa) color else Color(0xFF1F2B45), RoundedCornerShape(16.dp)).clickable { seleccion = r.id }.padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.nombre, style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                        Icon(if (activa) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (activa) color else GColors.TextSecondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Straighten, null, tint = GColors.TextSecondary, modifier = Modifier.size(14.dp))
                        Text(" ${r.distanciaKm} km   ", style = GType.bodySm, color = GColors.TextSecondary)
                        Icon(Icons.Filled.Schedule, null, tint = GColors.TextSecondary, modifier = Modifier.size(14.dp))
                        Text(" ${r.minutos} min   ", style = GType.bodySm, color = GColors.TextSecondary)
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = GColors.TextSecondary, modifier = Modifier.size(14.dp))
                        Text(" ${r.elevacionM}m", style = GType.bodySm, color = GColors.TextSecondary)
                    }
                }
            }
            BotonPrimario("Comenzar navegación", onNavegar, icono = Icons.Filled.Navigation)
        }
    }
}

@Composable
fun NavegacionRutaScreen(onCronometro: () -> Unit, onCerrar: () -> Unit) {
    var carrera by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color(0xFF090F1A))) {
        MapaPlaceholder(rutaPrincipal = true)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, GColors.BgPrimary.copy(alpha = 0.6f)))))
        Box(Modifier.fillMaxWidth().height(420.dp).align(Alignment.Center)) {
            Runner3D(if (carrera) ClipCorredor.Run else ClipCorredor.Jog, camara = Position(0f, 1.6f, 3.8f))
        }
        Column(Modifier.statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Tarjeta(fondo = GColors.Secondary700, borde = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoCirculo(Icons.Filled.Navigation, GColors.TextPrimary.copy(alpha = 0.2f), GColors.TextPrimary, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Siguiente giro en", style = GType.labelMd, color = GColors.TextPrimary.copy(alpha = 0.8f))
                        Text("200 metros", style = GType.headlineSm, color = GColors.TextPrimary)
                    }
                }
                Text("Gira a la derecha en Av. Universitaria", style = GType.bodyMd, color = GColors.TextPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pastilla("Trote", !carrera, { carrera = false })
                Pastilla("Carrera", carrera, { carrera = true })
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(GColors.BgSecondary.copy(alpha = 0.95f)).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Metrica("2.3", "km", grande = true, color = GColors.Primary500)
                Metrica("12:15", "tiempo", grande = true)
                Metrica("5:18", "/km", grande = true, color = GColors.Secondary500)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onCronometro, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GColors.Primary, contentColor = GColors.TextPrimary),
                ) {
                    Icon(Icons.Filled.Timer, null)
                    Text("  Ver cronómetro", style = GType.labelLg)
                }
                IconButton(onClick = onCerrar, modifier = Modifier.size(52.dp).clip(CircleShape).background(GColors.Danger)) {
                    Icon(Icons.Filled.Close, "Terminar navegación", tint = GColors.TextPrimary)
                }
            }
        }
    }
}
