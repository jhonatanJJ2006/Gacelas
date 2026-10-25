package com.gacelas.rutas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gacelas.rutas.domain.casosuso.ConsultaRutas
import com.gacelas.rutas.domain.casosuso.rutasVisibles
import com.gacelas.rutas.domain.modelo.FiltrosRuta
import com.gacelas.rutas.domain.modelo.PestanaRutas
import com.gacelas.rutas.domain.modelo.ResenaRuta
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.rutas.domain.modelo.RutasLoja
import com.gacelas.rutas.domain.repositorio.ExplorarRutas
import com.gacelas.rutas.domain.repositorio.RutasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lista de rutas (Explorar / Mis Rutas / Guardadas). [ejemplo] se muestra mientras no hay
 * rutas del servidor, igual que antes en la pantalla.
 */
class ListaRutasViewModel(
    private val explorar: ExplorarRutas?,
    private val repo: RutasRepository,
    private val usuarioId: String,
    private val ejemplo: List<Ruta>,
) : ViewModel() {
    private val _consulta = MutableStateFlow(ConsultaRutas())
    val consulta: StateFlow<ConsultaRutas> = _consulta.asStateFlow()

    val guardadas: StateFlow<Set<String>> = repo.guardadas.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    private val remotas = explorar?.rutas ?: flowOf(null)

    val lista: StateFlow<List<Ruta>> = combine(remotas, guardadas, _consulta) { r, g, c -> rutasVisibles(RutasLoja.conOficiales(r ?: ejemplo), g, usuarioId, c) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, rutasVisibles(RutasLoja.conOficiales(explorar?.rutas?.value ?: ejemplo), emptySet(), usuarioId, ConsultaRutas()))

    init { explorar?.let { e -> viewModelScope.launch { e.refrescar() } } }

    fun pestana(p: PestanaRutas) = _consulta.update { it.copy(pestana = p) }
    fun buscar(texto: String) = _consulta.update { it.copy(texto = texto) }
    fun filtrar(f: FiltrosRuta) = _consulta.update { it.copy(filtros = f) }
    fun quitarCercania() = _consulta.update { it.copy(cerca = null) }
    fun ordenarCerca(lat: Double, lng: Double) {
        val e = explorar ?: return
        viewModelScope.launch { val cerca = e.cercanas(lat, lng); _consulta.update { it.copy(cerca = cerca) } }
    }
    fun alternarGuardada(id: String) { viewModelScope.launch { repo.alternarGuardada(id) } }

    private inline fun MutableStateFlow<ConsultaRutas>.update(f: (ConsultaRutas) -> ConsultaRutas) { value = f(value) }
}

/** Datos sociales del detalle de una ruta: resenas, fotos y corredores. */
class DetalleRutaViewModel(private val id: String, private val explorar: ExplorarRutas?) : ViewModel() {
    private val _resenas = MutableStateFlow<List<ResenaRuta>>(emptyList())
    val resenas: StateFlow<List<ResenaRuta>> = _resenas.asStateFlow()
    private val _fotos = MutableStateFlow<List<String>>(emptyList())
    val fotos: StateFlow<List<String>> = _fotos.asStateFlow()
    private val _corredores = MutableStateFlow(emptyList<Pair<String, Int>>() to 0)
    val corredores: StateFlow<Pair<List<Pair<String, Int>>, Int>> = _corredores.asStateFlow()

    /** Carga todo; solo tiene sentido cuando hay servidor. */
    suspend fun cargar() {
        val e = explorar ?: return
        _resenas.value = e.resenas(id); _fotos.value = e.fotos(id); _corredores.value = e.corredores(id)
    }
    suspend fun recargarResenas() { explorar?.let { _resenas.value = it.resenas(id) } }
    suspend fun recargarFotos() { explorar?.let { _fotos.value = it.fotos(id) } }
}
