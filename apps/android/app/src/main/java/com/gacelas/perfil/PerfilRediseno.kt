package com.gacelas.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.components.BarrasC2
import com.gacelas.ui.components.CeldaCifraC2
import com.gacelas.ui.components.DatoCifra
import com.gacelas.ui.components.SelectorSegmentadoC2
import com.gacelas.ui.components.TarjetaSeccionC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

private const val SEMANA_MS = 7 * 86_400_000L

/** Totales de un periodo [desde, hasta). */
internal data class Periodo(val carreras: Int, val km: Double, val segundos: Int, val desnivel: Int) {
    val ritmo: Int? get() = if (km > 0.05) (segundos / km).toInt() else null
}

internal fun periodo(carreras: List<Carrera>, desde: Long, hasta: Long): Periodo {
    val l = carreras.filter { it.inicioMs in desde until hasta }
    return Periodo(l.size, l.sumOf { it.distanciaKm }, l.sumOf { it.segundos }, l.sumOf { it.desnivelM })
}

/** Km por semana de las ultimas [n] semanas (la ultima es la actual). */
internal fun volumenSemanal(carreras: List<Carrera>, ahora: Long, n: Int): List<Double> {
    val inicio = com.gacelas.actividad.domain.reglas.Estadisticas.inicioSemana(ahora)
    return (n - 1 downTo 0).map { k ->
        val desde = inicio - k * SEMANA_MS
        carreras.filter { it.inicioMs in desde until desde + SEMANA_MS }.sumOf { it.distanciaKm }
    }
}

/**
 * Perfil (rediseno movil): selector de periodo, 4 indicadores con su tendencia frente al periodo
 * anterior y volumen semanal de 12 semanas. Todo sale del historial real.
 */
@Composable
fun ResumenPeriodoPerfil(carreras: List<Carrera>, m: Boolean, ahora: Long = System.currentTimeMillis()) {
    var sel by rememberSaveable { mutableIntStateOf(0) }
    val semanas = listOf(4, 12, 52, null)
    val (actual, previo) = remember(carreras, sel) {
        val n = semanas[sel]
        if (n == null) periodo(carreras, 0, Long.MAX_VALUE) to null
        else periodo(carreras, ahora - n * SEMANA_MS, ahora + 86_400_000L) to periodo(carreras, ahora - 2 * n * SEMANA_MS, ahora - n * SEMANA_MS)
    }
    val volumen = remember(carreras) { volumenSemanal(carreras, ahora, 12) }
    Column(Modifier.fillMaxWidth().testTag("perfil-periodo"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SelectorSegmentadoC2(listOf("4 sem", "12 sem", "Año", "Todo"), sel, { sel = it }, tag = "periodo-perfil")
        fun tendencia(a: Double, b: Double?, menorEsMejor: Boolean = false): String {
            if (b == null || b <= 0.0) return ""
            val d = ((a - b) / b * 100).toInt()
            if (d == 0) return " ="
            val mejor = if (menorEsMejor) d < 0 else d > 0
            return (if (mejor) " +" else " -") + "${kotlin.math.abs(d)}%"
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CeldaCifraC2(DatoCifra(Unidades.texto(actual.km, m, 1), Unidades.etiquetaDistancia(m) + tendencia(actual.km, previo?.km), "Distancia", GColors.C2Violeta), Modifier.weight(1f), 17)
            CeldaCifraC2(DatoCifra("${actual.carreras}", tendencia(actual.carreras.toDouble(), previo?.carreras?.toDouble()).trim(), "Carreras"), Modifier.weight(1f), 17)
            CeldaCifraC2(DatoCifra(formatearRitmo(Unidades.ritmo(actual.ritmo, m)), tendencia(actual.ritmo?.toDouble() ?: 0.0, previo?.ritmo?.toDouble(), menorEsMejor = true).trim(), "Ritmo", GColors.C2Menta), Modifier.weight(1f), 17)
            CeldaCifraC2(DatoCifra("+${actual.desnivel}", "m", "Desnivel", GColors.C2Cielo), Modifier.weight(1f), 17)
        }
        if (previo != null) Text("Tendencia frente a las ${semanas[sel]} semanas anteriores", style = GType.bodySm, color = GColors.C2Muted)
        TarjetaSeccionC2("Volumen semanal · 12 semanas", tag = "volumen-semanal") {
            BarrasC2(volumen.map { it.toFloat() }, resaltada = volumen.lastIndex, alto = 70.dp)
            Text(
                "Esta semana ${Unidades.texto(volumen.last(), m, 1)} ${Unidades.etiquetaDistancia(m)} · media ${Unidades.texto(volumen.average(), m, 1)} ${Unidades.etiquetaDistancia(m)}",
                style = GType.bodySm, color = GColors.C2Muted,
            )
        }
    }
}
