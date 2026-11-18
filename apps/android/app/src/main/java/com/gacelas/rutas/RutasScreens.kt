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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
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
    // Del servidor pueden llegar sin path: primero el trazo del catalogo oficial y si no el de ejemplo.
    val lista = remember(listaVm) {
        listaVm.map { r ->
            if (r.puntos.isNotEmpty()) r
            else r.copy(puntos = com.gacelas.rutas.domain.modelo.RutasLoja.todas.firstOrNull { it.id == r.id }?.puntos
                ?: com.gacelas.rutas.infrastructure.TrazosRutas.puntos[r.id.removePrefix("ruta-")].orEmpty())
        }
    }
    val perfilSesion by com.gacelas.ui.sesionState()
    val remotas by (repo?.rutas ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }).collectAsState()
    var leidas by remember { mutableStateOf(false) }
    LaunchedEffect(repo) { if (repo != null && remotas == null) runCatching { repo.refrescar() }; leidas = true }
    // Seguridad real por ruta (seguridad_rutas, migracion 040); sin servidor no se muestra.
    val seguridad by androidx.compose.runtime.produceState(emptyMap<String, String>(), c.incidentes) {
        value = c.incidentes?.seguridadRutas().orEmpty().mapValues { it.value.texto }
    }
    // Rutas abre en Lista; el Mapa es la vista secundaria y "atras" desde el Mapa vuelve a la Lista.
    var comoLista by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    val cargando = repo != null && remotas == null && !leidas && q.pestana == com.gacelas.rutas.domain.modelo.PestanaRutas.Explorar
    val pedirUbicacion = com.gacelas.ui.rememberPedirUbicacion { u -> vm.ordenarCerca(u.first, u.second) }
    val onCerca = {
        when {
            q.cerca != null -> vm.quitarCercania()
            repo == null -> context.aviso("Sin conexión con el servidor no podemos ordenar por cercanía")
            else -> pedirUbicacion()
        }
    }
    val alternarGuardada = { id: String, guardada: Boolean ->
        vm.alternarGuardada(id)
        context.aviso(if (guardada) "Ruta quitada de guardadas" else "Ruta guardada")
    }
    if (comoLista) {
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
            imagen = { url, mod -> com.gacelas.ui.components.FotoRemotaC2(url, mod, "Foto de la ruta") },
        )
        return
    }
    val espacio = com.gacelas.ui.components.LocalEspacioDock.current
    androidx.activity.compose.BackHandler { comoLista = true }
    RutasTablero(
        lista = lista, guardadas = guardadas, consulta = q.texto, onConsulta = vm::buscar, filtros = q.filtros, onFiltros = vm::filtrar,
        cerca = q.cerca, onCerca = onCerca, m = m, puedeDibujar = repo != null && perfilSesion != null,
        onAtras = { comoLista = true }, onRuta = onRuta,
        onCorrerRuta = { id -> com.gacelas.rutas.infrastructure.RutaActiva.id = id; onCorrerRuta() },
        onDibujar = onDibujar, onAlternarGuardada = alternarGuardada, onVerLista = { comoLista = true },
        seguridad = seguridad, espacioInferior = espacio, cargando = cargando,
        mapa = { elegida, otras, alElegir -> MapaRutasTablero(elegida, otras, alElegir) },
    )
}

/**
 * Mapa del tablero de Rutas: cada ruta con el color de su dificultad (facil verde, media ambar,
 * dificil rojo), la elegida con brillo y pines como en el buscador de Inicio y la camara volando a
 * ella. Controles: Centrar (pide la ubicacion con el dialogo nativo) y Norte.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.MapaRutasTablero(elegida: com.gacelas.rutas.domain.modelo.Ruta?, otras: List<com.gacelas.rutas.domain.modelo.Ruta>, alElegir: (String) -> Unit) {
    val context = LocalContext.current
    val dens = androidx.compose.ui.platform.LocalDensity.current.density
    var mapa by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var rumbo by remember { mutableStateOf(0f) }
    val principal = remember(elegida) { elegida?.let(::trazoRuta).orEmpty() }
    val resto = remember(otras) { otras.map { it to trazoRuta(it) }.filter { it.second.size >= 2 } }
    val salidas = remember(otras, elegida) {
        (listOfNotNull(elegida) + otras).mapNotNull { r -> trazoRuta(r).firstOrNull()?.let { com.gacelas.ui.components.CirculoMapa(it, 7f, com.gacelas.rutas.colorDificultad(r.dificultad), Color.White, 1.5f, id = r.id) } }
    }
    // Tu posicion (punto azul con halo) para saber donde estas respecto a las rutas.
    var yo by remember { mutableStateOf(ultimaUbicacion(context)?.let { LatLng(it.first, it.second) }) }
    val tuPunto = yo?.let { listOf(com.gacelas.ui.components.CirculoMapa(it, 18f, GColors.Info.copy(alpha = 0.25f), Color.Transparent, 0f), com.gacelas.ui.components.CirculoMapa(it, 8f, GColors.Info, Color.White, 3f)) }.orEmpty()
    val centrar = com.gacelas.ui.rememberPedirUbicacion { u ->
        yo = LatLng(u.first, u.second)
        mapa?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(LatLng(u.first, u.second), 15.0), 900)
    }
    if (com.gacelas.ui.components.Mapas.configurado()) {
        com.gacelas.ui.components.MapaLibre(
            Modifier.fillMaxSize(),
            trazos = resto.map { (r, t) -> com.gacelas.ui.components.TrazoMapa(t, com.gacelas.rutas.colorDificultad(r.dificultad), 4f, 0.8f) } +
                (if (principal.size >= 2) com.gacelas.ui.components.trazoSeleccionado(principal, com.gacelas.rutas.colorDificultad(elegida!!.dificultad)) else emptyList()),
            circulos = salidas + (if (principal.size >= 2) com.gacelas.ui.components.pinesRuta(principal) else emptyList()) + tuPunto, zoom = 13.6,
            centro = principal.takeIf { it.isNotEmpty() }?.let { p -> LatLng(p.sumOf { it.latitude } / p.size, p.sumOf { it.longitude } / p.size) } ?: com.gacelas.ui.components.Mapas.CENTRO_LOJA,
            alTocarCirculo = alElegir,
            alMoverCamara = { rumbo = it.cameraPosition.bearing.toFloat() },
            alListo = { mapa = it },
        )
        LaunchedEffect(principal, mapa) {
            val mp = mapa ?: return@LaunchedEffect
            // Encuadre en la franja libre entre la cabecera y el carrusel. Incluye tu posicion si estas cerca
            // (unos 15 km); si estas lejos de Loja, solo la ruta para no alejar el mapa.
            val cerca = yo?.takeIf { y -> principal.any { kotlin.math.abs(it.latitude - y.latitude) < 0.15 && kotlin.math.abs(it.longitude - y.longitude) < 0.15 } }
            if (principal.size >= 2) mp.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(com.gacelas.ui.components.MapaMat.limites(principal + listOfNotNull(cerca)),
                (40 * dens).toInt(), (190 * dens).toInt(), (90 * dens).toInt(), (330 * dens).toInt()), 900)
        }
    } else com.gacelas.ui.components.MapaEsquemaC2(
        elegida?.puntos.orEmpty(), Modifier.fillMaxSize(),
        otros = otras.take(8).map { it.puntos }.filter { it.size >= 2 },
        marcadores = (listOfNotNull(elegida) + otras.take(4)).mapNotNull { it.puntos.firstOrNull() },
        margenArriba = 130.dp, margenAbajo = 330.dp,
    )
    androidx.compose.foundation.layout.Column(
        // Debajo de la cabecera de RutasTablero: selector Lista | Mapa, buscador y chips.
        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 12.dp, top = 180.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        com.gacelas.correr.GrupoControlesC4("Mapa") {
            com.gacelas.correr.ControlEtiquetadoC4("Centrar", "Centrar el mapa en tu posición", centrar, "boton-recentrar") {
                Icon(Icons.Filled.MyLocation, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            com.gacelas.correr.ControlEtiquetadoC4("Norte", "Orientar el mapa al norte", {
                mapa?.let { mp -> mp.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(org.maplibre.android.camera.CameraPosition.Builder(mp.cameraPosition).bearing(0.0).build())) }
                rumbo = 0f
            }, "boton-brujula") { Icon(Icons.Filled.Navigation, null, tint = Color.White, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = -rumbo }) }
        }
    }
}

/** Trazo de una ruta: el suyo, el del catalogo oficial o el de ejemplo empaquetado (las del servidor pueden llegar sin path). */
private fun trazoRuta(r: com.gacelas.rutas.domain.modelo.Ruta): List<LatLng> =
    r.puntos.ifEmpty { com.gacelas.rutas.domain.modelo.RutasLoja.todas.firstOrNull { it.id == r.id }?.puntos.orEmpty() }
        .map { LatLng(it.first, it.second) }
        .ifEmpty { com.gacelas.ui.components.Mapas.trazo(r.id) }

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
    // Sugerencias al escribir: busquedas recientes, nombres de rutas y etiquetas.
    var recientes by remember { mutableStateOf(busquedasRecientes(context)) }
    var escribiendo by remember { mutableStateOf(false) }
    val candidatos = remember(todas) { todas.map { it.nombre } + com.gacelas.rutas.domain.modelo.ETIQUETAS_RUTA.values }
    val sugerencias = remember(consulta, candidatos, recientes) { com.gacelas.rutas.domain.reglas.SugerenciasBusqueda.sugerir(consulta, candidatos, recientes) }
    val foco = androidx.compose.ui.platform.LocalFocusManager.current
    val elegir = { texto: String ->
        consulta = texto
        todas.firstOrNull { it.nombre == texto }?.let { seleccion = it.id }
        recientes = guardarBusqueda(context, texto)
        escribiendo = false
        foco.clearFocus()
    }
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
                    Box(Modifier.onFocusChanged { if (it.hasFocus) escribiendo = true }) { CampoTexto("Destino", consulta, { consulta = it; escribiendo = true }, "¿A dónde quieres correr?", trailing = {
                        if (consulta.isNotEmpty()) IconButton(onClick = { consulta = "" }) { Icon(Icons.Filled.Close, "Borrar destino", tint = GColors.TextSecondary) }
                        else Icon(Icons.Filled.Search, null, tint = GColors.TextSecondary)
                    }) }
                }
            }
            if (escribiendo && sugerencias.isNotEmpty()) ListaSugerencias(sugerencias, elegir) { consulta = it }
            else androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                items(sugeridas.size, key = { sugeridas[it].id }) { i ->
                    val r = sugeridas[i]
                    TarjetaOpcionRuta(r, etiquetaOpcion(r, sugeridas), seleccion == r.id) { seleccion = r.id; recientes = guardarBusqueda(context, r.nombre) }
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

/** Opcion del buscador: todas iguales de ancho y alto; la elegida se ilumina y crece un poco. */
@Composable
private fun TarjetaOpcionRuta(r: RutaNavegable, etiqueta: String?, activa: Boolean, onElegir: () -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    val fondo by androidx.compose.animation.animateColorAsState(if (activa) GColors.Primary.copy(alpha = 0.16f) else Color(0xFF141C2E), com.gacelas.ui.movimiento.Mov.medio(), label = "fondo-opcion")
    val borde by androidx.compose.animation.animateColorAsState(if (activa) GColors.Primary500 else Color(0xFF1F2B45), com.gacelas.ui.movimiento.Mov.medio(), label = "borde-opcion")
    val escala by androidx.compose.animation.core.animateFloatAsState(if (activa) 1.03f else 1f, com.gacelas.ui.movimiento.Mov.medio(), label = "escala-opcion")
    Column(
        Modifier.width(200.dp).height(112.dp).graphicsLayer { scaleX = escala; scaleY = escala }.clip(forma).background(fondo)
            .border(2.dp, borde, forma).clickable(onClickLabel = "Elegir ruta", onClick = onElegir).padding(12.dp).testTag("opcion-ruta-${r.id}"),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // La fila de etiqueta siempre ocupa su alto para que todas las tarjetas se lean igual.
        Text((etiqueta ?: "Ruta").uppercase(), style = GType.labelMd, color = if (activa) GColors.Primary500 else if (etiqueta != null) GColors.Secondary500 else GColors.C2Muted, maxLines = 1)
        Text(r.nombre, style = GType.labelLg, color = GColors.TextPrimary, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${"%.1f".format(r.distanciaKm).replace('.', ',')} km", style = GType.bodySm, color = GColors.TextSecondary)
            Text("${r.minutos} min", style = GType.bodySm, color = GColors.TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = GColors.TextSecondary, modifier = Modifier.size(14.dp))
                Text(" ${r.elevacionM} m", style = GType.bodySm, color = GColors.TextSecondary)
            }
        }
    }
}

/** Lista de sugerencias bajo el campo: reloj para recientes, lupa para el resto, coincidencia en negrita y flecha para completar. */
@Composable
private fun ListaSugerencias(lista: List<com.gacelas.rutas.domain.reglas.Sugerencia>, onElegir: (String) -> Unit, onCompletar: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF0F1626)).padding(vertical = 6.dp).testTag("sugerencias-busqueda"),
    ) {
        lista.forEach { sg ->
            Row(
                Modifier.fillMaxWidth().clickable { onElegir(sg.texto) }.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp).testTag("sugerencia"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(if (sg.reciente) Icons.Filled.History else Icons.Filled.Search, null, tint = GColors.C2Muted, modifier = Modifier.size(20.dp))
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        val fin = (sg.desde + sg.largo).coerceAtMost(sg.texto.length)
                        append(sg.texto.substring(0, sg.desde))
                        withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = GColors.TextPrimary)) { append(sg.texto.substring(sg.desde, fin)) }
                        append(sg.texto.substring(fin))
                    },
                    style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f), maxLines = 1,
                )
                IconButton(onClick = { onCompletar(sg.texto) }) {
                    Icon(Icons.Filled.NorthWest, "Completar con ${sg.texto}", tint = GColors.C2Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private const val PREFS_BUSQUEDA = "busquedas_rutas"

/** Ultimas 8 busquedas (solo en este telefono). */
private fun busquedasRecientes(context: android.content.Context): List<String> =
    context.getSharedPreferences(PREFS_BUSQUEDA, android.content.Context.MODE_PRIVATE).getString("lista", null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

private fun guardarBusqueda(context: android.content.Context, texto: String): List<String> {
    val lista = (listOf(texto.trim()) + busquedasRecientes(context).filterNot { it.equals(texto.trim(), ignoreCase = true) }).filter { it.isNotBlank() }.take(8)
    context.getSharedPreferences(PREFS_BUSQUEDA, android.content.Context.MODE_PRIVATE).edit().putString("lista", lista.joinToString("\n")).apply()
    return lista
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
