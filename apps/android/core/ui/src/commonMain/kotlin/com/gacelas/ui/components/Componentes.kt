package com.gacelas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Pantalla base con fondo oscuro y scroll vertical. */
@Composable
fun Pantalla(
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier.fillMaxSize().background(GColors.BgPrimary)
    Column(
        modifier = (if (scroll) base.verticalScroll(rememberScrollState()) else base).padding(padding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

/** Encabezado con boton atras, titulo y accion opcional. */
@Composable
fun EncabezadoAtras(
    titulo: String = "",
    onAtras: () -> Unit,
    accion: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(GColors.BgSecondary).statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onAtras) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = GColors.TextPrimary)
        }
        Text(titulo, style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
        accion?.invoke(this)
    }
}

/** Marca Gacelas Loja: icono A "Gacela en ruta" con la palabra GACELAS (docs/MARCA.md). */
@Composable
fun MarcaGacelas(modifier: Modifier = Modifier) = MarcaGacelasA(44.dp, "LOJA, ECUADOR", modifier)

@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    enabled: Boolean = true,
    color: Color = GColors.Primary,
) {
    Box(modifier.fillMaxWidth().height(56.dp)) {
        // Sombra solida desplazada en violeta, firma visual C2.
        if (enabled) Box(Modifier.matchParentSize().offset(5.dp, 5.dp).clip(FormaBotonC2).background(GColors.C2Violeta))
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = FormaBotonC2,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = GColors.SobreColor),
    ) {
        Text(texto.uppercase(), style = GType.labelLg.copy(fontSize = GType.bodyLg.fontSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
        if (icono != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icono, contentDescription = null)
        }
    }
}
}

@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = GColors.TextPrimary) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth().height(52.dp)) {
        Text(texto, style = GType.labelLg, color = color)
    }
}

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    fondo: Color = GColors.SurfaceContainer,
    borde: Color? = GColors.BorderSubtle,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = FormaC2
    var m = modifier.fillMaxWidth().clip(shape).background(fondo)
    if (borde != null && borde != GColors.BorderSubtle) m = m.border(BorderStroke(1.dp, borde), shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String = "",
    password: Boolean = false,
    tipo: KeyboardType = KeyboardType.Text,
    lineas: Int = 1,
    ayuda: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    /** Si se da, el teclado muestra "Listo" y lo ejecuta (por ejemplo enviar el formulario). */
    onListo: (() -> Unit)? = null,
    /** En pantallas sin espacio (teclado abierto) la etiqueta se omite y se usa como placeholder. */
    mostrarEtiqueta: Boolean = true,
) {
    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    var visible by rememberSaveable { mutableStateOf(false) }
    val multilinea = lineas > 1
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (mostrarEtiqueta) Text(etiqueta, style = GType.labelLg, color = GColors.TextSecondary)
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            placeholder = { Text(if (mostrarEtiqueta) placeholder else etiqueta, color = GColors.TextSecondary.copy(alpha = 0.6f)) },
            singleLine = lineas == 1,
            minLines = lineas,
            visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else tipo,
                imeAction = if (multilinea) ImeAction.Default else if (onListo != null) ImeAction.Done else ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { if (!foco.moveFocus(FocusDirection.Down)) { foco.clearFocus(); teclado?.hide() } },
                onDone = { foco.clearFocus(); teclado?.hide(); onListo?.invoke() },
            ),
            trailingIcon = if (password) {
                {
                    IconButton(onClick = { visible = !visible }, modifier = Modifier.size(48.dp).testTag("ver-contrasena")) {
                        Icon(
                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = GColors.TextSecondary,
                        )
                    }
                }
            } else trailing,
            shape = RoundedCornerShape(12.dp),
            textStyle = GType.bodyLg,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GColors.SurfaceContainer,
                unfocusedContainerColor = GColors.SurfaceContainer,
                focusedBorderColor = GColors.Primary,
                unfocusedBorderColor = GColors.BorderSubtle,
                focusedTextColor = GColors.TextPrimary,
                unfocusedTextColor = GColors.TextPrimary,
                cursorColor = GColors.Primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (ayuda != null) Text(ayuda, style = GType.bodySm, color = GColors.TextSecondary)
    }
}

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier, accion: String? = null, onAccion: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
        if (accion != null) {
            Text(accion, style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.clickable(onClick = onAccion))
        }
    }
}

@Composable
fun EtiquetaGrupo(texto: String) {
    EtiquetaC2(texto, Modifier.padding(top = 12.dp, bottom = 2.dp))
}

@Composable
fun Interruptor(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = GColors.Primary,
            checkedThumbColor = GColors.SobreColor,
            uncheckedTrackColor = GColors.SurfaceHigh,
            uncheckedBorderColor = GColors.BorderSubtle,
        ),
    )
}

/** Fila de ajustes con icono, titulo, descripcion y control a la derecha. */
@Composable
fun FilaAjuste(
    icono: ImageVector,
    titulo: String,
    descripcion: String? = null,
    colorIcono: Color = GColors.Primary500,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GColors.TextSecondary)
    },
) {
    val m = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Row(m.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(4.dp)).background(GColors.C2S2), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = GType.bodyMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = GColors.TextPrimary)
            if (descripcion != null) Text(descripcion, style = GType.bodySm, color = GColors.TextSecondary)
        }
        trailing()
    }
}

@Composable
fun FilaInterruptor(icono: ImageVector, titulo: String, descripcion: String? = null, checked: Boolean, onChange: (Boolean) -> Unit, colorIcono: Color = GColors.Primary500) {
    FilaAjuste(icono, titulo, descripcion, colorIcono, onClick = { onChange(!checked) }) { Interruptor(checked, onChange) }
}

@Composable
fun Pastilla(texto: String, activo: Boolean, onClick: () -> Unit = {}, icono: ImageVector? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(2.dp))
            .background(if (activo) GColors.C2Violeta else GColors.C2S2)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icono != null) Icon(icono, null, Modifier.size(16.dp), tint = if (activo) GColors.BgPrimary else GColors.TextSecondary)
        Text(texto.uppercase(), style = GType.labelMd.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = androidx.compose.ui.text.font.FontWeight.Black), color = if (activo) GColors.BgPrimary else GColors.TextSecondary)
    }
}

/** Metrica con valor grande y etiqueta pequena. */
@Composable
fun Metrica(valor: String, etiqueta: String, modifier: Modifier = Modifier, color: Color = GColors.TextPrimary, grande: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = if (grande) GType.headlineLg else GType.headlineSm, color = color)
        Text(etiqueta, style = GType.labelMd, color = GColors.TextSecondary)
    }
}

/** Circulo de avatar con degradado (placeholder de foto). */
@Composable
fun AvatarCirculo(tamano: Dp = 40.dp, indice: Int = 0) {
    val colores = listOf(
        listOf(GColors.Primary, GColors.Primary400),
        listOf(GColors.Secondary, GColors.Secondary500),
        listOf(GColors.Info, GColors.Tertiary),
    )[indice % 3]
    Box(Modifier.size(tamano).clip(CircleShape).background(Brush.linearGradient(colores)).border(2.dp, GColors.BgPrimary, CircleShape))
}

@Composable
fun IconoCirculo(icono: ImageVector, fondo: Color, tint: Color, tamano: Dp = 48.dp) {
    Box(Modifier.size(tamano).clip(CircleShape).background(fondo), contentAlignment = Alignment.Center) {
        Icon(icono, null, tint = tint, modifier = Modifier.size(tamano / 2))
    }
}

/** Forma C2 de tarjeta: esquinas de 4 dp y corte diagonal abajo a la derecha. */
val FormaC2: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.CutCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 18.dp, bottomStart = 0.dp)

/** Forma C2 de boton principal: paralelogramo con cortes diagonales. */
val FormaBotonC2: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)

/** Etiqueta C2: mayusculas, violeta, espaciado amplio. */
@Composable
fun EtiquetaC2(texto: String, modifier: Modifier = Modifier, color: Color = GColors.C2Violeta) {
    Text(texto.uppercase(), style = GType.labelMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, letterSpacing = androidx.compose.ui.unit.TextUnit(0.12f, androidx.compose.ui.unit.TextUnitType.Em)), color = color, modifier = modifier)
}

/** Metrica C2 compacta: numero Saira Semi Condensed pequeno y etiqueta en mayusculas. */
@Composable
fun MetricaC2(valor: String, etiqueta: String, modifier: Modifier = Modifier, color: Color = GColors.TextPrimary) {
    Column(modifier) {
        Text(etiqueta.uppercase(), style = GType.labelMd.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = GColors.C2Muted)
        Text(valor, style = GType.headlineSm, color = color)
    }
}

/** Bloque de fecha C2: dia en Saira Semi Condensed sobre violeta oscuro y mes en mayusculas. */
@Composable
fun FechaC2(dia: String, mes: String, modifier: Modifier = Modifier) {
    Column(
        modifier.width(52.dp).clip(FormaBotonC2).background(GColors.C2Morado.copy(alpha = 0.35f)).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(dia, style = GType.headlineMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black), color = GColors.TextPrimary)
        Text(mes.uppercase(), style = GType.labelMd.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)), color = GColors.C2Violeta)
    }
}

/** Boton pequeno C2 inclinado (Asistire, Seguir, Inscribirse). [activo] lo pinta en gris como estado ya elegido. */
@Composable
fun BotonChipC2(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, activo: Boolean = false) {
    Text(
        texto.uppercase(),
        style = GType.labelMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
        color = if (activo) GColors.C2Violeta else GColors.SobreColor,
        modifier = modifier.clip(FormaBotonC2).background(if (activo) GColors.C2S2 else GColors.C2Morado)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** Grupo de ajustes C2: tarjeta unica con filas separadas por lineas finas. */
@Composable
fun GrupoC2(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(FormaC2).background(GColors.C2Card).border(1.dp, GColors.C2Linea, FormaC2), content = content)
}
