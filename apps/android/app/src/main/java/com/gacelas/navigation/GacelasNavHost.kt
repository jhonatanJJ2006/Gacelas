package com.gacelas.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gacelas.acceso.BienvenidaScreen
import com.gacelas.acceso.InicioSesionScreen
import com.gacelas.acceso.RecuperarContrasenaScreen
import com.gacelas.acceso.RegistroScreen
import com.gacelas.ajustes.AjustesPreferenciasScreen
import com.gacelas.ajustes.ConfiguracionNotificacionesScreen
import com.gacelas.ajustes.PermisosUbicacionScreen
import com.gacelas.ajustes.PrivacidadUbicacionScreen
import com.gacelas.comunidad.DetalleEventoScreen
import com.gacelas.comunidad.EventosScreen
import com.gacelas.comunidad.FeedScreen
import com.gacelas.correr.CarreraEnCursoScreen
import com.gacelas.correr.CorrerScreen
import com.gacelas.correr.CronometroScreen
import com.gacelas.correr.ResumenPostCarreraScreen
import com.gacelas.comunidad.domain.modelo.TipoNotificacion
import com.gacelas.inicio.InicioScreen
import com.gacelas.notificaciones.NotificacionesScreen
import com.gacelas.perfil.Avatar3DScreen
import com.gacelas.perfil.EditarPerfilScreen
import com.gacelas.perfil.PerfilScreen
import com.gacelas.rutas.BuscadorRutasScreen
import com.gacelas.rutas.ListaRutasScreen
import com.gacelas.rutas.NavegacionRutaScreen
import com.gacelas.ui.components.ScaffoldPrincipal

/**
 * Solo navega si la pantalla visible esta RESUMED: un doble toque rapido (o dos botones a la vez)
 * ya no apila dos veces el mismo destino ni hace dos "atras" seguidos que sacaban de la app.
 */
fun NavHostController.ir(ruta: String, opciones: androidx.navigation.NavOptionsBuilder.() -> Unit = {}) {
    if (estaActiva()) navigate(ruta, opciones)
}

/** "Atras" protegido contra doble toque; devuelve false si se ignoro. */
fun NavHostController.atras(): Boolean = if (estaActiva()) popBackStack() else false

private fun NavHostController.estaActiva(): Boolean =
    currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) ?: true

/** Navega a una pestana principal sin apilar pantallas repetidas. */
fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(Destinos.INICIO) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Sale del flujo de carrera a Inicio sin restaurar estado guardado: con restoreState, la
 * pestaña Inicio podia restaurar una pila que aun tenia el resumen y la app se quedaba ahi.
 */
fun NavHostController.volverAInicio() {
    navigate(Destinos.INICIO) {
        popUpTo(Destinos.INICIO) { inclusive = true }
        launchSingleTop = true
    }
}

/** Entra a la app y limpia el flujo de acceso de la pila. */
fun NavHostController.entrarApp() {
    navigate(Destinos.INICIO) {
        popUpTo(Destinos.BIENVENIDA) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun GacelasNavHost(
    nav: NavHostController = rememberNavController(),
    inicio: String = Destinos.BIENVENIDA,
    rutaInicial: String? = null,
    sesionActiva: Boolean? = null,
) {
    // Rutas protegidas: sin sesion, cualquier pantalla fuera del acceso vuelve a Bienvenida.
    if (sesionActiva == false) {
        val entrada by nav.currentBackStackEntryAsState()
        val ruta = entrada?.destination?.route
        LaunchedEffect(ruta) {
            if (ruta != null && ruta !in Destinos.acceso) {
                nav.navigate(Destinos.BIENVENIDA) { popUpTo(0) { inclusive = true } }
            }
        }
    }
    if (rutaInicial != null && rutaInicial != inicio) {
        LaunchedEffect(rutaInicial) { nav.navigate(rutaInicial) }
    }
    // Destino pedido desde la notificacion de la carrera (abrir carrera en curso o resumen).
    if (sesionActiva != false) {
        val pendiente by com.gacelas.tracking.DestinoPendiente.valor.collectAsState()
        LaunchedEffect(pendiente) {
            val d = pendiente ?: return@LaunchedEffect
            com.gacelas.tracking.DestinoPendiente.valor.value = null
            if (nav.currentDestination?.route != d) runCatching { nav.navigate(d) { launchSingleTop = true } }
        }
    }
    // Transiciones C2 (MovimientoC2 / TransicionesC2); entre pestanas, fundido sin desplazamiento.
    val escala = com.gacelas.ui.movimiento.LocalEscalaMovimiento.current
    fun entrePestanas(a: String?, b: String?) = a in Destinos.conBarraInferior && b in Destinos.conBarraInferior
    NavHost(
        navController = nav, startDestination = inicio,
        enterTransition = { com.gacelas.ui.movimiento.TransicionesC2.entrar(escala, entrePestanas(initialState.destination.route, targetState.destination.route)) },
        exitTransition = { com.gacelas.ui.movimiento.TransicionesC2.salir(escala) },
        popEnterTransition = { com.gacelas.ui.movimiento.TransicionesC2.volverEntrar(escala) },
        popExitTransition = { com.gacelas.ui.movimiento.TransicionesC2.volverSalir(escala, entrePestanas(initialState.destination.route, targetState.destination.route)) },
    ) {
        accesoGraph(nav)
        principalGraph(nav)
        secundariasGraph(nav)
    }
}

private fun NavGraphBuilder.accesoGraph(nav: NavHostController) {
    composable(Destinos.BIENVENIDA) {
        BienvenidaScreen(
            onSaltar = { nav.ir(Destinos.INICIO_SESION) },
            onCrearCuenta = { nav.ir(Destinos.REGISTRO) },
            onIniciarSesion = { nav.ir(Destinos.INICIO_SESION) },
            onEntrarGoogle = nav::entrarApp,
        )
    }
    composable(Destinos.INICIO_SESION) {
        InicioSesionScreen(
            onAtras = { nav.atras() },
            onEntrar = nav::entrarApp,
            onRecuperar = { nav.ir(Destinos.RECUPERAR_CONTRASENA) },
            onRegistro = { nav.ir(Destinos.REGISTRO) },
            onLegal = { t -> nav.ir(if (t) Destinos.TERMINOS else Destinos.POLITICA_PRIVACIDAD) },
        )
    }
    composable(Destinos.REGISTRO) {
        RegistroScreen(
            onAtras = { nav.atras() },
            onCrear = { nav.ir(Destinos.PERMISOS_UBICACION) },
            onIniciarSesion = { nav.ir(Destinos.INICIO_SESION) },
            onLegal = { t -> nav.ir(if (t) Destinos.TERMINOS else Destinos.POLITICA_PRIVACIDAD) },
        )
    }
    composable(Destinos.RECUPERAR_CONTRASENA) { RecuperarContrasenaScreen(onAtras = { nav.atras() }) }
    composable(Destinos.PERMISOS_UBICACION) { PermisosUbicacionScreen(onPermitir = nav::entrarApp, onMasTarde = nav::entrarApp) }
}

private fun NavGraphBuilder.conBarra(nav: NavHostController, ruta: String, content: @Composable () -> Unit) {
    composable(ruta) {
        ScaffoldPrincipal(ruta, onNavegar = { destino ->
            if (destino in Destinos.conBarraInferior) nav.irAPestana(destino) else nav.ir(destino)
        }) { content() }
    }
}

private fun NavGraphBuilder.principalGraph(nav: NavHostController) {
    conBarra(nav, Destinos.INICIO) {
        InicioScreen(onCorrer = { nav.ir(Destinos.CORRER) }, onEventos = { nav.ir(Destinos.EVENTOS) }, onEvento = { nav.ir(Destinos.detalleEvento(it)) }, onComunidad = { nav.irAPestana(Destinos.COMUNIDAD) }, onRutas = { nav.irAPestana(Destinos.RUTAS) }, onRuta = { nav.ir(Destinos.detalleRuta(it)) })
    }
    conBarra(nav, Destinos.RUTAS) {
        ListaRutasScreen(onBuscar = { nav.ir(Destinos.BUSCADOR_RUTAS) }, onRuta = { nav.ir(Destinos.detalleRuta(it)) }, onDibujar = { nav.ir(Destinos.CREAR_RUTA) })
    }
    conBarra(nav, Destinos.COMUNIDAD) { FeedScreen(onEventos = { nav.ir(Destinos.EVENTOS) }, onEnVivo = { nav.ir(Destinos.EN_VIVO) }, onClubes = { nav.ir(Destinos.CLUBES) }, onEvento = { nav.ir(Destinos.detalleEvento(it)) }) }
    conBarra(nav, Destinos.EVENTOS) { EventosScreen(onEvento = { nav.ir(Destinos.detalleEvento(it)) }, onClubes = { nav.ir(Destinos.CLUBES) }) }
    conBarra(nav, Destinos.PERFIL) {
        PerfilScreen(
            onEditar = { nav.ir(Destinos.EDITAR_PERFIL) },
            onAvatar = { nav.ir(Destinos.AVATAR) },
            onAjustes = { nav.ir(Destinos.AJUSTES) },
            onCarrera = { nav.ir(Destinos.detalleCarrera(it)) },
            onReportes = { nav.ir(Destinos.MIS_REPORTES) },
        )
    }
}

private fun NavGraphBuilder.secundariasGraph(nav: NavHostController) {
    composable(Destinos.CORRER) {
        CorrerScreen(onCerrar = { nav.atras() }, onEmpezar = { nav.ir(Destinos.CARRERA_EN_CURSO) }, onRuta = { nav.ir(Destinos.detalleRuta(it)) }, onPrivacidad = { nav.ir(Destinos.PRIVACIDAD) })
    }
    composable(Destinos.CARRERA_EN_CURSO) {
        CarreraEnCursoScreen(
            onCerrar = { nav.irAPestana(Destinos.INICIO) },
            onFinalizar = { nav.ir(Destinos.RESUMEN) },
            onCronometro = { nav.ir(Destinos.CRONOMETRO) },
        )
    }
    composable(Destinos.CRONOMETRO) { CronometroScreen(onAtras = { nav.atras() }, onFin = { nav.ir(Destinos.RESUMEN) }) }
    composable(Destinos.RESUMEN) {
        val volver = { nav.volverAInicio() }
        ResumenPostCarreraScreen(onCerrar = volver, onGuardar = volver, onDescartar = volver)
    }
    composable(Destinos.BUSCADOR_RUTAS) { BuscadorRutasScreen(onAtras = { nav.atras() }, onNavegar = { nav.ir(Destinos.NAVEGACION) }) }
    composable(Destinos.NAVEGACION) {
        NavegacionRutaScreen(onCronometro = { nav.ir(Destinos.CRONOMETRO) }, onCerrar = { conCarrera -> if (conCarrera) nav.ir(Destinos.RESUMEN) else nav.irAPestana(Destinos.RUTAS) })
    }
    composable(Destinos.DETALLE_EVENTO) { entry ->
        val idEvento = entry.arguments?.getString("id") ?: "1"
        DetalleEventoScreen(id = idEvento, onAtras = { nav.atras() }, onEnVivo = { nav.ir(Destinos.eventoEnVivo(idEvento)) })
    }
    composable(Destinos.CLUBES) { com.gacelas.comunidad.ClubesScreen(onAtras = { nav.atras() }, onClub = { nav.ir(Destinos.detalleClub(it)) }) }
    composable(Destinos.DETALLE_CLUB) { entry ->
        com.gacelas.comunidad.DetalleClubScreen(
            id = entry.arguments?.getString("id").orEmpty(), onAtras = { nav.atras() },
            onEvento = { nav.ir(Destinos.detalleEvento(it)) }, onCrearEvento = { com.gacelas.comunidad.infrastructure.EventoDeClub.id = it; nav.ir(Destinos.EVENTOS) },
            onClub = { nav.ir(Destinos.detalleClub(it)) },
        )
    }
    composable(Destinos.EVENTO_EN_VIVO) { entry ->
        com.gacelas.comunidad.EventoEnVivoScreen(id = entry.arguments?.getString("id").orEmpty(), onAtras = { nav.atras() }, onCorrer = { nav.ir(Destinos.CORRER) })
    }
    composable(Destinos.CREAR_RUTA) {
        com.gacelas.rutas.DibujarRutaScreen(onAtras = { nav.atras() }, onCreada = { id ->
            nav.ir(Destinos.detalleRuta(id)) { popUpTo(Destinos.CREAR_RUTA) { inclusive = true } }
        })
    }
    composable(Destinos.DETALLE_RUTA) { entry ->
        com.gacelas.rutas.DetalleRutaScreen(id = entry.arguments?.getString("id") ?: "", onAtras = { nav.atras() }, onCorrer = { nav.ir(Destinos.BUSCADOR_RUTAS) })
    }
    composable(Destinos.EN_VIVO) { com.gacelas.envivo.ListaEnVivoScreen(onAtras = { nav.atras() }, onSesion = { nav.ir(Destinos.detalleEnVivo(it)) }, onMapa = { nav.ir(Destinos.MAPA_VIVO) }) }
    composable(Destinos.DETALLE_EN_VIVO) { entry ->
        com.gacelas.envivo.DetalleEnVivoScreen(id = entry.arguments?.getString("id") ?: "", onAtras = { nav.atras() })
    }
    composable(Destinos.MIS_REPORTES) { com.gacelas.perfil.MisReportesScreen(onAtras = { nav.atras() }) }
    composable(Destinos.EDITAR_PERFIL) { EditarPerfilScreen(onAtras = { nav.atras() }, onGuardar = { nav.atras() }, onAvatar = { nav.ir(Destinos.AVATAR) }) }
    composable(Destinos.DETALLE_CARRERA) { e -> com.gacelas.perfil.DetalleCarreraScreen(e.arguments?.getString("id").orEmpty(), onAtras = { nav.atras() }) }
    composable(Destinos.AVATAR) { Avatar3DScreen(onAtras = { nav.atras() }, onGuardar = { nav.atras() }) }
    composable(Destinos.AJUSTES) {
        AjustesPreferenciasScreen(
            onAtras = { nav.atras() },
            onEditarPerfil = { nav.ir(Destinos.EDITAR_PERFIL) },
            onPrivacidad = { nav.ir(Destinos.PRIVACIDAD) },
            onNotificaciones = { nav.ir(Destinos.CONFIG_NOTIFICACIONES) },
            onCerrarSesion = { nav.ir(Destinos.BIENVENIDA) { popUpTo(0) { inclusive = true } } },
            onMapasOffline = { nav.ir(Destinos.MAPAS_OFFLINE) },
            onBrujula = { nav.ir(Destinos.BRUJULA) },
            onLegal = { t -> nav.ir(if (t) Destinos.TERMINOS else Destinos.POLITICA_PRIVACIDAD) },
            onAdmin = { nav.ir(Destinos.ADMIN) },
        )
    }
    // El panel vuelve a comprobar el rol en el servidor: sin rol muestra "Acceso denegado".
    composable(Destinos.ADMIN) {
        com.gacelas.administracion.presentation.PanelAdmin(com.gacelas.ui.contenedor().admin, onAtras = { nav.atras() })
    }
    composable(Destinos.BRUJULA) { com.gacelas.ajustes.BrujulaScreen(onAtras = { nav.atras() }) }
    composable(Destinos.MAPAS_OFFLINE) { com.gacelas.ajustes.MapasOfflineScreen(onAtras = { nav.atras() }) }
    composable(Destinos.PRIVACIDAD) {
        PrivacidadUbicacionScreen(
            onAtras = { nav.atras() },
            onLegal = { t -> nav.ir(if (t) Destinos.TERMINOS else Destinos.POLITICA_PRIVACIDAD) },
            onCuentaEliminada = { nav.ir(Destinos.BIENVENIDA) { popUpTo(0) { inclusive = true } } },
        )
    }
    composable(Destinos.TERMINOS) { com.gacelas.legal.DocumentoLegalScreen(terminos = true, onAtras = { nav.atras() }) }
    composable(Destinos.POLITICA_PRIVACIDAD) { com.gacelas.legal.DocumentoLegalScreen(terminos = false, onAtras = { nav.atras() }) }
    composable(Destinos.MAPA_VIVO) { com.gacelas.vivo.MapaVivoScreen(onAtras = { nav.atras() }, onPrivacidad = { nav.ir(Destinos.PRIVACIDAD) }) }
    composable(Destinos.CONFIG_NOTIFICACIONES) { ConfiguracionNotificacionesScreen(onAtras = { nav.atras() }) }
    composable(Destinos.NOTIFICACIONES) {
        NotificacionesScreen(onAtras = { nav.atras() }, onAbrir = { n ->
            when (n.tipo) {
                // La notificacion no trae el id del evento: se abre la lista en vez de un evento fijo.
                TipoNotificacion.Evento -> nav.ir(Destinos.EVENTOS)
                TipoNotificacion.EnVivo -> nav.ir(Destinos.EN_VIVO)
                TipoNotificacion.Comentario, TipoNotificacion.Like -> nav.irAPestana(Destinos.COMUNIDAD)
                TipoNotificacion.Seguidor, TipoNotificacion.Racha, TipoNotificacion.Reporte -> nav.irAPestana(Destinos.PERFIL)
            }
        })
    }
}
