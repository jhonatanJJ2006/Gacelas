package com.gacelas.rutas

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Draw
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
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
import com.gacelas.rutas.domain.modelo.PestanaRutas
import com.gacelas.rutas.domain.reglas.filtrarRutas
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
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.data.mock.MockData
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.IconoCirculo
import com.gacelas.ui.components.MapaGacelas
import com.gacelas.ui.components.MapaMat
import com.gacelas.ui.components.MapaPlaceholder
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.components.aLatLng
import org.maplibre.android.geometry.LatLng
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
fun ListaRutasScreen(onBuscar: () -> Unit, onRuta: (String) -> Unit, onDibujar: () -> Unit = {}, onAtras: () -> Unit = {}, onCorrerRuta: () -> Unit = {}) {
    val c = contenedor()
    val context = LocalContext.current
    val repo = c.explorar
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { c.listaRutasViewModel(MockData.ID_USUARIO, MockData.rutas) }
    val guardadas by vm.guardadas.collectAsState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val q by vm.consulta.collectAsState()
    val listaVm by vm.lista.collectAsState()
    // Rutas del catalogo local sin geometria: se completa con el trazo real empaquetado (TrazosRutas).
    val lista = remember(listaVm) { listaVm.map { r -> if (r.puntos.isNotEmpty()) r else r.copy(puntos = com.gacelas.rutas.infrastructure.TrazosRutas.puntos[r.id.removePrefix("ruta-")].orEmpty()) } }
    val perfilSesion by com.gacelas.ui.sesionState()
    val remotas by (repo?.rutas ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }).collectAsState()
    var leidas by remember { mutableStateOf(false) }
    LaunchedEffect(repo) { if (repo != null && remotas == null) runCatching { repo.refrescar() }; leidas = true }
    // Seguridad real por ruta (seguridad_rutas, migracion 040); sin servidor no se muestra.
    val seguridad by androidx.compose.runtime.produceState(emptyMap<String, String>(), c.incidentes) {
        value = c.incidentes?.seguridadRutas().orEmpty().mapValues { it.value.texto }
    }
    var comoLista by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val cargando = repo != null && remotas == null && !leidas && q.pestana == com.gacelas.rutas.domain.modelo.PestanaRutas.Explorar
    val onCerca = {
        if (q.cerca != null) vm.quitarCercania()
        else {
            val u = ultimaUbicacion(context)
            if (u == null || repo == null) context.aviso("Activa la ubicación para ver rutas cercanas")
            else vm.ordenarCerca(u.first, u.second)
        }
    }
    val alternarGuardada = { id: String, guardada: Boolean ->
        vm.alternarGuardada(id)
        context.aviso(if (guardada) "Ruta quitada de guardadas" else "Ruta guardada")
    }
    if (comoLista) {
        androidx.activity.compose.BackHandler { comoLista = false }
        ListaRutasContenido(
            cargando = cargando,
            lista = lista, pestana = q.pestana, onPestana = vm::pestana, guardadas = guardadas,
            consulta = q.texto, onConsulta = vm::buscar, filtros = q.filtros, onFiltros = vm::filtrar,
            cerca = q.cerca, onCerca = onCerca,
            m = m, puedeDibujar = repo != null && perfilSesion != null,
            onBuscar = onBuscar, onRuta = onRuta, onDibujar = onDibujar,
            onAlternarGuardada = alternarGuardada,
            mapaRutas = { MapaRutasAgrupadas(lista, onRuta, it) },
            onVerMapa = { comoLista = false },
        )
        return
    }
    val espacio = com.gacelas.ui.components.LocalEspacioDock.current
    RutasTablero(
        lista = lista, guardadas = guardadas, consulta = q.texto, onConsulta = vm::buscar, filtros = q.filtros, onFiltros = vm::filtrar,
        cerca = q.cerca, onCerca = onCerca, m = m, puedeDibujar = repo != null && perfilSesion != null,
        onAtras = onAtras, onRuta = onRuta,
        onCorrerRuta = { id -> com.gacelas.rutas.infrastructure.RutaActiva.id = id; onCorrerRuta() },
        onDibujar = onDibujar, onAlternarGuardada = alternarGuardada, onVerLista = { comoLista = true },
        seguridad = seguridad, espacioInferior = espacio, cargando = cargando,
        mapa = { elegida, otras, alElegir -> MapaRutasTablero(elegida, otras, alElegir) },
    )
}

/** Mapa del tablero de Rutas: todas las rutas tenues, la elegida resaltada y controles MAPA y REPORTAR reales. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.MapaRutasTablero(elegida: com.gacelas.rutas.domain.modelo.Ruta?, otras: List<com.gacelas.rutas.domain.modelo.Ruta>, alElegir: (String) -> Unit) {
    val context = LocalContext.current
    val dens = androidx.compose.ui.platform.LocalDensity.current.density
    var mapa by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var vista3d by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var rumbo by remember { mutableStateOf(0f) }
    var reportar by remember { mutableStateOf(false) }
    val trazo = { r: com.gacelas.rutas.domain.modelo.Ruta -> r.puntos.map { LatLng(it.first, it.second) }.ifEmpty { com.gacelas.ui.components.Mapas.trazo(r.id) } }
    val principal = remember(elegida?.id) { elegida?.let(trazo).orEmpty() }
    val resto = remember(otras) { otras.take(12).map(trazo).filter { it.size >= 2 } }
    val salidas = remember(otras, elegida) { (listOfNotNull(elegida) + otras).filter { it.puntos.isNotEmpty() }.map { com.gacelas.ui.components.CirculoMapa(LatLng(it.puntos.first().first, it.puntos.first().second), 8f, GColors.C2Morado, id = it.id) } }
    if (com.gacelas.ui.components.Mapas.configurado()) {
        com.gacelas.ui.components.MapaLibre(
            Modifier.fillMaxSize(),
            trazos = resto.map { com.gacelas.ui.components.TrazoMapa(it, Color(0xFF8B8FB8), 3f, 0.7f) } +
                (if (principal.size >= 2) listOf(com.gacelas.ui.components.TrazoMapa(principal, GColors.C2Violeta, 14f, 0.3f), com.gacelas.ui.components.TrazoMapa(principal, GColors.C2Violeta, 5f)) else emptyList()),
            circulos = salidas, zoom = 13.6,
            centro = principal.takeIf { it.isNotEmpty() }?.let { p -> LatLng(p.sumOf { it.latitude } / p.size, p.sumOf { it.longitude } / p.size) } ?: com.gacelas.ui.components.Mapas.CENTRO_LOJA,
            alTocarCirculo = alElegir,
            alMoverCamara = { rumbo = it.cameraPosition.bearing.toFloat() },
            alListo = { mapa = it },
        )
        LaunchedEffect(elegida?.id, mapa) {
            val mp = mapa ?: return@LaunchedEffect
            // Encuadre en la franja libre entre la cabecera y el carrusel.
            if (principal.size >= 2) mp.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(com.gacelas.ui.components.MapaMat.limites(principal),
                (40 * dens).toInt(), (140 * dens).toInt(), (90 * dens).toInt(), (340 * dens).toInt()))
        }
    } else com.gacelas.ui.components.MapaEsquemaC2(
        elegida?.puntos.orEmpty(), Modifier.fillMaxSize().graphicsLayer { if (vista3d) { rotationX = 30f; cameraDistance = 12f * density } },
        otros = otras.take(8).map { it.puntos }.filter { it.size >= 2 },
        marcadores = (listOfNotNull(elegida) + otras.take(4)).mapNotNull { it.puntos.firstOrNull() },
        margenArriba = 130.dp, margenAbajo = 330.dp,
    )
    androidx.compose.foundation.layout.Column(
        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 12.dp, top = 118.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        com.gacelas.correr.GrupoControlesC4("MAPA") {
            com.gacelas.correr.ControlEtiquetadoC4("Centrar", "Centrar el mapa en tu posición", {
                val u = ultimaUbicacion(context)
                val mp = mapa
                if (u == null) context.aviso("Activa la ubicación para centrar el mapa")
                else mp?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(LatLng(u.first, u.second), 15.0))
            }, "boton-recentrar") { Icon(Icons.Filled.MyLocation, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            com.gacelas.correr.ControlEtiquetadoC4("Norte", "Orientar el mapa al norte", {
                mapa?.let { mp -> mp.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(org.maplibre.android.camera.CameraPosition.Builder(mp.cameraPosition).bearing(0.0).build())) }
                rumbo = 0f
            }, "boton-brujula") { Icon(Icons.Filled.Navigation, null, tint = Color.White, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = -rumbo }) }
            com.gacelas.correr.SelectorVistaC4(vista3d) { v ->
                vista3d = v
                mapa?.let { mp -> mp.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(org.maplibre.android.camera.CameraPosition.Builder(mp.cameraPosition).tilt(if (v) 55.0 else 0.0).build())) }
            }
        }
        com.gacelas.ui.components.BotonReportarMapa({ reportar = true })
    }
    if (reportar) com.gacelas.seguridad.ui.HojaReportarIncidente(ultimaUbicacion(context), onCerrar = { reportar = false })
}

@Composable
fun BuscadorRutasScreen(onAtras: () -> Unit, onNavegar: () -> Unit) {
    val context = LocalContext.current
    var consulta by rememberSaveable { mutableStateOf("") }
    val c = contenedor()
    val remotas by (c.explorar?.rutas ?: remember { kotlinx.coroutines.flow.MutableStateFlow(null) }).collectAsState()
    val locales by c.rutasOffline.rutas.collectAsState(initial = emptyList())
    val guardadas by c.rutas.guardadas.collectAsState(initial = emptySet())
    LaunchedEffect(Unit) { c.explorar?.refrescar() }
    val elegida = remember { com.gacelas.rutas.infrastructure.RutaActiva.id }
    val todas = remember(remotas, locales, guardadas) { rutasNavegables(remotas, locales, guardadas, elegida) }
    var seleccion by rememberSaveable { mutableStateOf(elegida ?: com.gacelas.rutas.infrastructure.RutaActiva.navegarId ?: "primary") }
    val sugeridas = todas.filter { consulta.isBlank() || it.nombre.contains(consulta.trim(), ignoreCase = true) }.take(8)
    val trazoSeleccion = todas.firstOrNull { it.id == seleccion }?.trazo.orEmpty().map { org.maplibre.android.geometry.LatLng(it.first, it.second) }
    val scope = rememberCoroutineScope()
    val actual = todas.firstOrNull { it.id == seleccion }
    Box(Modifier.fillMaxSize().background(com.gacelas.ui.components.Tablero.Fondo)) {
        MapaGacelas(trazoSeleccion, Modifier.fillMaxSize(), zoom = 14f) { MapaPlaceholder(rutaPrincipal = seleccion == "primary") }
        Column(Modifier.statusBarsPadding().padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF0F1626)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = GColors.TextPrimary) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MyLocation, null, tint = GColors.Primary500, modifier = Modifier.size(16.dp))
                        Text("  Mi ubicación GPS", style = GType.bodyMd, color = GColors.TextPrimary)
                    }
                    CampoTexto("Destino", consulta, { consulta = it }, "¿A dónde quieres correr?", trailing = {
                        if (consulta.isNotEmpty()) IconButton(onClick = { consulta = "" }) { Icon(Icons.Filled.Close, "Borrar destino", tint = GColors.TextSecondary) }
                        else Icon(Icons.Filled.Search, null, tint = GColors.TextSecondary)
                    })
                }
            }
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(todas.take(4).size) { i ->
                    val r = todas[i]
                    com.gacelas.ui.components.Pastilla(r.nombre, consulta == r.nombre, { consulta = r.nombre; seleccion = r.id })
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF0F1626)).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(GColors.BorderSubtle))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ruta seleccionada", style = GType.labelMd, color = GColors.TextSecondary)
                    Text(actual?.nombre ?: "Elige una ruta", style = GType.headlineSm, color = GColors.TextPrimary)
                }
                actual?.let { Text("${it.minutos} min estimado", style = GType.labelLg, color = GColors.Primary500) }
            }
            if (sugeridas.isEmpty()) Text("Sin rutas para \"$consulta\". Prueba con Jipiro o Universitaria.", style = GType.bodyMd, color = GColors.TextSecondary)
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sugeridas.size) { i ->
                    val r = sugeridas[i]
                    val activa = seleccion == r.id
                    val etiqueta = etiquetaOpcion(r, sugeridas)
                    Column(
                        Modifier.width(170.dp).clip(RoundedCornerShape(16.dp)).background(if (activa) GColors.Primary.copy(alpha = 0.12f) else Color(0xFF141C2E))
                            .border(2.dp, if (activa) GColors.Primary else Color(0xFF1F2B45), RoundedCornerShape(16.dp)).clickable { seleccion = r.id }.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        etiqueta?.let { Text(it.uppercase(), style = GType.labelMd, color = if (activa) GColors.Primary500 else GColors.Secondary500) }
                        Text(r.nombre, style = GType.labelLg, color = GColors.TextPrimary, maxLines = 1)
                        Text("${"%.1f".format(r.distanciaKm)} km · ${r.minutos} min", style = GType.bodySm, color = GColors.TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = GColors.TextSecondary, modifier = Modifier.size(14.dp))
                            Text(" ${r.elevacionM} m", style = GType.bodySm, color = GColors.TextSecondary)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    BotonPrimario("Confirmar y comenzar navegación", {
                        com.gacelas.rutas.infrastructure.RutaActiva.navegarId = actual?.id
                        // Solo las rutas reales de la comunidad registran la corrida al guardar.
                        com.gacelas.rutas.infrastructure.RutaActiva.id = actual?.id?.takeIf { actual.real }
                        onNavegar()
                    }, icono = Icons.Filled.Navigation, enabled = sugeridas.any { it.id == seleccion })
                }
                val guardada = actual?.id in guardadas
                IconButton(
                    onClick = {
                        actual?.let { r ->
                            scope.launch { c.rutas.alternarGuardada(r.id) }
                            context.aviso(if (guardada) "Ruta quitada de guardadas" else "Ruta guardada")
                        }
                    },
                    modifier = Modifier.clip(CircleShape).background(GColors.BgSecondary).testTag("guardar-ruta"),
                ) { Icon(if (guardada) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (guardada) "Quitar de guardadas" else "Guardar ruta", tint = GColors.Primary500) }
            }
        }
    }
}

/** Etiqueta de la alternativa: la mas corta es Optima, la de menos desnivel Plano y la de mas, Trail (igual que la web). */
internal fun etiquetaOpcion(o: RutaNavegable, todas: List<RutaNavegable>): String? {
    if (todas.size < 2) return null
    if (todas.minBy { it.distanciaKm } === o) return "Óptima"
    if (todas.minBy { it.elevacionM } === o) return "Plano"
    if (todas.maxBy { it.elevacionM } === o) return "Trail"
    return null
}

@Composable
fun NavegacionRutaScreen(onCronometro: () -> Unit, onCerrar: (conCarrera: Boolean) -> Unit) {
    val context = LocalContext.current
    val tracker = contenedor().tracker
    val estado by tracker.estado.collectAsState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    var vista3d by rememberSaveable { mutableStateOf(true) }
    // Al entrar a la navegacion se empieza a grabar la carrera con GPS real si hay permiso.
    LaunchedEffect(Unit) {
        if (!estado.activa && LocationService.tienePermiso(context)) empezarCarrera(context, tracker)
    }
    MantenerPantallaEncendida()
    RelojCarrera(tracker, estado)
    // La ruta elegida se lee de la copia local, asi la navegacion funciona sin red.
    val c = contenedor()
    val remotas by (c.explorar?.rutas ?: remember { kotlinx.coroutines.flow.MutableStateFlow(null) }).collectAsState()
    val locales by c.rutasOffline.rutas.collectAsState(initial = emptyList())
    val guardadas by c.rutas.guardadas.collectAsState(initial = emptySet())
    val idRuta = rememberSaveable { com.gacelas.rutas.infrastructure.RutaActiva.navegarId ?: "primary" }
    val ruta = remember(remotas, locales, guardadas) { rutasNavegables(remotas, locales, guardadas).firstOrNull { it.id == idRuta } }
    val trazo = ruta?.trazo.orEmpty()
    val plan = remember(trazo) { trazo.map { org.maplibre.android.geometry.LatLng(it.first, it.second) } }
    val ultimo = estado.puntos.lastOrNull()
    val guia = remember(trazo, ultimo) { com.gacelas.rutas.domain.reglas.Guia.calcular(trazo, ultimo?.let { it.lat to it.lng }) }
    val fueraDeRuta = guia != null && ultimo != null && guia.desvioM > com.gacelas.rutas.domain.reglas.Guia.DESVIO_M
    Box(Modifier.fillMaxSize().background(com.gacelas.ui.components.Tablero.Fondo)) {
        val clip = MapaMat.clipPorRitmo(estado.ritmoActualSegKm)
        if (Mapas.configurado()) {
            // Estilo navegacion: mapa inclinado con el rumbo arriba, ruta brillante por delante y el corredor 3D en el mapa.
            MapaGacelas(estado.puntos.aLatLng(), Modifier.fillMaxSize(), seguir = true, zoom = 17f, modo3d = vista3d, ritmoSegKm = estado.ritmoActualSegKm, plan = plan, adelanteM = 120.0) { }
        } else {
            MapaPlaceholder(rutaPrincipal = true)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, GColors.BgPrimary.copy(alpha = 0.6f)))))
            Box(Modifier.fillMaxWidth().height(420.dp).align(Alignment.Center)) {
                Runner3D(if (clip == ClipCorredor.Idle) ClipCorredor.Jog else clip, camara = Position(0f, 1.6f, 3.8f))
            }
        }
        Column(Modifier.statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Volver: igual que cerrar la navegacion; si hubo carrera se guarda y va al resumen (no se pierden datos).
            com.gacelas.ui.components.BotonVolverMapa({
                if (estado.segundos > 0) terminarCarrera(context, tracker) else descartarCarrera(context, tracker)
                onCerrar(estado.segundos > 0)
            })
            Tarjeta(fondo = GColors.Secondary700, borde = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoCirculo(Icons.Filled.Navigation, GColors.TextPrimary.copy(alpha = 0.2f), GColors.TextPrimary, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(ruta?.nombre ?: "Ruta no disponible sin conexión", style = GType.labelMd, color = GColors.TextPrimary.copy(alpha = 0.8f), maxLines = 1)
                        Text(
                            when {
                                guia == null -> "--"
                                guia.llegada -> "Destino"
                                fueraDeRuta -> "Fuera de ruta"
                                else -> "${guia.hastaGiroM.toInt()} metros"
                            },
                            style = GType.headlineSm, color = GColors.TextPrimary,
                        )
                    }
                }
                Text(
                    when {
                        guia == null -> "Elige una ruta guardada para navegar"
                        fueraDeRuta -> "Vuelve a la ruta, estás a ${guia.desvioM.toInt()} m"
                        guia.llegada -> guia.texto
                        else -> "${guia.texto}  ·  faltan ${Unidades.texto(guia.restanteM / 1000.0, m, 1)} ${Unidades.etiquetaDistancia(m)}"
                    },
                    style = GType.bodyMd, color = GColors.TextPrimary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Mapas.configurado()) {
                    Pastilla("3D", vista3d, { vista3d = true })
                    Pastilla("2D", !vista3d, { vista3d = false })
                }
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
