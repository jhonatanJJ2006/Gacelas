package com.gacelas.inicio

import com.gacelas.ui.components.InsigniaRacha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.components.AvatarCirculo
import com.gacelas.ui.components.IconoCirculo
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.MetricaC2
import com.gacelas.ui.components.EtiquetaC2
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.components.TituloSeccion
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

import androidx.compose.foundation.layout.BoxScope
import com.gacelas.carreras.domain.reglas.ResumenSemana
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.carreras.domain.reglas.Racha

/** Saludo segun la hora local (0-23), igual en Android e iOS. */
fun saludoPorHora(hora: Int): String = when (hora) { in 5..11 -> "Buenos días"; in 12..18 -> "Buenas tardes"; else -> "Buenas noches" }

/**
 * Pantalla de inicio compartida (Android/iOS). Recibe los datos ya calculados; la figura 3D
 * (SceneView en Android) llega como slot porque depende de la plataforma.
 */
@Composable
fun InicioContenido(
    saludo: String,
    nombre: String,
    semana: ResumenSemana,
    hoy: Int,
    m: Boolean,
    racha: Racha,
    evento: Evento,
    onCorrer: () -> Unit,
    onEventos: () -> Unit,
    onEvento: (String) -> Unit,
    corredor3D: @Composable BoxScope.() -> Unit,
    totales: com.gacelas.carreras.domain.reglas.ResumenPeriodo = com.gacelas.carreras.domain.reglas.ResumenPeriodo(semana.carreras, semana.km, 0, 0),
    meta: Int = 3,
    rango: String = "",
    reto: com.gacelas.carreras.domain.reglas.RetoMes? = null,
    amigos: List<com.gacelas.comunidad.domain.modelo.Publicacion> = emptyList(),
    inscrito: Boolean = false,
    onComunidad: () -> Unit = {},
    /** Nivel del corredor ("Gacela Intermedia"); null lo oculta. */
    nivel: String? = null,
    /** Ruta sugerida para hoy; null muestra el estado vacio. */
    rutaSugerida: com.gacelas.rutas.domain.modelo.Ruta? = null,
    onRutas: () -> Unit = {},
) {
    Pantalla {
        // Encabezado C2: saludo en Anton, pildoras de clima, racha y nivel.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(saludo, style = GType.labelMd, color = GColors.C2Muted)
                Text("Hola, $nombre".uppercase(), style = GType.headlineLg, color = GColors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PildoraC2("Loja 14° Fresco", GColors.C2Bg, GColors.TextPrimary, borde = true)
                    InsigniaRacha(racha)
                }
                nivel?.let {
                    PildoraC2(it, GColors.C2Violeta, GColors.C2Bg, modifier = Modifier.testTag("insignia-nivel"))
                }
            }
        }
        // Heroe con el corredor 3D destacado.
        Box(
            Modifier.fillMaxWidth().height(320.dp).clip(com.gacelas.ui.components.FormaC2)
                .background(Brush.verticalGradient(listOf(GColors.C2Bg, GColors.Primary.copy(alpha = 0.25f)))),
        ) {
            corredor3D()
            Column(Modifier.align(Alignment.TopStart).padding(16.dp)) {
                EtiquetaC2("Avatar")
                Text("${semana.diasCorridos.size} " + if (semana.diasCorridos.size == 1) "día" else "días", style = GType.headlineSm, color = GColors.TextPrimary)
            }
            Column(Modifier.align(Alignment.TopEnd).padding(16.dp), horizontalAlignment = Alignment.End) {
                EtiquetaC2("Esta semana")
                Text("${Unidades.texto(semana.km, m, 1)} ${Unidades.etiquetaDistancia(m)}", style = GType.headlineSm, color = GColors.C2Violeta)
            }
        }
        com.gacelas.ui.components.BotonPrimario("Empezar a correr", onCorrer, icono = Icons.Filled.PlayArrow)
        Row(Modifier.fillMaxWidth().clip(com.gacelas.ui.components.FormaC2).background(GColors.C2Card).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricaC2("${semana.diasCorridos.size}", "Días")
            MetricaC2(Unidades.texto(semana.km, m, 1), Unidades.etiquetaDistancia(m), color = GColors.C2Violeta)
            MetricaC2(formatearRitmo(Unidades.ritmo(semana.ritmoSegKm, m)), Unidades.etiquetaRitmo(m), color = GColors.C2Menta)
        }
        SemanaAtletica(totales, semana.diasCorridos, hoy, meta, rango, m)
        reto?.let { TarjetaReto(it, m) }
        RutaSugeridaC2(rutaSugerida, m, onRutas)
        AmigosEnRuta(amigos, m, onComunidad)
        TituloSeccion("Próximo evento", accion = "Ver todos", onAccion = onEventos)
        TarjetaEvento(evento, inscrito) { onEvento(evento.id) }
    }
}

/** Pildora C2 inclinada (rectangular, mayusculas, cursiva). */
@Composable
fun PildoraC2(texto: String, fondo: androidx.compose.ui.graphics.Color, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, borde: Boolean = false) {
    var m = modifier.clip(RoundedCornerShape(2.dp)).background(fondo)
    if (borde) m = m.border(1.dp, GColors.C2Linea, RoundedCornerShape(2.dp))
    Text(
        texto.uppercase(),
        style = GType.labelMd.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = androidx.compose.ui.text.font.FontWeight.Black),
        color = color, modifier = m.padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Ruta sugerida para hoy; si no hay rutas muestra un estado vacio con acceso a Rutas. */
@Composable
fun RutaSugeridaC2(ruta: com.gacelas.rutas.domain.modelo.Ruta?, m: Boolean, onRutas: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(com.gacelas.ui.components.FormaC2).background(GColors.C2Card).clickable(onClick = onRutas).padding(16.dp).testTag("ruta-sugerida"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            EtiquetaC2("Ruta sugerida para hoy")
            EtiquetaC2("Ver rutas")
        }
        if (ruta == null) {
            Text("Aún no hay rutas cerca. Explora o dibuja la tuya.", style = GType.bodyMd, color = GColors.C2Muted)
        } else {
            Text(ruta.nombre.uppercase(), style = GType.headlineSm, color = GColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricaC2("${Unidades.texto(ruta.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)}", "Distancia", color = GColors.C2Violeta)
                MetricaC2("+${ruta.elevacionM} m", "Desnivel", color = GColors.C2Cielo)
            }
        }
    }
}
