package com.gacelas.correr

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gacelas.data.mock.MockData
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.BotonSecundario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.IconoCirculo
import com.gacelas.ui.components.Interruptor
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.delay

@Composable
private fun PantallaCompleta(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(GColors.BgPrimary).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

@Composable
private fun BotonCerrar(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.clip(CircleShape).background(GColors.SurfaceContainer)) {
        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = GColors.TextPrimary)
    }
}

@Composable
private fun BotonRedondo(icono: ImageVector, descripcion: String, color: Color, tamano: Dp, onClick: () -> Unit, tint: Color = GColors.TextPrimary) {
    Box(Modifier.size(tamano).clip(CircleShape).background(color).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icono, contentDescription = descripcion, tint = tint, modifier = Modifier.size(tamano * 0.5f))
    }
}

@Composable
fun CorrerScreen(onCerrar: () -> Unit, onEmpezar: () -> Unit) {
    PantallaCompleta {
        BotonCerrar(onCerrar)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IconoCirculo(Icons.AutoMirrored.Filled.DirectionsRun, GColors.Primary.copy(alpha = 0.2f), GColors.Primary500, 112.dp)
            Text("¿Listo para correr?", style = GType.headlineLg, color = GColors.TextPrimary)
            Text("Activa el GPS y comienza a grabar tu carrera", style = GType.bodyLg, color = GColors.TextSecondary, textAlign = TextAlign.Center)
        }
        Tarjeta(fondo = GColors.Secondary.copy(alpha = 0.12f), borde = GColors.Secondary) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoCirculo(Icons.Filled.LocationOn, GColors.Secondary.copy(alpha = 0.25f), GColors.Secondary500, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("GPS Activado", style = GType.titleMd, color = GColors.TextPrimary)
                    Text("Señal fuerte", style = GType.bodySm, color = GColors.Secondary500)
                }
                Icon(Icons.Filled.CheckCircle, null, tint = GColors.Secondary500)
            }
        }
        Tarjeta {
            FilaDato("Tipo de actividad", "Carrera")
            FilaDato("Ubicación", "Loja, Ecuador")
            FilaDato("Temperatura", "14°C")
        }
        Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonRedondo(Icons.Filled.PlayArrow, "Comenzar", GColors.Primary, 112.dp, onEmpezar)
            Text("Toca para comenzar", style = GType.labelLg, color = GColors.TextSecondary)
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(valor, style = GType.labelLg, color = GColors.TextPrimary)
    }
}

@Composable
fun CarreraEnCursoScreen(onCerrar: () -> Unit, onFinalizar: () -> Unit, onCronometro: () -> Unit, vm: CarreraViewModel = viewModel()) {
    val estado by vm.estado.collectAsState()
    LaunchedEffect(estado.pausada) {
        while (!estado.pausada) {
            delay(1000)
            vm.tick()
        }
    }
    PantallaCompleta {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BotonCerrar(onCerrar)
            Spacer(Modifier.weight(1f))
            Row(Modifier.clip(CircleShape).background(GColors.SurfaceContainer).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (estado.pausada) GColors.Primary500 else GColors.Secondary500))
                Text(if (estado.pausada) "  En pausa" else "  En curso", style = GType.labelMd, color = GColors.TextPrimary)
            }
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(estado.distanciaTexto, style = GType.metricHuge.copy(fontSize = GType.metricHuge.fontSize * 1.8f, lineHeight = GType.metricHuge.lineHeight * 1.8f), color = GColors.TextPrimary)
            Text("Kilómetros", style = GType.labelLg, color = GColors.TextSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Metrica(estado.tiempoTexto, "Tiempo", grande = true)
            Metrica("${estado.ritmoMin}:00", "Ritmo /km", grande = true, color = GColors.Primary500)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            BotonRedondo(Icons.Filled.Timer, "Ver cronómetro", GColors.SurfaceHigh, 56.dp, onCronometro)
            BotonRedondo(if (estado.pausada) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (estado.pausada) "Reanudar" else "Pausar", GColors.Primary, 88.dp, vm::alternarPausa)
            BotonRedondo(Icons.Filled.Stop, "Finalizar", GColors.Danger, 56.dp, onFinalizar)
        }
        Tarjeta {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Metrica("120", "BPM", color = GColors.Danger)
                Metrica("128", "Pasos/min")
                Metrica("85", "Calorías", color = GColors.Primary500)
            }
        }
    }
}

private enum class PestanaCrono(val etiqueta: String, val icono: ImageVector) {
    Splits("Splits", Icons.AutoMirrored.Filled.ListAlt),
    Intervalos("Intervalos", Icons.Filled.Repeat),
    Metronomo("Metrónomo", Icons.Filled.GraphicEq),
}

@Composable
fun CronometroScreen(onAtras: () -> Unit, onFin: () -> Unit, segundosIniciales: Int = 1753, corriendo: Boolean = true) {
    var segundos by rememberSaveable { mutableIntStateOf(segundosIniciales) }
    var pausado by rememberSaveable { mutableStateOf(!corriendo) }
    var pestana by rememberSaveable { mutableStateOf(PestanaCrono.Splits) }
    LaunchedEffect(pausado) {
        while (!pausado) {
            delay(1000)
            segundos++
        }
    }
    val progreso = (segundos / 3600f).coerceIn(0f, 1f)
    PantallaCompleta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = GColors.TextPrimary) }
            Text("Cronómetro Profesional", style = GType.titleMd, color = GColors.TextPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipModo(Icons.AutoMirrored.Filled.DirectionsRun, "Modo Libre", true, Modifier.weight(1f))
            ChipModo(Icons.Filled.Timer3, "Fartlek", false, Modifier.weight(1f))
            ChipModo(Icons.Filled.Speed, "Ritmo Pacer", false, Modifier.weight(1f))
        }
        Tarjeta(fondo = GColors.Secondary.copy(alpha = 0.1f), borde = GColors.Secondary.copy(alpha = 0.5f), padding = 12.dp) {
            Text("Objetivo Pacer: 5:00 /km", style = GType.labelLg, color = GColors.TextPrimary)
            Text("0.8s más rápido que objetivo", style = GType.bodySm, color = GColors.Secondary500)
        }
        Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(240.dp)) {
                drawCircle(GColors.SurfaceHigh, style = Stroke(6.dp.toPx()))
                drawArc(GColors.Primary500, -90f, 360f * progreso, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (pausado) "En Pausa  •  168 spm" else "En Marcha  •  168 spm", style = GType.labelMd, color = GColors.Secondary500)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatearMs(segundos), style = GType.metricHuge, color = GColors.TextPrimary)
                    Text(".45", style = GType.headlineSm, color = GColors.TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Flag, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
                    Text(" Km 6 en curso: 2:18", style = GType.labelMd, color = GColors.TextSecondary)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CeldaMetrica(Icons.Filled.Straighten, "Distancia Total", "5.42 km", Modifier.weight(1f))
            CeldaMetrica(Icons.Filled.Speed, "Ritmo Actual", "4:52 /km", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CeldaMetrica(Icons.AutoMirrored.Filled.TrendingUp, "Ritmo Promedio", "5:08 /km", Modifier.weight(1f))
            CeldaMetrica(Icons.Filled.Favorite, "Cardio Z3", "154 bpm", Modifier.weight(1f), GColors.Danger)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            BotonRedondo(Icons.Filled.Flag, "Vuelta", GColors.SurfaceHigh, 64.dp, {})
            BotonRedondo(if (pausado) Icons.Filled.PlayArrow else Icons.Filled.Pause, "Pausa", GColors.Primary, 88.dp, { pausado = !pausado })
            BotonRedondo(Icons.Filled.Stop, "Fin", GColors.Danger, 64.dp, onFin)
        }
        Text("Mantén presionado Fin por 2 seg para guardar tu sesión.", style = GType.bodySm, color = GColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceLow).padding(4.dp)) {
            PestanaCrono.entries.forEach { p ->
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (p == pestana) GColors.SurfaceContainer else Color.Transparent)
                        .clickable { pestana = p }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(p.icono, null, tint = if (p == pestana) GColors.OnSurface else GColors.TextSecondary, modifier = Modifier.size(16.dp))
                    Text(" ${p.etiqueta}", style = GType.labelMd, color = if (p == pestana) GColors.OnSurface else GColors.TextSecondary)
                }
            }
        }
        when (pestana) {
            PestanaCrono.Splits -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.padding(horizontal = 8.dp)) {
                    Text("PARCIAL / DISTANCIA", style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
                    Text("RITMO    TIEMPO", style = GType.labelMd, color = GColors.TextSecondary)
                }
                MockData.splits.forEach { s ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (s.actual) GColors.SurfaceContainer else GColors.SurfaceLow).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val fondo = when { s.actual -> GColors.Secondary.copy(alpha = 0.2f); s.id == 3 -> GColors.PrimaryContainer; else -> GColors.SurfaceHigh }
                        Box(Modifier.size(24.dp).clip(CircleShape).background(fondo), contentAlignment = Alignment.Center) {
                            Text("${s.id}", style = GType.labelMd, color = if (s.actual) GColors.Secondary500 else GColors.OnSurface)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (s.actual) "Km ${s.distancia}" else "${s.distancia} km", style = GType.labelLg, color = GColors.TextPrimary)
                            Text(s.etiqueta, style = GType.bodySm, color = GColors.TextSecondary)
                        }
                        Text(s.ritmo, style = GType.labelLg, color = GColors.TextPrimary)
                        Spacer(Modifier.width(20.dp))
                        Text(s.tiempo, style = GType.labelLg, color = if (s.actual) GColors.Primary400 else GColors.TextSecondary)
                    }
                }
            }
            PestanaCrono.Intervalos -> Tarjeta { Text("Intervalos: 6 x 400 m con 90 s de recuperación", style = GType.bodyMd, color = GColors.TextSecondary) }
            PestanaCrono.Metronomo -> Tarjeta { Text("Metrónomo: 168 pasos por minuto", style = GType.bodyMd, color = GColors.TextSecondary) }
        }
    }
}

@Composable
private fun ChipModo(icono: ImageVector, texto: String, activo: Boolean, modifier: Modifier) {
    Row(
        modifier.clip(CircleShape).background(if (activo) GColors.Primary else GColors.SurfaceContainer).padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, null, tint = if (activo) GColors.TextPrimary else GColors.TextSecondary, modifier = Modifier.size(16.dp))
        Text(" $texto", style = GType.labelMd, color = if (activo) GColors.TextPrimary else GColors.TextSecondary, maxLines = 1)
    }
}

@Composable
private fun CeldaMetrica(icono: ImageVector, etiqueta: String, valor: String, modifier: Modifier, color: Color = GColors.Primary500) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(GColors.SurfaceContainer).border(1.dp, GColors.BorderSubtle, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
            Text(" $etiqueta", style = GType.labelMd, color = GColors.TextSecondary)
        }
        Text(valor, style = GType.headlineSm, color = GColors.TextPrimary)
    }
}

@Composable
fun ResumenPostCarreraScreen(onCerrar: () -> Unit, onGuardar: () -> Unit, onDescartar: () -> Unit) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var nota by rememberSaveable { mutableStateOf("") }
    var publica by rememberSaveable { mutableStateOf(true) }
    PantallaCompleta {
        BotonCerrar(onCerrar)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconoCirculo(Icons.Filled.CheckCircle, GColors.Secondary.copy(alpha = 0.2f), GColors.Secondary500, 96.dp)
            Text("Carrera completada", style = GType.headlineLg, color = GColors.TextPrimary)
            Text("Gran trabajo, has terminado tu carrera", style = GType.bodyLg, color = GColors.TextSecondary)
        }
        Tarjeta {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Metrica("5.2", "km", grande = true, color = GColors.Primary500)
                Metrica("28:34", "Tiempo", grande = true)
                Metrica("5:30", "/km", grande = true, color = GColors.Secondary500)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("245" to "Cal", "142" to "BPM", "125" to "SPM", "45m" to "Elev").forEach { (v, e) ->
                Tarjeta(Modifier.weight(1f), padding = 10.dp) { Metrica(v, e, Modifier.fillMaxWidth()) }
            }
        }
        CampoTexto("Título", titulo, { titulo = it }, "Título de la carrera (opcional)")
        CampoTexto("Notas", nota, { nota = it }, "¿Cómo te fue? Comparte tus pensamientos...", lineas = 3)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Hacer pública", style = GType.bodyLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            Interruptor(publica) { publica = it }
        }
        BotonPrimario("Guardar carrera", onGuardar, icono = Icons.Filled.Save)
        BotonSecundario("Descartar", onDescartar, color = GColors.TextSecondary)
    }
}
