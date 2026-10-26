package com.gacelas.analitica

import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.analitica.application.FormatoExportacion
import com.gacelas.analitica.application.VerResumenPersonal
import com.gacelas.analitica.domain.ConsultaAnalitica
import com.gacelas.analitica.domain.InicioRuta
import com.gacelas.analitica.domain.PeriodoReporte
import com.gacelas.analitica.presentation.ReportesPersonalesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class ConsultaMemoria : ConsultaAnalitica {
    override val carreras = MutableStateFlow<List<Carrera>>(emptyList())
    override fun iniciosRutas(): List<InicioRuta> = emptyList()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportesPersonalesViewModelTest {
    @BeforeTest fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun despues() = Dispatchers.resetMain()

    private val ahora = 1_761_480_000_000L // 26 oct 2025, 12:00 UTC

    @Test fun calculaElPeriodoYSeActualizaConCadaCarrera() = runTest {
        val consulta = ConsultaMemoria()
        val vm = ReportesPersonalesViewModel(VerResumenPersonal(consulta)) { ahora }
        assertEquals(PeriodoReporte.Semana, vm.estado.value.periodo)
        assertEquals(0, vm.estado.value.reporte?.actual?.carreras)

        consulta.carreras.value = listOf(Carrera(id = "a", titulo = "Matutina", inicioMs = ahora - 3_600_000, segundos = 1800, distanciaKm = 5.0))
        assertEquals(1, vm.estado.value.reporte?.actual?.carreras)
        assertEquals(5.0, vm.estado.value.reporte!!.actual.km, 0.001)
    }

    @Test fun cambiarPeriodoYExportar() = runTest {
        val consulta = ConsultaMemoria()
        consulta.carreras.value = listOf(Carrera(id = "a", titulo = "Matutina", inicioMs = ahora - 3_600_000, segundos = 1800, distanciaKm = 5.0))
        val vm = ReportesPersonalesViewModel(VerResumenPersonal(consulta)) { ahora }
        vm.elegir(PeriodoReporte.Anio)
        assertEquals(PeriodoReporte.Anio, vm.estado.value.periodo)
        assertEquals(PeriodoReporte.Anio, vm.estado.value.reporte?.periodo)
        val csv = assertNotNull(vm.exportar(FormatoExportacion.Csv))
        assertTrue(csv.isNotBlank())
    }
}
