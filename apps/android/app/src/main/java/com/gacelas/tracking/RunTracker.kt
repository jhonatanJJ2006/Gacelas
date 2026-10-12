package com.gacelas.tracking

import com.gacelas.data.carreras.CarreraGuardada
import com.gacelas.data.carreras.Parcial
import com.gacelas.data.carreras.PuntoGps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class FaseCarrera { Inactiva, Corriendo, Pausada, Terminada }

data class EstadoCarrera(
    val fase: FaseCarrera = FaseCarrera.Inactiva,
    val inicioMs: Long = 0L,
    val segundos: Int = 0,
    val distanciaKm: Double = 0.0,
    val desnivelM: Double = 0.0,
    val puntos: List<PuntoGps> = emptyList(),
    val parciales: List<Parcial> = emptyList(),
    val ultimaPrecisionM: Float? = null,
    val ritmoActualSegKm: Int? = null,
) {
    val activa: Boolean get() = fase == FaseCarrera.Corriendo || fase == FaseCarrera.Pausada
    val pausada: Boolean get() = fase == FaseCarrera.Pausada
    val ritmoPromedioSegKm: Int? get() = if (distanciaKm > 0.01) (segundos / distanciaKm).toInt() else null
    val tiempoTexto: String get() = formatearHms(segundos)
}

/**
 * Lleva la carrera en curso: tiempo (con pausas), distancia a partir de puntos GPS reales,
 * ritmo, desnivel y parciales por km. Recibe los puntos desde LocationService.
 *
 * @param reloj reloj monotono en milisegundos (inyectable para pruebas).
 * @param ahora reloj de pared en milisegundos.
 */
class RunTracker(
    private val reloj: () -> Long = { android.os.SystemClock.elapsedRealtime() },
    private val ahora: () -> Long = { System.currentTimeMillis() },
) {
    companion object {
        /** Puntos con peor precision se descartan. */
        const val PRECISION_MAX_M = 35f
        /** Saltos menores se consideran ruido del GPS. */
        const val MOVIMIENTO_MIN_M = 2.0
        /** Velocidad maxima creible para una persona corriendo (m/s). */
        const val VELOCIDAD_MAX_MS = 12.0
        private const val VENTANA_RITMO_MS = 30_000L
        /** Sin avance valido durante este tiempo se considera que estas quieto. */
        const val PARADO_MS = 6_000L
    }

    private val _estado = MutableStateFlow(EstadoCarrera())
    val estado: StateFlow<EstadoCarrera> = _estado.asStateFlow()

    private var acumuladoMs = 0L
    private var desdeMs: Long? = null
    private var ultimoValido: PuntoGps? = null
    private var segundosUltimoParcial = 0

    fun iniciar() {
        acumuladoMs = 0L
        desdeMs = reloj()
        ultimoValido = null
        segundosUltimoParcial = 0
        _estado.value = EstadoCarrera(fase = FaseCarrera.Corriendo, inicioMs = ahora())
    }

    fun pausar() {
        if (_estado.value.fase != FaseCarrera.Corriendo) return
        actualizarTiempo()
        desdeMs?.let { acumuladoMs += reloj() - it }
        desdeMs = null
        ultimoValido = null
        _estado.update { it.copy(fase = FaseCarrera.Pausada, ritmoActualSegKm = null) }
    }

    fun reanudar() {
        if (_estado.value.fase != FaseCarrera.Pausada) return
        desdeMs = reloj()
        _estado.update { it.copy(fase = FaseCarrera.Corriendo) }
    }

    fun alternarPausa() = if (_estado.value.pausada) reanudar() else pausar()

    private fun milisegundos(): Long = acumuladoMs + (desdeMs?.let { reloj() - it } ?: 0L)

    /** Recalcula el tiempo transcurrido. La UI lo llama cada segundo. */
    fun actualizarTiempo() {
        if (!_estado.value.activa) return
        val seg = (milisegundos() / 1000).toInt()
        _estado.update { it.copy(segundos = seg) }
    }

    /** Registra un punto GPS. Ignora puntos imprecisos, ruido y saltos imposibles. */
    fun registrarPunto(p: PuntoGps) {
        val e = _estado.value
        if (e.fase != FaseCarrera.Corriendo) return
        actualizarTiempo()
        if (p.precisionM > PRECISION_MAX_M) {
            _estado.update { it.copy(ultimaPrecisionM = p.precisionM) }
            return
        }
        val previo = ultimoValido
        if (previo == null) {
            ultimoValido = p
            _estado.update { it.copy(puntos = it.puntos + p, ultimaPrecisionM = p.precisionM) }
            return
        }
        val d = haversineM(previo, p)
        val dt = (p.tiempoMs - previo.tiempoMs).coerceAtLeast(1L) / 1000.0
        if (d < MOVIMIENTO_MIN_M || d / dt > VELOCIDAD_MAX_MS) {
            // Quieto: sin puntos validos el ritmo "actual" se quedaba con el ultimo valor y el mapa 3D
            // creia que seguias corriendo (rumbo del GPS en vez de la brujula). Se borra tras PARADO_MS.
            val parado = p.tiempoMs - previo.tiempoMs >= PARADO_MS
            _estado.update { it.copy(ultimaPrecisionM = p.precisionM, ritmoActualSegKm = if (parado) null else it.ritmoActualSegKm) }
            return
        }
        ultimoValido = p
        val subida = if (previo.altitudM != null && p.altitudM != null) (p.altitudM - previo.altitudM).coerceAtLeast(0.0) else 0.0
        _estado.update { actual ->
            val distancia = actual.distanciaKm + d / 1000.0
            val puntos = actual.puntos + p
            var parciales = actual.parciales
            val kmCompletos = distancia.toInt()
            val automaticos = parciales.count { !it.manual }
            if (kmCompletos > automaticos) {
                parciales = parciales + Parcial(automaticos + 1, 1.0, actual.segundos - segundosUltimoParcial)
                segundosUltimoParcial = actual.segundos
            }
            actual.copy(
                distanciaKm = distancia,
                desnivelM = actual.desnivelM + subida,
                puntos = puntos,
                parciales = parciales,
                ultimaPrecisionM = p.precisionM,
                ritmoActualSegKm = ritmoVentana(puntos),
            )
        }
    }

    private fun ritmoVentana(puntos: List<PuntoGps>): Int? {
        val fin = puntos.lastOrNull() ?: return null
        val ventana = puntos.filter { fin.tiempoMs - it.tiempoMs <= VENTANA_RITMO_MS }
        if (ventana.size < 2) return null
        val metros = ventana.zipWithNext { a, b -> haversineM(a, b) }.sum()
        val seg = (fin.tiempoMs - ventana.first().tiempoMs) / 1000.0
        return if (metros < 5) null else (seg / (metros / 1000.0)).toInt()
    }

    /** Marca una vuelta manual. */
    fun vuelta() {
        val e = _estado.value
        if (!e.activa) return
        actualizarTiempo()
        val manuales = e.parciales.count { it.manual }
        val distanciaPrevia = e.parciales.filter { it.manual }.sumOf { it.distanciaKm }
        _estado.update {
            it.copy(parciales = it.parciales + Parcial(manuales + 1, (it.distanciaKm - distanciaPrevia).coerceAtLeast(0.0), it.segundos - it.parciales.filter { p -> p.manual }.sumOf { p -> p.segundos }, manual = true))
        }
    }

    /** Termina la carrera y deja el estado listo para el resumen. */
    fun finalizar(): EstadoCarrera {
        if (_estado.value.fase == FaseCarrera.Corriendo) pausar()
        _estado.update { it.copy(fase = FaseCarrera.Terminada) }
        return _estado.value
    }

    fun reiniciar() {
        acumuladoMs = 0L
        desdeMs = null
        ultimoValido = null
        _estado.value = EstadoCarrera()
    }

    fun aCarreraGuardada(titulo: String, nota: String, publica: Boolean, id: String = UUID.randomUUID().toString()): CarreraGuardada {
        val e = _estado.value
        return CarreraGuardada(
            id = id,
            titulo = titulo.ifBlank { tituloPorHora(e.inicioMs) },
            nota = nota,
            publica = publica,
            inicioMs = e.inicioMs,
            segundos = e.segundos,
            distanciaKm = e.distanciaKm,
            desnivelM = e.desnivelM.toInt(),
            parciales = e.parciales,
            puntos = e.puntos,
        )
    }
}

fun tituloPorHora(ms: Long): String {
    val h = java.util.Calendar.getInstance().apply { timeInMillis = ms }.get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..11 -> "Carrera matutina"
        in 12..18 -> "Carrera de la tarde"
        else -> "Carrera nocturna"
    }
}
