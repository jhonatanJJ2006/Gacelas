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
    NavHost(navController = nav, startDestination = inicio) {
        accesoGraph(nav)
        principalGraph(nav)
        secundariasGraph(nav)
    }
}

private fun NavGraphBuilder.accesoGraph(nav: NavHostController) {
    composable(Destinos.BIENVENIDA) {
        BienvenidaScreen(
            onSaltar = { nav.navigate(Destinos.INICIO_SESION) },
            onCrearCuenta = { nav.navigate(Destinos.REGISTRO) },
            onIniciarSesion = { nav.navigate(Destinos.INICIO_SESION) },
            onEntrarGoogle = nav::entrarApp,
        )
    }
    composable(Destinos.INICIO_SESION) {
        InicioSesionScreen(
            onAtras = { nav.popBackStack() },
            onEntrar = nav::entrarApp,
            onRecuperar = { nav.navigate(Destinos.RECUPERAR_CONTRASENA) },
            onRegistro = { nav.navigate(Destinos.REGISTRO) },
        )
    }
    composable(Destinos.REGISTRO) {
        RegistroScreen(
            onAtras = { nav.popBackStack() },
            onCrear = { nav.navigate(Destinos.PERMISOS_UBICACION) },
            onIniciarSesion = { nav.navigate(Destinos.INICIO_SESION) },
        )
    }
    composable(Destinos.RECUPERAR_CONTRASENA) { RecuperarContrasenaScreen(onAtras = { nav.popBackStack() }) }
    composable(Destinos.PERMISOS_UBICACION) { PermisosUbicacionScreen(onPermitir = nav::entrarApp, onMasTarde = nav::entrarApp) }
}

private fun NavGraphBuilder.conBarra(nav: NavHostController, ruta: String, content: @Composable () -> Unit) {
    composable(ruta) {
        ScaffoldPrincipal(ruta, onNavegar = { destino ->
            if (destino in Destinos.conBarraInferior) nav.irAPestana(destino) else nav.navigate(destino)
        }) { content() }
    }
}

private fun NavGraphBuilder.principalGraph(nav: NavHostController) {
    conBarra(nav, Destinos.INICIO) {
        InicioScreen(onCorrer = { nav.navigate(Destinos.CORRER) }, onEventos = { nav.navigate(Destinos.EVENTOS) }, onEvento = { nav.navigate(Destinos.detalleEvento(it)) }, onComunidad = { nav.irAPestana(Destinos.COMUNIDAD) })
    }
    conBarra(nav, Destinos.RUTAS) {
        ListaRutasScreen(onBuscar = { nav.navigate(Destinos.BUSCADOR_RUTAS) }, onRuta = { nav.navigate(Destinos.detalleRuta(it)) }, onDibujar = { nav.navigate(Destinos.CREAR_RUTA) })
    }
    conBarra(nav, Destinos.COMUNIDAD) { FeedScreen(onEventos = { nav.navigate(Destinos.EVENTOS) }, onEnVivo = { nav.navigate(Destinos.EN_VIVO) }, onClubes = { nav.navigate(Destinos.CLUBES) }) }
    conBarra(nav, Destinos.EVENTOS) { EventosScreen(onEvento = { nav.navigate(Destinos.detalleEvento(it)) }, onClubes = { nav.navigate(Destinos.CLUBES) }) }
    conBarra(nav, Destinos.PERFIL) {
        PerfilScreen(
            onEditar = { nav.navigate(Destinos.EDITAR_PERFIL) },
            onAvatar = { nav.navigate(Destinos.AVATAR) },
            onAjustes = { nav.navigate(Destinos.AJUSTES) },
            onCarrera = { nav.navigate(Destinos.detalleCarrera(it)) },
        )
    }
}

private fun NavGraphBuilder.secundariasGraph(nav: NavHostController) {
    composable(Destinos.CORRER) { CorrerScreen(onCerrar = { nav.popBackStack() }, onEmpezar = { nav.navigate(Destinos.CARRERA_EN_CURSO) }) }
    composable(Destinos.CARRERA_EN_CURSO) {
        CarreraEnCursoScreen(
            onCerrar = { nav.irAPestana(Destinos.INICIO) },
            onFinalizar = { nav.navigate(Destinos.RESUMEN) },
            onCronometro = { nav.navigate(Destinos.CRONOMETRO) },
        )
    }
    composable(Destinos.CRONOMETRO) { CronometroScreen(onAtras = { nav.popBackStack() }, onFin = { nav.navigate(Destinos.RESUMEN) }) }
    composable(Destinos.RESUMEN) {
        val volver = { nav.volverAInicio() }
        ResumenPostCarreraScreen(onCerrar = volver, onGuardar = volver, onDescartar = volver)
    }
    composable(Destinos.BUSCADOR_RUTAS) { BuscadorRutasScreen(onAtras = { nav.popBackStack() }, onNavegar = { nav.navigate(Destinos.NAVEGACION) }) }
    composable(Destinos.NAVEGACION) {
        NavegacionRutaScreen(onCronometro = { nav.navigate(Destinos.CRONOMETRO) }, onCerrar = { conCarrera -> if (conCarrera) nav.navigate(Destinos.RESUMEN) else nav.irAPestana(Destinos.RUTAS) })
    }
    composable(Destinos.DETALLE_EVENTO) { entry ->
        val idEvento = entry.arguments?.getString("id") ?: "1"
        DetalleEventoScreen(id = idEvento, onAtras = { nav.popBackStack() }, onEnVivo = { nav.navigate(Destinos.eventoEnVivo(idEvento)) })
    }
    composable(Destinos.CLUBES) { com.gacelas.comunidad.ClubesScreen(onAtras = { nav.popBackStack() }, onClub = { nav.navigate(Destinos.detalleClub(it)) }) }
    composable(Destinos.DETALLE_CLUB) { entry ->
        com.gacelas.comunidad.DetalleClubScreen(
            id = entry.arguments?.getString("id").orEmpty(), onAtras = { nav.popBackStack() },
            onEvento = { nav.navigate(Destinos.detalleEvento(it)) }, onCrearEvento = { com.gacelas.comunidad.data.EventoDeClub.id = it; nav.navigate(Destinos.EVENTOS) },
        )
    }
    composable(Destinos.EVENTO_EN_VIVO) { entry ->
        com.gacelas.comunidad.EventoEnVivoScreen(id = entry.arguments?.getString("id").orEmpty(), onAtras = { nav.popBackStack() }, onCorrer = { nav.navigate(Destinos.CORRER) })
    }
    composable(Destinos.CREAR_RUTA) {
        com.gacelas.rutas.DibujarRutaScreen(onAtras = { nav.popBackStack() }, onCreada = { id ->
            nav.navigate(Destinos.detalleRuta(id)) { popUpTo(Destinos.CREAR_RUTA) { inclusive = true } }
        })
    }
    composable(Destinos.DETALLE_RUTA) { entry ->
        com.gacelas.rutas.DetalleRutaScreen(id = entry.arguments?.getString("id") ?: "", onAtras = { nav.popBackStack() }, onCorrer = { nav.navigate(Destinos.BUSCADOR_RUTAS) })
    }
    composable(Destinos.EN_VIVO) { com.gacelas.envivo.ListaEnVivoScreen(onAtras = { nav.popBackStack() }, onSesion = { nav.navigate(Destinos.detalleEnVivo(it)) }) }
    composable(Destinos.DETALLE_EN_VIVO) { entry ->
        com.gacelas.envivo.DetalleEnVivoScreen(id = entry.arguments?.getString("id") ?: "", onAtras = { nav.popBackStack() })
    }
    composable(Destinos.EDITAR_PERFIL) { EditarPerfilScreen(onAtras = { nav.popBackStack() }, onGuardar = { nav.popBackStack() }) }
    composable(Destinos.DETALLE_CARRERA) { e -> com.gacelas.perfil.DetalleCarreraScreen(e.arguments?.getString("id").orEmpty(), onAtras = { nav.popBackStack() }) }
    composable(Destinos.AVATAR) { Avatar3DScreen(onAtras = { nav.popBackStack() }, onGuardar = { nav.popBackStack() }) }
    composable(Destinos.AJUSTES) {
        AjustesPreferenciasScreen(
            onAtras = { nav.popBackStack() },
            onEditarPerfil = { nav.navigate(Destinos.EDITAR_PERFIL) },
            onPrivacidad = { nav.navigate(Destinos.PRIVACIDAD) },
            onNotificaciones = { nav.navigate(Destinos.CONFIG_NOTIFICACIONES) },
            onCerrarSesion = { nav.navigate(Destinos.BIENVENIDA) { popUpTo(0) { inclusive = true } } },
        )
    }
    composable(Destinos.PRIVACIDAD) { PrivacidadUbicacionScreen(onAtras = { nav.popBackStack() }) }
    composable(Destinos.CONFIG_NOTIFICACIONES) { ConfiguracionNotificacionesScreen(onAtras = { nav.popBackStack() }) }
    composable(Destinos.NOTIFICACIONES) {
        NotificacionesScreen(onAtras = { nav.popBackStack() }, onAbrir = { n ->
            when (n.tipo) {
                TipoNotificacion.Evento -> nav.navigate(Destinos.detalleEvento("1"))
                TipoNotificacion.EnVivo -> nav.navigate(Destinos.EN_VIVO)
                TipoNotificacion.Comentario, TipoNotificacion.Like -> nav.irAPestana(Destinos.COMUNIDAD)
                TipoNotificacion.Seguidor, TipoNotificacion.Racha -> nav.irAPestana(Destinos.PERFIL)
            }
        })
    }
}
