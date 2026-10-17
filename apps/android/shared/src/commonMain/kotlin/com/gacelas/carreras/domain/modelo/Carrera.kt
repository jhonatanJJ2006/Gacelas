package com.gacelas.carreras.domain.modelo

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Punto GPS registrado durante una carrera. */
@Serializable
data class PuntoGps(val lat: Double, val lng: Double, val tiempoMs: Long, val precisionM: Float = 0f, val altitudM: Double? = null)

/** Parcial: automatico por kilometro o manual (boton Vuelta). */
@Serializable
data class Parcial(val numero: Int, val distanciaKm: Double, val segundos: Int, val manual: Boolean = false) {
    /** "Km N" o "Vuelta N" (misma regla en el panel de carrera y el detalle). */
    val etiqueta: String get() = if (manual) "Vuelta $numero" else "Km $numero"
}

/** Value object: distancia en kilometros. */
@JvmInline
value class Distancia(val km: Double) {
    val metros: Double get() = km * 1000.0
    operator fun plus(otra: Distancia) = Distancia(km + otra.km)
    /** Kilometros completos (para los parciales automaticos). */
    val kmCompletos: Int get() = km.toInt()

    companion object {
        val CERO = Distancia(0.0)
        fun metros(m: Double) = Distancia(m / 1000.0)
    }
}

/** Value object: ritmo en segundos por kilometro. */
@JvmInline
value class Ritmo(val segPorKm: Int) {
    companion object {
        /** Distancia minima para calcular un ritmo (10 m). */
        const val DISTANCIA_MIN_KM = 0.01

        /** Ritmo promedio, o null si casi no hay distancia. */
        fun promedio(segundos: Int, distancia: Distancia): Ritmo? =
            if (distancia.km > DISTANCIA_MIN_KM) Ritmo((segundos / distancia.km).toInt()) else null
    }
}

/** Carrera terminada y guardada en el historial (aggregate root del contexto carreras). */
@Serializable
data class Carrera(
    val id: String,
    val titulo: String,
    val nota: String = "",
    val publica: Boolean = true,
    val inicioMs: Long,
    val segundos: Int,
    val distanciaKm: Double,
    val desnivelM: Int = 0,
    val parciales: List<Parcial> = emptyList(),
    val puntos: List<PuntoGps> = emptyList(),
) {
    val distancia: Distancia get() = Distancia(distanciaKm)
    val ritmo: Ritmo? get() = Ritmo.promedio(segundos, distancia)
    /** Ritmo promedio en segundos por km, o null si no hay distancia. */
    val ritmoSegKm: Int? get() = ritmo?.segPorKm
}
