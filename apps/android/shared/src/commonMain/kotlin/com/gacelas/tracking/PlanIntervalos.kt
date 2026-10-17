package com.gacelas.tracking

import com.gacelas.cuenta.domain.modelo.ConfigIntervalos

enum class FaseIntervalo { Trabajo, Descanso, Terminado }

data class EstadoIntervalo(val fase: FaseIntervalo, val ronda: Int, val restanteSeg: Int)

/** Calcula en que fase del entrenamiento por intervalos se esta segun el tiempo transcurrido. */
class PlanIntervalos(private val config: ConfigIntervalos) {
    val duracionTotalSeg: Int get() = config.rondas * (config.trabajoSeg + config.descansoSeg) - config.descansoSeg

    fun estado(transcurridoSeg: Int): EstadoIntervalo {
        if (config.rondas <= 0 || transcurridoSeg >= duracionTotalSeg) return EstadoIntervalo(FaseIntervalo.Terminado, config.rondas, 0)
        val ciclo = config.trabajoSeg + config.descansoSeg
        val ronda = transcurridoSeg / ciclo + 1
        val enCiclo = transcurridoSeg % ciclo
        return if (enCiclo < config.trabajoSeg) {
            EstadoIntervalo(FaseIntervalo.Trabajo, ronda, config.trabajoSeg - enCiclo)
        } else {
            EstadoIntervalo(FaseIntervalo.Descanso, ronda, ciclo - enCiclo)
        }
    }
}

/** Intervalo entre ticks del metronomo en milisegundos. */
fun intervaloMetronomoMs(bpm: Int): Long = 60_000L / bpm.coerceIn(40, 240)
