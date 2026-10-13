package com.gacelas.acceso

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.gacelas.data.auth.ResultadoAuth
import com.gacelas.ui.aviso
import com.gacelas.ui.contenedor
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.BotonSecundario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.IconoCirculo
import com.gacelas.ui.components.MarcaGacelas
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

@Composable
fun BienvenidaScreen(onSaltar: () -> Unit, onCrearCuenta: () -> Unit, onIniciarSesion: () -> Unit, onEntrarGoogle: () -> Unit = {}) {
    BienvenidaC4(
        version = com.gacelas.actualizacion.textoVersion(),
        onSaltar = onSaltar, onCrearCuenta = onCrearCuenta, onIniciarSesion = onIniciarSesion,
        botonGoogle = { BotonGoogle(onEntrarGoogle, portada = true) },
    )
}

@Composable
private fun EnlaceAcceso(texto: String, enlace: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = GType.bodyMd, color = GColors.TextSecondary)
        Text(enlace, style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(4.dp))
    }
}

@Composable
private fun ChipBeneficio(icono: ImageVector, texto: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceContainer).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icono, null, tint = GColors.Primary500)
        Text(texto, style = GType.labelMd, color = GColors.TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun InicioSesionScreen(onAtras: () -> Unit, onEntrar: () -> Unit, onRecuperar: () -> Unit, onRegistro: () -> Unit) {
    val auth = contenedor().auth
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val entrar = {
        if (!cargando) {
            cargando = true
            scope.launch {
                when (val r = auth.iniciarSesion(email, password)) {
                    is ResultadoAuth.Ok -> onEntrar()
                    is ResultadoAuth.Error -> error = r.mensaje
                }
                cargando = false
            }
        }
    }
    InicioSesionC4(
        email = email, password = password, error = error, cargando = cargando,
        onEmail = { email = it; error = null }, onPassword = { password = it; error = null },
        onEntrar = { entrar() }, onAtras = onAtras, onRecuperar = onRecuperar, onRegistro = onRegistro,
        botonGoogle = { BotonGoogle(onEntrar, portada = true, alto = 48.dp) },
    )
}

@Composable
fun RegistroScreen(onAtras: () -> Unit, onCrear: () -> Unit, onIniciarSesion: () -> Unit) {
    val auth = contenedor().auth
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    RegistroContenido(
        nombre = nombre, email = email, password = password, error = error, cargando = cargando,
        onNombre = { nombre = it; error = null }, onEmail = { email = it; error = null }, onPassword = { password = it; error = null },
        onCrear = {
            cargando = true
            scope.launch {
                when (val r = auth.registrar(nombre, email, password)) {
                    is ResultadoAuth.Ok -> { context.aviso("Cuenta creada"); onCrear() }
                    is ResultadoAuth.Error -> error = r.mensaje
                }
                cargando = false
            }
        },
        onAtras = onAtras, onIniciarSesion = onIniciarSesion,
        botonGoogle = { BotonGoogle(onCrear) },
    )
}

@Composable
fun RecuperarContrasenaScreen(onAtras: () -> Unit, enviadoInicial: Boolean = false) {
    val auth = contenedor().auth
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var enviado by rememberSaveable { mutableStateOf(enviadoInicial) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    RecuperarContenido(
        email = email, enviado = enviado, error = error,
        onEmail = { email = it; error = null },
        onEnviar = {
            scope.launch {
                when (val r = auth.recuperarContrasena(email)) {
                    is ResultadoAuth.Ok -> enviado = true
                    is ResultadoAuth.Error -> error = r.mensaje
                }
            }
        },
        onAtras = onAtras,
    )
}
