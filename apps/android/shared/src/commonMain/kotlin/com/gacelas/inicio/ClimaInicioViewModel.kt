package com.gacelas.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gacelas.clima.domain.ClimaActual
import com.gacelas.clima.domain.ClimaRepositorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado del chip de clima de Inicio: primero la cache (sin parpadeo), luego el refresco
 * (el repositorio decide si toca, minimo 15 min). [refrescar] se llama al abrir Inicio.
 */
class ClimaInicioViewModel(private val repo: ClimaRepositorio, private val ahora: () -> Long) : ViewModel() {
    private val _clima = MutableStateFlow<ClimaActual?>(null)
    val clima: StateFlow<ClimaActual?> = _clima.asStateFlow()

    fun refrescar() {
        viewModelScope.launch {
            if (_clima.value == null) repo.enCache()?.let { _clima.value = it }
            repo.actual(ahora())?.let { _clima.value = it }
        }
    }
}
