package com.gacelas.correr

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Estado de una carrera simulada. TODO: alimentar con LocationService y GPS real. */
data class EstadoCarrera(val segundos: Int = 0, val distanciaKm: Double = 0.0, val pausada: Boolean = false) {
    /** Ritmo en minutos por km (entero), 0 si aun no hay distancia. */
    val ritmoMin: Int get() = if (distanciaKm > 0) (segundos / distanciaKm / 60).toInt() else 0
    val tiempoTexto: String get() = formatearHms(segundos)
    val distanciaTexto: String get() = "%.2f".format(java.util.Locale.US, distanciaKm)
}

fun formatearHms(segundos: Int): String = "%02d:%02d:%02d".format(segundos / 3600, (segundos % 3600) / 60, segundos % 60)

fun formatearMs(segundos: Int): String = "%d:%02d".format(segundos / 60, segundos % 60)

class CarreraViewModel(private val avancePorSegundoKm: () -> Double = { Math.random() * 0.01 }) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoCarrera())
    val estado: StateFlow<EstadoCarrera> = _estado.asStateFlow()

    /** Avanza un segundo si la carrera no esta pausada. */
    fun tick() {
        _estado.update { if (it.pausada) it else it.copy(segundos = it.segundos + 1, distanciaKm = it.distanciaKm + avancePorSegundoKm()) }
    }

    fun alternarPausa() = _estado.update { it.copy(pausada = !it.pausada) }

    fun reiniciar() = _estado.update { EstadoCarrera() }
}
