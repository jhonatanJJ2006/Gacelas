package com.gacelas.correr

import com.gacelas.ui.movimiento.entradaEscalonada
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.gacelas.ui.components.Pastilla

import com.gacelas.ui.components.Mapas

import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Sensors
import com.gacelas.ui.components.MapaGacelas
import com.gacelas.ui.components.aLatLng
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gacelas.core.location.LocationService
import com.gacelas.ajustes.permisosCarrera
import com.gacelas.identidad.domain.modelo.ConfigIntervalos
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.tracking.Avisos
import com.gacelas.actividad.domain.EstadoCarrera
import com.gacelas.actividad.domain.FaseCarrera
import com.gacelas.tracking.FaseIntervalo
import com.gacelas.tracking.PlanIntervalos
import com.gacelas.actividad.domain.SesionCarrera
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.tracking.intervaloMetronomoMs
import com.gacelas.ui.ajustesState
import com.gacelas.ui.aviso
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.EstadoC2
import com.gacelas.ui.components.BotonSecundario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.IconoCirculo
import com.gacelas.ui.components.Interruptor
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Inicia el seguimiento real: SesionCarrera + servicio de ubicacion en primer plano. */
fun empezarCarrera(context: Context, tracker: SesionCarrera) {
    if (!tracker.estado.value.activa) tracker.iniciar()
    LocationService.iniciar(context)
}

/** Detiene el servicio y deja la carrera lista para el resumen. */
private fun terminarEnVivo(context: Context) {
    val repo = (context.applicationContext as? com.gacelas.GacelasApp)?.contenedor?.enVivo ?: return
    if (repo.sesionId != null) kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { repo.terminar() }
}

fun terminarCarrera(context: Context, tracker: SesionCarrera) {
    terminarEnVivo(context)
    tracker.finalizar()
    LocationService.detener(context)
}

fun descartarCarrera(context: Context, tracker: SesionCarrera) {
    terminarEnVivo(context)
    LocationService.detener(context)
    tracker.reiniciar()
}

/** Mantiene la pantalla encendida mientras la carrera esta visible. */
@Composable
fun MantenerPantallaEncendida() {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/** Actualiza el tiempo del tracker cada segundo mientras la carrera esta activa. */
@Composable
fun RelojCarrera(tracker: SesionCarrera, estado: EstadoCarrera) {
    LaunchedEffect(estado.fase) {
        while (estado.fase == FaseCarrera.Corriendo) {
            tracker.actualizarTiempo()
            delay(1000)
        }
    }
}

@Composable
private fun PantallaCompleta(content: @Composable () -> Unit) {
    Column(
        // Fondo C2 como Inicio: degradado morado arriba que baja al azul de marca (nunca negro).
        Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF2A0A3D), Color(0xFF1A0B2E), GColors.BgPrimary)))
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

/** Tarjeta con los colores C2 de Inicio (fondo #191F2F, borde #2C3446, esquina cortada). */
@Composable
private fun TarjetaC2(
    modifier: Modifier = Modifier,
    fondo: Color = GColors.C2Card,
    borde: Color? = GColors.C2Linea,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) = com.gacelas.ui.components.Tarjeta(modifier, fondo = fondo, borde = borde, onClick = onClick, padding = padding, content = content)

@Composable
private fun BotonCerrar(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.clip(CircleShape).background(GColors.C2S2)) {
        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = GColors.TextPrimary)
    }
}

@Composable
private fun BotonRedondo(icono: ImageVector, descripcion: String, color: Color, tamano: Dp, onClick: () -> Unit, tint: Color = GColors.TextPrimary) {
    Box(Modifier.size(tamano).clip(CircleShape).background(color).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icono, contentDescription = descripcion, tint = tint, modifier = Modifier.size(tamano * 0.5f))
    }
}

@SuppressLint("MissingPermission")
@Composable
fun CorrerScreen(onCerrar: () -> Unit, onEmpezar: () -> Unit, onRuta: (String) -> Unit = {}, onPrivacidad: () -> Unit = {}) {
    val context = LocalContext.current
    val tracker = contenedor().tracker
    var permiso by remember { mutableStateOf(LocationService.tienePermiso(context)) }
    var permisoDenegado by remember { mutableStateOf(false) }
    if (permisoDenegado) com.gacelas.ui.components.ModalC2(
        icono = androidx.compose.material.icons.Icons.Filled.LocationOff, titulo = "Permiso de ubicación",
        texto = "Sin permiso de ubicación no se puede grabar la carrera. Actívalo en los ajustes del teléfono.",
        primario = "Abrir ajustes", onCerrar = { permisoDenegado = false }, secundario = "Ahora no", tag = "modal-permiso",
        onPrimario = {
            permisoDenegado = false
            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null)))
        },
    )
    var precision by remember { mutableStateOf<Float?>(null) }
    var posicion by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val notificaciones by com.gacelas.core.notificaciones.notificacionesPermitidasState()
    val activarNotificaciones = com.gacelas.core.notificaciones.rememberActivarNotificaciones()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permiso = LocationService.tienePermiso(context)
        if (!permiso) permisoDenegado = true
    }
    LaunchedEffect(permiso) {
        if (permiso) {
            runCatching {
                LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { l -> precision = l?.accuracy; posicion = l?.let { it.latitude to it.longitude } }
            }
        }
    }
    val senal = when {
        !permiso -> "Permiso de ubicación pendiente"
        precision == null -> "Buscando señal GPS..."
        precision!! <= 10f -> "Señal fuerte (±${precision!!.toInt()} m)"
        precision!! <= 30f -> "Señal media (±${precision!!.toInt()} m)"
        else -> "Señal débil (±${precision!!.toInt()} m)"
    }
    val ajustesCorrer by ajustesState()
    var cuenta by remember { mutableStateOf<Int?>(null) }
    val consentimientos = contenedor().consentimientos
    val scopeGps = rememberCoroutineScope()
    var pedirGps by remember { mutableStateOf(false) }
    val iniciar = {
        if (ajustesCorrer.cuentaRegresivaSeg > 0) cuenta = ajustesCorrer.cuentaRegresivaSeg
        else { empezarCarrera(context, tracker); onEmpezar() }
    }
    if (pedirGps) com.gacelas.ui.components.ModalC2(
        icono = Icons.Filled.GpsFixed, titulo = "Usar tu ubicación durante la carrera",
        texto = "Runcity usa el GPS del teléfono solo mientras grabas una carrera, para medir distancia, ritmo y dibujar tu recorrido. La ruta se guarda en tu historial y solo se comparte si tú la publicas. Nadie te ve en el mapa en vivo salvo que actives ese permiso aparte. Puedes revocarlo cuando quieras en Ajustes, Privacidad.",
        primario = "Aceptar y empezar", secundario = "Ahora no", tag = "modal-consentimiento-gps",
        onCerrar = { pedirGps = false },
        onPrimario = {
            pedirGps = false
            scopeGps.launch { consentimientos.registrar(com.gacelas.legal.TipoConsentimiento.GpsCarrera, true); iniciar() }
        },
    )
    LaunchedEffect(cuenta) {
        val n = cuenta ?: return@LaunchedEffect
        kotlinx.coroutines.delay(1000)
        if (n <= 1) { cuenta = null; empezarCarrera(context, tracker); onEmpezar() } else cuenta = n - 1
    }
    cuenta?.let { n ->
        PantallaCompleta {
            Column(Modifier.fillMaxWidth().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Prepárate, arrancamos en", style = GType.bodyLg, color = GColors.TextSecondary)
                // Cada numero "late": entra grande (1,5x) y transparente y se asienta en 350 ms.
                val escalaMov = com.gacelas.ui.movimiento.Mov.escala
                val latido = remember(n) { androidx.compose.animation.core.Animatable(if (escalaMov <= 0f) 1f else 0f) }
                LaunchedEffect(n) { latido.animateTo(1f, com.gacelas.ui.movimiento.MovimientoC2.spec(com.gacelas.ui.movimiento.MovimientoC2.LARGO, escalaMov, com.gacelas.ui.movimiento.MovimientoC2.Entrar)) }
                Text("$n", style = GType.headlineLg.copy(fontSize = androidx.compose.ui.unit.TextUnit(96f, androidx.compose.ui.unit.TextUnitType.Sp)), color = GColors.Primary500,
                    modifier = Modifier.graphicsLayer { val p = latido.value; alpha = p; scaleX = 1.5f - 0.5f * p; scaleY = scaleX }.testTag("cuenta-regresiva"))
                TextButton(onClick = { cuenta = null; empezarCarrera(context, tracker); onEmpezar() }) { Text("Empezar ya") }
            }
        }
        return
    }
    val pedirPermisoOEmpezar = {
        if (!permiso) {
            launcher.launch(permisosCarrera())
        } else {
            if (!com.gacelas.core.notificaciones.PermisoNotificaciones.permitidas(context)) {
                context.aviso("Notificaciones bloqueadas: no verás la carrera en la barra de Android")
                if (com.gacelas.core.notificaciones.PermisoNotificaciones.sePuedePedir(context)) activarNotificaciones()
            }
            scopeGps.launch {
                // Se pregunta una sola vez: si no esta en el telefono (p. ej. tras reinstalar) se busca en el servidor.
                val gps = com.gacelas.legal.TipoConsentimiento.GpsCarrera
                if (consentimientos.otorgado(gps) || (consentimientos.sincronizar() && consentimientos.otorgado(gps))) iniciar() else pedirGps = true
            }
        }
        Unit
    }
    PreCarrera(
        permiso = permiso, precision = precision, posicion = posicion, notificaciones = notificaciones,
        onActivarNotificaciones = activarNotificaciones, onCerrar = onCerrar, onEmpezar = pedirPermisoOEmpezar,
        onPermiso = { launcher.launch(permisosCarrera()) }, onRuta = onRuta, onPrivacidad = onPrivacidad,
    )
}

/**
 * Correr, pre-carrera (rediseno movil): mapa a pantalla completa con la posicion y la ruta segura del
 * dia, y una hoja inferior pegada al borde con la ruta, el objetivo, el estado de Voz, En vivo y SOS, y
 * el boton EMPEZAR. Todo sale de datos reales: ajustes, historial y la ruta sugerida.
 */
@Composable
private fun PreCarrera(
    permiso: Boolean, precision: Float?, posicion: Pair<Double, Double>?, notificaciones: Boolean,
    onActivarNotificaciones: () -> Unit, onCerrar: () -> Unit, onEmpezar: () -> Unit, onPermiso: () -> Unit,
    onRuta: (String) -> Unit, onPrivacidad: () -> Unit,
) {
    val c = contenedor()
    val ajustes by ajustesState()
    val ajustesVm = com.gacelas.ui.ajustesViewModel()
    val m = ajustes.unidadesMetricas
    val carreras by com.gacelas.ui.carrerasState()
    val ahora = remember { System.currentTimeMillis() }
    val semana = remember(carreras) { com.gacelas.actividad.domain.reglas.EstadisticasPerfil.totalesSemana(carreras, ahora) }
    val racha = com.gacelas.ui.rachaState()
    val ritmoMedio = remember(carreras) {
        val ultimas = carreras.sortedByDescending { it.inicioMs }.take(10).filter { it.distanciaKm > 0.2 }
        if (ultimas.isEmpty()) null else (ultimas.sumOf { it.segundos } / ultimas.sumOf { it.distanciaKm }).toInt()
    }
    val delDia by androidx.compose.runtime.produceState<com.gacelas.rutas.infrastructure.RutaDelDia?>(null, carreras.size) {
        value = runCatching {
            c.rutaSugerida.hoy(
                com.gacelas.data.mock.MockData.rutas,
                carreras.map { r -> com.gacelas.rutas.domain.reglas.CarreraSugerida(r.inicioMs, r.distanciaKm, r.puntos.firstOrNull()?.let { it.lat to it.lng }) },
                System.currentTimeMillis(), posicion,
            )
        }.getOrNull()
    }
    val ruta = (delDia as? com.gacelas.rutas.infrastructure.RutaDelDia.Ok)?.ruta
    val trazoRuta = remember(ruta) { ruta?.puntos?.ifEmpty { null } ?: ruta?.let { com.gacelas.rutas.infrastructure.TrazosRutas.puntos[it.id] }.orEmpty() }
    val objetivo = ObjetivoCarrera.de(ajustes.objetivoTipo, ajustes.objetivoValor)
    val elegir = { o: ObjetivoCarrera -> ajustesVm.actualizar { it.copy(objetivoTipo = o.tipo.clave, objetivoValor = o.valor) } }
    // Modo elegido en la hoja: Ruta del dia (sigue la ruta segura), Objetivo (distancia, tiempo o ritmo) o Libre.
    // Sin eleccion explicita: la ruta del dia si existe; si no, el objetivo guardado o Libre.
    var modoElegido by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val modo = modoElegido ?: when { ruta != null -> "ruta"; objetivo.tipo != ObjetivoCarrera.Tipo.Libre -> "objetivo"; else -> "libre" }
    var editarObjetivo by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var vista3d by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(ajustes.vistaMapa3d) }
    var reportar by remember { mutableStateOf(false) }
    val conRuta = modo == "ruta" && ruta != null
    Box(Modifier.fillMaxSize().background(com.gacelas.ui.components.Tablero.Fondo).testTag("pantalla-correr")) {
        val plan = trazoRuta.map { org.maplibre.android.geometry.LatLng(it.first, it.second) }
        val controles: @Composable BoxScope.(() -> Unit, () -> Unit, Float, Boolean) -> Unit = { recentrar, norte, rumbo, siguiendo ->
            ControlesMapaCorrer(vista3d, { vista3d = it; ajustesVm.actualizar { a -> a.copy(vistaMapa3d = it) } }, recentrar, norte, rumbo, siguiendo, onReportar = { reportar = true })
        }
        MapaGacelas(
            posicion?.let { listOf(org.maplibre.android.geometry.LatLng(it.first, it.second)) }.orEmpty(), Modifier.fillMaxSize(),
            seguir = posicion != null, zoom = 15f, modo3d = vista3d, plan = if (modo == "libre") emptyList() else plan, controles = false, avisoArriba = 110.dp,
            controlesPropios = controles,
        ) {
            com.gacelas.ui.components.MapaEsquemaC2(if (modo == "libre") emptyList() else trazoRuta, Modifier.fillMaxSize(), yo = posicion ?: trazoRuta.getOrNull((trazoRuta.size * 0.3).toInt()),
                margenArriba = 70.dp, margenAbajo = 400.dp)
            Box(Modifier.fillMaxSize()) { controles({}, {}, 0f, true) }
        }
        if (reportar) com.gacelas.seguridad.ui.HojaReportarIncidente(posicion, onCerrar = { reportar = false })
        // Encima del mapa: volver y estado del GPS.
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.gacelas.ui.components.BotonVolverMapa(onCerrar)
                val (texto, color) = when {
                    !permiso -> "Ubicación sin permiso" to GColors.C2Peligro
                    precision == null -> "Buscando señal GPS" to GColors.C2Muted
                    precision <= 30f -> "GPS listo · ± ${precision.toInt()} m" to GColors.C2Menta
                    else -> "GPS débil · ± ${precision.toInt()} m" to Color(0xFFF59E0B)
                }
                com.gacelas.ui.components.ChipVidrio(texto, onClick = { if (!permiso) onPermiso() }, punto = color, tag = "chip-gps-correr")
            }
            if (!notificaciones) com.gacelas.core.notificaciones.AvisoNotificacionesBloqueadas(
                "Puedes correr igual, pero no verás la notificación con tiempo, distancia y los botones Pausar/Terminar.",
                onActivarNotificaciones,
            )
        }
        com.gacelas.ui.components.HojaTablero(Modifier.align(Alignment.BottomCenter).verticalScroll(rememberScrollState()), tag = "hoja-correr") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.gacelas.ui.components.RotuloWidget("Elige cómo corres", Modifier.weight(1f))
                Text("Editar", style = GType.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = GColors.C2Violeta,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { editarObjetivo = true }.padding(horizontal = 6.dp, vertical = 4.dp).testTag("correr-editar"))
            }
            Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val objetivoVisible = if (objetivo.tipo == ObjetivoCarrera.Tipo.Libre) ObjetivoCarrera.predeterminado(ObjetivoCarrera.Tipo.Distancia, ritmoMedio) else objetivo
                if (ruta != null) ModoCorrer("Ruta del día", "${ruta.nombre.substringBefore(' ').take(10)} · ${Unidades.texto(ruta.distanciaKm, m, 1).replace('.', ',')} ${Unidades.etiquetaDistancia(m)}", Icons.Filled.Map, modo == "ruta", Modifier.weight(1f), "modo-ruta") {
                    modoElegido = "ruta"; elegir(ObjetivoCarrera(ObjetivoCarrera.Tipo.Distancia, (ruta.distanciaKm * 1000).toInt().coerceAtLeast(1000)))
                }
                ModoCorrer("Objetivo", "${objetivoVisible.tipo.titulo} ${objetivoVisible.valorTexto(m)}", Icons.Filled.Flag, modo == "objetivo", Modifier.weight(1f), "modo-objetivo") {
                    if (modo == "objetivo") editarObjetivo = true else { modoElegido = "objetivo"; elegir(objetivoVisible) }
                }
                ModoCorrer("Libre", "Sin meta", Icons.Filled.PlayArrow, modo == "libre", Modifier.weight(1f), "modo-libre") {
                    modoElegido = "libre"; elegir(ObjetivoCarrera(ObjetivoCarrera.Tipo.Libre, 0))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EstadoTablero("Voz", if (ajustes.anunciosVoz) "Cada km" else "Apagada", Icons.AutoMirrored.Filled.VolumeUp, ajustes.anunciosVoz, Modifier.weight(1f), "estado-voz") {
                    ajustesVm.actualizar { it.copy(anunciosVoz = !it.anunciosVoz) }
                }
                EstadoTablero("En vivo", when (ajustes.compartirEnVivo) { true -> "Seguidores"; false -> "No compartes"; null -> "Se pregunta" }, Icons.Filled.Sensors,
                    ajustes.compartirEnVivo == true, Modifier.weight(1f), "estado-en-vivo", onPrivacidad)
                EstadoTablero("SOS", if (ajustes.contactosEmergencia.isEmpty()) "Solo 911" else "${ajustes.contactosEmergencia.size} contacto${if (ajustes.contactosEmergencia.size == 1) "" else "s"}",
                    Icons.Filled.Shield, true, Modifier.weight(1f), "estado-sos", onPrivacidad)
            }
            // Efecto real sobre la meta semanal y la racha.
            val meta = ajustes.metaKmSemana.toDouble()
            val corrioHoy = racha.ultimoDiaMs?.let { com.gacelas.actividad.domain.reglas.Rachas.inicioDia(it) == com.gacelas.actividad.domain.reglas.Rachas.inicioDia(ahora) } == true
            val sumaKm = if (modo != "libre" && objetivo.tipo == ObjetivoCarrera.Tipo.Distancia) objetivo.valor / 1000.0 else null
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp).testTag("efecto-meta"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                Text(
                    buildString {
                        if (sumaKm != null) append("Con ${Unidades.texto(sumaKm, m, 1)} ${Unidades.etiquetaDistancia(m)} llegas a ${Unidades.texto(semana.km + sumaKm, m, 1)} de ${Unidades.texto(meta, m, 0)} ${Unidades.etiquetaDistancia(m)} esta semana".replace('.', ','))
                        else append("Llevas ${Unidades.texto(semana.km, m, 1)} de ${Unidades.texto(meta, m, 0)} ${Unidades.etiquetaDistancia(m)} esta semana".replace('.', ','))
                        append(if (corrioHoy) ". Hoy ya sumaste a tu racha." else if (racha.diaria > 0) " y mantienes tu racha de ${racha.diaria} días." else ".")
                    },
                    style = GType.bodySm.copy(fontSize = 11.5.sp), color = Color.White.copy(alpha = 0.85f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Cuenta regresiva real (ajustes): tocar cambia entre 0, 3, 5 y 10 s.
                val seg = ajustes.cuentaRegresivaSeg
                Column(
                    Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f)).border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                        .clickable(onClickLabel = "Cambiar cuenta regresiva") {
                            val siguiente = listOf(0, 3, 5, 10).let { l -> l[(l.indexOf(seg).coerceAtLeast(0) + 1) % l.size] }
                            ajustesVm.actualizar { it.copy(cuentaRegresivaSeg = siguiente) }
                        }.testTag("correr-cuenta"),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Text(if (seg == 0) "YA" else "$seg", style = androidx.compose.ui.text.TextStyle(fontFamily = com.gacelas.ui.theme.SpaceGrotesk, fontSize = 22.sp), color = Color.White)
                    Text(if (seg == 0) "SIN CUENTA" else "SEG", style = GType.labelMd.copy(fontSize = 8.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = Color.White.copy(alpha = 0.6f), maxLines = 1)
                }
                com.gacelas.ui.components.BotonTablero(
                    when {
                        !permiso -> "Dar permiso"
                        conRuta -> "Empezar ruta"
                        else -> "Empezar"
                    },
                    onEmpezar, Modifier.weight(1f), alto = 64.dp, tag = "boton-empezar",
                )
            }
        }
        if (editarObjetivo) com.gacelas.ui.components.HojaC2(onCerrar = { editarObjetivo = false }) { Column(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp).testTag("hoja-objetivo"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tu objetivo", style = GType.headlineSm, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ObjetivoCarrera.Tipo.entries.forEach { t ->
                    val elegido = objetivo.tipo == t
                    val o = if (elegido) objetivo else ObjetivoCarrera.predeterminado(t, ritmoMedio)
                    com.gacelas.ui.components.OpcionTarjetaC2(t.titulo, o.valorTexto(m), elegido, { elegir(o); modoElegido = if (t == ObjetivoCarrera.Tipo.Libre) "libre" else "objetivo" }, Modifier.weight(1f), tag = "objetivo-${t.clave}")
                }
            }
            if (objetivo.activo) AjusteObjetivo(objetivo, m) { elegir(it) }
            com.gacelas.ui.components.BotonTablero("Listo", { editarObjetivo = false }, icono = null, tag = "objetivo-listo")
        } }
    }
}

/** Tarjeta de modo de la hoja de Correr (activa en degradado). */
@Composable
private fun ModoCorrer(titulo: String, detalle: String, icono: ImageVector, activo: Boolean, modifier: Modifier, tag: String, onClick: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxHeight().clip(forma)
            .then(if (activo) Modifier.background(com.gacelas.ui.components.GradienteC2).border(2.dp, Color.White.copy(alpha = 0.6f), forma) else Modifier.background(Color.White.copy(alpha = 0.06f)).border(1.dp, Color.White.copy(alpha = 0.1f), forma))
            .clickable(role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick).semantics { selected = activo }.testTag(tag).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icono, null, tint = if (activo) Color.White else GColors.C2Violeta, modifier = Modifier.size(20.dp))
        com.gacelas.ui.components.TextoAjustableC2(titulo, GType.labelLg.copy(fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), Color.White, minimo = 0.7f)
        Text(detalle, style = GType.bodySm.copy(fontSize = 11.sp), color = Color.White.copy(alpha = if (activo) 0.9f else 0.6f), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** Estado tocable (Voz, En vivo, SOS) de la hoja de Correr. */
@Composable
private fun EstadoTablero(titulo: String, detalle: String, icono: ImageVector, activo: Boolean, modifier: Modifier, tag: String, onClick: () -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier.clip(forma).background(Color.White.copy(alpha = 0.06f)).border(1.dp, Color.White.copy(alpha = 0.1f), forma)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).testTag(tag).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icono, null, tint = if (activo) GColors.C2Menta else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        Column {
            com.gacelas.ui.components.TextoAjustableC2(titulo, GType.labelLg.copy(fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), Color.White, minimo = 0.6f)
            com.gacelas.ui.components.TextoAjustableC2(detalle, GType.bodySm.copy(fontSize = 10.5.sp), Color.White.copy(alpha = 0.6f), minimo = 0.6f)
        }
    }
}

/** Columna de controles del mapa de Correr: MAPA (centrar, norte, 3D/2D) y REPORTAR debajo. */
@Composable
private fun BoxScope.ControlesMapaCorrer(vista3d: Boolean, onVista: (Boolean) -> Unit, recentrar: () -> Unit, norte: () -> Unit, rumbo: Float, siguiendo: Boolean, onReportar: () -> Unit) {
    Column(
        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 12.dp, top = 64.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        com.gacelas.correr.GrupoControlesC4("MAPA") {
            com.gacelas.correr.ControlEtiquetadoC4("Centrar", "Centrar el mapa en tu posición", recentrar, "boton-recentrar", activo = !siguiendo) {
                Icon(Icons.Filled.MyLocation, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            com.gacelas.correr.ControlEtiquetadoC4("Norte", "Orientar el mapa al norte", norte, "boton-brujula") {
                Icon(Icons.Filled.Navigation, null, tint = Color.White, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = -rumbo })
            }
            com.gacelas.correr.SelectorVistaC4(vista3d, onVista)
        }
        com.gacelas.ui.components.BotonReportarMapa(onReportar)
    }
}

/** Ajuste fino del objetivo elegido: menos y mas. */
@Composable
private fun AjusteObjetivo(o: ObjetivoCarrera, m: Boolean, onCambiar: (ObjetivoCarrera) -> Unit) {
    val (paso, min, max) = when (o.tipo) {
        ObjetivoCarrera.Tipo.Distancia -> Triple(1000, 1000, 42_000)
        ObjetivoCarrera.Tipo.Tiempo -> Triple(5 * 60, 10 * 60, 300 * 60)
        ObjetivoCarrera.Tipo.Ritmo -> Triple(5, 150, 900)
        ObjetivoCarrera.Tipo.Libre -> Triple(0, 0, 0)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${o.tipo.titulo}: ${o.valorTexto(m)}", style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f).testTag("objetivo-valor"))
        com.gacelas.ui.components.BotonChipC2("Menos", { onCambiar(o.copy(valor = (o.valor - paso).coerceIn(minOf(min, max), maxOf(min, max)))) }, activo = true)
        com.gacelas.ui.components.BotonChipC2("Más", { onCambiar(o.copy(valor = (o.valor + paso).coerceIn(minOf(min, max), maxOf(min, max)))) })
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
private fun DialogoDescartar(onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    com.gacelas.ui.components.ModalC2(
        icono = androidx.compose.material.icons.Icons.Filled.Delete, titulo = "¿Descartar la carrera?",
        texto = "Se perderá el recorrido grabado hasta ahora. Esta acción no se puede deshacer.",
        primario = "Descartar", onPrimario = onConfirmar, onCerrar = onCancelar,
        secundario = "Seguir corriendo", peligro = true, tag = "modal-descartar",
    )
}

@Composable
fun CarreraEnCursoScreen(onCerrar: () -> Unit, onFinalizar: () -> Unit, @Suppress("UNUSED_PARAMETER") onCronometro: () -> Unit) {
    // Diseño V1 "Barra superior" (Stitch): el cronometro ahora es un panel lateral dentro de la pantalla.
    CarreraEnCursoC4(onCerrar, onFinalizar)
}

@Composable
internal fun DialogoDescartarCarrera(onConfirmar: () -> Unit, onCancelar: () -> Unit) = DialogoDescartar(onConfirmar, onCancelar)

private enum class PestanaCrono(val etiqueta: String, val icono: ImageVector) {
    Splits("Splits", Icons.AutoMirrored.Filled.ListAlt),
    Intervalos("Intervalos", Icons.Filled.Repeat),
    Metronomo("Metrónomo", Icons.Filled.GraphicEq),
}

private enum class ModoCrono(val etiqueta: String, val icono: ImageVector) {
    Libre("Modo Libre", Icons.AutoMirrored.Filled.DirectionsRun),
    Fartlek("Fartlek", Icons.Filled.Timer3),
    Pacer("Ritmo Pacer", Icons.Filled.Speed),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CronometroScreen(onAtras: () -> Unit, onFin: () -> Unit) {
    val context = LocalContext.current
    val c = contenedor()
    val tracker = c.tracker
    val estado by tracker.estado.collectAsState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val scope = rememberCoroutineScope()
    val avisos = remember { Avisos(context) }
    DisposableEffect(Unit) { onDispose { avisos.liberar() } }
    var pestana by rememberSaveable { mutableStateOf(PestanaCrono.Splits) }
    var modo by rememberSaveable { mutableStateOf(ModoCrono.Libre) }
    MantenerPantallaEncendida()
    RelojCarrera(tracker, estado)
    val objetivoSeg = ajustes.ritmoObjetivoSeg
    val ritmo = estado.ritmoActualSegKm ?: estado.ritmoPromedioSegKm
    val progreso = (estado.segundos / 3600f).coerceIn(0f, 1f)
    PantallaCompleta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = GColors.TextPrimary) }
            Text("CRONÓMETRO", style = GType.headlineLg, color = GColors.TextPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModoCrono.entries.forEach { mo ->
                ChipModo(mo.icono, mo.etiqueta, mo == modo, Modifier.weight(1f)) {
                    modo = mo
                    if (mo == ModoCrono.Fartlek) pestana = PestanaCrono.Intervalos
                }
            }
        }
        if (modo == ModoCrono.Pacer) {
            val dif = ritmo?.let { it - objetivoSeg }
            TarjetaC2(padding = 12.dp) {
                Text("Objetivo Pacer: ${formatearRitmo(Unidades.ritmo(objetivoSeg, m))} ${Unidades.etiquetaRitmo(m)}", style = GType.labelLg, color = GColors.TextPrimary)
                Text(
                    when {
                        dif == null -> "Empieza a moverte para comparar tu ritmo"
                        dif <= 0 -> "${-dif}s más rápido que el objetivo"
                        else -> "${dif}s más lento que el objetivo"
                    },
                    style = GType.bodySm, color = if (dif != null && dif > 0) GColors.Primary500 else GColors.Secondary500,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(240.dp)) {
                drawCircle(GColors.C2S2, style = Stroke(6.dp.toPx()))
                drawArc(
                    androidx.compose.ui.graphics.Brush.sweepGradient(listOf(Color(0xFF7B1FA2), GColors.C2Violeta, Color(0xFF7B1FA2))),
                    -90f, 360f * progreso, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (estado.fase) {
                        FaseCarrera.Corriendo -> "En Marcha  •  ${ajustes.metronomoBpm} spm"
                        FaseCarrera.Pausada -> "En Pausa"
                        else -> "Sin carrera activa"
                    },
                    style = GType.labelMd, color = GColors.C2Menta,
                )
                Text(formatearMs(estado.segundos), style = GType.metricHuge, color = GColors.TextPrimary)
                val kmActual = estado.distanciaKm.toInt() + 1
                val segKm = estado.segundos - estado.parciales.filter { !it.manual }.sumOf { it.segundos }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Flag, null, tint = GColors.C2Violeta, modifier = Modifier.size(14.dp))
                    Text(" Km $kmActual en curso: ${formatearMs(segKm)}", style = GType.labelMd, color = GColors.TextSecondary)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CeldaMetrica(Icons.Filled.Straighten, "Distancia Total", "${Unidades.texto(estado.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}", Modifier.weight(1f))
            CeldaMetrica(Icons.Filled.Speed, "Ritmo Actual", "${formatearRitmo(Unidades.ritmo(estado.ritmoActualSegKm, m))} ${Unidades.etiquetaRitmo(m)}", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CeldaMetrica(Icons.AutoMirrored.Filled.TrendingUp, "Ritmo Promedio", "${formatearRitmo(Unidades.ritmo(estado.ritmoPromedioSegKm, m))} ${Unidades.etiquetaRitmo(m)}", Modifier.weight(1f))
            CeldaMetrica(Icons.Filled.Favorite, "Desnivel", "${estado.desnivelM.toInt()} m", Modifier.weight(1f), GColors.Danger)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            BotonRedondo(Icons.Filled.Flag, "Vuelta", GColors.C2S2, 64.dp, {
                if (estado.activa) tracker.vuelta() else context.aviso("Inicia una carrera para marcar vueltas")
            })
            BotonRedondo(
                if (estado.fase == FaseCarrera.Corriendo) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (estado.fase == FaseCarrera.Corriendo) "Pausar" else "Reanudar", Color(0xFF7B1FA2), 88.dp,
                {
                    if (estado.activa) tracker.alternarPausa() else if (LocationService.tienePermiso(context)) empezarCarrera(context, tracker) else context.aviso("Da permiso de ubicación desde Correr")
                },
            )
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(GColors.Danger).combinedClickable(
                    onClick = { context.aviso("Mantén presionado Fin para guardar") },
                    onLongClick = {
                        if (estado.activa) {
                            terminarCarrera(context, tracker)
                            onFin()
                        } else {
                            context.aviso("No hay una carrera activa")
                        }
                    },
                ),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Stop, "Fin", tint = GColors.TextPrimary, modifier = Modifier.size(32.dp)) }
        }
        Text("Mantén presionado Fin por 2 seg para guardar tu sesión.", style = GType.bodySm, color = GColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().clip(com.gacelas.ui.components.FormaC2).background(GColors.C2Card).border(1.dp, GColors.C2Linea, com.gacelas.ui.components.FormaC2).padding(4.dp)) {
            PestanaCrono.entries.forEach { p ->
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(if (p == pestana) GColors.C2S2 else Color.Transparent)
                        .clickable { pestana = p }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(p.icono, null, tint = if (p == pestana) GColors.C2Violeta else GColors.C2Muted, modifier = Modifier.size(16.dp))
                    Text(" ${p.etiqueta}", style = GType.labelMd, color = if (p == pestana) GColors.TextPrimary else GColors.C2Muted)
                }
            }
        }
        when (pestana) {
            PestanaCrono.Splits -> TablaParciales(estado, m)
            PestanaCrono.Intervalos -> PanelIntervalos(ajustes.intervalos, avisos) { nuevo -> scope.launch { c.ajustes.actualizar { it.copy(intervalos = nuevo) } } }
            PestanaCrono.Metronomo -> PanelMetronomo(ajustes.metronomoBpm, avisos) { bpm -> scope.launch { c.ajustes.actualizar { it.copy(metronomoBpm = bpm) } } }
        }
    }
}

@Composable
private fun TablaParciales(estado: EstadoCarrera, metricas: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.padding(horizontal = 8.dp)) {
            Text("PARCIAL / DISTANCIA", style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
            Text("RITMO    TIEMPO", style = GType.labelMd, color = GColors.TextSecondary)
        }
        if (estado.parciales.isEmpty()) {
            TarjetaC2 { Text("Aún no hay parciales. Se agregan solos cada kilómetro o con el botón Vuelta.", style = GType.bodyMd, color = GColors.TextSecondary) }
        }
        estado.parciales.reversed().forEach { s ->
            val ritmo = if (s.distanciaKm > 0.01) (s.segundos / s.distanciaKm).toInt() else null
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceLow).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(if (s.manual) GColors.PrimaryContainer else GColors.SurfaceHigh), contentAlignment = Alignment.Center) {
                    Text("${s.numero}", style = GType.labelMd, color = GColors.OnSurface)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${Unidades.texto(s.distanciaKm, metricas)} ${Unidades.etiquetaDistancia(metricas)}", style = GType.labelLg, color = GColors.TextPrimary)
                    Text(if (s.manual) "Vuelta manual" else "Kilómetro ${s.numero}", style = GType.bodySm, color = GColors.TextSecondary)
                }
                Text(formatearRitmo(Unidades.ritmo(ritmo, metricas)), style = GType.labelLg, color = GColors.TextPrimary)
                Spacer(Modifier.width(20.dp))
                Text(formatearMs(s.segundos), style = GType.labelLg, color = GColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun Paso(etiqueta: String, valor: String, onMenos: () -> Unit, onMas: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
        IconButton(onClick = onMenos) { Icon(Icons.Filled.Remove, "Menos $etiqueta", tint = GColors.TextPrimary) }
        Text(valor, style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
        IconButton(onClick = onMas) { Icon(Icons.Filled.Add, "Más $etiqueta", tint = GColors.TextPrimary) }
    }
}

@Composable
private fun PanelIntervalos(config: ConfigIntervalos, avisos: Avisos, onCambio: (ConfigIntervalos) -> Unit) {
    var activo by rememberSaveable { mutableStateOf(false) }
    var transcurrido by rememberSaveable { mutableIntStateOf(0) }
    val plan = remember(config) { PlanIntervalos(config) }
    val estado = plan.estado(transcurrido)
    LaunchedEffect(activo, config) {
        var faseAnterior = plan.estado(transcurrido).fase
        while (activo) {
            delay(1000)
            transcurrido++
            val nueva = plan.estado(transcurrido).fase
            if (nueva != faseAnterior) {
                avisos.cambioFase(config.sonido, config.vibracion)
                faseAnterior = nueva
            }
            if (nueva == FaseIntervalo.Terminado) activo = false
        }
    }
    TarjetaC2 {
        val color = when (estado.fase) {
            FaseIntervalo.Trabajo -> GColors.Primary500
            FaseIntervalo.Descanso -> GColors.Secondary500
            FaseIntervalo.Terminado -> GColors.TextSecondary
        }
        Text(
            when {
                !activo && transcurrido == 0 -> "Listo: ${config.rondas} rondas de ${formatearMs(config.trabajoSeg)} / ${formatearMs(config.descansoSeg)}"
                estado.fase == FaseIntervalo.Terminado -> "Entrenamiento completado"
                else -> "${if (estado.fase == FaseIntervalo.Trabajo) "Trabajo" else "Descanso"}  ·  Ronda ${estado.ronda} de ${config.rondas}"
            },
            style = GType.titleMd, color = color,
        )
        if (transcurrido > 0 && estado.fase != FaseIntervalo.Terminado) Text(formatearMs(estado.restanteSeg), style = GType.headlineXl, color = GColors.TextPrimary)
        Paso("Trabajo", formatearMs(config.trabajoSeg), { onCambio(config.copy(trabajoSeg = (config.trabajoSeg - 15).coerceAtLeast(15))) }, { onCambio(config.copy(trabajoSeg = config.trabajoSeg + 15)) })
        Paso("Descanso", formatearMs(config.descansoSeg), { onCambio(config.copy(descansoSeg = (config.descansoSeg - 15).coerceAtLeast(0))) }, { onCambio(config.copy(descansoSeg = config.descansoSeg + 15)) })
        Paso("Rondas", "${config.rondas}", { onCambio(config.copy(rondas = (config.rondas - 1).coerceAtLeast(1))) }, { onCambio(config.copy(rondas = config.rondas + 1)) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vibración", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
            Interruptor(config.vibracion) { onCambio(config.copy(vibracion = it)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sonido", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
            Interruptor(config.sonido) { onCambio(config.copy(sonido = it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonPrimario(if (activo) "Pausar" else if (transcurrido > 0 && estado.fase != FaseIntervalo.Terminado) "Continuar" else "Iniciar", {
                if (estado.fase == FaseIntervalo.Terminado) transcurrido = 0
                if (!activo && transcurrido == 0) avisos.cambioFase(config.sonido, config.vibracion)
                activo = !activo
            }, Modifier.weight(1f), icono = if (activo) Icons.Filled.Pause else Icons.Filled.PlayArrow)
            BotonSecundario("Reiniciar", { activo = false; transcurrido = 0 }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PanelMetronomo(bpm: Int, avisos: Avisos, onBpm: (Int) -> Unit) {
    var sonando by rememberSaveable { mutableStateOf(false) }
    var pulso by remember { mutableStateOf(false) }
    LaunchedEffect(sonando, bpm) {
        while (sonando) {
            avisos.tick()
            pulso = !pulso
            delay(intervaloMetronomoMs(bpm))
        }
    }
    TarjetaC2 {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(if (sonando && pulso) GColors.Primary500 else GColors.SurfaceHigh))
            Text("  $bpm pasos por minuto", style = GType.titleMd, color = GColors.TextPrimary)
        }
        Slider(
            value = bpm.toFloat(), onValueChange = { onBpm(it.toInt()) }, valueRange = 120f..200f,
            colors = SliderDefaults.colors(thumbColor = GColors.Primary, activeTrackColor = GColors.Primary, inactiveTrackColor = GColors.SurfaceHigh),
        )
        Paso("Ajuste fino", "$bpm", { onBpm((bpm - 1).coerceAtLeast(120)) }, { onBpm((bpm + 1).coerceAtMost(200)) })
        Text("Una cadencia de 170 a 180 pasos por minuto ayuda a reducir el impacto en las rodillas.", style = GType.bodySm, color = GColors.TextSecondary)
        BotonPrimario(if (sonando) "Detener metrónomo" else "Iniciar metrónomo", { sonando = !sonando }, icono = if (sonando) Icons.Filled.Stop else Icons.Filled.PlayArrow)
    }
}

@Composable
private fun ChipModo(icono: ImageVector, texto: String, activo: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(com.gacelas.ui.components.FormaBotonC2).background(if (activo) Color(0xFF7B1FA2) else GColors.C2Card)
            .border(1.dp, if (activo) GColors.C2Violeta else GColors.C2Linea, com.gacelas.ui.components.FormaBotonC2).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, null, tint = if (activo) GColors.TextPrimary else GColors.C2Violeta, modifier = Modifier.size(16.dp))
        Text(" $texto", style = GType.labelMd, color = if (activo) GColors.TextPrimary else GColors.C2Muted, maxLines = 1)
    }
}

@Composable
private fun CeldaMetrica(icono: ImageVector, etiqueta: String, valor: String, modifier: Modifier, color: Color = GColors.C2Violeta) {
    Column(modifier.clip(com.gacelas.ui.components.FormaC2).background(GColors.C2Card).border(1.dp, GColors.C2Linea, com.gacelas.ui.components.FormaC2).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
            Text(" ${etiqueta.uppercase()}", style = GType.labelMd, color = GColors.C2Muted)
        }
        Text(valor, style = GType.headlineSm, color = GColors.TextPrimary)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ResumenPostCarreraScreen(onCerrar: () -> Unit, onGuardar: () -> Unit, onDescartar: () -> Unit) {
    val context = LocalContext.current
    val c = contenedor()
    val tracker = c.tracker
    val estado by tracker.estado.collectAsState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val scope = rememberCoroutineScope()
    var titulo by rememberSaveable { mutableStateOf("") }
    var nota by rememberSaveable { mutableStateOf("") }
    var publica by rememberSaveable { mutableStateOf(ajustes.visibilidad != com.gacelas.identidad.domain.modelo.Visibilidad.Privado) }
    var guardando by remember { mutableStateOf(false) }
    val carrerasPrevias by com.gacelas.ui.carrerasState()
    val rachaAntes = remember(carrerasPrevias, ajustes.metaSemanal) { com.gacelas.actividad.domain.reglas.Rachas.calcular(carrerasPrevias, System.currentTimeMillis(), ajustes.metaSemanal) }
    val rachaDespues = remember(carrerasPrevias, ajustes.metaSemanal) {
        val ahora = System.currentTimeMillis()
        com.gacelas.actividad.domain.reglas.Rachas.calcular(carrerasPrevias + com.gacelas.actividad.domain.modelo.Carrera(id = "nueva", titulo = "", inicioMs = ahora, segundos = 0, distanciaKm = 0.0), ahora, ajustes.metaSemanal)
    }
    val salirSinGuardar = {
        descartarCarrera(context, tracker)
        onDescartar()
    }
    val record = remember(carrerasPrevias, estado.segundos) {
        if (estado.segundos > 0) com.gacelas.actividad.domain.reglas.EstadisticasPerfil.nuevoRecord(carrerasPrevias,
            com.gacelas.actividad.domain.modelo.Carrera(id = "nueva", titulo = "", inicioMs = estado.inicioMs, segundos = estado.segundos, distanciaKm = estado.distanciaKm, parciales = estado.parciales)) else null
    }
    val explorar = c.explorar
    val perfilActual by com.gacelas.ui.sesionState()
    var comoRuta by rememberSaveable { mutableStateOf(false) }
    var dificultadRuta by rememberSaveable { mutableStateOf(com.gacelas.rutas.domain.modelo.Dificultad.Moderado) }
    var etiquetasRuta by rememberSaveable { mutableStateOf(listOf<String>()) }
    val puedeSerRuta = explorar != null && perfilActual != null && estado.puntos.size >= 2 && estado.distanciaKm >= 0.3
    var compartiendo by remember { mutableStateOf(false) }
    val guardar = {
        if (estado.segundos == 0) {
            context.aviso("No hay una carrera para guardar")
            salirSinGuardar()
        } else {
            guardando = true
            scope.launch {
                val carrera = tracker.aCarrera(titulo, nota, publica)
                c.carreras.guardar(carrera)
                com.gacelas.rutas.infrastructure.RutaActiva.id?.let { r ->
                    com.gacelas.rutas.infrastructure.RutaActiva.id = null
                    explorar?.registrarCorrida(r, carrera.id)
                }
                if (comoRuta && puedeSerRuta) {
                    runCatching {
                        val trazo = com.gacelas.rutas.domain.reglas.trazoParaRuta(estado.puntos.map { it.lat to it.lng })
                        val perfil = com.gacelas.rutas.infrastructure.Elevacion.perfil(trazo)
                        explorar!!.crear(
                            titulo.ifBlank { carrera.titulo }, nota, dificultadRuta, etiquetasRuta,
                            estado.puntos.map { it.lat to it.lng },
                            if (perfil.size > 1) com.gacelas.rutas.infrastructure.Elevacion.desnivelPositivo(perfil) else estado.desnivelM.toInt(), carrera.id, perfil,
                        )
                    }.onSuccess { context.aviso("Ruta publicada") }.onFailure { context.aviso("No se pudo publicar la ruta: ${it.message}") }
                }
                tracker.reiniciar()
                context.aviso("Carrera guardada")
                onGuardar()
            }
        }
        Unit
    }
    val objetivo = ObjetivoCarrera.de(ajustes.objetivoTipo, ajustes.objetivoValor)
    val ahora = remember { System.currentTimeMillis() }
    // Media real de tus ultimas 10 carreras (para comparar el ritmo de hoy).
    val ritmoMedio = remember(carrerasPrevias) {
        val ultimas = carrerasPrevias.sortedByDescending { it.inicioMs }.take(10).filter { it.distanciaKm > 0.2 }
        if (ultimas.isEmpty()) null else (ultimas.sumOf { it.segundos } / ultimas.sumOf { it.distanciaKm }).toInt()
    }
    val semana = remember(carrerasPrevias) { com.gacelas.actividad.domain.reglas.EstadisticasPerfil.totalesSemana(carrerasPrevias, ahora) }
    val horaTexto = remember(estado.inicioMs) {
        val ms = if (estado.inicioMs > 0) estado.inicioMs else ahora
        java.text.SimpleDateFormat("EEEE d MMM · HH:mm", java.util.Locale("es", "EC")).format(java.util.Date(ms))
    }
    var vista3d by rememberSaveable { mutableStateOf(false) }
    val kmParciales = estado.parciales.filter { !it.manual && it.distanciaKm > 0 }
    Box(Modifier.fillMaxSize().background(com.gacelas.ui.components.Tablero.Fondo).testTag("pantalla-resumen")) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Mapa del recorrido arriba con volver y Ver en 3D; chips, titulo y fecha sobre el degradado.
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                val puntosMapa = estado.puntos.map { it.lat to it.lng }
                if (estado.puntos.size >= 2) MapaGacelas(estado.puntos.aLatLng(), Modifier.fillMaxSize(), controles = false, seguir = vista3d, modo3d = vista3d) {
                    com.gacelas.ui.components.MapaEsquemaC2(puntosMapa, Modifier.fillMaxSize().graphicsLayer { if (vista3d) { rotationX = 38f; cameraDistance = 12f * density } }, grosor = 4.dp,
                        margenArriba = 50.dp, margenAbajo = 110.dp)
                } else com.gacelas.ui.components.MapaEsquemaC2(emptyList(), Modifier.fillMaxSize())
                Box(Modifier.fillMaxWidth().height(150.dp).align(Alignment.BottomCenter).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, com.gacelas.ui.components.Tablero.Noche))))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.gacelas.ui.components.BotonVolverMapa({ descartarCarrera(context, tracker); onCerrar() })
                    Spacer(Modifier.weight(1f))
                    if (estado.puntos.size >= 2) Row(
                        Modifier.height(44.dp).vidrioResumen().clickable(onClickLabel = if (vista3d) "Ver en 2D" else "Ver en 3D") { vista3d = !vista3d }.testTag("resumen-3d").padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Layers, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(if (vista3d) "  Ver en 2D" else "  Ver en 3D", style = GType.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Color.White)
                    }
                }
                if (estado.puntos.size < 2) Text("Sin recorrido GPS", style = GType.labelLg, color = GColors.C2Muted, modifier = Modifier.align(Alignment.Center))
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (estado.segundos == 0) ChipLogro("Sin datos", GColors.C2Muted, "chip-sin-datos")
                        if (objetivo.activo && objetivo.cumplido(estado)) ChipLogro("Objetivo cumplido", GColors.C2Menta, "chip-objetivo-cumplido")
                        record?.let { ChipLogro("Récord ${it.etiqueta}", Color(0xFFF59E0B), "nuevo-record") }
                    }
                    Text(titulo.ifBlank { if (estado.segundos > 0) tituloSugerido(estado.inicioMs.takeIf { it > 0 } ?: ahora) else "Carrera completada" }, style = GType.headlineLg.copy(fontSize = 30.sp, lineHeight = 34.sp), color = Color.White, maxLines = 2)
                    Text(if (estado.segundos > 0) horaTexto.replaceFirstChar { it.uppercase() } else "No hay datos de carrera para guardar", style = GType.bodySm, color = Color.White.copy(alpha = 0.65f))
                }
            }
            Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Distancia y tiempo en el mosaico grande en degradado.
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(com.gacelas.ui.components.GradienteC2).padding(16.dp).testTag("resumen-principal"),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f)) {
                        com.gacelas.ui.components.RotuloWidget("Distancia", color = Color.White.copy(alpha = 0.85f))
                        Row(verticalAlignment = Alignment.Bottom) {
                            com.gacelas.ui.movimiento.CifraAnimada(estado.distanciaKm, { Unidades.texto(it, m).replace('.', ',') }, androidx.compose.ui.text.TextStyle(fontFamily = com.gacelas.ui.theme.SpaceGrotesk, fontSize = 62.sp, lineHeight = 64.sp), Color.White)
                            Text(" ${Unidades.etiquetaDistancia(m)}", style = GType.labelLg, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        com.gacelas.ui.components.RotuloWidget("Tiempo", color = Color.White.copy(alpha = 0.85f))
                        Text(if (estado.segundos < 3600) formatearMs(estado.segundos) else com.gacelas.tracking.formatearHms(estado.segundos), style = androidx.compose.ui.text.TextStyle(fontFamily = com.gacelas.ui.theme.SpaceGrotesk, fontSize = 32.sp), color = Color.White, modifier = Modifier.testTag("resumen-tiempo"))
                    }
                }
                Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.gacelas.ui.components.WidgetC2("Ritmo medio", Modifier.weight(1f).fillMaxHeight(), tag = "resumen-ritmo-medio") {
                        com.gacelas.ui.components.CifraTablero(formatearRitmo(Unidades.ritmo(estado.ritmoPromedioSegKm, m)), Unidades.etiquetaRitmo(m), tamano = 32, color = GColors.C2Menta)
                        val hoyRitmo = estado.ritmoPromedioSegKm
                        if (hoyRitmo != null && ritmoMedio != null) {
                            val d = ritmoMedio - hoyRitmo
                            Text(
                                when { d > 0 -> "${formatearMs(d)} más rápido que tu media"; d < 0 -> "${formatearMs(-d)} más lento que tu media"; else -> "Igual que tu media" },
                                style = GType.bodySm.copy(fontSize = 11.5.sp), color = if (d >= 0) GColors.C2Menta else Color.White.copy(alpha = 0.6f), modifier = Modifier.testTag("ritmo-vs-media"),
                            )
                        }
                    }
                    val meta = ajustes.metaKmSemana.toDouble().coerceAtLeast(1.0)
                    val total = semana.km + estado.distanciaKm
                    com.gacelas.ui.components.WidgetC2("Meta semanal", Modifier.weight(1f).fillMaxHeight(), tag = "resumen-meta") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            com.gacelas.ui.components.AnilloC2((total / meta).toFloat().coerceIn(0f, 1f), tamano = 62.dp, grosor = 8.dp) {
                                Text("${((total / meta) * 100).toInt().coerceAtMost(999)}%", style = GType.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Color.White)
                            }
                            Column {
                                com.gacelas.ui.components.TextoAjustableC2("${Unidades.texto(total, m, 1)} de ${Unidades.texto(meta, m, 0)} ${Unidades.etiquetaDistancia(m)}".replace('.', ','), GType.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), Color.White, Modifier, minimo = 0.6f)
                                Text(if (total >= meta) "Meta cumplida" else "faltan ${Unidades.texto(meta - total, m, 1).replace('.', ',')}", style = GType.bodySm.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
                // Parciales por km en mosaicos (verde el mejor).
                if (kmParciales.isNotEmpty()) com.gacelas.ui.components.WidgetC2("Parciales por km", Modifier.fillMaxWidth(), tag = "resumen-parciales", accion = {
                    val ritmos = kmParciales.map { it.segundos / it.distanciaKm }
                    Text("Mejor km ${kmParciales[ritmos.indexOf(ritmos.min())].numero}", style = GType.labelMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = GColors.C2Menta)
                }) {
                    val ritmos = kmParciales.map { it.segundos / it.distanciaKm }
                    val mejor = ritmos.indexOf(ritmos.min()); val peor = ritmos.max(); val rapido = ritmos.min()
                    Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                        kmParciales.takeLast(8).forEachIndexed { i, p ->
                            val idx = kmParciales.size - minOf(8, kmParciales.size) + i
                            val r = ritmos[idx]
                            val f = if (peor - rapido < 1.0) 0.7f else (0.55f + 0.45f * ((peor - r) / (peor - rapido)).toFloat())
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(formatearMs(p.segundos), style = GType.labelMd.copy(fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = if (idx == mejor) GColors.C2Menta else Color.White.copy(alpha = 0.85f), maxLines = 1)
                                Box(Modifier.fillMaxWidth().height((52 * f).dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (idx == mejor) androidx.compose.ui.graphics.SolidColor(GColors.C2Menta) else com.gacelas.ui.components.GradienteC2))
                                Text("${p.numero}", style = GType.labelMd.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
                val alturas = estado.puntos.mapNotNull { it.altitudM }
                Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (alturas.size > 2) com.gacelas.ui.components.WidgetC2("Elevación", Modifier.weight(1f).fillMaxHeight(), tag = "resumen-elevacion") {
                        com.gacelas.ui.components.CifraTablero("+${estado.desnivelM.toInt()}", "m", tamano = 26, color = GColors.C2Cielo)
                        com.gacelas.ui.components.LineaC2(alturas.map { it.toFloat() }, Modifier.height(48.dp))
                    }
                    val peso = ajustes.pesoKg
                    com.gacelas.ui.components.WidgetC2(if (peso != null) "Energía" else "Racha", Modifier.weight(1f).fillMaxHeight(), tag = "resumen-energia") {
                        if (peso != null) {
                            com.gacelas.ui.components.CifraTablero("${(peso * estado.distanciaKm * 1.036).toInt()}", "kcal", tamano = 26)
                            Text("Según tu peso (${peso.toInt()} kg)", style = GType.bodySm.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.6f))
                        } else com.gacelas.ui.components.CifraTablero("${rachaDespues.diaria}", if (rachaDespues.diaria == 1) "día" else "días", tamano = 26)
                        if (estado.segundos > 0 && rachaDespues.diaria > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Text(" Racha de ${rachaDespues.diaria} día${if (rachaDespues.diaria == 1) "" else "s"}", style = GType.labelMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Color(0xFFF59E0B))
                        }
                    }
                }
                com.gacelas.ui.components.WidgetC2("Cuéntalo", Modifier.fillMaxWidth(), tag = "resumen-cuentalo") {
                    CampoTexto("Título", titulo, { titulo = it }, "Título de la carrera (opcional)")
                    CampoTexto("Notas", nota, { nota = it }, "¿Cómo te fue? Comparte tus pensamientos...", lineas = 3)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Pública en Comunidad", style = GType.bodyMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                        Interruptor(publica) { publica = it }
                    }
                    if (puedeSerRuta) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Publicar como ruta", style = GType.bodyMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                            Interruptor(comoRuta) { comoRuta = it }
                        }
                        if (comoRuta) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.gacelas.rutas.domain.modelo.Dificultad.entries.forEach { d -> com.gacelas.ui.components.Pastilla(d.etiqueta, d == dificultadRuta, { dificultadRuta = d }) }
                        }
                        if (comoRuta) androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.gacelas.rutas.domain.modelo.ETIQUETAS_RUTA.forEach { (k, v) ->
                                com.gacelas.ui.components.Pastilla("#$v", k in etiquetasRuta, { etiquetasRuta = if (k in etiquetasRuta) etiquetasRuta - k else etiquetasRuta + k })
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (estado.segundos > 0) Box(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.07f)).clickable { compartiendo = true }.testTag("resumen-instagram"),
                        contentAlignment = Alignment.Center,
                    ) { Text("Historia para Instagram", style = GType.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Color.White, maxLines = 1) }
                    Box(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x33BE185D)).clickable(onClick = salirSinGuardar).testTag("resumen-descartar"),
                        contentAlignment = Alignment.Center,
                    ) { Text("Descartar carrera", style = GType.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Color(0xFFF9A8D4), maxLines = 1) }
                }
            }
        }
        // Barra inferior fija: Compartir y GUARDAR Y PUBLICAR.
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, com.gacelas.ui.components.Tablero.Noche3)))
                .navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp).testTag("barra-guardar"),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            if (estado.segundos > 0) Row(
                Modifier.height(58.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF1C1433)).border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .clickable { compartiendo = true }.padding(horizontal = 16.dp).testTag("boton-compartir"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("  Compartir", style = GType.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Color.White)
            }
            com.gacelas.ui.components.BotonTablero(
                if (guardando) "Guardando..." else if (publica) "Guardar y publicar" else "Guardar carrera", guardar,
                Modifier.weight(1f), icono = Icons.Filled.Save, enabled = !guardando, tag = "boton-guardar",
            )
        }
    }
    if (compartiendo) com.gacelas.compartir.CompartirDialogo(
        com.gacelas.compartir.DatosCompartir(
            com.gacelas.compartir.DatosCompartir.Tipo.Carrera,
            titulo.ifBlank { "Mi carrera" },
            // Privacidad: sin los primeros/ultimos 250 m (no revela la casa).
            com.gacelas.compartir.Compartir.recortarExtremos(estado.puntos.map { it.lat to it.lng }),
            estado.distanciaKm, estado.segundos, estado.ritmoPromedioSegKm, estado.desnivelM.toInt(), rachaDespues.diaria,
        ),
    ) { compartiendo = false }
}

/**
 * Compartir en vivo sin boton en el mapa: la primera vez que se abre la carrera se pregunta con un modal
 * y la respuesta queda en Ajustes (sincronizada en user_settings.prefs). Se cambia desde Privacidad.
 */
@Composable
internal fun CompartirEnVivoAutomatico(tracker: com.gacelas.actividad.domain.SesionCarrera) {
    val c = contenedor()
    val repo = c.enVivo ?: return
    val perfil by com.gacelas.ui.sesionState()
    if (perfil == null) return
    // null mientras DataStore no responde: evita mostrar el modal a quien ya contesto.
    val cargados by c.ajustes.ajustes.collectAsState(initial = null)
    val ajustesVm = com.gacelas.ui.ajustesViewModel()
    val a = cargados ?: return
    val decision = a.compartirEnVivo
    val scope = rememberCoroutineScope()
    val visible = com.gacelas.legal.TipoConsentimiento.UbicacionVisible
    // Sin decision en Ajustes (p. ej. tras reinstalar): antes de preguntar se busca la respuesta ya
    // dada en este u otro dispositivo (consentimiento ubicacion_visible en el servidor).
    var buscando by remember { mutableStateOf(true) }
    LaunchedEffect(decision) {
        if (decision != null) { buscando = false; return@LaunchedEffect }
        val previa = c.consentimientos.decision(visible)
            ?: run { c.consentimientos.sincronizar(); c.consentimientos.decision(visible) }
        if (previa != null) ajustesVm.actualizar { it.copy(compartirEnVivo = previa) }
        buscando = false
    }
    if (decision == null) {
        if (buscando) return
        val responder = { v: Boolean ->
            ajustesVm.actualizar { it.copy(compartirEnVivo = v) }
            scope.launch { c.consentimientos.registrar(visible, v) }
            Unit
        }
        ModalCompartirEnVivo(onSi = { responder(true) }, onNo = { responder(false) })
        return
    }
    val estado by tracker.estado.collectAsState()
    var activo by remember { mutableStateOf(repo.sesionId != null) }
    LaunchedEffect(decision) {
        if (decision && !activo) activo = repo.iniciar() != null
        if (!decision && activo) { repo.terminar(); activo = false }
    }
    LaunchedEffect(activo, estado.puntos.size, estado.pausada) {
        val p = estado.puntos.lastOrNull()
        if (activo && !estado.pausada && p != null) repo.enviar(p.lat, p.lng, estado.distanciaKm, estado.segundos)
    }
}

@Composable
internal fun ModalCompartirEnVivo(onSi: () -> Unit, onNo: () -> Unit) {
    com.gacelas.ui.components.ModalC2(
        icono = Icons.Filled.Sensors, titulo = "¿Compartir tu carrera en vivo?",
        texto = "Tus seguidores podrán ver tu posición mientras corres. La zona de tu casa siempre se oculta. Puedes cambiarlo cuando quieras en Configuración > Privacidad.",
        primario = "Sí, compartir", onPrimario = onSi, onCerrar = onNo,
        secundario = "No, gracias", tag = "modal-compartir-en-vivo",
    )
}


/** Chip de logro del resumen (objetivo cumplido, record). */
@Composable
private fun ChipLogro(texto: String, color: Color, tag: String) {
    Text(texto.uppercase(), style = GType.labelMd.copy(fontSize = 10.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Black), color = Color(0xFF0C1322),
        modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(color).padding(horizontal = 10.dp, vertical = 5.dp).testTag(tag))
}

private fun Modifier.vidrioResumen(): Modifier = with(com.gacelas.ui.components.Tablero) { this@vidrioResumen.clip(RoundedCornerShape(16.dp)).background(Vidrio).border(1.dp, VidrioBorde, RoundedCornerShape(16.dp)) }

/** "Mañana en Loja", "Tarde en Loja" o "Noche en Loja" segun la hora de inicio. */
internal fun tituloSugerido(inicioMs: Long): String {
    val h = java.util.Calendar.getInstance().apply { timeInMillis = inicioMs }.get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) { in 5..11 -> "Mañana en Loja"; in 12..18 -> "Tarde en Loja"; else -> "Noche en Loja" }
}
