package com.gacelas.rutas

import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.repositorio.PublicadorRutas
import com.gacelas.rutas.presentation.DibujarRutaViewModel
import com.gacelas.rutas.presentation.haversineKm
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DibujarRutaTest {
    @BeforeTest fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun despues() = Dispatchers.resetMain()

    @Test fun dibujaValidaYPublica() = runTest {
        var creada: List<Any?>? = null
        var falla = false
        val pub = object : PublicadorRutas {
            override suspend fun crear(nombre: String, descripcion: String, dificultad: Dificultad, etiquetas: List<String>, puntos: List<Pair<Double, Double>>, elevacionM: Int, actividadId: String?, perfil: List<Int>, origen: String): String {
                if (falla) error("sin red")
                creada = listOf(nombre, descripcion, dificultad, etiquetas, puntos.size, elevacionM, perfil, origen); return "r1"
            }
            override suspend fun pendiente(id: String) = id == "r1"
        }
        val vm = DibujarRutaViewModel(pub, { p -> p.map { 2000 + it.first.toInt() } }, { it.size * 10 })
        val avisos = mutableListOf<String>()
        val ids = mutableListOf<String>()
        vm.publicar({ avisos += it }, { ids += it })
        assertEquals("Ponle un nombre de al menos 3 letras", avisos.last())
        vm.nombre("  Jipiro  "); vm.publicar({ avisos += it }, { ids += it })
        assertEquals("Toca el mapa al menos dos veces", avisos.last())
        vm.agregar(-3.99, -79.20); vm.agregar(-3.98, -79.20); vm.agregar(0.0, 0.0); vm.deshacer()
        assertEquals(2, vm.estado.value.puntos.size)
        assertTrue(vm.estado.value.km in 1.10..1.12)
        vm.alternarEtiqueta("parque"); vm.alternarEtiqueta("x"); vm.alternarEtiqueta("x"); vm.dificultad(Dificultad.Facil)
        vm.publicar({ avisos += it }, { ids += it })
        assertEquals(listOf("r1"), ids)
        assertEquals("Sin conexión: la ruta quedó en el celular y se publicará al volver la red", avisos.last())
        assertEquals(listOf("Jipiro", "", Dificultad.Facil, listOf("parque"), 2, 20, listOf(1997, 1997), "dibujada"), creada)
        falla = true
        val vm2 = DibujarRutaViewModel(pub, { emptyList() }, { 0 })
        vm2.nombre("Ruta"); vm2.agregar(0.0, 0.0); vm2.agregar(0.0, 1.0)
        vm2.publicar({ avisos += it }, { ids += it })
        assertEquals("sin red", avisos.last()); assertFalse(vm2.estado.value.guardando)
        DibujarRutaViewModel(null, { emptyList() }, { 0 }).publicar({ avisos += it }, {})
        assertEquals("Inicia sesión para publicar rutas", avisos.last())
        assertEquals(111.19, haversineKm(0.0 to 0.0, 0.0 to 1.0), 0.01)
    }
}
