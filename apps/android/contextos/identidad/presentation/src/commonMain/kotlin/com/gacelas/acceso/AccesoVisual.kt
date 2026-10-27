package com.gacelas.acceso

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gacelas.shared.decimales
import com.gacelas.ui.theme.SpaceGrotesk
import com.gacelas.ui.theme.Inter

/**
 * Traduccion 1:1 del export de Stitch de la variante C4 (tools/mapa-acceso/C4-*.html).
 * Los valores (hex, tamanos en px = dp/sp, radios, sombras) salen tal cual del HTML/Tailwind.
 */
/** Tokens de acceso alineados al rediseño C2 Deporte cinetico. */
object C4 {
    val Fondo = Color(0xFF0C1322)            // surface-container-lowest
    val Tarjeta = Color(0xFF191F2F)          // bg-[#131926]/85 (bienvenida) /90 (login)
    val SurfaceContainer = Color(0xFF232A3A)
    val OnSurface = Color(0xFFDFE2EE)
    val Secondary = Color(0xFFA8B0C2)
    val Naranja = Color(0xFF7B1FA2)
    val Enlace = Color(0xFFC084FC)
    val Magenta = Color(0xFFC084FC)
    val MagentaOscuro = Color(0xFF7B1FA2)
    val Subtitulo = Color(0xFFD5DAE4)
    val Blanco10 = Color.White.copy(alpha = 0.10f)
    val Blanco5 = Color.White.copy(alpha = 0.05f)
}

object C4Texto {
    /** label-caps: Inter 11/14, 0.08em, 600 (tracking-wider = 0.05em). */
    val labelCaps = TextStyle(fontFamily = Inter, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.12.em, fontWeight = FontWeight.ExtraBold)
    val headlineSm = TextStyle(fontFamily = SpaceGrotesk, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.03.em, fontWeight = FontWeight.Bold)
    val headlineLgMobile = TextStyle(fontFamily = SpaceGrotesk, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = 0.015.em, fontWeight = FontWeight.Bold)
    val bodyMd = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 24.sp, letterSpacing = (-0.005).em)
    val bodySm = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp)
}

/** Datos reales de la ruta: trazo "1" (Parque Jipiro) de Mapas.trazos; la distancia se calcula. */
object PortadaRuta {
    const val NOMBRE = "Jipiro"
    fun km(): Double = com.gacelas.rutas.domain.reglas.Guia.longitudM(com.gacelas.rutas.infrastructure.TrazosRutas.puntos.getValue("1")) / 1000.0
    fun etiqueta(): String = "$NOMBRE · ${decimales(km(), 1)} KM"
}

/** Fondo de mapa exportado de Stitch (390x884 @3x). Ocupa toda la ventana, el teclado no lo mueve. */
@Composable
fun FondoMapaC4(login: Boolean) {
    Image(
        imagenGacelas(if (login) ImagenGacelas.MapaLogin else ImagenGacelas.MapaBienvenida),
        contentDescription = "Mapa de Loja con la ruta del Parque Jipiro",
        contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize().testTag("mapa-portada"),
    )
}

/** Toca fuera de un campo: quita el foco y esconde el teclado. */
@Composable
fun Modifier.cerrarTecladoAlTocarFuera(): Modifier {
    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    return this.pointerInput(Unit) { detectTapGestures { foco.clearFocus(); teclado?.hide() } }
}

/** Marca de la cabecera C4: cuadro naranja 32dp r8 + GACELAS / LOJA RUNNING CO. */
@Composable
fun MarcaC4() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        com.gacelas.ui.components.IconoGacelas(36.dp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text("GACELAS", style = C4Texto.headlineSm.copy(letterSpacing = 0.015.em), color = Color.White)
            Text("LOJA RUNNING CO.", style = TextStyle(fontFamily = Inter, fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.SemiBold),
                color = C4.Secondary, modifier = Modifier.offset(y = (-4).dp))
        }
    }
}

@Composable
fun ChipC4(texto: String, modifier: Modifier = Modifier) {
    Row(
        modifier.shadow(5.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(CircleShape).background(C4.SurfaceContainer.copy(alpha = 0.75f)).border(1.dp, C4.Blanco10, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.shadow(4.dp, CircleShape, ambientColor = C4.Magenta, spotColor = C4.Magenta).size(6.dp).clip(CircleShape).background(C4.Magenta))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = C4Texto.labelCaps, color = C4.OnSurface, maxLines = 1)
    }
}

/** Indicador "AV. ORILLAS DEL ZAMORA" sobre el mapa. */
@Composable
fun IndicadorZonaC4(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(C4.Fondo.copy(alpha = 0.70f)).border(1.dp, C4.Blanco5, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(C4.Magenta))
        Spacer(Modifier.width(8.dp))
        Text("AV. ORILLAS DEL ZAMORA", style = C4Texto.labelCaps.copy(fontSize = 10.sp, letterSpacing = 0.1.em, fontWeight = FontWeight.Normal), color = C4.Secondary)
    }
}

/** Tarjeta "aero-glass": r28, #131926 (Stitch usa 85/90% + backdrop-blur-xl; sin blur de fondo en Compose se sube a 95/96% para el mismo resultado), borde blanco 10%, sombra 0 16 40 negro 65%, hairline superior. */
@Composable
fun TarjetaC4(modifier: Modifier = Modifier, alpha: Float = 0.95f, padding: Dp = 24.dp, content: @Composable ColumnScope.() -> Unit) {
    val forma = RoundedCornerShape(28.dp)
    Box(
        modifier.fillMaxWidth()
            .shadow(20.dp, forma, ambientColor = Color.Black.copy(alpha = 0.65f), spotColor = Color.Black.copy(alpha = 0.65f))
            .clip(forma).background(C4.Tarjeta.copy(alpha = alpha)).border(1.dp, C4.Blanco10, forma),
    ) {
        Column(Modifier.fillMaxWidth().padding(padding), content = content)
        Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.20f), Color.Transparent))))
    }
}

/** Boton principal naranja: pildora #D97706, Space Grotesk bold, flecha; glow 0 4 22 naranja (atenuado). */
@Composable
fun BotonC4(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, alto: Dp = 52.dp, tamTexto: Int = 18, enabled: Boolean = true) {
    Row(
        modifier.fillMaxWidth().height(alto)
            .shadow(8.dp, CircleShape, ambientColor = C4.Naranja.copy(alpha = 0.30f), spotColor = C4.Naranja.copy(alpha = 0.38f))
            .clip(CircleShape).background(if (enabled) C4.Naranja else C4.Naranja.copy(alpha = 0.6f))
            .clickable(enabled = enabled, onClick = onClick).testTag("boton-principal"),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, style = C4Texto.headlineSm.copy(fontSize = tamTexto.sp), color = Color.White)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Pie con borde superior blanco 5%: "¿Ya tienes cuenta? Inicia sesión". */
@Composable
fun PieC4(texto: String, enlace: String, onClick: () -> Unit, arriba: Dp = 12.dp) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C4.Blanco5))
        Row(Modifier.fillMaxWidth().padding(top = arriba), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(texto, style = C4Texto.bodySm, color = C4.Secondary)
            Text(
                enlace, style = C4Texto.bodySm.copy(fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline), color = C4.Enlace,
                modifier = Modifier.padding(start = 4.dp).clickable(onClick = onClick),
            )
        }
    }
}

/** Titular "Corre. Comparte. / Supera." con degradado blanco → #BCC7DE en la segunda linea. */
@Composable
fun TitularC4(compacto: Boolean) {
    val estilo = C4Texto.headlineLgMobile
    Text(
        buildAnnotatedString {
            append("Corre. Comparte.")
            if (compacto) append(" ") else append("\n")
            withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(Color.White, Color.White, C4.Secondary)))) { append("Supera.") }
        },
        style = estilo, color = Color.White,
    )
}

@Composable
fun SeparadorC4() {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(C4.Blanco10))
        Text("o", style = C4Texto.bodySm.copy(fontSize = 12.sp), color = C4.Secondary.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(C4.Blanco10))
    }
}

/** Boton circular "atras" 32dp de la cabecera del login. */
@Composable
fun AtrasC4(onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(C4.SurfaceContainer.copy(alpha = 0.8f)).border(1.dp, C4.Blanco10, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White, modifier = Modifier.size(19.dp)) }
}

/** Campo del login C4: etiqueta label-caps, caja h48 r12 #1C2028/90, icono 18 a la izquierda, borde naranja con foco. */
@Composable
fun CampoC4(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    tipo: KeyboardType = KeyboardType.Text,
    mostrarEtiqueta: Boolean = true,
    accion: (@Composable () -> Unit)? = null,
    onListo: (() -> Unit)? = null,
) {
    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    var visible by rememberSaveable { mutableStateOf(false) }
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (mostrarEtiqueta || accion != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (mostrarEtiqueta) Text(etiqueta.uppercase(), style = C4Texto.labelCaps, color = C4.Secondary, modifier = Modifier.weight(1f))
                else Spacer(Modifier.weight(1f))
                accion?.invoke()
            }
        }
        val forma = RoundedCornerShape(12.dp)
        BasicTextField(
            value = valor, onValueChange = onCambio, singleLine = true, interactionSource = interaccion,
            textStyle = C4Texto.bodySm.copy(color = Color.White),
            cursorBrush = SolidColor(C4.Naranja),
            visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else tipo, imeAction = if (onListo != null) ImeAction.Done else ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { if (!foco.moveFocus(FocusDirection.Down)) { foco.clearFocus(); teclado?.hide() } },
                onDone = { foco.clearFocus(); teclado?.hide(); onListo?.invoke() },
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("campo-$etiqueta"),
            decorationBox = { campo ->
                Row(
                    Modifier.fillMaxSize().clip(forma).background(C4.SurfaceContainer.copy(alpha = 0.9f))
                        .border(1.dp, if (enfocado) C4.Naranja else C4.Blanco10, forma).padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icono, null, tint = C4.Secondary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (valor.isEmpty()) Text(if (mostrarEtiqueta) placeholder else etiqueta, style = C4Texto.bodySm, color = C4.Secondary.copy(alpha = 0.5f))
                        campo()
                    }
                    if (password) {
                        Box(Modifier.size(36.dp).clip(CircleShape).clickable { visible = !visible }.testTag("ver-contrasena"), contentAlignment = Alignment.Center) {
                            Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                if (visible) "Ocultar contraseña" else "Mostrar contraseña", tint = C4.Secondary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
        )
    }
}

object IconosC4 { val Correo = Icons.Outlined.Mail; val Candado = Icons.Outlined.Lock }

/** Alturas a partir de las cuales cada pantalla deja de mostrar piezas secundarias (sin scroll). */
object AlturasAcceso {
    val BIENVENIDA_COMPACTA = 700.dp
    /** Login: por debajo (teclado abierto) se ocultan subtitulo, separador, Google, pie e indicador. */
    val LOGIN_COMPACTO = 680.dp
    /** Login: por debajo se quitan tambien etiquetas y cabecera (el gesto atras sigue funcionando). */
    val LOGIN_MINIMO = 480.dp
}
