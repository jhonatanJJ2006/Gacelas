package com.gacelas.actividad

import com.gacelas.actividad.domain.SesionCarrera
import com.gacelas.actividad.domain.FaseCarrera
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.repositorio.CarrerasRepository
import com.gacelas.actividad.presentation.CarreraEnCursoViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import com.gacelas.actividad.presentation.DetalleCarreraViewModel
import com.gacelas.actividad.presentation.HistorialCarrerasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class RepoMemoria : CarrerasRepository {
    override val carreras = MutableStateFlow<List<Carrera>>(emptyList())
    override suspend fun guardar(carrera: Carrera) { carreras.value = listOf(carrera) + carreras.value.filterNot { it.id == carrera.id } }
    override suspend fun eliminar(id: String) { carreras.value = carreras.value.filterNot { it.id == id } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsCarrerasTest {
    @BeforeTest fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun despues() = Dispatchers.resetMain()

    private val muestra = Carrera(id = "a", titulo = "Carrera matutina", inicioMs = 0L, segundos = 892, distanciaKm = 2.34)

    @Test fun detalleMuestraYElimina() = runTest {
        val repo = RepoMemoria().apply { guardar(muestra) }
        val vm = DetalleCarreraViewModel("a", repo)
        assertEquals(muestra, vm.estado.value.carrera)
        assertTrue(!vm.estado.value.cargando)
        vm.eliminar()
        assertNull(vm.estado.value.carrera)
    }

    @Test fun historial() = runTest {
        val repo = RepoMemoria()
        val vm = HistorialCarrerasViewModel(repo)
        repo.guardar(muestra)
        assertEquals(listOf(muestra), vm.carreras.value)
    }

    @Test fun carreraEnCurso() {
        var reloj = 0L
        val sesion = SesionCarrera(reloj = { reloj }, ahora = { 0L })
        val vm = CarreraEnCursoViewModel(sesion)
        sesion.iniciar()
        reloj = 5_000L; vm.tic()
        assertEquals(5, vm.estado.value.segundos)
        vm.alternarPausa()
        assertEquals(FaseCarrera.Pausada, vm.estado.value.fase)
        vm.vuelta()
        assertEquals("Vuelta 1", vm.estado.value.parciales.single().etiqueta)
    }
}
