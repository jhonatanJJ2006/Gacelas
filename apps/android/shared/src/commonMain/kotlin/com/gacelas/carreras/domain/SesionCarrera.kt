package com.gacelas.carreras.domain

import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.carreras.domain.modelo.Distancia
import com.gacelas.carreras.domain.modelo.Parcial
import com.gacelas.carreras.domain.modelo.PuntoGps
import com.gacelas.carreras.domain.modelo.Ritmo
import com.gacelas.carreras.domain.reglas.Geo
import com.gacelas.carreras.domain.reglas.ReglasGps
import com.gacelas.carreras.domain.reglas.ReglasParciales
import com.gacelas.carreras.domain.reglas.ReglasRitmo
import com.gacelas.carreras.domain.reglas.tituloPorHora
import com.gacelas.shared.ahoraMs
import com.gacelas.shared.nuevoUuid
import com.gacelas.tracking.formatearHms
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    val ritmoPromedioSegKm: Int? get() = Ritmo.promedio(segundos, Distancia(distanciaKm))?.segPorKm
    val tiempoTexto: String get() = formatearHms(segundos)
}

/**
 * Lleva la carrera en curso: tiempo (con pausas), distancia a partir de puntos GPS reales,
 * ritmo, desnivel y parciales por km. Recibe los puntos desde LocationService.
 *
 * @param reloj reloj monotono en milisegundos (inyectable para pruebas).
 * @param ahora reloj de pared en milisegundos.
 */
class SesionCarrera(
    private val reloj: () -> Long = { com.gacelas.shared.relojMonotonoMs() },
    private val ahora: () -> Long = { ahoraMs() },
) {
    companion object {
        const val PRECISION_MAX_M = ReglasGps.PRECISION_MAX_M
        const val MOVIMIENTO_MIN_M = ReglasGps.MOVIMIENTO_MIN_M
        const val VELOCIDAD_MAX_MS = ReglasGps.VELOCIDAD_MAX_MS
        const val PARADO_MS = ReglasGps.PARADO_MS
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
        if (ReglasGps.impreciso(p)) {
            _estado.update { it.copy(ultimaPrecisionM = p.precisionM) }
            return
        }
        val previo = ultimoValido
        if (previo == null) {
            ultimoValido = p
            _estado.update { it.copy(puntos = it.puntos + p, ultimaPrecisionM = p.precisionM) }
            return
        }
        val d = Geo.haversineM(previo, p)
        if (ReglasGps.descartar(previo, p, d)) {
            // Quieto: sin puntos validos el ritmo "actual" se quedaba con el ultimo valor y el mapa 3D
            // creia que seguias corriendo (rumbo del GPS en vez de la brujula). Se borra tras PARADO_MS.
            val parado = ReglasGps.parado(previo, p)
            _estado.update { it.copy(ultimaPrecisionM = p.precisionM, ritmoActualSegKm = if (parado) null else it.ritmoActualSegKm) }
            return
        }
        ultimoValido = p
        val subida = ReglasGps.subida(previo, p)
        _estado.update { actual ->
            val distancia = actual.distanciaKm + d / 1000.0
            val puntos = actual.puntos + p
            var parciales = actual.parciales
            ReglasParciales.automatico(parciales, distancia, actual.segundos, segundosUltimoParcial)?.let {
                parciales = parciales + it
                segundosUltimoParcial = actual.segundos
            }
            actual.copy(
                distanciaKm = distancia,
                desnivelM = actual.desnivelM + subida,
                puntos = puntos,
                parciales = parciales,
                ultimaPrecisionM = p.precisionM,
                ritmoActualSegKm = ReglasRitmo.ventana(puntos),
            )
        }
    }

    /** Marca una vuelta manual. */
    fun vuelta() {
        val e = _estado.value
        if (!e.activa) return
        actualizarTiempo()
        _estado.update { it.copy(parciales = it.parciales + ReglasParciales.vuelta(it.parciales, it.distanciaKm, it.segundos)) }
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

    fun aCarrera(titulo: String, nota: String, publica: Boolean, id: String = nuevoUuid()): Carrera {
        val e = _estado.value
        return Carrera(
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

    /** Alias historico de [aCarrera]. */
    fun aCarreraGuardada(titulo: String, nota: String, publica: Boolean, id: String = nuevoUuid()): Carrera = aCarrera(titulo, nota, publica, id)
}
