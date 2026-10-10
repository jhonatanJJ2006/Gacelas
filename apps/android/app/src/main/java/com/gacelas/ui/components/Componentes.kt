package com.gacelas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

/** Marca Gacelas Loja con icono de corredor. */
@Composable
fun MarcaGacelas(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(GColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = GColors.OnPrimary)
        }
        Column {
            Text("GACELAS", style = GType.titleMd.copy(fontWeight = FontWeight.Black), color = GColors.TextPrimary)
            Text("LOJA, ECUADOR", style = GType.labelMd, color = GColors.Primary500)
        }
    }
}

@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    enabled: Boolean = true,
    color: Color = GColors.Primary,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = GColors.SobreColor),
    ) {
        Text(texto, style = GType.labelLg.copy(fontSize = GType.bodyLg.fontSize))
        if (icono != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icono, contentDescription = null)
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
    val shape = RoundedCornerShape(16.dp)
    var m = modifier.fillMaxWidth().clip(shape).background(fondo)
    if (borde != null) m = m.border(BorderStroke(1.dp, borde), shape)
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(etiqueta, style = GType.labelLg, color = GColors.TextSecondary)
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            placeholder = { Text(placeholder, color = GColors.TextSecondary.copy(alpha = 0.6f)) },
            singleLine = lineas == 1,
            minLines = lineas,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else tipo),
            trailingIcon = trailing,
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
    Text(texto.uppercase(), style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.padding(top = 8.dp))
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
        Icon(icono, contentDescription = null, tint = colorIcono)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = GType.bodyLg, color = GColors.TextPrimary)
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
        Modifier.clip(CircleShape)
            .background(if (activo) GColors.Primary else GColors.SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icono != null) Icon(icono, null, Modifier.size(16.dp), tint = if (activo) GColors.BgPrimary else GColors.TextSecondary)
        Text(texto, style = GType.labelMd, color = if (activo) GColors.BgPrimary else GColors.TextSecondary)
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
