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
) {
    Pantalla {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = GColors.Primary500, modifier = Modifier.size(14.dp))
                    Text(" Loja, Ecuador", style = GType.labelMd, color = GColors.TextSecondary)
                }
                Text("$saludo, $nombre", style = GType.headlineMd, color = GColors.TextPrimary)
            }
            Row(Modifier.clip(CircleShape).background(GColors.SurfaceContainer).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbTwilight, null, tint = GColors.Primary400, modifier = Modifier.size(16.dp))
                Text(" 14°C Fresco", style = GType.labelMd, color = GColors.TextPrimary)
            }
        }
        Tarjeta(fondo = GColors.SurfaceLow, padding = 0.dp) {
            Box(Modifier.fillMaxWidth().height(300.dp).background(Brush.verticalGradient(listOf(GColors.SurfaceLow, GColors.Primary.copy(alpha = 0.12f))))) {
                corredor3D()
                Row(Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(GColors.BgPrimary.copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(GColors.Secondary500))
                    Text("  AUTOPILOT ACTIVO", style = GType.labelMd, color = GColors.Secondary500)
                }
                Row(Modifier.align(Alignment.TopEnd).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    nivel?.let {
                        Text(
                            it, style = GType.labelMd, color = GColors.Secondary500,
                            modifier = Modifier.clip(CircleShape).background(GColors.Secondary.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 4.dp).testTag("insignia-nivel"),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    InsigniaRacha(racha)
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                Metrica("${semana.diasCorridos.size}", "Días", color = GColors.TextPrimary)
                Metrica(Unidades.texto(semana.km, m, 1), Unidades.etiquetaDistancia(m), color = GColors.Primary500)
                Metrica(formatearRitmo(Unidades.ritmo(semana.ritmoSegKm, m)), Unidades.etiquetaRitmo(m), color = GColors.Secondary500)
            }
        }
        TituloSeccion("Acción rápida")
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(GColors.Primary, GColors.Primary700)))
                .clickable(onClick = onCorrer).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Empezar a correr", style = GType.headlineSm, color = GColors.TextPrimary)
                Text("Graba tu siguiente carrera", style = GType.bodyMd, color = GColors.TextPrimary.copy(alpha = 0.85f))
            }
            IconoCirculo(Icons.Filled.PlayArrow, GColors.TextPrimary.copy(alpha = 0.2f), GColors.TextPrimary, 56.dp)
        }
        SemanaAtletica(totales, semana.diasCorridos, hoy, meta, rango, m)
        TituloSeccion("Próximo encuentro", accion = "Ver todos", onAccion = onEventos)
        TarjetaEvento(evento, inscrito) { onEvento(evento.id) }
        reto?.let { TarjetaReto(it, m) }
        AmigosEnRuta(amigos, m, onComunidad)
    }
}
