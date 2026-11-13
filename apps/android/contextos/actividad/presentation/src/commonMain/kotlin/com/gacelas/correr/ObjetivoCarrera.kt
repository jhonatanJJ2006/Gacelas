package com.gacelas.correr

import com.gacelas.actividad.domain.EstadoCarrera
import com.gacelas.shared.fmt
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo

/** Objetivo que se elige en Correr y se sigue en la carrera. Libre = sin meta. */
data class ObjetivoCarrera(val tipo: Tipo, val valor: Int) {
    enum class Tipo(val clave: String, val titulo: String) { Libre("libre", "Libre"), Distancia("distancia", "Distancia"), Tiempo("tiempo", "Tiempo"), Ritmo("ritmo", "Ritmo") }

    val activo: Boolean get() = tipo != Tipo.Libre && valor > 0

    /** Texto corto del valor: "6 km", "30 min", "5:30". */
    fun valorTexto(m: Boolean): String = when (tipo) {
        Tipo.Libre -> "Sin meta"
        Tipo.Distancia -> "${Unidades.texto(valor / 1000.0, m, if (valor % 1000 == 0 && m) 0 else 1)} ${Unidades.etiquetaDistancia(m)}"
        Tipo.Tiempo -> "${valor / 60} min"
        Tipo.Ritmo -> formatearRitmo(Unidades.ritmo(valor, m))
    }

    /** Progreso 0..1 (ritmo: null, no es acumulable). */
    fun progreso(e: EstadoCarrera): Float? = when (tipo) {
        Tipo.Distancia -> (e.distanciaKm * 1000 / valor).toFloat().coerceIn(0f, 1f)
        Tipo.Tiempo -> (e.segundos.toFloat() / valor).coerceIn(0f, 1f)
        else -> null
    }

    /** "Faltan 3,66 km", "Faltan 12:30", "Vas 6:21, 0:51 más lento". */
    fun estado(e: EstadoCarrera, m: Boolean): String = when (tipo) {
        Tipo.Libre -> ""
        Tipo.Distancia -> {
            val falta = valor / 1000.0 - e.distanciaKm
            if (falta <= 0) "Objetivo cumplido" else "Faltan ${Unidades.texto(falta, m)} ${Unidades.etiquetaDistancia(m)}"
        }
        Tipo.Tiempo -> {
            val falta = valor - e.segundos
            if (falta <= 0) "Objetivo cumplido" else "Faltan ${formatearMs(falta)}"
        }
        Tipo.Ritmo -> {
            val actual = e.ritmoPromedioSegKm
            if (actual == null || actual <= 0) "Aún sin ritmo"
            else {
                val d = actual - valor
                when {
                    d > 0 -> "Vas ${formatearRitmo(Unidades.ritmo(actual, m))}, %d:%02d más lento".fmt(d / 60, d % 60)
                    else -> "Vas ${formatearRitmo(Unidades.ritmo(actual, m))}, en ritmo"
                }
            }
        }
    }

    fun cumplido(e: EstadoCarrera): Boolean = when (tipo) {
        Tipo.Distancia -> e.distanciaKm * 1000 >= valor
        Tipo.Tiempo -> e.segundos >= valor
        Tipo.Ritmo -> (e.ritmoPromedioSegKm ?: Int.MAX_VALUE) <= valor && e.distanciaKm >= 1.0
        Tipo.Libre -> false
    }

    companion object {
        fun de(tipo: String, valor: Int) = ObjetivoCarrera(Tipo.entries.firstOrNull { it.clave == tipo } ?: Tipo.Libre, valor)

        /** Valores por defecto al elegir un tipo (los de la maqueta: 6 km, 30 min, 5:30). */
        fun predeterminado(t: Tipo, ritmoMedio: Int?): ObjetivoCarrera = when (t) {
            Tipo.Libre -> ObjetivoCarrera(t, 0)
            Tipo.Distancia -> ObjetivoCarrera(t, 6000)
            Tipo.Tiempo -> ObjetivoCarrera(t, 30 * 60)
            Tipo.Ritmo -> ObjetivoCarrera(t, ritmoMedio?.let { (it - 10).coerceAtLeast(180) } ?: 330)
        }
    }
}
