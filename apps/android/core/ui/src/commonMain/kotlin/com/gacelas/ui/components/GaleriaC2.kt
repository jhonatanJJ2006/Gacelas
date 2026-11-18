package com.gacelas.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gacelas.ui.movimiento.Mov
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.launch

/**
 * Galeria deslizable de fotos (o mapa + fotos): se desliza o se toca el tercio izquierdo/derecho
 * para pasar de pagina, como las historias. El centro llama a [onTocar]. Lleva puntos y contador.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GaleriaC2(
    paginas: Int,
    modifier: Modifier = Modifier,
    alto: Dp = 170.dp,
    forma: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    onTocar: (() -> Unit)? = null,
    tag: String = "galeria",
    pagina: @Composable (Int) -> Unit,
) {
    val estado = rememberPagerState { paginas }
    val alcance = rememberCoroutineScope()
    val tocar by androidx.compose.runtime.rememberUpdatedState(onTocar)
    val ir = { i: Int -> alcance.launch { estado.animateScrollToPage(i.coerceIn(0, paginas - 1)) }; Unit }
    Box(
        modifier.fillMaxWidth().height(alto).clip(forma).testTag(tag)
            .semantics { contentDescription = "Galería, foto ${estado.currentPage + 1} de $paginas" }
            // En el padre: el pager (hijo) consume el deslizamiento y aqui solo llegan los toques.
            .pointerInput(paginas) {
                detectTapGestures { p ->
                    val tercio = size.width / 3f
                    when {
                        p.x < tercio && estado.currentPage > 0 -> ir(estado.currentPage - 1)
                        p.x > tercio * 2 && estado.currentPage < paginas - 1 -> ir(estado.currentPage + 1)
                        else -> tocar?.invoke()
                    }
                }
            },
    ) {
        HorizontalPager(estado, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { i ->
            Box(Modifier.fillMaxSize()) { pagina(i) }
        }
        if (paginas > 1) {
            Text(
                "${estado.currentPage + 1}/$paginas", style = GType.labelMd, color = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
            if (estado.currentPage > 0) Flecha(Icons.AutoMirrored.Filled.KeyboardArrowLeft, Modifier.align(Alignment.CenterStart))
            if (estado.currentPage < paginas - 1) Flecha(Icons.AutoMirrored.Filled.KeyboardArrowRight, Modifier.align(Alignment.CenterEnd))
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(paginas) { i ->
                    val activo = i == estado.currentPage
                    val ancho by animateDpAsState(if (activo) 14.dp else 6.dp, Mov.corto(), label = "punto")
                    val color by animateColorAsState(if (activo) Color.White else Color.White.copy(alpha = 0.5f), Mov.corto(), label = "color-punto")
                    Box(Modifier.width(ancho).height(6.dp).clip(CircleShape).background(color))
                }
            }
        }
    }
}

@Composable
private fun Flecha(icono: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Box(
        modifier.padding(6.dp).size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icono, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
}
