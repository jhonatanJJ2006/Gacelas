package com.gacelas.shared

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import com.gacelas.acceso.BienvenidaC4
import com.gacelas.data.mock.MockData
import com.gacelas.data.eventos.yaPaso
import com.gacelas.navigation.Destinos
import com.gacelas.ui.components.ScaffoldPrincipal
import com.gacelas.acceso.BotonGoogleVisual
import com.gacelas.acceso.InicioSesionC4
import com.gacelas.data.auth.ValidacionAuth
import com.gacelas.ui.theme.GacelasTheme
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import com.gacelas.core.sensores.SeguimientoSensores
import com.gacelas.core.sensores.fuenteRumboNativa
import com.gacelas.core.sensores.fuenteUbicacionNativa
import kotlinx.coroutines.delay
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIViewController
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip

/** Argumento de lanzamiento (CI): abre directamente la pantalla de sensores con una carrera en curso. */
private fun argumento(a: String) = NSProcessInfo.processInfo.arguments.contains(a)

/** Punto de entrada de iOS: SwiftUI (iosApp) muestra este UIViewController de Compose. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val auto = remember { argumento("--sensores") }
    var enSensores by remember { mutableStateOf(auto) }
    var pantalla by remember {
        mutableStateOf(
            when {
                argumento("--pantalla=login") -> PantallaIos.Login
                argumento("--pantalla=inicio") -> PantallaIos.Inicio
                argumento("--pantalla=carrera") -> PantallaIos.Carrera
                argumento("--pantalla=modal") -> PantallaIos.Inicio
                argumento("--pantalla=ajustes") -> PantallaIos.Ajustes
                argumento("--pantalla=detalle") -> PantallaIos.Detalle
                argumento("--pantalla=rutas") -> PantallaIos.Rutas
                else -> PantallaIos.Bienvenida
            },
        )
    }
    if (!enSensores) {
        GacelasTheme {
            when (pantalla) {
                PantallaIos.Bienvenida -> BienvenidaC4(
                    version = versionIos(),
                    onSaltar = { pantalla = PantallaIos.Inicio },
                    onCrearCuenta = { pantalla = PantallaIos.Login },
                    onIniciarSesion = { pantalla = PantallaIos.Login },
                    botonGoogle = { BotonGoogleVisual(onClick = {}, cargando = false, portada = true) },
                )
                PantallaIos.Login -> LoginIos(onAtras = { pantalla = PantallaIos.Bienvenida })
                PantallaIos.Inicio, PantallaIos.Rutas -> androidx.compose.foundation.layout.Box {
                    ScaffoldPrincipal(
                        if (pantalla == PantallaIos.Rutas) Destinos.RUTAS else Destinos.INICIO,
                        sinLeer = MockData.notificaciones.count { !it.leida },
                        onNavegar = {
                            when (it) {
                                Destinos.CORRER -> pantalla = PantallaIos.Carrera
                                Destinos.INICIO -> pantalla = PantallaIos.Inicio
                                Destinos.RUTAS -> pantalla = PantallaIos.Rutas
                                Destinos.PERFIL -> pantalla = PantallaIos.Ajustes
                            }
                        },
                    ) {
                        if (pantalla == PantallaIos.Rutas) RutasIos() else InicioIos(onCorrer = { pantalla = PantallaIos.Carrera })
                    }
                    // Modal de actualizacion V3 (la descarga/instalacion en iOS va por App Store/TestFlight).
                    var modal by remember { mutableStateOf(argumento("--pantalla=modal")) }
                    if (modal && pantalla == PantallaIos.Inicio) com.gacelas.actualizacion.ContenidoModal(
                        "0.6.9", com.gacelas.correr.CarreraMuestra.version, obligatoria = false,
                        fase = com.gacelas.actualizacion.Fase.Info, bytes = com.gacelas.correr.CarreraMuestra.version.apkBytes,
                        onActualizar = {}, onMasTarde = { modal = false }, onPermiso = {},
                    )
                }
                PantallaIos.Ajustes -> AjustesIos(onAtras = { pantalla = PantallaIos.Inicio }, onCerrarSesion = { pantalla = PantallaIos.Bienvenida })
                PantallaIos.Detalle -> com.gacelas.perfil.DetalleCarreraContenido(
                    c = com.gacelas.correr.CarreraMuestra.guardada,
                    fecha = fechaLarga(com.gacelas.correr.CarreraMuestra.guardada.inicioMs), m = true,
                    onAtras = { pantalla = PantallaIos.Inicio }, onEliminar = { pantalla = PantallaIos.Inicio },
                    mapa = { c ->
                        com.gacelas.correr.TrazadoRuta(
                            c.puntos,
                            androidx.compose.ui.Modifier.fillMaxWidth().height(240.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                        )
                    },
                )
                PantallaIos.Carrera -> CarreraIos(panelInicial = argumento("--panel"), onSalir = { pantalla = PantallaIos.Inicio })
            }
        }
    } else {
        val gps = remember { fuenteUbicacionNativa() }
        val s = remember { SeguimientoSensores(gps, fuenteRumboNativa()) }
        DisposableEffect(s) { s.iniciar(carrera = auto); onDispose { s.detener() } }
        LaunchedEffect(s) { while (true) { delay(1000); s.tic() } }
        val e by s.estado.collectAsState()
        PantallaSensores(
            e,
            onCarrera = { if (s.tracker.estado.value.activa) s.tracker.finalizar() else s.tracker.iniciar(); s.tic() },
            onVolver = { enSensores = false },
        )
    }
}

private enum class PantallaIos { Bienvenida, Login, Inicio, Carrera, Ajustes, Detalle, Rutas }

/** "Versión 0.7.0 (700)", mismo formato que textoVersion() de Android, desde el Info.plist. */
private fun versionIos(): String {
    val info = NSBundle.mainBundle.infoDictionary
    val nombre = info?.get("CFBundleShortVersionString") as? String ?: VersionGacelas.NOMBRE
    val codigo = info?.get("CFBundleVersion") as? String ?: VersionGacelas.codigo().toString()
    return "Versión $nombre ($codigo)"
}

/**
 * Login C4 compartido. La sesion real (Supabase) y Google/Apple en iOS llegan en su etapa;
 * por ahora valida el formulario con las mismas reglas que Android.
 */
@Composable
private fun LoginIos(onAtras: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    InicioSesionC4(
        email = email, password = password, error = error, cargando = false,
        onEmail = { email = it; error = null }, onPassword = { password = it; error = null },
        onEntrar = { error = ValidacionAuth.email(email) ?: if (password.isEmpty()) "Ingresa tu contraseña" else "Iniciar sesión en iOS aún no está disponible" },
        onAtras = onAtras, onRecuperar = {}, onRegistro = {},
        botonGoogle = { BotonGoogleVisual(onClick = {}, cargando = false, portada = true, alto = 48.dp) },
    )
}

/**
 * Inicio compartido. Sin carreras guardadas todavia en iOS (llegan con la sesion). El 3D (SceneView
 * en Android) llega en su etapa; mientras, la misma imagen de reemplazo que usa Android sin 3D.
 */
@Composable
private fun InicioIos(onCorrer: () -> Unit) {
    val ahora = remember { ahoraMs() }
    val evento = remember {
        MockData.eventos.firstOrNull { !it.yaPaso(ahora) } ?: MockData.eventos.maxBy { it.inicioMs ?: 0L }
    }
    com.gacelas.inicio.InicioContenido(
        saludo = com.gacelas.inicio.saludoPorHora(Fechas.hora(ahora)),
        nombre = "Corredor",
        semana = remember { com.gacelas.data.carreras.Estadisticas.semana(emptyList(), ahora) },
        hoy = Fechas.diaSemana(ahora),
        m = true,
        racha = remember { com.gacelas.data.rachas.Rachas.calcular(emptyList(), ahora) },
        evento = evento,
        onCorrer = onCorrer, onEventos = {}, onEvento = {},
        corredor3D = { com.gacelas.ui.three.Runner3DFallback() },
    )
}

/**
 * Carrera V1 compartida con el tracker y el GPS reales de iOS (CoreLocation).
 * El mapa MapLibre iOS llega en la etapa 4: mientras, el mismo trazado de respaldo que usa
 * Android sin MapLibre (TrazadoRuta), con los controles del diseño.
 */
@Composable
private fun CarreraIos(panelInicial: Boolean, onSalir: () -> Unit) {
    val s = remember { SeguimientoSensores(fuenteUbicacionNativa(), fuenteRumboNativa()) }
    DisposableEffect(s) { s.iniciar(carrera = true); onDispose { s.detener() } }
    LaunchedEffect(s) { while (true) { delay(1000); s.tic() } }
    val real by s.tracker.estado.collectAsState()
    // --carrera-muestra (solo capturas de CI): los mismos datos fijos que la captura Android.
    val e = if (argumento("--carrera-muestra")) com.gacelas.correr.CarreraMuestra.estado else real
    com.gacelas.correr.ContenidoCarreraC4(
        com.gacelas.correr.VistaCarrera(e),
        onPausa = { s.tracker.alternarPausa(); s.tic() },
        onStop = { s.tracker.finalizar(); onSalir() },
        onVuelta = { s.tracker.vuelta() },
        onAtras = onSalir,
        panelInicial = panelInicial,
    ) { _, controles, _ ->
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier.fillMaxSize().background(com.gacelas.correr.K1.Fondo),
        ) {
            com.gacelas.correr.TrazadoRuta(e.puntos, androidx.compose.ui.Modifier.fillMaxSize())
            controles({}, {}, 0f, true)
        }
    }
}

/** "lunes 12 de octubre, 06:30" como SimpleDateFormat de Android (es-EC). */
private fun fechaLarga(ms: Long): String = platform.Foundation.NSDateFormatter().apply {
    locale = platform.Foundation.NSLocale(localeIdentifier = "es_EC")
    dateFormat = "EEEE d 'de' MMMM, HH:mm"
}.stringFromDate(NSDate.dateWithTimeIntervalSince1970(ms / 1000.0))

/** Ajustes compartidos; guardados en memoria hasta conectar DataStore/sesion en iOS. */
@Composable
private fun AjustesIos(onAtras: () -> Unit, onCerrarSesion: () -> Unit) {
    var a by remember { mutableStateOf(com.gacelas.data.ajustes.Ajustes()) }
    com.gacelas.ajustes.AjustesContenido(
        a = a, version = versionIos(), guardar = { t -> a = t(a) },
        onAtras = onAtras, onEditarPerfil = {}, onPrivacidad = {}, onNotificaciones = {},
        onSoporte = {
            platform.Foundation.NSURL.URLWithString("mailto:soporte@gacelas.app?subject=Ayuda%20con%20Gacelas")?.let {
                platform.UIKit.UIApplication.sharedApplication.openURL(it)
            }
        },
        onCerrarSesion = onCerrarSesion,
        // Sin MapLibre iOS todavia (etapa 4): la tarjeta como Android sin mapa configurado.
        mapaOffline = {
            com.gacelas.ajustes.TarjetaMapaOffline(
                nombre = "Loja, Ecuador", texto = "Descarga las calles de Loja (OpenStreetMap) para correr sin datos",
                lista = false, porcentaje = null, disponible = false, onDescargar = {}, onEliminar = {},
            )
        },
    )
}

/** Lista de rutas compartida con las rutas de ejemplo (las remotas llegan con la sesion). */
@Composable
private fun RutasIos() {
    var pestana by remember { mutableStateOf(com.gacelas.data.rutas.PestanaRutas.Explorar) }
    var consulta by remember { mutableStateOf("") }
    var filtros by remember { mutableStateOf(com.gacelas.data.rutas.FiltrosRuta()) }
    var guardadas by remember { mutableStateOf(emptySet<String>()) }
    val lista = com.gacelas.data.rutas.aplicarFiltros(
        com.gacelas.data.rutas.filtrarRutas(MockData.rutas, pestana, guardadas, MockData.ID_USUARIO, consulta), filtros,
    )
    com.gacelas.rutas.ListaRutasContenido(
        lista = lista, pestana = pestana, onPestana = { pestana = it }, guardadas = guardadas,
        consulta = consulta, onConsulta = { consulta = it }, filtros = filtros, onFiltros = { filtros = it },
        cerca = null, onCerca = {}, m = true, puedeDibujar = false,
        onBuscar = {}, onRuta = {}, onDibujar = {},
        onAlternarGuardada = { id, g -> guardadas = if (g) guardadas - id else guardadas + id },
        mapaRutas = {},
    )
}
