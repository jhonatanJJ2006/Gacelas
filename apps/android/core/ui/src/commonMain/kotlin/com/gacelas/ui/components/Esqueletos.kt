package com.gacelas.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/*
 * Esqueletos de carga C2 (0.9.14): bloques del color de las tarjetas que laten de opacidad mientras
 * llegan los datos. Solo animan alfa en la capa de dibujo (graphicsLayer), sin recomponer por cuadro.
 * Misma idea que src/shared/ui/Esqueletos.tsx de la web.
 */

/** Pulso compartido de 0,45 a 1 (900 ms, ida y vuelta). */
@Composable
fun pulsoEsqueleto(): State<Float> = rememberInfiniteTransition(label = "esqueleto")
    .animateFloat(0.45f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alfa")

/** Bloque gris C2 que late. */
@Composable
fun BloqueEsqueleto(modifier: Modifier = Modifier, alto: Dp = 14.dp, ancho: Float = 1f, forma: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)) {
    val alfa by pulsoEsqueleto()
    Box(modifier.fillMaxWidth(ancho).height(alto).graphicsLayer { alpha = alfa }.clip(forma).background(GColors.C2S2))
}

/** [n] tarjetas C2 con miniatura y dos lineas: listas de rutas, eventos, clubes, feed. */
@Composable
fun EsqueletoTarjetas(n: Int = 3, modifier: Modifier = Modifier, etiqueta: String = "Cargando", conImagen: Boolean = false) {
    val alfa by pulsoEsqueleto()
    Column(
        modifier.fillMaxWidth().testTag("esqueleto").semantics { contentDescription = etiqueta },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(n) {
            Column(
                Modifier.fillMaxWidth().clip(FormaC2).background(GColors.C2Card).border(1.dp, GColors.C2Linea, FormaC2).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (conImagen) Box(Modifier.fillMaxWidth().height(150.dp).graphicsLayer { alpha = alfa }.clip(RoundedCornerShape(6.dp)).background(GColors.C2S2))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).graphicsLayer { alpha = alfa }.clip(RoundedCornerShape(10.dp)).background(GColors.C2S2))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.fillMaxWidth(0.65f).height(16.dp).graphicsLayer { alpha = alfa }.clip(RoundedCornerShape(4.dp)).background(GColors.C2S2))
                        Box(Modifier.fillMaxWidth(0.4f).height(12.dp).graphicsLayer { alpha = alfa }.clip(RoundedCornerShape(4.dp)).background(GColors.C2S2))
                    }
                }
                Box(Modifier.fillMaxWidth(0.85f).height(12.dp).graphicsLayer { alpha = alfa }.clip(RoundedCornerShape(4.dp)).background(GColors.C2S2))
            }
        }
    }
}

/** Fila de metricas (perfil, estadisticas, reportes) mientras se calculan. */
@Composable
fun EsqueletoMetricas(columnas: Int = 3, modifier: Modifier = Modifier) {
    val alfa by pulsoEsqueleto()
    Row(
        modifier.fillMaxWidth().clip(FormaC2).background(GColors.C2Card).border(1.dp, GColors.C2Linea, FormaC2).padding(16.dp).testTag("esqueleto-metricas"),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(columnas) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(0.7f).height(26.dp).graphicsLayer { alpha = alfa }.clip(RoundedCornerShape(4.dp)).background(GColors.C2S2))
                Box(Modifier.fillMaxWidth(0.5f).height(10.dp).graphicsLayer { alpha = alfa }.clip(RoundedCornerShape(4.dp)).background(GColors.C2S2))
            }
        }
    }
}

/** Pantalla completa de carga (reportes, panel admin): titulo, metricas y tarjetas. */
@Composable
fun EsqueletoPantalla(modifier: Modifier = Modifier, etiqueta: String = "Cargando") {
    Column(modifier.fillMaxWidth().padding(16.dp).semantics { contentDescription = etiqueta }.testTag("esqueleto-pantalla"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BloqueEsqueleto(alto = 30.dp, ancho = 0.55f)
        EsqueletoMetricas()
        EsqueletoTarjetas(3, etiqueta = etiqueta)
    }
}

/** Mapa cargando: fondo del mapa oscuro con calles insinuadas que laten. */
@Composable
fun EsqueletoMapa(modifier: Modifier = Modifier) {
    val alfa by pulsoEsqueleto()
    Box(modifier.fillMaxSize().background(Color(0xFF0A0E16)).testTag("esqueleto-mapa").semantics { contentDescription = "Cargando mapa" }) {
        Column(Modifier.fillMaxSize().padding(24.dp).graphicsLayer { alpha = alfa * 0.6f }, verticalArrangement = Arrangement.SpaceEvenly) {
            listOf(0.9f, 0.6f, 0.8f, 0.5f, 0.75f).forEach { BloqueEsqueleto(alto = 6.dp, ancho = it, forma = RoundedCornerShape(3.dp)) }
        }
        Text("Cargando mapa", style = GType.labelMd, color = GColors.C2Muted, modifier = Modifier.align(Alignment.Center))
    }
}

/** Escena 3D (corredor o avatar) cargando: halo morado que late. */
@Composable
fun EsqueletoEscena3D(modifier: Modifier = Modifier, texto: String = "Cargando avatar 3D") {
    val alfa by pulsoEsqueleto()
    Box(modifier.fillMaxSize().testTag("esqueleto-3d").semantics { contentDescription = texto }, contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(150.dp).graphicsLayer { alpha = alfa }.clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF7B1FA2).copy(alpha = 0.55f), Color.Transparent))),
        )
        Text(texto, style = GType.labelMd, color = GColors.C2Muted, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}
