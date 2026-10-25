package com.gacelas.acceso

import androidx.compose.runtime.collectAsState
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
import com.gacelas.cuenta.domain.modelo.ResultadoAuth
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
fun InicioSesionScreen(onAtras: () -> Unit, onEntrar: () -> Unit, onRecuperar: () -> Unit, onRegistro: () -> Unit, onLegal: ((Boolean) -> Unit)? = null) {
    val c = contenedor()
    val auth = c.auth
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { c.loginViewModel(auth) }
    val e by vm.estado.collectAsState()
    InicioSesionC4(
        email = e.email, password = e.password, error = e.error, cargando = e.cargando,
        onEmail = vm::email, onPassword = vm::password,
        onEntrar = { vm.entrar { onEntrar() } }, onAtras = onAtras, onRecuperar = onRecuperar, onRegistro = onRegistro,
        botonGoogle = { BotonGoogle(onEntrar, portada = true, alto = 48.dp) },
        onLegal = onLegal,
    )
}

@Composable
fun RegistroScreen(onAtras: () -> Unit, onCrear: () -> Unit, onIniciarSesion: () -> Unit, onLegal: ((Boolean) -> Unit)? = null) {
    val c = contenedor()
    val auth = c.auth
    val context = LocalContext.current
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { c.registroViewModel(auth) }
    val e by vm.estado.collectAsState()
    val ajustesVm = com.gacelas.ui.ajustesViewModel()
    var nivel by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("principiante") }
    var acepta by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    RegistroContenido(
        nombre = e.nombre, email = e.email, password = e.password, error = e.error, cargando = e.cargando,
        onNombre = vm::nombre, onEmail = vm::email, onPassword = vm::password,
        onCrear = { vm.crear {
            ajustesVm.actualizar { it.copy(nivelDeclarado = nivel) }
            scope.launch {
                c.consentimientos.registrar(com.gacelas.legal.TipoConsentimiento.Terminos, true)
                c.consentimientos.registrar(com.gacelas.legal.TipoConsentimiento.Privacidad, true)
            }
            context.aviso("Cuenta creada"); onCrear()
        } },
        onAtras = onAtras, onIniciarSesion = onIniciarSesion,
        botonGoogle = { BotonGoogle(onCrear) },
        nivel = nivel, onNivel = { nivel = it },
        aceptaTerminos = acepta, onAceptaTerminos = { acepta = it },
        onVerTerminos = onLegal?.let { { it(true) } }, onVerPrivacidad = onLegal?.let { { it(false) } },
    )
}

@Composable
fun RecuperarContrasenaScreen(onAtras: () -> Unit, enviadoInicial: Boolean = false) {
    val c = contenedor()
    val auth = c.auth
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { c.recuperarViewModel(enviadoInicial, auth) }
    val e by vm.estado.collectAsState()
    RecuperarContenido(
        email = e.email, enviado = e.enviado, error = e.error,
        onEmail = vm::email,
        onEnviar = vm::enviar,
        onAtras = onAtras,
    )
}
