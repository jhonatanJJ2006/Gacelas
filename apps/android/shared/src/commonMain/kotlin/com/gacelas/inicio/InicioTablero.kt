package com.gacelas.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.actividad.domain.reglas.Racha
import com.gacelas.actividad.domain.reglas.ResumenSemana
import com.gacelas.clima.domain.ClimaActual
import com.gacelas.clima.domain.HoraClima
import com.gacelas.clima.domain.ReglasClima
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.seguridad.domain.ResumenZona
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.components.AnilloC2
import com.gacelas.ui.components.BotonVidrio
import com.gacelas.ui.components.ChipVidrio
import com.gacelas.ui.components.CifraTablero
import com.gacelas.ui.components.GradienteC2
import com.gacelas.ui.components.LineaC2
import com.gacelas.ui.components.RotuloWidget
import com.gacelas.ui.components.Tablero
import com.gacelas.ui.components.TextoAjustableC2
import com.gacelas.ui.components.WidgetC2
import com.gacelas.ui.components.spAcotado
import com.gacelas.ui.components.vidrioC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.gacelas.ui.theme.SpaceGrotesk
import kotlinx.coroutines.launch

/** Decimal con coma (es-EC). */
internal fun dec1(x: Double): String {
    val r = kotlin.math.round(x * 10) / 10.0
    val e = r.toLong(); val d = kotlin.math.abs(kotlin.math.round((r - e) * 10)).toInt()
    return "$e,$d"
}

/**
 * Inicio en la direccion A "Tablero" (rediseno movil v2): el mapa vivo de Loja arriba como
 * protagonista, la ruta segura del dia sobre el mapa y debajo una cuadricula de widgets con datos
 * reales. Cada widget sin fuente se oculta; todo es tocable.
 */
@Composable
fun InicioTablero(
    semana: ResumenSemana,
    hoy: Int,
    m: Boolean,
    racha: Racha,
    /** Mapa vivo (MapLibre en Android, esquema en pruebas). */
    mapa: @Composable BoxScope.() -> Unit,
    onCorrer: () -> Unit,
    onBuscar: () -> Unit,
    onNotificaciones: () -> Unit,
    onProgreso: () -> Unit,
    onMapaVivo: () -> Unit,
    onComunidad: () -> Unit,
    onEvento: (String) -> Unit,
    onRutas: () -> Unit,
    onRuta: (String) -> Unit,
    sinLeer: Int = 0,
    metaKm: Int? = null,
    kmPorDia: List<Double> = emptyList(),
    clima: ClimaActual? = null,
    /** Ritmo medio (seg/km) de las ultimas 8 semanas, null donde no hubo carreras. */
    ritmoSemanas: List<Int?> = emptyList(),
    evento: Evento? = null,
    inscrito: Boolean = false,
    rutaDelDia: Ruta? = null,
    rutaDelDiaNota: String? = null,
    /** Texto de seguridad de la ruta del dia ("seguridad 92" o "riesgo bajo", de seguridad_rutas); null lo oculta. */
    seguridadRuta: String? = null,
    zona: ResumenZona? = null,
    /** Corredores compartiendo posicion ahora (runner_positions); 0 oculta el chip. */
    corriendo: Int = 0,
    avisoPendientes: @Composable () -> Unit = {},
) {
    val scroll = rememberScrollState()
    val alcance = rememberCoroutineScope()
    // Posiciones (en px dentro del scroll) de los widgets a los que llevan los chips del mapa.
    val pos = androidx.compose.runtime.remember { FloatArray(3) }
    fun marcar(i: Int) = Modifier.onGloballyPositioned { pos[i] = it.positionInRoot().y + scroll.value - pos[0] }
    Column(Modifier.fillMaxSize().onGloballyPositioned { pos[0] = it.positionInRoot().y }.verticalScroll(scroll).testTag("inicio-tablero")) {
        // 1. Mapa vivo protagonista con buscador, clima, campana, chips y la ruta del dia.
        Box(Modifier.fillMaxWidth().height(452.dp).testTag("inicio-mapa")) {
            mapa()
            Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Tablero.Noche.copy(alpha = 0.92f), Color.Transparent))))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Tablero.Noche))))
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f).height(48.dp).vidrioC2(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClickLabel = "Buscar rutas", onClick = onBuscar)
                            .testTag("inicio-buscar").padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Search, null, tint = GColors.C2Violeta, modifier = Modifier.size(20.dp))
                        com.gacelas.ui.components.TextoAjustableC2("¿Por dónde corres hoy?", GType.bodyMd, Color.White.copy(alpha = 0.72f), minimo = 0.6f)
                    }
                    if (clima != null) Row(
                        Modifier.height(48.dp).vidrioC2(RoundedCornerShape(16.dp))
                            .clickable(role = Role.Button, onClickLabel = "Ver clima para correr") { alcance.launch { scroll.animateScrollTo(pos[1].toInt()) } }
                            .testTag("inicio-clima").padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(iconoClima(clima.horas.firstOrNull()?.codigoWmo ?: codigoDe(clima.descripcion)), null, tint = GColors.C2Cielo, modifier = Modifier.size(20.dp))
                        Text("${clima.temperatura}°", style = GType.labelLg.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
                    }
                    BotonVidrio(Icons.Filled.Notifications, "Notificaciones", onNotificaciones, tag = "inicio-notificaciones", punto = sinLeer > 0)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (zona != null) ChipVidrio(
                        if (zona.reportes7d == 0) "Zona tranquila" else "${zona.reportes7d} reporte${if (zona.reportes7d == 1) "" else "s"} · 7 días",
                        onClick = { alcance.launch { scroll.animateScrollTo(pos[2].toInt()) } },
                        punto = if (zona.reportes7d == 0) GColors.C2Menta else Tablero.Ambar, tag = "chip-zona",
                    )
                    if (corriendo > 0) ChipVidrio("$corriendo corriendo", onClick = onMapaVivo, icono = Icons.Filled.Groups, tag = "chip-corriendo")
                }
            }
            TarjetaRutaDelDia(rutaDelDia, rutaDelDiaNota, seguridadRuta, m, onRuta, onRutas, Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp, vertical = 8.dp))
        }

        // 2. Cuadricula de widgets.
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 4.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            avisoPendientes()
            WidgetSemana(semana, hoy, m, metaKm, kmPorDia, onProgreso)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WidgetC2("Racha", Modifier.weight(1f).fillMaxHeight(), onClick = onProgreso, tag = "widget-racha", descripcion = "Ver tu progreso") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFF97316), modifier = Modifier.size(26.dp))
                        CifraTablero("${racha.diaria}", if (racha.diaria == 1) "día" else "días", tamano = 40)
                    }
                    Text(
                        when {
                            racha.diaria == 0 -> "Corre hoy para empezar una racha"
                            hoy in semana.diasCorridos -> "Hoy ya sumaste. Mejor: ${racha.mejorDiaria}"
                            else -> "Corre hoy para llegar a ${racha.diaria + 1}"
                        }, style = GType.bodySm.copy(fontSize = 11.5.sp), color = Color.White.copy(alpha = 0.7f),
                    )
                }
                val horas = clima?.horas.orEmpty()
                if (horas.isNotEmpty()) Box(Modifier.weight(1f).fillMaxHeight().then(marcar(1))) {
                    WidgetClima(horas, onCorrer, Modifier.fillMaxSize())
                } else if (evento != null) WidgetEvento(evento, inscrito, onEvento, Modifier.weight(1f).fillMaxHeight())
            }
            val hayRitmo = ritmoSemanas.count { it != null } >= 2
            val eventoAbajo = evento != null && !clima?.horas.isNullOrEmpty()
            if (hayRitmo || eventoAbajo) Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hayRitmo) WidgetRitmo(ritmoSemanas, m, onProgreso, Modifier.weight(1f).fillMaxHeight())
                if (eventoAbajo) WidgetEvento(evento!!, inscrito, onEvento, Modifier.weight(1f).fillMaxHeight())
                else if (!hayRitmo) Spacer(Modifier.weight(1f))
            }
            if (zona != null) Box(marcar(2)) {
                WidgetSeguridad(zona, onMapaVivo)
            }
            if (evento == null) WidgetC2("Comunidad", Modifier.fillMaxWidth(), onClick = onComunidad, tag = "widget-comunidad", descripcion = "Abrir comunidad") {
                Text("Aún no hay salidas próximas. Mira lo que publica tu club.", style = GType.bodySm, color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}


/** Codigo WMO aproximado desde la descripcion guardada (cache antigua sin horas). */
private fun codigoDe(descripcion: String): Int = when {
    "tormenta" in descripcion -> 95
    "lluv" in descripcion || "llovizna" in descripcion || "chubasco" in descripcion -> 61
    "nublado" in descripcion || "niebla" in descripcion -> 3
    else -> 0
}

internal fun iconoClima(codigo: Int): ImageVector = when (ReglasClima.tipo(codigo)) {
    "sol" -> Icons.Filled.WbSunny
    "nube" -> Icons.Filled.Cloud
    "lluvia" -> Icons.Filled.Umbrella
    else -> Icons.Filled.Thunderstorm
}

@Composable
private fun TarjetaRutaDelDia(ruta: Ruta?, nota: String?, seguridad: String?, m: Boolean, onRuta: (String) -> Unit, onRutas: () -> Unit, modifier: Modifier) {
    val forma = RoundedCornerShape(20.dp)
    Row(
        modifier.fillMaxWidth().clip(forma).background(Color(0xD11A0B2E)).border(1.dp, Color(0x4DC084FC), forma)
            .clickable(role = Role.Button, onClickLabel = if (ruta != null) "Abrir ruta del día" else "Ver rutas") { if (ruta != null) onRuta(ruta.id) else onRutas() }
            .testTag("inicio-ruta-dia").padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(GradienteC2), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Map, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            RotuloWidget("Ruta segura del día", color = GColors.C2Violeta)
            if (ruta != null) {
                TextoAjustableC2(ruta.nombre, GType.headlineSm.copy(fontSize = 21.sp, lineHeight = 24.sp), Color.White, minimo = 0.7f)
                val partes = listOfNotNull(
                    "${dec1(Unidades.distancia(ruta.distanciaKm, m))} ${Unidades.etiquetaDistancia(m)}",
                    if (ruta.elevacionM > 0) "+${ruta.elevacionM} m" else null,
                    seguridad,
                )
                Text(partes.joinToString(" · "), style = GType.bodySm.copy(fontSize = 11.5.sp), color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else Text(nota ?: "Buscando una ruta segura para hoy...", style = GType.bodySm, color = Color.White.copy(alpha = 0.75f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White)
                .clickable(role = Role.Button) { if (ruta != null) onRuta(ruta.id) else onRutas() }.testTag("inicio-ruta-ir").padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (ruta != null) "IR" else "VER", style = GType.labelLg.copy(fontWeight = FontWeight.Black), color = Color(0xFF3B0764))
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF3B0764), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun WidgetSemana(semana: ResumenSemana, hoy: Int, m: Boolean, metaKm: Int?, kmPorDia: List<Double>, onProgreso: () -> Unit) {
    val km = Unidades.distancia(semana.km, m)
    WidgetC2("Esta semana", Modifier.fillMaxWidth(), onClick = onProgreso, tag = "widget-semana", descripcion = "Ver tu progreso semanal") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (metaKm != null && metaKm > 0) {
                val p = (semana.km / metaKm).toFloat().coerceIn(0f, 1f)
                AnilloC2(p, tamano = 96.dp, grosor = 11.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(p * 100).toInt()}%", style = androidx.compose.ui.text.TextStyle(fontFamily = SpaceGrotesk, fontSize = 22.sp), color = Color.White)
                        Text("de $metaKm ${Unidades.etiquetaDistancia(m)}", style = GType.bodySm.copy(fontSize = 9.5.sp), color = Color.White.copy(alpha = 0.6f), maxLines = 1)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CifraTablero(dec1(km), Unidades.etiquetaDistancia(m), tamano = 44, tag = "semana-km")
                val falta = metaKm?.let { it - semana.km }
                Text(
                    buildAnnotatedString {
                        when {
                            falta == null -> append("${semana.carreras} carrera${if (semana.carreras == 1) "" else "s"} esta semana")
                            falta <= 0 -> withStyle(SpanStyle(color = GColors.C2Menta, fontWeight = FontWeight.Bold)) { append("Meta semanal cumplida") }
                            else -> { append("Te faltan "); withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append("${dec1(Unidades.distancia(falta, m))} ${Unidades.etiquetaDistancia(m)}") }; append(" para tu meta.") }
                        }
                    }, style = GType.bodySm.copy(fontSize = 11.5.sp), color = Color.White.copy(alpha = 0.7f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, d ->
                        val corrido = i in semana.diasCorridos || (kmPorDia.getOrNull(i) ?: 0.0) > 0
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            val forma = RoundedCornerShape(6.dp)
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1.15f).clip(forma)
                                    .background(if (corrido) GradienteC2 else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))))
                                    .then(if (i == hoy && !corrido) Modifier.border(1.5.dp, GColors.C2Violeta, forma) else Modifier)
                                    .semantics { contentDescription = "$d ${if (corrido) "corrido" else "sin carrera"}" },
                            )
                            Text(d, style = GType.labelMd.copy(fontSize = 9.sp), color = Color.White.copy(alpha = 0.55f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetClima(horas: List<HoraClima>, onCorrer: () -> Unit, modifier: Modifier) {
    val mejor = ReglasClima.mejorHora(horas)
    WidgetC2("Clima para correr", modifier, onClick = onCorrer, tag = "widget-clima", descripcion = "Empezar a correr") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            horas.take(5).forEach { h ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${h.hora}h", style = GType.labelMd.copy(fontSize = spAcotado(10f)), color = Color.White.copy(alpha = 0.6f), maxLines = 1)
                    Icon(iconoClima(h.codigoWmo), null, tint = if (h == mejor) Color(0xFFFBBF24) else GColors.C2Cielo, modifier = Modifier.size(16.dp))
                    Text("${h.temperatura}°", style = GType.labelMd.copy(fontSize = spAcotado(11f), fontWeight = FontWeight.Bold), color = if (h == mejor) Color(0xFFFBBF24) else Color.White, maxLines = 1)
                }
            }
        }
        if (mejor != null) Text("Mejor hora: ${mejor.hora} a ${(mejor.hora + 1) % 24} h", style = GType.labelMd.copy(fontSize = spAcotado(11f), fontWeight = FontWeight.Bold), color = GColors.C2Menta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        else Text("Lluvia en las próximas horas", style = GType.labelMd.copy(fontSize = spAcotado(11f)), color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WidgetRitmo(ritmos: List<Int?>, m: Boolean, onProgreso: () -> Unit, modifier: Modifier) {
    val validos = ritmos.filterNotNull()
    val actual = validos.last()
    val anterior = validos.getOrNull(validos.size - 2)
    WidgetC2("Tu ritmo · 8 semanas", modifier, onClick = onProgreso, tag = "widget-ritmo", descripcion = "Ver tu progreso") {
        Row(verticalAlignment = Alignment.Bottom) {
            CifraTablero(formatearRitmo(Unidades.ritmo(actual, m)), Unidades.etiquetaRitmo(m), Modifier.weight(1f), tamano = 30)
            if (anterior != null && anterior != actual) {
                val dif = actual - anterior
                Text("${if (dif < 0) "-" else "+"}${formatearRitmo(kotlin.math.abs(dif)).removePrefix("0")}", style = GType.labelMd.copy(fontWeight = FontWeight.Bold),
                    color = if (dif < 0) GColors.C2Menta else Tablero.Ambar, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        // Linea invertida: ritmo mas bajo (mas rapido) arriba.
        LineaC2(validos.map { it.toFloat() }, Modifier.height(54.dp), color = GColors.C2Menta, invertida = true)
    }
}

@Composable
private fun WidgetEvento(evento: Evento, inscrito: Boolean, onEvento: (String) -> Unit, modifier: Modifier) {
    WidgetC2("Club · ${evento.fecha.substringBefore(' ').lowercase()} ${evento.hora}".trim(), modifier, onClick = { onEvento(evento.id) }, tag = "widget-evento", descripcion = "Abrir evento") {
        Text(evento.titulo, style = GType.titleMd.copy(fontSize = 17.sp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val n = evento.participantes.coerceAtLeast(0)
            (0 until minOf(3, n)).forEach { i ->
                Box(Modifier.offset(x = (-8 * i).dp).size(26.dp).clip(CircleShape).background(listOf(GColors.C2Morado, Color(0xFF2563EB), Color(0xFF0D9488))[i]).border(2.dp, Tablero.Noche, CircleShape))
            }
            Text(if (n == 0) "Sé el primero" else "$n van", style = GType.bodySm.copy(fontSize = 11.5.sp), color = Color.White.copy(alpha = 0.7f), maxLines = 1,
                modifier = Modifier.offset(x = (-8 * minOf(2, n)).dp))
        }
        Box(
            Modifier.fillMaxWidth().heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp))
                .then(if (inscrito) Modifier.background(GColors.C2Menta.copy(alpha = 0.18f)) else Modifier.border(1.dp, Color(0x80C084FC), RoundedCornerShape(12.dp)))
                .clickable(role = Role.Button) { onEvento(evento.id) }.testTag("inicio-evento-voy"),
            contentAlignment = Alignment.Center,
        ) { Text(if (inscrito) "VAS" else "VOY", style = GType.labelLg.copy(fontWeight = FontWeight.Black), color = if (inscrito) GColors.C2Menta else GColors.C2Violeta) }
    }
}

@Composable
private fun WidgetSeguridad(zona: ResumenZona, onMapaVivo: () -> Unit) {
    WidgetC2("Seguridad en tu zona · ECU 911", Modifier.fillMaxWidth(), onClick = onMapaVivo, tag = "widget-seguridad", descripcion = "Ver incidentes en el mapa") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CifraTablero("${zona.reportes7d}", tamano = 36, color = if (zona.reportes7d == 0) GColors.C2Menta else Tablero.Ambar, tag = "seguridad-reportes")
                Text("7 días", style = GType.labelMd.copy(fontSize = 9.5.sp), color = Color.White.copy(alpha = 0.55f))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when (zona.reportes7d) {
                        0 -> "Sin reportes de corredores en los últimos 7 días."
                        1 -> "1 reporte de corredores en los últimos 7 días."
                        else -> "${zona.reportes7d} reportes de corredores en los últimos 7 días."
                    }, style = GType.bodySm.copy(fontSize = 11.5.sp), color = Color.White.copy(alpha = 0.78f),
                )
                val top = zona.ecu.firstOrNull()
                if (top != null) Text("ECU 911${zona.mesEcu?.let { " (${mesTexto(it)})" } ?: ""}: ${zona.totalEcu} incidentes, sobre todo ${top.first.lowercase()}.",
                    style = GType.bodySm.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

private fun mesTexto(aaaaMm: String): String {
    val meses = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    val mes = aaaaMm.substringAfter('-').toIntOrNull() ?: return aaaaMm
    return meses.getOrNull(mes - 1) ?: aaaaMm
}
