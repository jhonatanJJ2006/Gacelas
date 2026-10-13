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
import com.gacelas.comunidad.domain.reglas.yaPaso
import com.gacelas.navigation.Destinos
import com.gacelas.ui.components.ScaffoldPrincipal
import com.gacelas.acceso.BotonGoogleVisual
import com.gacelas.acceso.InicioSesionC4
import com.gacelas.cuenta.domain.reglas.ValidacionAuth
import com.gacelas.ui.theme.GacelasTheme
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import com.gacelas.core.sensores.SeguimientoSensores
import com.gacelas.core.sensores.fuenteRumboNativa
import com.gacelas.core.sensores.fuenteUbicacionNativa
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
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
                argumento("--pantalla=registro") -> PantallaIos.Registro
                argumento("--pantalla=recuperar") -> PantallaIos.Recuperar
                else -> PantallaIos.Bienvenida
            },
        )
    }
    // Sesion guardada (Supabase o local): al abrir sin pantalla pedida, entra directo a Inicio como Android.
    val sinPantalla = remember { NSProcessInfo.processInfo.arguments.none { (it as? String)?.startsWith("--") == true } }
    LaunchedEffect(Unit) {
        (com.gacelas.acceso.SesionIos.auth as? com.gacelas.cuenta.data.SupabaseAuthRepository)?.let { runCatching { it.restaurar() } }
        if (sinPantalla && pantalla == PantallaIos.Bienvenida && com.gacelas.acceso.SesionIos.auth.sesion.firstOrNull() != null) pantalla = PantallaIos.Inicio
    }
    if (!enSensores) {
        GacelasTheme {
            when (pantalla) {
                PantallaIos.Bienvenida -> BienvenidaC4(
                    version = versionIos(),
                    onSaltar = { pantalla = PantallaIos.Inicio },
                    onCrearCuenta = { pantalla = PantallaIos.Registro },
                    onIniciarSesion = { pantalla = PantallaIos.Login },
                    botonGoogle = { com.gacelas.acceso.BotonesSocialesIos({ pantalla = PantallaIos.Inicio }, portada = true) },
                )
                PantallaIos.Login -> LoginIos(
                    onAtras = { pantalla = PantallaIos.Bienvenida }, onEntrar = { pantalla = PantallaIos.Inicio },
                    onRecuperar = { pantalla = PantallaIos.Recuperar }, onRegistro = { pantalla = PantallaIos.Registro },
                )
                PantallaIos.Registro -> RegistroIos(
                    onAtras = { pantalla = PantallaIos.Bienvenida }, onCrear = { pantalla = PantallaIos.Inicio },
                    onIniciarSesion = { pantalla = PantallaIos.Login },
                )
                PantallaIos.Recuperar -> RecuperarIos(onAtras = { pantalla = PantallaIos.Login })
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
                        // Mismo mapa que DetalleCarrera en Android; sin MapLibre, el trazado de respaldo.
                        val mod = androidx.compose.ui.Modifier.fillMaxWidth().height(240.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        com.gacelas.mapa.MapaGacelasIos(c.puntos.map { com.gacelas.mapa.Coord(it.lat, it.lng) }, mod) {
                            com.gacelas.correr.TrazadoRuta(c.puntos, mod)
                        }
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

private enum class PantallaIos { Bienvenida, Login, Registro, Recuperar, Inicio, Carrera, Ajustes, Detalle, Rutas }

/** "Versión 0.7.0 (700)", mismo formato que textoVersion() de Android, desde el Info.plist. */
private fun versionIos(): String {
    val info = NSBundle.mainBundle.infoDictionary
    val nombre = info?.get("CFBundleShortVersionString") as? String ?: VersionGacelas.NOMBRE
    val codigo = info?.get("CFBundleVersion") as? String ?: VersionGacelas.codigo().toString()
    return "Versión $nombre ($codigo)"
}

/**
 * Login C4 compartido con el mismo LoginViewModel que Android (Supabase o cuentas locales).
 */
@Composable
private fun LoginIos(onAtras: () -> Unit, onEntrar: () -> Unit, onRecuperar: () -> Unit, onRegistro: () -> Unit) {
    val auth = com.gacelas.acceso.SesionIos.auth
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { com.gacelas.acceso.SesionIos.contenedor.loginViewModel(auth) }
    val e by vm.estado.collectAsState()
    InicioSesionC4(
        email = e.email, password = e.password, error = e.error, cargando = e.cargando,
        onEmail = vm::email, onPassword = vm::password,
        onEntrar = { vm.entrar { onEntrar() } },
        onAtras = onAtras, onRecuperar = onRecuperar, onRegistro = onRegistro,
        botonGoogle = { com.gacelas.acceso.BotonesSocialesIos(onEntrar, portada = true, alto = 48.dp) },
    )
}

/** Crear cuenta compartida (RegistroContenido) con el mismo RegistroViewModel que Android. */
@Composable
private fun RegistroIos(onAtras: () -> Unit, onCrear: () -> Unit, onIniciarSesion: () -> Unit) {
    val auth = com.gacelas.acceso.SesionIos.auth
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { com.gacelas.acceso.SesionIos.contenedor.registroViewModel(auth) }
    val e by vm.estado.collectAsState()
    com.gacelas.acceso.RegistroContenido(
        nombre = e.nombre, email = e.email, password = e.password, error = e.error, cargando = e.cargando,
        onNombre = vm::nombre, onEmail = vm::email, onPassword = vm::password,
        onCrear = { vm.crear { onCrear() } },
        onAtras = onAtras, onIniciarSesion = onIniciarSesion,
        botonGoogle = { com.gacelas.acceso.BotonesSocialesIos(onCrear) },
    )
}

/** Recuperar contraseña compartida (RecuperarContenido) con RecuperarViewModel. */
@Composable
private fun RecuperarIos(onAtras: () -> Unit) {
    val auth = com.gacelas.acceso.SesionIos.auth
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { com.gacelas.acceso.SesionIos.contenedor.recuperarViewModel(a = auth) }
    val e by vm.estado.collectAsState()
    com.gacelas.acceso.RecuperarContenido(
        email = e.email, enviado = e.enviado, error = e.error, onEmail = vm::email,
        onEnviar = vm::enviar,
        onAtras = onAtras,
    )
}

/**
 * Inicio compartido. Sin carreras guardadas todavia en iOS (llegan con la sesion). El 3D (SceneView
 * en Android) llega en su etapa; mientras, la misma imagen de reemplazo que usa Android sin 3D.
 */
@Composable
private fun InicioIos(onCorrer: () -> Unit) {
    val perfil by com.gacelas.acceso.SesionIos.auth.sesion.collectAsState(null)
    val ahora = remember { ahoraMs() }
    val evento = remember {
        MockData.eventos.firstOrNull { !it.yaPaso(ahora) } ?: MockData.eventos.maxBy { it.inicioMs ?: 0L }
    }
    com.gacelas.inicio.InicioContenido(
        saludo = com.gacelas.inicio.saludoPorHora(Fechas.hora(ahora)),
        nombre = perfil?.nombre?.substringBefore(' ')?.ifBlank { null } ?: "Corredor",
        semana = remember { com.gacelas.carreras.domain.reglas.Estadisticas.semana(emptyList(), ahora) },
        hoy = Fechas.diaSemana(ahora),
        m = true,
        racha = remember { com.gacelas.carreras.domain.reglas.Rachas.calcular(emptyList(), ahora) },
        evento = evento,
        onCorrer = onCorrer, onEventos = {}, onEvento = {},
        corredor3D = { com.gacelas.ui.three.Runner3DFallback() },
    )
}

/**
 * Carrera V1 compartida con el tracker y el GPS reales de iOS (CoreLocation).
 * Mapa MapLibre iOS (etapa 4) como en Android; sin MapLibre, el mismo trazado de respaldo que
 * Android sin MapLibre (TrazadoRuta), con los controles del diseño.
 */
@Composable
private fun CarreraIos(panelInicial: Boolean, onSalir: () -> Unit) {
    val s = remember { SeguimientoSensores(fuenteUbicacionNativa(), fuenteRumboNativa()) }
    DisposableEffect(s) { s.iniciar(carrera = true); onDispose { s.detener() } }
    LaunchedEffect(s) { while (true) { delay(1000); s.tic() } }
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { com.gacelas.acceso.SesionIos.contenedor.carreraEnCursoViewModel(s.tracker) }
    val real by vm.estado.collectAsState()
    // --carrera-muestra (solo capturas de CI): los mismos datos fijos que la captura Android.
    val e = if (argumento("--carrera-muestra")) com.gacelas.correr.CarreraMuestra.estado else real
    com.gacelas.correr.ContenidoCarreraC4(
        com.gacelas.correr.VistaCarrera(e),
        onPausa = { vm.alternarPausa(); s.tic() },
        onStop = { s.tracker.finalizar(); onSalir() },
        onVuelta = vm::vuelta,
        onAtras = onSalir,
        panelInicial = panelInicial,
    ) { vista3d, controles, _ ->
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier.fillMaxSize().background(com.gacelas.correr.K1.Fondo),
        ) {
            // Igual que CarreraAndroid: MapaGacelas con seguimiento, ruta magenta y los controles C4.
            com.gacelas.mapa.MapaGacelasIos(
                e.puntos.map { com.gacelas.mapa.Coord(it.lat, it.lng) }, androidx.compose.ui.Modifier.fillMaxSize(),
                seguir = true, zoom = 17f, modo3d = vista3d, ritmoSegKm = e.ritmoActualSegKm, adelanteM = 60.0,
                rutaMagenta = true, avisoArriba = 210.dp, controlesPropios = controles,
            ) {
                com.gacelas.correr.TrazadoRuta(e.puntos, androidx.compose.ui.Modifier.fillMaxSize())
                controles({}, {}, 0f, true)
            }
        }
    }
}

/** "lunes 12 de octubre, 06:30" como SimpleDateFormat de Android (es-EC). */
private fun fechaLarga(ms: Long): String = platform.Foundation.NSDateFormatter().apply {
    locale = platform.Foundation.NSLocale(localeIdentifier = "es_EC")
    dateFormat = "EEEE d 'de' MMMM, HH:mm"
}.stringFromDate(NSDate.dateWithTimeIntervalSince1970(ms / 1000.0))

/** Ajustes compartidos con AjustesViewModel, guardados en DataStore como en Android. */
@Composable
private fun AjustesIos(onAtras: () -> Unit, onCerrarSesion: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val repo = com.gacelas.acceso.SesionIos.ajustes
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { com.gacelas.acceso.SesionIos.contenedor.ajustesViewModel(repo) }
    val a by vm.ajustes.collectAsState()
    com.gacelas.ajustes.AjustesContenido(
        a = a, version = versionIos(), guardar = vm::actualizar,
        onAtras = onAtras, onEditarPerfil = {}, onPrivacidad = {}, onNotificaciones = {},
        onSoporte = {
            platform.Foundation.NSURL.URLWithString("mailto:soporte@gacelas.app?subject=Ayuda%20con%20Gacelas")?.let {
                platform.UIKit.UIApplication.sharedApplication.openURL(it)
            }
        },
        onCerrarSesion = { scope.launch { com.gacelas.acceso.SesionIos.auth.cerrarSesion(); onCerrarSesion() } },
        // Igual que MapaOfflineAjuste en Android: estado real de la descarga de Loja.
        mapaOffline = {
            val estado by com.gacelas.mapa.MapaOfflineIos.estado.collectAsState()
            com.gacelas.ajustes.TarjetaMapaOffline(
                nombre = com.gacelas.mapa.MapaGacelasConst.LOJA_NOMBRE, texto = com.gacelas.mapa.textoEstadoMapaOffline(estado),
                lista = estado is com.gacelas.mapa.EstadoMapaOffline.Lista,
                porcentaje = (estado as? com.gacelas.mapa.EstadoMapaOffline.Descargando)?.porcentaje,
                disponible = com.gacelas.mapa.MapaIos.disponible(),
                onDescargar = { com.gacelas.mapa.MapaIos.offline?.descargar() },
                onEliminar = { com.gacelas.mapa.MapaIos.offline?.eliminar() },
            )
        },
    )
}

/** Lista de rutas compartida con las rutas de ejemplo (las remotas llegan con la sesion). */
@Composable
private fun RutasIos() {
    var pestana by remember { mutableStateOf(com.gacelas.rutas.domain.modelo.PestanaRutas.Explorar) }
    var consulta by remember { mutableStateOf("") }
    var filtros by remember { mutableStateOf(com.gacelas.rutas.domain.modelo.FiltrosRuta()) }
    var guardadas by remember { mutableStateOf(emptySet<String>()) }
    val lista = com.gacelas.rutas.domain.reglas.aplicarFiltros(
        com.gacelas.rutas.domain.reglas.filtrarRutas(MockData.rutas, pestana, guardadas, MockData.ID_USUARIO, consulta), filtros,
    )
    com.gacelas.rutas.ListaRutasContenido(
        lista = lista, pestana = pestana, onPestana = { pestana = it }, guardadas = guardadas,
        consulta = consulta, onConsulta = { consulta = it }, filtros = filtros, onFiltros = { filtros = it },
        cerca = null, onCerca = {}, m = true, puedeDibujar = false,
        onBuscar = {}, onRuta = {}, onDibujar = {},
        onAlternarGuardada = { id, g -> guardadas = if (g) guardadas - id else guardadas + id },
        mapaRutas = { com.gacelas.mapa.MapaRutasAgrupadasIos(lista, {}, it) },
        verMapaInicial = argumento("--ver-mapa"),
    )
}
