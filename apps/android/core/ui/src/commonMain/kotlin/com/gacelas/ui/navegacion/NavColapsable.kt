package com.gacelas.ui.navegacion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.ui.movimiento.Mov
import com.gacelas.ui.movimiento.MovimientoC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/**
 * Navbar B "menu colapsable" en movil: barra con una burbuja morada elevada que salta a la seccion
 * activa. Es generico: quien lo usa pasa sus items; el componente no conoce pantallas ni datos.
 *
 * - [insignia] es opcional y solo se dibuja con un conteo real mayor que cero.
 * - La burbuja se anima con las especificaciones de movimiento de la app (si el sistema quita las
 *   animaciones, salta sin transicion).
 * - Los colores salen de [ColoresNavColapsable]; por defecto, los tokens de la app.
 */
data class ItemNavColapsable(
    val id: String,
    val etiqueta: String,
    val icono: ImageVector,
    val insignia: Int? = null,
)

data class ColoresNavColapsable(
    val fondo: Color,
    val barra: Color,
    val borde: Color,
    val texto: Color,
    val activo: Color,
    val burbujaDesde: Color,
    val burbujaHasta: Color,
    val sobreBurbuja: Color,
    val insignia: Color,
    val sobreInsignia: Color,
)

@Composable
fun coloresNavColapsable(): ColoresNavColapsable = ColoresNavColapsable(
    fondo = GColors.C2Bg,
    barra = GColors.C2Card,
    borde = GColors.C2Linea,
    texto = GColors.C2Muted,
    activo = GColors.C2Violeta,
    burbujaDesde = GColors.C2Morado,
    burbujaHasta = GColors.Primary,
    sobreBurbuja = Color.White,
    insignia = Color(0xFFEF4444),
    sobreInsignia = Color.White,
)

private val CurvaSalto = CubicBezierEasing(0.3f, 1.3f, 0.5f, 1f)
private val TamBurbuja = 54.dp
private val AltoBarra = 64.dp

private fun texto(n: Int) = if (n > 99) "99+" else n.toString()

@Composable
fun BarraNavColapsable(
    items: List<ItemNavColapsable>,
    seleccionado: String,
    onElegir: (String) -> Unit,
    modifier: Modifier = Modifier,
    colores: ColoresNavColapsable = coloresNavColapsable(),
    prefijoTag: String = "navc",
) {
    if (items.isEmpty()) return
    val indice = items.indexOfFirst { it.id == seleccionado }.coerceAtLeast(0)
    Column(modifier.fillMaxWidth().background(colores.fondo).navigationBarsPadding().testTag("$prefijoTag-barra")) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(top = 22.dp, bottom = 8.dp)) {
            val ancho = maxWidth / items.size
            val x: Dp by animateDpAsState(
                ancho * indice + (ancho - TamBurbuja) / 2,
                MovimientoC2.spec(MovimientoC2.LARGO, Mov.escala, CurvaSalto),
                label = "burbuja",
            )
            Row(
                Modifier.fillMaxWidth().height(AltoBarra).clip(RoundedCornerShape(22.dp)).background(colores.barra)
                    .border(BorderStroke(1.dp, colores.borde), RoundedCornerShape(22.dp)),
            ) {
                items.forEachIndexed { i, item -> Ranura(item, i == indice, colores, prefijoTag, Modifier.width(ancho)) { onElegir(item.id) } }
            }
            // Burbuja elevada: queda sobre la ranura activa y cubre su icono.
            val activo = items[indice]
            Box(
                Modifier.offset(x = x, y = (-24).dp).size(TamBurbuja + 8.dp).clip(CircleShape).background(colores.fondo)
                    .padding(4.dp).testTag("$prefijoTag-burbuja"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(TamBurbuja).shadow(10.dp, CircleShape, ambientColor = colores.burbujaDesde, spotColor = colores.burbujaDesde)
                        .clip(CircleShape).background(Brush.linearGradient(listOf(colores.burbujaDesde, colores.burbujaHasta))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(activo.icono, contentDescription = null, tint = colores.sobreBurbuja, modifier = Modifier.size(26.dp))
                    activo.insignia?.takeIf { it > 0 }?.let { Insignia(it, colores, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)) }
                }
            }
        }
    }
}

@Composable
private fun Ranura(
    item: ItemNavColapsable,
    activa: Boolean,
    colores: ColoresNavColapsable,
    prefijoTag: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(if (activa) colores.activo else colores.texto, Mov.corto(), label = "ranura")
    val cuenta = item.insignia?.takeIf { it > 0 }
    val descripcion = if (cuenta != null) "${item.etiqueta}, $cuenta pendientes" else item.etiqueta
    Column(
        modifier.height(AltoBarra)
            .clickable(remember { MutableInteractionSource() }, null, role = Role.Tab, onClick = onClick)
            .semantics { selected = activa; contentDescription = descripcion }
            .testTag("$prefijoTag-${item.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(top = 9.dp).size(26.dp), contentAlignment = Alignment.Center) {
            // La ranura activa deja su icono vacio: lo dibuja la burbuja.
            if (!activa) {
                Icon(item.icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                if (cuenta != null) Insignia(cuenta, colores, Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-5).dp))
            }
        }
        Text(
            item.etiqueta,
            style = GType.labelMd.copy(fontSize = 10.sp, fontWeight = if (activa) FontWeight.Bold else FontWeight.SemiBold),
            color = color, maxLines = 1, overflow = TextOverflow.Clip, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
        )
    }
}

@Composable
private fun Insignia(n: Int, colores: ColoresNavColapsable, modifier: Modifier = Modifier) {
    Box(
        modifier.height(16.dp).width(if (n > 9) 24.dp else 16.dp).clip(RoundedCornerShape(8.dp)).background(colores.insignia)
            .border(1.5.dp, colores.barra, RoundedCornerShape(8.dp)).testTag("navc-insignia"),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto(n), color = colores.sobreInsignia, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}
