package com.gacelas.analitica.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gacelas.analitica.application.ExportarReporte
import com.gacelas.analitica.application.FormatoExportacion
import com.gacelas.analitica.application.VerResumenPersonal
import com.gacelas.analitica.domain.PeriodoReporte
import com.gacelas.analitica.domain.ReportePersonal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Estado de la pantalla de reportes personales. [reporte] es null mientras se calcula. */
data class EstadoReportes(val periodo: PeriodoReporte = PeriodoReporte.Semana, val reporte: ReportePersonal? = null)

/** Reportes personales del corredor: usa el caso de uso VerResumenPersonal y se actualiza con cada carrera. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportesPersonalesViewModel(
    private val ver: VerResumenPersonal,
    private val ahora: () -> Long,
) : ViewModel() {
    private val periodo = MutableStateFlow(PeriodoReporte.Semana)

    val estado: StateFlow<EstadoReportes> = periodo
        .flatMapLatest { p -> ver(p, ahora()).map { EstadoReportes(p, it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoReportes())

    fun elegir(p: PeriodoReporte) {
        periodo.value = p
    }

    /** Texto para compartir o CSV del reporte actual; null si aun no hay reporte. */
    fun exportar(formato: FormatoExportacion): String? = estado.value.reporte?.let { ExportarReporte(it, formato) }
}
