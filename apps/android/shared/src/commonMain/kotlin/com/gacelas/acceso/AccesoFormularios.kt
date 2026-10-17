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
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.testTag
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

/** Crear cuenta (compartida Android/iOS). El estado y la llamada a Auth van en cada plataforma. */
@Composable
fun RegistroContenido(
    nombre: String,
    email: String,
    password: String,
    error: String?,
    cargando: Boolean,
    onNombre: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onCrear: () -> Unit,
    onAtras: () -> Unit,
    onIniciarSesion: () -> Unit,
    botonGoogle: @Composable () -> Unit,
) {
    var verTerminos by remember { mutableStateOf(false) }
    PantallaAcceso(onAtras) {
        EncabezadoFormulario("Únete a la manada", "Crea tu cuenta y empieza a correr con la comunidad")
        CampoTexto("Nombre completo", nombre, onNombre, "Tu nombre")
        CampoTexto("Correo electrónico", email, onEmail, "tu@email.com", tipo = KeyboardType.Email)
        CampoTexto("Contraseña", password, onPassword, "Mínimo 8 caracteres", password = true,
            ayuda = "Usa mínimo 8 caracteres con letras y números", onListo = {})
        MensajeError(error)
        BotonPrimario(if (cargando) "Creando cuenta..." else "Crear cuenta", onCrear, enabled = !cargando)
        Text("Al crear una cuenta, aceptas nuestros Términos de Servicio y Política de Privacidad",
            style = GType.bodySm, color = GColors.Primary500, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable { verTerminos = true })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("¿Ya tienes cuenta? ", style = GType.bodyMd, color = GColors.TextSecondary)
            Text("Iniciar sesión", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.clickable(onClick = onIniciarSesion))
        }
        botonGoogle()
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

/** Recuperar contraseña (compartida Android/iOS). */
@Composable
fun RecuperarContenido(
    email: String,
    enviado: Boolean,
    error: String?,
    onEmail: (String) -> Unit,
    onEnviar: () -> Unit,
    onAtras: () -> Unit,
) {
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
            CampoTexto("Correo electrónico", email, onEmail, "tu@email.com", tipo = KeyboardType.Email)
            MensajeError(error)
            BotonPrimario("Enviar enlace", onEnviar)
        }
    }
}
