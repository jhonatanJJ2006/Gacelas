package com.gacelas.correr

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

@SuppressLint("MissingPermission")
@Composable
fun CorrerScreen(onCerrar: () -> Unit, onEmpezar: () -> Unit) {
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
                    .addOnSuccessListener { l -> precision = l?.accuracy }
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
                Text("$n", style = GType.headlineLg.copy(fontSize = androidx.compose.ui.unit.TextUnit(96f, androidx.compose.ui.unit.TextUnitType.Sp)), color = GColors.Primary500,
                    modifier = Modifier.testTag("cuenta-regresiva"))
                TextButton(onClick = { cuenta = null; empezarCarrera(context, tracker); onEmpezar() }) { Text("Empezar ya") }
            }
        }
        return
    }
    PantallaCompleta {
        BotonCerrar(onCerrar)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IconoCirculo(Icons.AutoMirrored.Filled.DirectionsRun, GColors.Primary.copy(alpha = 0.2f), GColors.Primary500, 112.dp)
            Text("¿Listo para correr?", style = GType.headlineLg, color = GColors.TextPrimary)
            Text("Activa el GPS y comienza a grabar tu carrera", style = GType.bodyLg, color = GColors.TextSecondary, textAlign = TextAlign.Center)
        }
        val colorGps = if (permiso) GColors.Secondary500 else GColors.Primary500
        Tarjeta(fondo = colorGps.copy(alpha = 0.12f), borde = colorGps, onClick = if (permiso) null else ({ launcher.launch(permisosCarrera()) })) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoCirculo(if (permiso) Icons.Filled.GpsFixed else Icons.Filled.GpsOff, colorGps.copy(alpha = 0.25f), colorGps, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (permiso) "GPS Activado" else "Activar GPS", style = GType.titleMd, color = GColors.TextPrimary)
                    Text(senal, style = GType.bodySm, color = colorGps)
                }
                if (permiso) Icon(Icons.Filled.CheckCircle, null, tint = colorGps)
            }
        }
        Tarjeta {
            FilaDato("Tipo de actividad", "Carrera")
            FilaDato("Ubicación", "Loja, Ecuador")
            FilaDato("Grabación", if (permiso) "GPS en primer plano" else "Requiere permiso")
        }
        if (!notificaciones) com.gacelas.core.notificaciones.AvisoNotificacionesBloqueadas(
            "Puedes correr igual, pero no verás la notificación con tiempo, distancia y los botones Pausar/Terminar.",
            activarNotificaciones,
        )
        Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonRedondo(Icons.Filled.PlayArrow, "Comenzar", GColors.Primary, 112.dp, {
                if (!permiso) {
                    launcher.launch(permisosCarrera())
                } else {
                    if (!com.gacelas.core.notificaciones.PermisoNotificaciones.permitidas(context)) {
                        context.aviso("Notificaciones bloqueadas: no verás la carrera en la barra de Android")
                        if (com.gacelas.core.notificaciones.PermisoNotificaciones.sePuedePedir(context)) activarNotificaciones()
                    }
                    scopeGps.launch {
                        if (consentimientos.otorgado(com.gacelas.legal.TipoConsentimiento.GpsCarrera)) iniciar() else pedirGps = true
                    }
                }
            })
            Text(if (permiso) "Toca para comenzar" else "Toca para dar permiso de ubicación", style = GType.labelLg, color = GColors.TextSecondary)
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
            Text("Cronómetro Profesional", style = GType.titleMd, color = GColors.TextPrimary)
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
            Tarjeta(fondo = GColors.Secondary.copy(alpha = 0.1f), borde = GColors.Secondary.copy(alpha = 0.5f), padding = 12.dp) {
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
                drawCircle(GColors.SurfaceHigh, style = Stroke(6.dp.toPx()))
                drawArc(GColors.Primary500, -90f, 360f * progreso, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (estado.fase) {
                        FaseCarrera.Corriendo -> "En Marcha  •  ${ajustes.metronomoBpm} spm"
                        FaseCarrera.Pausada -> "En Pausa"
                        else -> "Sin carrera activa"
                    },
                    style = GType.labelMd, color = GColors.Secondary500,
                )
                Text(formatearMs(estado.segundos), style = GType.metricHuge, color = GColors.TextPrimary)
                val kmActual = estado.distanciaKm.toInt() + 1
                val segKm = estado.segundos - estado.parciales.filter { !it.manual }.sumOf { it.segundos }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Flag, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
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
            BotonRedondo(Icons.Filled.Flag, "Vuelta", GColors.SurfaceHigh, 64.dp, {
                if (estado.activa) tracker.vuelta() else context.aviso("Inicia una carrera para marcar vueltas")
            })
            BotonRedondo(
                if (estado.fase == FaseCarrera.Corriendo) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Pausa", GColors.Primary, 88.dp,
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
            Tarjeta { Text("Aún no hay parciales. Se agregan solos cada kilómetro o con el botón Vuelta.", style = GType.bodyMd, color = GColors.TextSecondary) }
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
    Tarjeta {
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
    Tarjeta {
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
        modifier.clip(CircleShape).background(if (activo) GColors.Primary else GColors.SurfaceContainer).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 8.dp),
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
    PantallaCompleta {
        BotonCerrar {
            descartarCarrera(context, tracker)
            onCerrar()
        }
        val record = remember(carrerasPrevias, estado.segundos) {
            if (estado.segundos > 0) com.gacelas.actividad.domain.reglas.EstadisticasPerfil.nuevoRecord(carrerasPrevias,
                com.gacelas.actividad.domain.modelo.Carrera(id = "nueva", titulo = "", inicioMs = estado.inicioMs, segundos = estado.segundos, distanciaKm = estado.distanciaKm, parciales = estado.parciales)) else null
        }
        Tarjeta(fondo = GColors.Primary.copy(alpha = 0.12f)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IconoCirculo(if (record != null) Icons.Filled.MilitaryTech else Icons.Filled.CheckCircle, GColors.Primary.copy(alpha = 0.2f), GColors.Primary500, 88.dp)
                Text(if (estado.segundos > 0) "¡Excelente zancada, gacela!" else "Carrera completada", style = GType.headlineMd, color = GColors.TextPrimary, textAlign = TextAlign.Center)
                Text(if (estado.segundos > 0) "Carrera completada" else "No hay datos de carrera para guardar", style = GType.bodyLg, color = GColors.TextSecondary)
                record?.let { Text("Nuevo récord personal en ${it.etiqueta}", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.testTag("nuevo-record")) }
                Text("Loja, Ecuador", style = GType.bodySm, color = GColors.TextSecondary)
            }
        }
        if (estado.segundos > 0) com.gacelas.ui.components.CelebracionRacha(rachaAntes, rachaDespues)
        if (estado.puntos.size >= 2) Tarjeta(padding = 8.dp) {
            MapaGacelas(estado.puntos.aLatLng(), Modifier.fillMaxWidth().height(180.dp)) { TrazadoRuta(estado.puntos, Modifier.fillMaxWidth().height(180.dp)) }
            val alturas = estado.puntos.mapNotNull { it.altitudM }
            if (alturas.size > 1) Text("${alturas.min().toInt()} m - ${alturas.max().toInt()} m", style = GType.labelMd, color = GColors.TextSecondary)
        }
        Tarjeta {
            Text("DISTANCIA TOTAL", style = GType.labelMd, color = GColors.TextSecondary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Unidades.texto(estado.distanciaKm, m), style = GType.headlineLg, color = GColors.TextPrimary)
                Text("  ${if (m) "Kilómetros" else "Millas"}", style = GType.labelLg, color = GColors.Primary500)
            }
        }
        val kcal = (estado.distanciaKm * 62).toInt()
        listOf(
            listOf(Triple("TIEMPO", formatearMs(estado.segundos), "Tiempo en movimiento"), Triple("RITMO MEDIO", formatearRitmo(Unidades.ritmo(estado.ritmoPromedioSegKm, m)), Unidades.etiquetaRitmo(m))),
            listOf(Triple("DESNIVEL (+)", "+${estado.desnivelM.toInt()} m", "Subida acumulada"), Triple("CALORÍAS", "$kcal", "kcal estimadas")),
        ).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { (t, v, d) ->
                    Tarjeta(Modifier.weight(1f), padding = 12.dp) {
                        Text(t, style = GType.labelMd, color = GColors.TextSecondary)
                        Text(v, style = GType.headlineMd, color = GColors.TextPrimary)
                        Text(d, style = GType.bodySm, color = GColors.TextSecondary)
                    }
                }
            }
        }
        CampoTexto("Título", titulo, { titulo = it }, "Título de la carrera (opcional)")
        CampoTexto("Notas", nota, { nota = it }, "¿Cómo te fue? Comparte tus pensamientos...", lineas = 3)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Hacer pública", style = GType.bodyLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            Interruptor(publica) { publica = it }
        }
        val explorar = c.explorar
        val perfilActual by com.gacelas.ui.sesionState()
        var comoRuta by rememberSaveable { mutableStateOf(false) }
        var dificultadRuta by rememberSaveable { mutableStateOf(com.gacelas.rutas.domain.modelo.Dificultad.Moderado) }
        var etiquetasRuta by rememberSaveable { mutableStateOf(listOf<String>()) }
        val puedeSerRuta = explorar != null && perfilActual != null && estado.puntos.size >= 2 && estado.distanciaKm >= 0.3
        if (puedeSerRuta) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Publicar también como ruta", style = GType.bodyLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
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
        BotonPrimario(if (guardando) "Guardando..." else "Guardar carrera", {
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
        }, icono = Icons.Filled.Save, enabled = !guardando)
        var compartiendo by remember { mutableStateOf(false) }
        if (estado.segundos > 0) BotonSecundario("Compartir en redes", { compartiendo = true })
        if (compartiendo) com.gacelas.compartir.CompartirDialogo(
            com.gacelas.compartir.DatosCompartir(
                com.gacelas.compartir.DatosCompartir.Tipo.Carrera,
                titulo.ifBlank { "Mi carrera" },
                // Privacidad: sin los primeros/ultimos 250 m (no revela la casa).
                com.gacelas.compartir.Compartir.recortarExtremos(estado.puntos.map { it.lat to it.lng }),
                estado.distanciaKm, estado.segundos, estado.ritmoPromedioSegKm, estado.desnivelM.toInt(), rachaDespues.diaria,
            ),
        ) { compartiendo = false }
        BotonSecundario("Descartar", salirSinGuardar, color = GColors.TextSecondary)
    }
}

/** Boton "Compartir en vivo": envia la posicion cada 5 s mientras se corre. */
@Composable
internal fun BotonCompartirEnVivo(tracker: com.gacelas.actividad.domain.SesionCarrera) {
    val repo = contenedor().enVivo ?: return
    val perfil by com.gacelas.ui.sesionState()
    if (perfil == null) return
    val estado by tracker.estado.collectAsState()
    val scope = rememberCoroutineScope()
    var activo by remember { mutableStateOf(repo.sesionId != null) }
    var visible by remember { mutableStateOf(repo.visible) }
    LaunchedEffect(activo, estado.puntos.size, estado.pausada) {
        val p = estado.puntos.lastOrNull()
        if (activo && !estado.pausada && p != null && repo.enviar(p.lat, p.lng, estado.distanciaKm, estado.segundos)) visible = repo.visible
    }
    androidx.compose.material3.TextButton(
        onClick = { scope.launch { if (activo) { repo.terminar(); activo = false } else activo = repo.iniciar() != null } },
        modifier = Modifier.height(44.dp).clip(CircleShape).background(if (activo) GColors.Danger else GColors.SurfaceContainer),
    ) {
        Icon(Icons.Filled.Sensors, null, tint = if (activo) Color.White else GColors.TextPrimary, modifier = Modifier.size(18.dp))
        Text(if (!activo) " Compartir en vivo" else if (visible == false) " En vivo · casa oculta" else " En vivo", style = GType.labelMd, color = if (activo) Color.White else GColors.TextPrimary)
    }
}
