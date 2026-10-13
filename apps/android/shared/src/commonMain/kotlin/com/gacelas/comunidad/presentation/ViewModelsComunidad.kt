package com.gacelas.comunidad.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.Notificacion
import com.gacelas.comunidad.domain.modelo.PestanaEventos
import com.gacelas.comunidad.domain.reglas.filtrarEventos
import com.gacelas.comunidad.domain.reglas.filtrarNotificaciones
import com.gacelas.comunidad.domain.reglas.notificacionesVisibles
import com.gacelas.comunidad.domain.repositorio.EventosRepository
import com.gacelas.comunidad.domain.repositorio.FuenteClubes
import com.gacelas.comunidad.domain.repositorio.FuenteNotificaciones
import com.gacelas.cuenta.domain.modelo.Ajustes
import com.gacelas.cuenta.domain.repositorio.AjustesRepository
import com.gacelas.shared.ahoraMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Eventos: pestana elegida, inscripcion y creacion. La lista base la arma la pantalla (servidor o ejemplo). */
class EventosViewModel(private val repo: EventosRepository) : ViewModel() {
    private val _pestana = MutableStateFlow(PestanaEventos.Proximos)
    val pestana: StateFlow<PestanaEventos> = _pestana.asStateFlow()
    fun pestana(p: PestanaEventos) { _pestana.value = p }
    fun visibles(eventos: List<Evento>, inscritos: Set<String>, ahora: Long = ahoraMs()) = filtrarEventos(eventos, _pestana.value, inscritos, ahora)
    fun alternarInscripcion(id: String) { viewModelScope.launch { repo.alternarInscripcion(id) } }
    /** Crea el evento y muestra "Mis eventos". */
    fun crear(evento: Evento) {
        viewModelScope.launch { repo.crear(evento) }
        _pestana.value = PestanaEventos.MisEventos
    }
}

/** Notificaciones visibles segun ajustes y filtro ("Todas", "Sin leer", "En vivo"). */
class NotificacionesViewModel(
    private val fuente: FuenteNotificaciones?,
    private val ajustesRepo: AjustesRepository,
    ejemplo: List<Notificacion>,
) : ViewModel() {
    private val base: StateFlow<List<Notificacion>> = fuente?.lista ?: MutableStateFlow(ejemplo)
    private val _filtro = MutableStateFlow("Todas")
    val filtro: StateFlow<String> = _filtro.asStateFlow()
    private val ajustes = ajustesRepo.ajustes.stateIn(viewModelScope, SharingStarted.Eagerly, Ajustes())

    /** Visibles (para el contador de sin leer) y filtradas (para la lista). */
    val visibles: StateFlow<List<Notificacion>> = combine(base, ajustes) { b, a -> notificacionesVisibles(a, b) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, notificacionesVisibles(Ajustes(), base.value))
    val lista: StateFlow<List<Notificacion>> = combine(visibles, _filtro) { v, f -> filtrarNotificaciones(v, f) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, filtrarNotificaciones(visibles.value, "Todas"))

    init { fuente?.let { f -> viewModelScope.launch { f.refrescar() } } }

    fun filtro(f: String) { _filtro.value = f }
    fun marcarTodas() = marcar(base.value.map { it.id })
    fun marcar(ids: List<String>) {
        viewModelScope.launch {
            fuente?.marcarLeidas(ids)
            ajustesRepo.actualizar { a -> a.copy(notificacionesLeidas = a.notificacionesLeidas + ids) }
        }
    }
}

/** Lista de clubes: null mientras carga; sin servidor queda vacia. */
class ClubesViewModel(private val fuente: FuenteClubes?) : ViewModel() {
    private val _datos = MutableStateFlow<Pair<List<Club>, Set<String>>?>(null)
    val datos: StateFlow<Pair<List<Club>, Set<String>>?> = _datos.asStateFlow()
    init { recargar() }
    fun recargar() {
        viewModelScope.launch { _datos.value = runCatching { fuente?.listar() }.getOrNull() ?: (emptyList<Club>() to emptySet()) }
    }
}
