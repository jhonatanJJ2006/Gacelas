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

/** Contenedor comun de las pantallas de acceso (sin barra inferior). */
@Composable
private fun PantallaAcceso(onAtras: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(GColors.BgPrimary).cerrarTecladoAlTocarFuera().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        if (onAtras != null) {
            IconButton(onClick = onAtras, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = GColors.TextPrimary)
            }
        }
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            content()
        }
    }
}

@Composable
fun BienvenidaScreen(onSaltar: () -> Unit, onCrearCuenta: () -> Unit, onIniciarSesion: () -> Unit, onEntrarGoogle: () -> Unit = {}) {
    BoxWithConstraints(Modifier.fillMaxSize().background(C4.Fondo).testTag("pantalla-bienvenida")) {
        val compacta = maxHeight < AlturasAcceso.BIENVENIDA_COMPACTA
        FondoMapaC4(login = false)
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = if (compacta) 8.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarcaC4()
                Spacer(Modifier.weight(1f))
                ChipC4(PortadaRuta.etiqueta())
                Text("Saltar", style = C4Texto.labelCaps, color = C4.Secondary,
                    modifier = Modifier.padding(start = 4.dp).clip(CircleShape).clickable(onClick = onSaltar).padding(8.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().padding(start = 20.dp, end = 20.dp, bottom = 12.dp), contentAlignment = Alignment.BottomStart) {
                IndicadorZonaC4()
            }
            TarjetaC4(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(top = 8.dp, bottom = if (compacta) 12.dp else 24.dp), padding = if (compacta) 20.dp else 24.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(C4.Magenta))
                    Spacer(Modifier.width(6.dp))
                    Text("RUNNING EN LOJA, ECUADOR", style = C4Texto.labelCaps, color = C4.Magenta)
                }
                Spacer(Modifier.height(12.dp))
                TitularC4(compacto = compacta)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Únete a la comunidad de runners en Loja. Explora rutas andinas, registra tus marcas y corre acompañado.",
                    style = C4Texto.bodyMd, color = C4.Secondary.copy(alpha = 0.9f),
                    maxLines = if (compacta) 2 else 4, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(if (compacta) 16.dp else 24.dp))
                BotonC4("Empezar", onCrearCuenta)
                Spacer(Modifier.height(12.dp))
                BotonGoogle(onEntrarGoogle, portada = true)
                PieC4("¿Ya tienes cuenta? ", "Inicia sesión", onIniciarSesion)
            }
        }
    }
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
private fun EncabezadoFormulario(titulo: String, subtitulo: String) {
    MarcaGacelas()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, style = GType.headlineLg, color = GColors.TextPrimary)
        Text(subtitulo, style = GType.bodyLg, color = GColors.TextSecondary)
    }
}

@Composable
private fun MensajeError(texto: String?) {
    if (texto != null) Text(texto, style = GType.bodyMd, color = GColors.Danger, modifier = Modifier.testTag("error-acceso"))
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
    // Sin scroll: el teclado (imePadding) achica el hueco y la pantalla se adapta por niveles.
    // El mapa es fondo de ventana completa (no se mueve con el teclado), igual que en el export de Stitch.
    BoxWithConstraints(Modifier.fillMaxSize().background(C4.Fondo).testTag("pantalla-login")) {
        FondoMapaC4(login = true)
        BoxWithConstraints(Modifier.fillMaxSize().imePadding().cerrarTecladoAlTocarFuera()) {
            val compacto = maxHeight < AlturasAcceso.LOGIN_COMPACTO
            val minimo = maxHeight < AlturasAcceso.LOGIN_MINIMO
            Column(Modifier.fillMaxSize()) {
                if (!minimo) {
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = if (compacto) 8.dp else 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AtrasC4(onAtras)
                        Spacer(Modifier.width(12.dp))
                        MarcaC4()
                        Spacer(Modifier.weight(1f))
                        ChipC4(PortadaRuta.etiqueta())
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().padding(start = 20.dp, end = 20.dp, bottom = 12.dp), contentAlignment = Alignment.BottomStart) {
                    if (!compacto) IndicadorZonaC4()
                }
                TarjetaC4(
                    Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(top = 8.dp, bottom = if (compacto) 8.dp else 24.dp),
                    alpha = 0.90f, padding = if (minimo) 16.dp else if (compacto) 20.dp else 24.dp,
                ) {
                    Text("Bienvenido de vuelta", style = C4Texto.headlineLgMobile.copy(fontSize = if (compacto) 20.sp else 24.sp), color = Color.White)
                    if (!compacto) {
                        Spacer(Modifier.height(6.dp))
                        Text("Inicia sesión para seguir corriendo", style = C4Texto.bodySm, color = C4.Subtitulo.copy(alpha = 0.8f))
                    }
                    Spacer(Modifier.height(if (minimo) 10.dp else if (compacto) 14.dp else 20.dp))
                    CampoC4("Correo electrónico", email, { email = it; error = null }, "tu@email.com", IconosC4.Correo,
                        tipo = KeyboardType.Email, mostrarEtiqueta = !minimo)
                    Spacer(Modifier.height(if (minimo) 10.dp else 16.dp))
                    CampoC4("Contraseña", password, { password = it; error = null }, "••••••••", IconosC4.Candado,
                        password = true, mostrarEtiqueta = !minimo, onListo = { entrar() },
                        accion = if (minimo) null else ({
                            Text("¿Olvidaste tu contraseña?", style = C4Texto.labelCaps.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp), color = C4.Enlace,
                                modifier = Modifier.clickable(onClick = onRecuperar))
                        }))
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, style = C4Texto.bodySm, color = Color(0xFFFFB4AB), modifier = Modifier.testTag("error-acceso"))
                    }
                    if (minimo) {
                        Text("¿Olvidaste tu contraseña?", style = C4Texto.labelCaps.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp), color = C4.Enlace,
                            modifier = Modifier.align(Alignment.End).padding(top = 8.dp).clickable(onClick = onRecuperar))
                    }
                    Spacer(Modifier.height(if (minimo) 10.dp else 20.dp))
                    BotonC4(if (cargando) "Ingresando..." else "Iniciar sesión", { entrar() }, alto = 48.dp, tamTexto = 16, enabled = !cargando)
                    if (!compacto) {
                        SeparadorC4()
                        BotonGoogle(onEntrar, portada = true, alto = 48.dp)
                        PieC4("¿No tienes cuenta? ", "Crear cuenta", onRegistro, arriba = 10.dp)
                    }
                }
            }
        }
    }
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
    var verTerminos by remember { mutableStateOf(false) }
    PantallaAcceso(onAtras) {
        EncabezadoFormulario("Únete a la manada", "Crea tu cuenta y empieza a correr con la comunidad")
        CampoTexto("Nombre completo", nombre, { nombre = it; error = null }, "Tu nombre")
        CampoTexto("Correo electrónico", email, { email = it; error = null }, "tu@email.com", tipo = KeyboardType.Email)
        CampoTexto("Contraseña", password, { password = it; error = null }, "Mínimo 8 caracteres", password = true,
            ayuda = "Usa mínimo 8 caracteres con letras y números", onListo = {})
        MensajeError(error)
        BotonPrimario(if (cargando) "Creando cuenta..." else "Crear cuenta", {
            cargando = true
            scope.launch {
                when (val r = auth.registrar(nombre, email, password)) {
                    is ResultadoAuth.Ok -> { context.aviso("Cuenta creada"); onCrear() }
                    is ResultadoAuth.Error -> error = r.mensaje
                }
                cargando = false
            }
        }, enabled = !cargando)
        Text("Al crear una cuenta, aceptas nuestros Términos de Servicio y Política de Privacidad",
            style = GType.bodySm, color = GColors.Primary500, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable { verTerminos = true })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("¿Ya tienes cuenta? ", style = GType.bodyMd, color = GColors.TextSecondary)
            Text("Iniciar sesión", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.clickable(onClick = onIniciarSesion))
        }
        BotonGoogle(onCrear)
    }
    if (verTerminos) {
        AlertDialog(
            onDismissRequest = { verTerminos = false },
            confirmButton = { TextButton(onClick = { verTerminos = false }) { Text("Entendido") } },
            title = { Text("Términos y privacidad") },
            text = { Text("Tus datos se guardan en tu cuenta de Gacelas y en este teléfono. Tú decides qué carreras son públicas y puedes ocultar la zona de tu casa en Ajustes.") },
            containerColor = GColors.BgSecondary,
        )
    }
}

@Composable
fun RecuperarContrasenaScreen(onAtras: () -> Unit, enviadoInicial: Boolean = false) {
    val auth = contenedor().auth
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var enviado by rememberSaveable { mutableStateOf(enviadoInicial) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    PantallaAcceso(onAtras) {
        if (enviado) {
            Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                IconoCirculo(Icons.Filled.MarkEmailRead, GColors.Secondary.copy(alpha = 0.2f), GColors.Secondary500, 96.dp)
                Text("Revisa tu correo", style = GType.headlineLg, color = GColors.TextPrimary)
                Text("Te hemos enviado un enlace para restablecer tu contraseña a $email", style = GType.bodyLg, color = GColors.TextSecondary, textAlign = TextAlign.Center)
                BotonPrimario("Volver al inicio de sesión", onAtras, icono = null)
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IconoCirculo(Icons.Filled.LockReset, GColors.Primary.copy(alpha = 0.2f), GColors.Primary500, 80.dp)
                Text("Recuperar contraseña", style = GType.headlineLg, color = GColors.TextPrimary)
                Text("Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña",
                    style = GType.bodyLg, color = GColors.TextSecondary, textAlign = TextAlign.Center)
            }
            CampoTexto("Correo electrónico", email, { email = it; error = null }, "tu@email.com", tipo = KeyboardType.Email)
            MensajeError(error)
            BotonPrimario("Enviar enlace", {
                scope.launch {
                    when (val r = auth.recuperarContrasena(email)) {
                        is ResultadoAuth.Ok -> enviado = true
                        is ResultadoAuth.Error -> error = r.mensaje
                    }
                }
            })
        }
    }
}
