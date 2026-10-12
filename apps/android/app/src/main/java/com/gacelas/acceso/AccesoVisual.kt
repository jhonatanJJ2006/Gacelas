package com.gacelas.acceso

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gacelas.R
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Colores de la portada de acceso (variante C4 aprobada). */
object AccesoColores {
    /** Camiseta del corredor 3D (textura de gacelas-runner-animated.glb). */
    val RutaMagenta = Color(0xFFAB0660)
    /** Tono claro derivado para el brillo de la ruta y los puntos. */
    val RutaBrillo = Color(0xFFD6338A)
    val Vidrio = Color(0xFF1F2937).copy(alpha = 0.82f)
    val BordeVidrio = Color(0xFFFFFFFF).copy(alpha = 0.08f)
}

/**
 * Datos reales de la ruta de la portada: el trazo "1" (Parque Jipiro) de Mapas.trazos.
 * La distancia se calcula del trazo, no se escribe a mano.
 */
object PortadaRuta {
    const val NOMBRE = "Jipiro"
    fun km(): Double = com.gacelas.data.rutas.Guia.longitudM(
        com.gacelas.ui.components.Mapas.trazos.getValue("1").map { it.latitude to it.longitude },
    ) / 1000.0
    fun etiqueta(): String = "$NOMBRE · ${"%.1f".format(java.util.Locale.US, km())} KM"
}

/**
 * Coloca la imagen del mapa de modo que la ruta (en 50% x 42% de la imagen) quede en el centro
 * del espacio visible, sin dejar bordes vacios. Asi el mapa se comprime sin perder la ruta.
 */
internal object FocoRuta : Alignment {
    const val FX = 0.5f
    const val FY = 0.42f
    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset {
        fun eje(contenido: Int, hueco: Int, foco: Float, centro: Float): Int {
            val deseado = (hueco * centro - contenido * foco).toInt()
            val min = minOf(0, hueco - contenido)
            return deseado.coerceIn(min, 0)
        }
        return IntOffset(eje(size.width, space.width, FX, 0.5f), eje(size.height, space.height, FY, 0.5f))
    }
}

/**
 * Mapa oscuro de Loja (OpenStreetMap, generado sin conexion: tools/mapa-acceso/render.py) con la ruta
 * real de Jipiro en magenta. Es una imagen empaquetada: funciona sin red ni teselas descargadas.
 */
@Composable
fun MapaPortada(modifier: Modifier = Modifier, contenido: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {}) {
    Box(modifier.testTag("mapa-portada")) {
        Image(
            painterResource(R.drawable.mapa_loja_jipiro), contentDescription = "Mapa de Loja con la ruta del Parque Jipiro",
            contentScale = ContentScale.Crop, alignment = FocoRuta, modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxWidth().height(96.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, GColors.BgPrimary))))
        Box(Modifier.fillMaxWidth().height(72.dp).align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(GColors.BgPrimary.copy(alpha = 0.7f), Color.Transparent))))
        contenido()
    }
}

@Composable
fun ChipRuta(texto: String, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.75f))
            .border(1.dp, AccesoColores.BordeVidrio, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(AccesoColores.RutaBrillo))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = GType.labelMd, color = GColors.TextPrimary, maxLines = 1)
    }
}

/** Tarjeta de vidrio esmerilado de la portada. */
@Composable
fun TarjetaVidrio(modifier: Modifier = Modifier, padding: androidx.compose.ui.unit.Dp = 20.dp, espacio: androidx.compose.ui.unit.Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    val forma = RoundedCornerShape(24.dp)
    Column(
        modifier.fillMaxWidth().clip(forma).background(AccesoColores.Vidrio).border(1.dp, AccesoColores.BordeVidrio, forma).padding(padding),
        verticalArrangement = Arrangement.spacedBy(espacio), content = content,
    )
}

/** Separador "o" centrado entre dos lineas iguales. */
@Composable
fun SeparadorO() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(GColors.BorderSubtle))
        Text("o", style = GType.labelMd, color = GColors.TextSecondary, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(GColors.BorderSubtle))
    }
}

/** Alturas a partir de las cuales cada pantalla deja de mostrar piezas secundarias (sin scroll). */
object AlturasAcceso {
    /** Bienvenida: por debajo se usa titulo y texto compactos. */
    val BIENVENIDA_COMPACTA = 700.dp
    /** Login: por debajo (p. ej. teclado abierto) se ocultan subtitulo, Google y pie. */
    val LOGIN_COMPACTO = 560.dp
    /** Login: por debajo se quitan tambien etiquetas, encabezado y mapa (el gesto atras sigue funcionando). */
    val LOGIN_MINIMO = 480.dp
}
