package com.gacelas.actividad.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gacelas.actividad.domain.EstadoCarrera
import com.gacelas.actividad.domain.SesionCarrera
import com.gacelas.actividad.application.AlternarPausa
import com.gacelas.actividad.application.EliminarCarrera
import com.gacelas.actividad.application.MarcarVuelta
import com.gacelas.actividad.application.ObtenerCarrera
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.repositorio.CarrerasRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado del detalle de carrera. [carrera] null mientras carga o si ya no existe. */
data class EstadoDetalleCarrera(val carrera: Carrera? = null, val cargando: Boolean = true)

/** Detalle de una carrera guardada: la pantalla solo renderiza [estado] y llama a [eliminar]. */
class DetalleCarreraViewModel(
    id: String,
    obtener: ObtenerCarrera,
    private val eliminarCarrera: EliminarCarrera,
) : ViewModel() {
    constructor(id: String, repo: CarrerasRepository) : this(id, ObtenerCarrera(repo), EliminarCarrera(repo))

    private val id = id
    val estado: StateFlow<EstadoDetalleCarrera> = obtener(id)
        .map { EstadoDetalleCarrera(it, cargando = false) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoDetalleCarrera())

    fun eliminar() {
        viewModelScope.launch { eliminarCarrera(id) }
    }
}

/**
 * Carrera en curso: expone el estado de [SesionCarrera] y las acciones del diseño (pausa, vuelta).
 * Lo propio de cada plataforma (servicio GPS en Android, CoreLocation en iOS, terminar/descartar) queda fuera.
 */
class CarreraEnCursoViewModel(private val sesion: SesionCarrera) : ViewModel() {
    private val alternar = AlternarPausa(sesion)
    private val marcarVuelta = MarcarVuelta(sesion)
    val estado: StateFlow<EstadoCarrera> = sesion.estado

    fun alternarPausa() = alternar()
    fun vuelta() = marcarVuelta()
    /** El reloj de la pantalla lo llama cada segundo. */
    fun tic() = sesion.actualizarTiempo()
}

/** Historial de carreras (perfil). */
class HistorialCarrerasViewModel(repo: CarrerasRepository) : ViewModel() {
    val carreras: StateFlow<List<Carrera>> = repo.carreras.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}

/** Tiempo de la carrera como "hh:mm:ss" (formato de presentacion, fuera del dominio). */
val com.gacelas.actividad.domain.EstadoCarrera.tiempoTexto: String get() = com.gacelas.tracking.formatearHms(segundos)
