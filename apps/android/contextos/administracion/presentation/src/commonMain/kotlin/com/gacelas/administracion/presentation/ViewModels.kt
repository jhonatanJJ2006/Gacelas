package com.gacelas.administracion.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gacelas.administracion.application.ConsultarAccesoAdmin
import com.gacelas.administracion.application.GuardarEvento
import com.gacelas.administracion.application.GuardarVersionMinima
import com.gacelas.administracion.application.ModerarReporte
import com.gacelas.administracion.domain.AccesoAdmin
import com.gacelas.administracion.domain.AccionModeracion
import com.gacelas.administracion.domain.AdminRepositorio
import com.gacelas.administracion.domain.AjusteApp
import com.gacelas.administracion.domain.ClubAdmin
import com.gacelas.administracion.domain.EstadisticasAdmin
import com.gacelas.administracion.domain.EstadoReporte
import com.gacelas.administracion.domain.EventoAdmin
import com.gacelas.administracion.domain.MetricasAdmin
import com.gacelas.administracion.domain.ReporteAdmin
import com.gacelas.administracion.domain.RolAdmin
import com.gacelas.administracion.domain.RutaAdmin
import com.gacelas.administracion.domain.UsuarioAdmin
import com.gacelas.administracion.domain.buscar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.JsonPrimitive

/** Estado de una carga remota: cargando, error con reintento o listo. */
sealed interface Carga<out T> {
    data object Cargando : Carga<Nothing>
    data class Error(val mensaje: String) : Carga<Nothing>
    data class Listo<T>(val datos: T) : Carga<T>
}

private fun Throwable.texto(): String = message?.takeIf { it.isNotBlank() } ?: "Algo salió mal. Intenta de nuevo."

/**
 * Base de las secciones: carga al crearse, permite reintentar y ejecuta acciones que recargan al
 * terminar. Los errores de acciones van a [mensaje] (toast) y no borran la lista.
 */
abstract class SeccionViewModel<T> : ViewModel() {
    private val _estado = MutableStateFlow<Carga<T>>(Carga.Cargando)
    val estado: StateFlow<Carga<T>> = _estado.asStateFlow()
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()
    private val _ocupado = MutableStateFlow(false)
    val ocupado: StateFlow<Boolean> = _ocupado.asStateFlow()

    protected abstract suspend fun cargar(): T

    fun recargar() {
        _estado.value = Carga.Cargando
        viewModelScope.launch { _estado.value = intentar { Carga.Listo(cargar()) } }
    }

    fun limpiarMensaje() { _mensaje.value = null }

    protected fun ejecutar(exito: String, accion: suspend () -> Unit) {
        if (_ocupado.value) return
        _ocupado.value = true
        viewModelScope.launch {
            try {
                accion()
                _mensaje.value = exito
                _estado.value = Carga.Listo(cargar())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensaje.value = e.texto()
            } finally {
                _ocupado.value = false
            }
        }
    }

    private suspend fun intentar(bloque: suspend () -> Carga<T>): Carga<T> = try {
        bloque()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Carga.Error(e.texto())
    }
}

/**
 * Rol de la sesion para mostrar la entrada "Administración" y proteger el panel. null = comprobando.
 * Se pregunta al servidor en cada creacion (al entrar a Ajustes o al panel), sin cache en disco.
 */
class AccesoAdminViewModel(private val consultar: ConsultarAccesoAdmin) : ViewModel() {
    private val _acceso = MutableStateFlow<AccesoAdmin?>(null)
    val acceso: StateFlow<AccesoAdmin?> = _acceso.asStateFlow()

    init { recargar() }

    fun recargar() {
        viewModelScope.launch { _acceso.value = consultar() }
    }
}

class DashboardAdminViewModel(private val repo: AdminRepositorio) : SeccionViewModel<MetricasAdmin>() {
    init { recargar() }
    override suspend fun cargar() = repo.metricas()
}

class UsuariosAdminViewModel(private val repo: AdminRepositorio) : SeccionViewModel<List<UsuarioAdmin>>() {
    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    init { recargar() }
    override suspend fun cargar() = repo.usuarios()

    fun buscar(texto: String) { _busqueda.value = texto }
    fun filtrar(todos: List<UsuarioAdmin>): List<UsuarioAdmin> = todos.buscar(_busqueda.value)

    fun suspender(u: UsuarioAdmin) = ejecutar(if (u.suspendido) "Cuenta reactivada" else "Cuenta suspendida") {
        repo.suspender(u.id, !u.suspendido)
    }

    fun cambiarRol(u: UsuarioAdmin, rol: RolAdmin?) = ejecutar("Rol actualizado") { repo.cambiarRol(u.id, rol) }
}

class ModeracionAdminViewModel(
    private val repo: AdminRepositorio,
    private val moderarReporte: ModerarReporte = ModerarReporte(repo),
) : SeccionViewModel<List<ReporteAdmin>>() {
    private val _filtroEstado = MutableStateFlow(EstadoReporte.Pendiente)
    val filtroEstado: StateFlow<EstadoReporte> = _filtroEstado.asStateFlow()
    private val _filtroTipo = MutableStateFlow<String?>(null)
    val filtroTipo: StateFlow<String?> = _filtroTipo.asStateFlow()
    private val _notas = MutableStateFlow<Map<String, String>>(emptyMap())
    val notas: StateFlow<Map<String, String>> = _notas.asStateFlow()

    init { recargar() }
    override suspend fun cargar() = repo.reportes(_filtroEstado.value, _filtroTipo.value)

    fun elegirEstado(e: EstadoReporte) { _filtroEstado.value = e; recargar() }
    fun elegirTipo(t: String?) { _filtroTipo.value = t; recargar() }
    fun escribirNota(id: String, nota: String) { _notas.update { it + (id to nota) } }

    fun moderar(r: ReporteAdmin, accion: AccionModeracion) = ejecutar("Caso actualizado") {
        moderarReporte(r, accion, _notas.value[r.id].orEmpty())
        _notas.update { it - r.id }
    }
}

class EventosAdminViewModel(
    private val repo: AdminRepositorio,
    private val guardarEvento: GuardarEvento = GuardarEvento(repo),
) : SeccionViewModel<List<EventoAdmin>>() {
    init { recargar() }
    override suspend fun cargar() = repo.eventos()

    fun publicar(e: EventoAdmin) = ejecutar(if (e.publicado) "Evento oculto" else "Evento publicado") {
        repo.guardarEvento(e.copy(publicado = !e.publicado))
    }

    fun guardar(e: EventoAdmin) = ejecutar("Evento guardado") { guardarEvento(e) }
}

data class RutasYClubes(val rutas: List<RutaAdmin>, val clubes: List<ClubAdmin>)

class RutasClubesAdminViewModel(private val repo: AdminRepositorio) : SeccionViewModel<RutasYClubes>() {
    init { recargar() }
    override suspend fun cargar() = RutasYClubes(repo.rutas(), repo.clubes())
    fun borrarRuta(r: RutaAdmin) = ejecutar("Ruta eliminada") { repo.borrarRuta(r.id) }
    fun borrarClub(c: ClubAdmin) = ejecutar("Club eliminado") { repo.borrarClub(c.id) }
}

class AjustesAdminViewModel(
    private val repo: AdminRepositorio,
    private val guardarVersion: GuardarVersionMinima = GuardarVersionMinima(repo),
) : SeccionViewModel<List<AjusteApp>>() {
    init { recargar() }
    override suspend fun cargar() = repo.ajustes()
    fun alternar(a: AjusteApp, valor: Boolean) = ejecutar("Ajuste guardado") { repo.guardarAjuste(a.clave, JsonPrimitive(valor)) }
    fun guardarVersionMinima(v: String) = ejecutar("Versión mínima guardada") { guardarVersion(v) }
}

/** Rangos de estadisticas que ofrece el panel (en dias hacia atras desde hoy). */
enum class RangoEstadisticas(val dias: Int, val etiqueta: String) { Semana(7, "7 días"), Mes(30, "30 días"), Trimestre(90, "90 días") }

class EstadisticasAdminViewModel(
    private val repo: AdminRepositorio,
    private val hoy: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : SeccionViewModel<EstadisticasAdmin>() {
    private val _rango = MutableStateFlow(RangoEstadisticas.Mes)
    val rango: StateFlow<RangoEstadisticas> = _rango.asStateFlow()

    init { recargar() }

    override suspend fun cargar(): EstadisticasAdmin {
        val hasta = hoy()
        val desde = hasta.minus(DatePeriod(days = _rango.value.dias))
        return repo.estadisticas(desde.toString(), hasta.toString())
    }

    fun elegir(r: RangoEstadisticas) { _rango.value = r; recargar() }
}
