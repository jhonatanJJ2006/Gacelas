package com.gacelas.rutas

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.gacelas.core.location.LocationService
import com.gacelas.correr.MantenerPantallaEncendida
import com.gacelas.correr.RelojCarrera
import com.gacelas.correr.descartarCarrera
import com.gacelas.correr.empezarCarrera
import com.gacelas.correr.terminarCarrera
import com.gacelas.data.rutas.PestanaRutas
import com.gacelas.data.rutas.filtrarRutas
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.ajustesState
import com.gacelas.ui.aviso
import com.gacelas.ui.contenedor
import kotlinx.coroutines.launch
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
import com.gacelas.ui.components.MapaGoogle
import com.gacelas.ui.components.MapaPlaceholder
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.components.aLatLng
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
    val c = contenedor()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val guardadas by c.rutas.guardadas.collectAsState(initial = emptySet())
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    var pestana by rememberSaveable { mutableStateOf(PestanaRutas.Explorar) }
    var consulta by rememberSaveable { mutableStateOf("") }
    val lista = filtrarRutas(MockData.rutas, pestana, guardadas, MockData.ID_USUARIO, consulta)
    Pantalla {
        Text("Rutas de Loja", style = GType.headlineLg, color = GColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PestanaRutas.entries.forEach { p -> Pastilla(if (p == PestanaRutas.Guardadas) "${p.etiqueta} (${guardadas.size})" else p.etiqueta, p == pestana, { pestana = p }) }
        }
        CampoTexto("Buscar", consulta, { consulta = it }, "Buscar rutas...", trailing = {
            IconButton(onClick = onBuscar) { Icon(Icons.Filled.Search, "Abrir buscador en el mapa", tint = GColors.TextSecondary) }
        })
        if (lista.isEmpty()) {
            Tarjeta {
                Text(
                    when (pestana) {
                        PestanaRutas.Guardadas -> "Aún no guardas rutas. Toca el marcador de una ruta para guardarla."
                        PestanaRutas.MisRutas -> "Todavía no creaste rutas."
                        PestanaRutas.Explorar -> "No hay rutas que coincidan con la búsqueda."
                    },
                    style = GType.bodyMd, color = GColors.TextSecondary,
                )
            }
        }
        lista.forEach { ruta ->
            val guardada = ruta.id in guardadas
            Tarjeta(padding = 0.dp, onClick = { onRuta(ruta.id) }) {
                Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.linearGradient(listOf(GColors.MapParks, GColors.SurfaceLow))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Route, null, tint = GColors.Primary500.copy(alpha = 0.7f), modifier = Modifier.size(56.dp))
                    IconButton(
                        onClick = {
                            scope.launch { c.rutas.alternarGuardada(ruta.id) }
                            context.aviso(if (guardada) "Ruta quitada de guardadas" else "Ruta guardada")
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.6f)),
                    ) {
                        Icon(if (guardada) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (guardada) "Quitar de guardadas" else "Guardar ruta", tint = GColors.Primary500)
                    }
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ruta.nombre, style = GType.titleMd, color = GColors.TextPrimary)
                    Text(ruta.descripcion, style = GType.bodySm, color = GColors.TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Straighten, null, tint = GColors.TextSecondary, modifier = Modifier.size(16.dp))
                        Text(" ${Unidades.texto(ruta.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)}   ", style = GType.bodyMd, color = GColors.TextSecondary)
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
    val sugeridas = MockData.rutasSugeridas.filter { consulta.isBlank() || it.nombre.contains(consulta.trim(), ignoreCase = true) }
    Box(Modifier.fillMaxSize().background(Color(0xFF090F1A))) {
        MapaGoogle(Mapas.trazo(seleccion), Modifier.fillMaxSize(), zoom = 14f) { MapaPlaceholder(rutaPrincipal = seleccion == "primary") }
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
            if (sugeridas.isEmpty()) Text("Sin rutas para \"$consulta\". Prueba con Jipiro o Universitaria.", style = GType.bodyMd, color = GColors.TextSecondary)
            sugeridas.forEach { r ->
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
            BotonPrimario("Comenzar navegación", onNavegar, icono = Icons.Filled.Navigation, enabled = sugeridas.any { it.id == seleccion })
        }
    }
}

@Composable
fun NavegacionRutaScreen(onCronometro: () -> Unit, onCerrar: (conCarrera: Boolean) -> Unit) {
    val context = LocalContext.current
    val tracker = contenedor().tracker
    val estado by tracker.estado.collectAsState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    var carrera by rememberSaveable { mutableStateOf(false) }
    // Al entrar a la navegacion se empieza a grabar la carrera con GPS real si hay permiso.
    LaunchedEffect(Unit) {
        if (!estado.activa && LocationService.tienePermiso(context)) empezarCarrera(context, tracker)
    }
    MantenerPantallaEncendida()
    RelojCarrera(tracker, estado)
    // Las indicaciones de giro siguen la ruta de ejemplo segun la distancia recorrida.
    val indicaciones = listOf("Gira a la derecha en Av. Universitaria", "Continúa recto por Av. Orillas del Zamora", "Gira a la izquierda hacia Parque Jipiro", "Llegaste a tu destino")
    val tramo = (estado.distanciaKm / 0.5).toInt().coerceAtMost(indicaciones.lastIndex)
    val faltanM = ((tramo + 1) * 500 - estado.distanciaKm * 1000).toInt().coerceAtLeast(0)
    Box(Modifier.fillMaxSize().background(Color(0xFF090F1A))) {
        MapaGoogle(estado.puntos.aLatLng(), Modifier.fillMaxSize(), seguir = true, zoom = 17f) { MapaPlaceholder(rutaPrincipal = true) }
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
                        Text(if (tramo == indicaciones.lastIndex) "Destino" else "$faltanM metros", style = GType.headlineSm, color = GColors.TextPrimary)
                    }
                }
                Text(indicaciones[tramo], style = GType.bodyMd, color = GColors.TextPrimary)
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
                Metrica(Unidades.texto(estado.distanciaKm, m), Unidades.etiquetaDistancia(m), grande = true, color = GColors.Primary500)
                Metrica(formatearMs(estado.segundos), "tiempo", grande = true)
                Metrica(formatearRitmo(Unidades.ritmo(estado.ritmoActualSegKm ?: estado.ritmoPromedioSegKm, m)), Unidades.etiquetaRitmo(m), grande = true, color = GColors.Secondary500)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onCronometro, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GColors.Primary, contentColor = GColors.SobreColor),
                ) {
                    Icon(Icons.Filled.Timer, null)
                    Text("  Ver cronómetro", style = GType.labelLg)
                }
                IconButton(onClick = {
                    if (estado.segundos > 0) terminarCarrera(context, tracker) else descartarCarrera(context, tracker)
                    onCerrar(estado.segundos > 0)
                }, modifier = Modifier.size(52.dp).clip(CircleShape).background(GColors.Danger)) {
                    Icon(Icons.Filled.Close, "Terminar navegación", tint = GColors.TextPrimary)
                }
            }
        }
    }
}
