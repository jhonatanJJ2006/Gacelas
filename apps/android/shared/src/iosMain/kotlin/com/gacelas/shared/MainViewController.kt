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
import com.gacelas.acceso.BotonGoogleVisual
import com.gacelas.acceso.InicioSesionC4
import com.gacelas.data.auth.ValidacionAuth
import com.gacelas.ui.theme.GacelasTheme
import platform.Foundation.NSBundle
import com.gacelas.core.sensores.SeguimientoSensores
import com.gacelas.core.sensores.fuenteRumboNativa
import com.gacelas.core.sensores.fuenteUbicacionNativa
import kotlinx.coroutines.delay
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIViewController
import androidx.compose.ui.unit.dp

/** Argumento de lanzamiento (CI): abre directamente la pantalla de sensores con una carrera en curso. */
private fun argumento(a: String) = NSProcessInfo.processInfo.arguments.contains(a)

/** Punto de entrada de iOS: SwiftUI (iosApp) muestra este UIViewController de Compose. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val auto = remember { argumento("--sensores") }
    var enSensores by remember { mutableStateOf(auto) }
    var pantalla by remember { mutableStateOf(if (argumento("--pantalla=login")) PantallaIos.Login else PantallaIos.Bienvenida) }
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
                PantallaIos.Inicio -> InicioComun(onEmpezar = { enSensores = true })
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

private enum class PantallaIos { Bienvenida, Login, Inicio }

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
