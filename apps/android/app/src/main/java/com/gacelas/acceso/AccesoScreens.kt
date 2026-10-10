package com.gacelas.acceso

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
        Modifier.fillMaxSize().background(GColors.BgPrimary).statusBarsPadding().navigationBarsPadding()
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
fun BienvenidaScreen(onSaltar: () -> Unit, onCrearCuenta: () -> Unit, onIniciarSesion: () -> Unit) {
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MarcaGacelas(Modifier.weight(1f))
            Text("Saltar", style = GType.labelLg, color = GColors.TextSecondary, modifier = Modifier.clickable(onClick = onSaltar).padding(8.dp))
        }
        Box(Modifier.fillMaxWidth().height(260.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF3B1F05), GColors.SurfaceLow)))) {
            Canvas(Modifier.fillMaxSize()) {
                val sx = size.width / 400f
                val sy = size.height / 200f
                fun path(vararg pts: Pair<Float, Float>, cerrar: Boolean) = Path().apply {
                    moveTo(pts[0].first * sx, pts[0].second * sy)
                    pts.drop(1).forEach { lineTo(it.first * sx, it.second * sy) }
                    if (cerrar) close()
                }
                drawPath(path(0f to 200f, 0f to 120f, 80f to 70f, 150f to 110f, 230f to 45f, 320f to 105f, 400f to 60f, 400f to 200f, cerrar = true), GColors.SurfaceLow)
                drawPath(path(0f to 200f, 0f to 150f, 60f to 110f, 130f to 135f, 200f to 90f, 280f to 140f, 360f to 100f, 400f to 130f, 400f to 200f, cerrar = true), GColors.BgSecondary)
                drawPath(path(0f to 150f, 60f to 110f, 130f to 135f, 200f to 90f, 280f to 140f, 360f to 100f, 400f to 130f, cerrar = false), GColors.Primary500.copy(alpha = 0.4f), style = Stroke(3f))
            }
            Row(Modifier.align(Alignment.BottomStart).padding(16.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.7f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(GColors.Secondary500))
                Spacer(Modifier.width(6.dp))
                Text("2,100 msnm", style = GType.labelMd, color = GColors.TextPrimary)
            }
            Row(Modifier.align(Alignment.TopEnd).padding(16.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.7f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbTwilight, null, tint = GColors.Primary400, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("14°C Fresco", style = GType.labelMd, color = GColors.TextPrimary)
            }
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(32.dp).height(6.dp).clip(CircleShape).background(GColors.Primary))
                Box(Modifier.width(8.dp).height(6.dp).clip(CircleShape).background(GColors.BorderSubtle))
                Box(Modifier.width(8.dp).height(6.dp).clip(CircleShape).background(GColors.BorderSubtle))
            }
            Text("Corre con la manada de Loja", style = GType.headlineXl, color = GColors.TextPrimary)
            Text("Graba tus carreras, descubre rutas locales y entrena con el grupo Gacelas.", style = GType.bodyLg, color = GColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipBeneficio(Icons.Filled.Route, "Jipiro y Zamora", Modifier.weight(1f))
                ChipBeneficio(Icons.Filled.Sensors, "GPS en vivo", Modifier.weight(1f))
                ChipBeneficio(Icons.Filled.Groups, "Comunidad", Modifier.weight(1f))
            }
            BotonPrimario("Crear cuenta", onCrearCuenta)
            BotonSecundario("Ya tengo cuenta, iniciar sesión", onIniciarSesion)
        }
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
fun InicioSesionScreen(onAtras: () -> Unit, onEntrar: () -> Unit, onRecuperar: () -> Unit, onRegistro: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    PantallaAcceso(onAtras) {
        EncabezadoFormulario("Bienvenido de vuelta", "Inicia sesión para continuar con tus carreras")
        CampoTexto("Correo electrónico", email, { email = it }, "tu@email.com", tipo = KeyboardType.Email)
        CampoTexto("Contraseña", password, { password = it }, "••••••••", password = true)
        Text("¿Olvidaste tu contraseña?", style = GType.labelLg, color = GColors.Primary500,
            modifier = Modifier.align(Alignment.End).clickable(onClick = onRecuperar))
        BotonPrimario("Iniciar sesión", onEntrar)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("¿No tienes cuenta? ", style = GType.bodyMd, color = GColors.TextSecondary)
            Text("Crear cuenta", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.clickable(onClick = onRegistro))
        }
    }
}

@Composable
fun RegistroScreen(onAtras: () -> Unit, onCrear: () -> Unit, onIniciarSesion: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    PantallaAcceso(onAtras) {
        EncabezadoFormulario("Únete a la manada", "Crea tu cuenta y empieza a correr con la comunidad")
        CampoTexto("Nombre completo", nombre, { nombre = it }, "Tu nombre")
        CampoTexto("Correo electrónico", email, { email = it }, "tu@email.com", tipo = KeyboardType.Email)
        CampoTexto("Contraseña", password, { password = it }, "Mínimo 8 caracteres", password = true,
            ayuda = "Usa mínimo 8 caracteres con letras y números")
        BotonPrimario("Crear cuenta", onCrear)
        Text("Al crear una cuenta, aceptas nuestros Términos de Servicio y Política de Privacidad",
            style = GType.bodySm, color = GColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("¿Ya tienes cuenta? ", style = GType.bodyMd, color = GColors.TextSecondary)
            Text("Iniciar sesión", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.clickable(onClick = onIniciarSesion))
        }
    }
}

@Composable
fun RecuperarContrasenaScreen(onAtras: () -> Unit, enviadoInicial: Boolean = false) {
    var email by rememberSaveable { mutableStateOf("") }
    var enviado by rememberSaveable { mutableStateOf(enviadoInicial) }
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
            CampoTexto("Correo electrónico", email, { email = it }, "tu@email.com", tipo = KeyboardType.Email)
            BotonPrimario("Enviar enlace", { enviado = true })
        }
    }
}

