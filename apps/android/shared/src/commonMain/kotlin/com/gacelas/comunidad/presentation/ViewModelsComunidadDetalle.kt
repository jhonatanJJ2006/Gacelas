package com.gacelas.comunidad.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.carreras.domain.repositorio.CarrerasRepository
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.CorredorEvento
import com.gacelas.comunidad.domain.modelo.EstadoEvento
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.FiltroFeed
import com.gacelas.comunidad.domain.modelo.MiembroClub
import com.gacelas.comunidad.domain.modelo.PostClub
import com.gacelas.comunidad.domain.modelo.Publicacion
import com.gacelas.comunidad.domain.modelo.RolClub
import com.gacelas.comunidad.domain.modelo.SesionEnVivo
import com.gacelas.comunidad.domain.reglas.clasificacion
import com.gacelas.comunidad.domain.reglas.filtrarFeed
import com.gacelas.comunidad.domain.reglas.puedeGestionar
import com.gacelas.comunidad.domain.repositorio.EventosRepository
import com.gacelas.comunidad.domain.repositorio.FuenteClubes
import com.gacelas.comunidad.domain.repositorio.FuenteEnVivo
import com.gacelas.cuenta.domain.modelo.Ajustes
import com.gacelas.cuenta.domain.repositorio.AjustesRepository
import com.gacelas.cuenta.domain.repositorio.AuthRepository
import com.gacelas.data.mock.Actividad
import com.gacelas.data.mock.MockData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Carreras publicas propias primero y luego la actividad de la comunidad (ejemplo). */
fun publicacionesFeed(carreras: List<Carrera>, nombre: String?, actividades: List<Actividad> = MockData.actividades): List<Publicacion> {
    val propias = carreras.filter { it.publica }.map {
        Publicacion("carrera-${it.id}", nombre ?: "Tú", true, 0, "Tu carrera", it.titulo, it.distanciaKm, it.segundos, it.ritmoSegKm, 0, 0, it.puntos)
    }
    val comunidad = actividades.mapIndexed { i, a ->
        Publicacion(
            "act-${a.id}", MockData.usuario(a.usuarioId)?.nombre ?: "", false, i + 1, a.fecha, a.titulo, a.distanciaKm,
            a.duracionSegundos, (a.duracionSegundos / a.distanciaKm).toInt(), a.likes, a.comentarios,
        )
    }
    return propias + comunidad
}

/** Feed de comunidad: filtro, me gusta, comentarios y publicaciones ocultas (guardados en ajustes). */
class FeedViewModel(
    carreras: CarrerasRepository,
    auth: AuthRepository,
    private val ajustesRepo: AjustesRepository,
    actividades: List<Actividad> = MockData.actividades,
) : ViewModel() {
    private val _filtro = MutableStateFlow(FiltroFeed.Todos)
    val filtro: StateFlow<FiltroFeed> = _filtro.asStateFlow()
    val ajustes: StateFlow<Ajustes> = ajustesRepo.ajustes.stateIn(viewModelScope, SharingStarted.Eagerly, Ajustes())
    private val base = combine(carreras.carreras, auth.sesion) { c, p -> publicacionesFeed(c, p?.nombre, actividades) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, publicacionesFeed(emptyList(), null, actividades))

    /** Publicaciones visibles con el filtro elegido. */
    val publicaciones: StateFlow<List<Publicacion>> = combine(base, _filtro, ajustes) { b, f, a -> filtrarFeed(b, f, a.ocultas) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, filtrarFeed(base.value, FiltroFeed.Todos, emptySet()))

    fun filtro(f: FiltroFeed) { _filtro.value = f }
    fun ocultar(id: String) { viewModelScope.launch { ajustesRepo.actualizar { it.copy(ocultas = it.ocultas + id) } } }
    fun alternarLike(id: String) {
        viewModelScope.launch { ajustesRepo.actualizar { a -> a.copy(likes = if (id in a.likes) a.likes - id else a.likes + id) } }
    }
    fun comentar(id: String, texto: String) {
        val t = texto.trim()
        if (t.isEmpty()) return
        viewModelScope.launch { ajustesRepo.actualizar { a -> a.copy(comentarios = a.comentarios + (id to (a.comentarios[id].orEmpty() + t))) } }
    }
}

/** Detalle de un evento: datos, cupo/lista de espera e inscripcion. */
class DetalleEventoViewModel(
    private val id: String,
    private val eventos: EventosRepository,
    private val clubes: FuenteClubes?,
    private val ejemplo: List<Evento> = MockData.eventos,
) : ViewModel() {
    /** Eventos (ejemplo + creados) e inscripciones, como en la lista de eventos. */
    val datos: StateFlow<Pair<List<Evento>, Set<String>>> = combine(eventos.creados, eventos.inscritos) { cr, ins -> (ejemplo + cr) to ins }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ejemplo to emptySet())
    private val remoto = MutableStateFlow<Evento?>(null)
    private val _estado = MutableStateFlow<EstadoEvento?>(null)
    val estado: StateFlow<EstadoEvento?> = _estado.asStateFlow()

    val evento: StateFlow<Evento> = combine(datos, remoto) { (lista, _), r -> elegir(lista, r) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, elegir(ejemplo, null))

    private fun elegir(lista: List<Evento>, r: Evento?) =
        lista.firstOrNull { it.id == id } ?: r ?: ejemplo.firstOrNull { it.id == id } ?: ejemplo.first()

    init {
        clubes?.let { c ->
            viewModelScope.launch { remoto.value = c.evento(id) }
            viewModelScope.launch { eventos.inscritos.collect { _estado.value = c.estadoEvento(id) } }
        }
    }

    /** Inscribe o cancela (o entra/sale de la lista de espera) y avisa el resultado. */
    fun alternarInscripcion(onAviso: (String) -> Unit = {}) {
        val e = evento.value
        val inscrito = e.id in datos.value.second
        viewModelScope.launch {
            eventos.alternarInscripcion(e.id)
            _estado.value = clubes?.estadoEvento(e.id)
            onAviso(
                when {
                    inscrito -> "Inscripción cancelada"
                    _estado.value?.miEstado == "waitlist" -> "Evento lleno: quedaste en la lista de espera"
                    else -> "Inscripción confirmada"
                },
            )
        }
    }
}

/** Datos del detalle de un club. */
data class DetalleClub(
    val club: Club? = null,
    val miembros: List<MiembroClub> = emptyList(),
    val posts: List<PostClub> = emptyList(),
    val eventos: List<Evento> = emptyList(),
    val yoId: String? = null,
) {
    val yo: MiembroClub? get() = miembros.firstOrNull { it.usuarioId == yoId }
    val gestiono: Boolean get() = yo?.activo == true && puedeGestionar(yo?.rol)
}

/** Detalle de club: unirse/salir, solicitudes, roles y publicaciones. */
class DetalleClubViewModel(private val id: String, private val fuente: FuenteClubes?) : ViewModel() {
    private val _estado = MutableStateFlow(DetalleClub())
    val estado: StateFlow<DetalleClub> = _estado.asStateFlow()
    private val _texto = MutableStateFlow("")
    val texto: StateFlow<String> = _texto.asStateFlow()

    init { recargar() }

    fun recargar() {
        val f = fuente ?: return
        viewModelScope.launch {
            _estado.value = DetalleClub(
                club = runCatching { f.detalle(id) }.getOrNull(),
                miembros = runCatching { f.miembros(id) }.getOrDefault(emptyList()),
                posts = f.posts(id),
                eventos = f.eventos(id),
                yoId = f.miId(),
            )
        }
    }

    fun escribir(t: String) { _texto.value = t.take(1000) }

    private fun intentar(ok: String, onAviso: (String) -> Unit, bloque: suspend (FuenteClubes) -> Unit) {
        val f = fuente ?: return
        viewModelScope.launch {
            runCatching { bloque(f) }
                .onSuccess { onAviso(ok); recargar() }
                .onFailure { onAviso(it.message ?: "Algo salió mal") }
        }
    }

    /** Boton principal: unirse (o solicitar) si no soy miembro, salir si lo soy. */
    fun accionPrincipal(onAviso: (String) -> Unit) {
        val e = _estado.value
        val yo = e.yo
        val conAprobacion = e.club?.conAprobacion == true
        when {
            yo == null -> intentar(if (conAprobacion) "Solicitud enviada" else "Te uniste al club", onAviso) { it.unirse(id) }
            yo.activo -> intentar("Saliste del club", onAviso) { it.salir(id) }
        }
    }
    fun aprobar(usuario: String, onAviso: (String) -> Unit) = intentar("Solicitud aprobada", onAviso) { it.aprobar(id, usuario) }
    fun rechazar(usuario: String, onAviso: (String) -> Unit) = intentar("Solicitud rechazada", onAviso) { it.salir(id, usuario) }
    fun alternarAdmin(m: MiembroClub, onAviso: (String) -> Unit) =
        intentar("Rol actualizado", onAviso) { it.cambiarRol(id, m.usuarioId, if (m.rol == RolClub.admin) RolClub.member else RolClub.admin) }
    fun publicar(onAviso: (String) -> Unit) {
        val t = _texto.value
        if (t.isBlank()) return
        intentar("Publicado", onAviso) { it.publicar(id, t); _texto.value = "" }
    }
}

/** Evento en vivo: corredores (cada 5 s), clasificacion y repeticion. */
class EventoEnVivoViewModel(private val id: String, fuente: FuenteClubes?, private val enVivo: FuenteEnVivo?) : ViewModel() {
    private val _corredores = MutableStateFlow(emptyList<CorredorEvento>())
    /** Clasificacion: mas distancia primero y, a igual distancia, menos tiempo. */
    val tabla: StateFlow<List<CorredorEvento>> = _corredores.map { clasificacion(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _repeticion = MutableStateFlow<Float?>(null)
    /** Avance de la repeticion (0..1); null sin repeticion. */
    val repeticion: StateFlow<Float?> = _repeticion.asStateFlow()
    private var animacion: Job? = null

    init {
        fuente?.let { f -> viewModelScope.launch { while (true) { _corredores.value = f.corredores(id); delay(5000) } } }
    }

    fun alternarRepeticion() {
        val r = _repeticion.value
        mover(if (r == null || r >= 1f) 0f else null)
    }

    fun mover(r: Float?) {
        _repeticion.value = r
        if (r == null || r >= 1f || animacion?.isActive == true) return
        animacion = viewModelScope.launch {
            while (true) {
                val v = _repeticion.value ?: break
                if (v >= 1f) break
                delay(80)
                _repeticion.value = _repeticion.value?.let { minOf(1f, it + 0.01f) } ?: break
            }
        }
    }

    /** Asocia la proxima carrera en vivo a este evento. */
    fun correrEnEvento() { enVivo?.eventoId = id }
}

/** Corredores en vivo que puedo ver; se actualiza cada 10 s. null mientras busca. */
class ListaEnVivoViewModel(fuente: FuenteEnVivo?) : ViewModel() {
    private val _sesiones = MutableStateFlow<List<SesionEnVivo>?>(if (fuente == null) emptyList() else null)
    val sesiones: StateFlow<List<SesionEnVivo>?> = _sesiones.asStateFlow()
    init {
        fuente?.let { f -> viewModelScope.launch { while (true) { _sesiones.value = f.listar(); delay(10_000) } } }
    }
}

/** Una carrera en vivo; se consulta cada 5 s hasta que termina. */
class DetalleEnVivoViewModel(id: String, fuente: FuenteEnVivo?) : ViewModel() {
    private val _sesion = MutableStateFlow<SesionEnVivo?>(null)
    val sesion: StateFlow<SesionEnVivo?> = _sesion.asStateFlow()
    private val _cargado = MutableStateFlow(fuente == null)
    val cargado: StateFlow<Boolean> = _cargado.asStateFlow()
    init {
        fuente?.let { f ->
            viewModelScope.launch {
                while (true) {
                    val s = f.leer(id)
                    _sesion.value = s
                    _cargado.value = true
                    if (s?.activa() == false) break
                    delay(5_000)
                }
            }
        }
    }
}
