package com.gacelas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.gacelas.ui.theme.Inter
import com.gacelas.ui.theme.SpaceGrotesk

/** Rojo de las acciones destructivas en los modales C2. */
val PeligroC2 = Color(0xFFBE185D)

/**
 * Modal C2 (extra/modales): hoja inferior oscura con linea violeta, titular Anton en cursiva,
 * texto atenuado y acciones apiladas. Misma forma de uso que AlertDialog para sustituirlo sin tocar la logica.
 */
@Composable
fun DialogoC2(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") containerColor: Color = GColors.C2Card,
    @Suppress("UNUSED_PARAMETER") titleContentColor: Color = Color.White,
    @Suppress("UNUSED_PARAMETER") textContentColor: Color = GColors.C2Muted,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = onDismissRequest),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier.fillMaxWidth().heightIn(max = 640.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(GColors.C2Card)
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .navigationBarsPadding().imePadding(),
            ) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, GColors.C2Violeta))))
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(GColors.C2Linea))
                }
                Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MaterialTheme(colorScheme = darkColorScheme(primary = GColors.C2Violeta, onPrimary = GColors.C2Bg, surface = GColors.C2Card)) {
                        if (icon != null || title != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (icon != null) Box(
                                Modifier.size(52.dp).clip(FormaBotonC2).background(GColors.C2S2),
                                contentAlignment = Alignment.Center,
                            ) { CompositionLocalProvider(LocalContentColor provides GColors.C2Violeta) { icon() } }
                            if (title != null) CompositionLocalProvider(
                                LocalContentColor provides Color.White,
                                LocalTextStyle provides TextStyle(fontFamily = SpaceGrotesk, fontStyle = FontStyle.Italic, fontSize = 26.sp, lineHeight = 30.sp),
                            ) { title() }
                        }
                        if (text != null) CompositionLocalProvider(
                            LocalContentColor provides GColors.C2Muted,
                            LocalTextStyle provides GType.bodyMd.copy(color = GColors.C2Muted),
                        ) { text() }
                        Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CompositionLocalProvider(LocalContentColor provides Color.White) {
                                confirmButton()
                                dismissButton?.invoke()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Boton de los modales C2: principal (violeta o rojo con sombra) o secundario (gris). */
@Composable
fun BotonModalC2(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, peligro: Boolean = false, secundario: Boolean = false, enabled: Boolean = true) {
    val forma = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
    val fondo = when {
        secundario -> Brush.linearGradient(listOf(GColors.C2S2, GColors.C2S2))
        peligro -> Brush.linearGradient(listOf(Color(0xFF9D174D), PeligroC2))
        else -> Brush.linearGradient(listOf(Color(0xFF7B1FA2), GColors.C2Morado))
    }
    Box(modifier.fillMaxWidth().height(52.dp)) {
        if (!secundario && enabled) Box(Modifier.matchParentSize().offset(4.dp, 4.dp).clip(forma).background(if (peligro) Color(0xFFF472B6) else GColors.C2Violeta))
        Box(
            Modifier.matchParentSize().clip(forma).background(fondo).clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                texto,
                style = TextStyle(fontFamily = Inter, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Black, fontSize = 15.sp),
                color = if (enabled) Color.White else GColors.C2Muted,
            )
        }
    }
}
