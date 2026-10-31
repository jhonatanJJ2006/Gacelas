package com.gacelas.comunidad.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gacelas.comunidad.domain.modelo.BorradorClub
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.reglas.mensajeErrorClub
import com.gacelas.comunidad.domain.reglas.validarClub
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.Notificacion
import com.gacelas.comunidad.domain.modelo.PestanaEventos
import com.gacelas.comunidad.domain.reglas.filtrarEventos
import com.gacelas.comunidad.domain.reglas.filtrarNotificaciones
import com.gacelas.comunidad.domain.reglas.notificacionesVisibles
import com.gacelas.comunidad.domain.repositorio.EventosRepository
import com.gacelas.comunidad.domain.repositorio.ClubesRepository
import com.gacelas.comunidad.domain.repositorio.NotificacionesRepository
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.identidad.domain.repositorio.AjustesRepository
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
    private val fuente: NotificacionesRepository?,
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

/**
 * Lista de clubes: null mientras carga; sin servidor queda vacia. Si la carga falla, [error] trae el
 * mensaje para el usuario y la lista queda vacia hasta [recargar].
 */
class ClubesViewModel(private val fuente: ClubesRepository?) : ViewModel() {
    private val _datos = MutableStateFlow<Pair<List<Club>, Set<String>>?>(null)
    val datos: StateFlow<Pair<List<Club>, Set<String>>?> = _datos.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    /** Clubes con aprobacion a los que pedi entrar en esta sesion (quedan pendientes). */
    private val _solicitados = MutableStateFlow<Set<String>>(emptySet())
    val solicitados: StateFlow<Set<String>> = _solicitados.asStateFlow()
    init { recargar() }
    fun recargar() {
        // Al reintentar tras un error se vuelve a mostrar la carga.
        if (_error.value != null) { _error.value = null; _datos.value = null }
        viewModelScope.launch {
            val f = fuente ?: run { _datos.value = emptyList<Club>() to emptySet(); return@launch }
            runCatching { f.listar() }
                .onSuccess { _datos.value = it }
                .onFailure { _error.value = "No pudimos cargar los grupos y comunidades. Revisa tu conexión."; _datos.value = emptyList<Club>() to emptySet() }
        }
    }

    /** Unirse desde la tarjeta; si el club pide aprobacion queda como solicitud enviada. */
    fun unirse(c: Club, onAviso: (String) -> Unit) {
        val f = fuente ?: return onAviso("Necesitas conexión con el servidor")
        viewModelScope.launch {
            runCatching { f.unirse(c.id) }
                .onSuccess {
                    if (c.conAprobacion) { _solicitados.value = _solicitados.value + c.id; onAviso("Solicitud enviada a ${c.nombre}") }
                    else { onAviso("Te uniste a ${c.nombre}"); recargar() }
                }
                .onFailure { onAviso(it.message?.let(::mensajeErrorClub) ?: "No pudimos unirte. Inténtalo de nuevo.") }
        }
    }

    /** Registra una comunidad o grupo; [onCreado] recibe su id y [onError] el mensaje si falla. */
    fun crear(b: BorradorClub, onCreado: (String) -> Unit, onError: (String) -> Unit) {
        val f = fuente ?: return onError("Necesitas conexión con el servidor")
        validarClub(b, _datos.value?.first.orEmpty())?.let { return onError(it) }
        viewModelScope.launch {
            runCatching { f.crear(b) }
                .onSuccess { onCreado(it); recargar() }
                .onFailure { onError(mensajeErrorClub(it.message)) }
        }
    }

    /** Crea un club; [onCreado] recibe su id y [onError] el mensaje si falla. */
    fun crear(nombre: String, descripcion: String, ubicacion: String, conAprobacion: Boolean, onCreado: (String) -> Unit, onError: (String) -> Unit) {
        val f = fuente ?: return
        viewModelScope.launch {
            runCatching { f.crear(nombre, descripcion, ubicacion, conAprobacion) }
                .onSuccess(onCreado)
                .onFailure { onError(it.message ?: "No se pudo crear") }
        }
    }
}
