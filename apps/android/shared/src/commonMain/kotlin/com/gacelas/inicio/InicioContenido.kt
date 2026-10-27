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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
import com.gacelas.actividad.domain.reglas.ResumenSemana
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.actividad.domain.reglas.Racha

/** Saludo segun la hora local (0-23), igual en Android e iOS. */
fun saludoPorHora(hora: Int): String = when (hora) { in 5..11 -> "Buenos días"; in 12..18 -> "Buenas tardes"; else -> "Buenas noches" }

/**
 * Pantalla de inicio compartida (Android/iOS). Recibe los datos ya calculados; la figura 3D
 * (SceneView en Android) llega como slot porque depende de la plataforma.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    totales: com.gacelas.actividad.domain.reglas.ResumenPeriodo = com.gacelas.actividad.domain.reglas.ResumenPeriodo(semana.carreras, semana.km, 0, 0),
    meta: Int = 3,
    rango: String = "",
    reto: com.gacelas.actividad.domain.reglas.RetoMes? = null,
    amigos: List<com.gacelas.comunidad.domain.modelo.Publicacion> = emptyList(),
    inscrito: Boolean = false,
    onComunidad: () -> Unit = {},
    /** Nivel del corredor ("Gacela Intermedia"); null lo oculta. */
    nivel: String? = null,
    /** Ruta sugerida para hoy; null muestra el estado vacio. */
    rutaSugerida: com.gacelas.rutas.domain.modelo.Ruta? = null,
    onRutas: () -> Unit = {},
    /** Linea bajo la ruta (por que es segura) o, sin ruta, el motivo del estado vacio. */
    rutaSugeridaNota: String? = null,
) {
    Pantalla {
        // Encabezado C2: saludo en Anton, pildoras de clima, racha y nivel.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(saludo, style = GType.labelMd, color = GColors.C2Muted)
                Text("Hola, $nombre".uppercase(), style = GType.headlineLg, color = GColors.TextPrimary)
                // Chips .wx del Figma: clima, racha (borde menta) y nivel (degradado morado).
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    ChipC2("Loja", "14 °C", " nublado", acento = Color(0xFF96CCFF))
                    ChipC2("Racha", "${racha.diaria} " + if (racha.diaria == 1) "día" else "días", acento = GColors.C2Menta, borde = GColors.C2Menta, modifier = Modifier.testTag("insignia-racha"))
                    nivel?.let {
                        ChipC2("Nivel", it, acento = Color(0xFFE9D5FF), borde = GColors.C2Violeta,
                            fondo = Brush.linearGradient(listOf(Color(0xFF5E1781), Color(0xFF7B1FA2))), modifier = Modifier.testTag("insignia-nivel"))
                    }
                }
            }
        }
        // Escenario C2 (Figma Movil / Inicio, "stage"): degradado morado a todo el ancho con el corredor 3D,
        // etiquetas y el boton de empezar dentro. Ver EscenarioInicioC2.kt.
        EscenarioInicioC2(
            etiquetaIzq = "Avatar", valorIzq = nivel ?: ("${semana.diasCorridos.size} " + if (semana.diasCorridos.size == 1) "día" else "días"),
            etiquetaDer = if (reto != null) "Este mes" else "Esta semana",
            valorDer = "${Unidades.texto(reto?.km ?: semana.km, m, 1)} ${Unidades.etiquetaDistancia(m)}",
            onCorrer = onCorrer, margenLateral = 16.dp, corredor3D = corredor3D,
        )
        Row(Modifier.fillMaxWidth().clip(com.gacelas.ui.components.FormaC2).background(GColors.C2Card).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricaC2("${semana.diasCorridos.size}", "Días")
            MetricaC2(Unidades.texto(semana.km, m, 1), Unidades.etiquetaDistancia(m), color = GColors.C2Violeta)
            MetricaC2(formatearRitmo(Unidades.ritmo(semana.ritmoSegKm, m)), Unidades.etiquetaRitmo(m), color = GColors.C2Menta)
        }
        SemanaAtletica(totales, semana.diasCorridos, hoy, meta, rango, m)
        reto?.let { TarjetaReto(it, m) }
        RutaSugeridaC2(rutaSugerida, m, onRutas, rutaSugeridaNota)
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
fun RutaSugeridaC2(ruta: com.gacelas.rutas.domain.modelo.Ruta?, m: Boolean, onRutas: () -> Unit, nota: String? = null) {
    Column(
        Modifier.fillMaxWidth().clip(com.gacelas.ui.components.FormaC2).background(GColors.C2Card).clickable(onClick = onRutas).padding(16.dp).testTag("ruta-sugerida"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            EtiquetaC2("Ruta sugerida para hoy")
            EtiquetaC2("Ver rutas")
        }
        if (ruta == null) {
            Text(
                (nota ?: "Aún no hay rutas cerca.") + " Explora o dibuja la tuya.",
                style = GType.bodyMd, color = GColors.C2Muted, modifier = Modifier.testTag("ruta-sugerida-vacia"),
            )
        } else {
            Text(ruta.nombre.uppercase(), style = GType.headlineSm, color = GColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricaC2("${Unidades.texto(ruta.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)}", "Distancia", color = GColors.C2Violeta)
                MetricaC2("+${ruta.elevacionM} m", "Desnivel", color = GColors.C2Cielo)
            }
            nota?.let { Text(it, style = GType.bodySm, color = GColors.C2Menta, modifier = Modifier.testTag("ruta-sugerida-segura")) }
        }
    }
}

/** Chip .wx del Figma C2: 12sp cursiva 800 en mayusculas, inclinado -10deg, valor resaltado en [acento]. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChipC2(
    rotulo: String, valor: String, resto: String = "", acento: Color, modifier: Modifier = Modifier,
    borde: Color = Color(0xFF2C3446), fondo: Brush = Brush.linearGradient(listOf(GColors.C2Card, GColors.C2Card)),
) {
    val estilo = GType.labelMd.copy(fontFamily = com.gacelas.ui.theme.Inter, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
    Text(
        androidx.compose.ui.text.buildAnnotatedString {
            append(rotulo.uppercase() + " ")
            pushStyle(androidx.compose.ui.text.SpanStyle(color = acento)); append(valor.uppercase()); pop()
            append(resto.uppercase())
        },
        style = estilo, color = Color.White,
        modifier = modifier.sesgoX(-10f).background(fondo).border(1.dp, borde).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
