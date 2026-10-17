package com.gacelas.rutas

import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.ResenaRuta
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.rutas.domain.repositorio.ExplorarRutas
import com.gacelas.rutas.domain.repositorio.RutasRepository
import com.gacelas.rutas.presentation.DetalleRutaViewModel
import com.gacelas.rutas.presentation.ListaRutasViewModel
import com.gacelas.rutas.domain.modelo.PestanaRutas
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

private val ejemplo = listOf(
    Ruta("1", "Parque Jipiro", 1.8, Dificultad.Facil, 45, "Panorámica"),
    Ruta("3", "Cerro Villonaco", 4.4, Dificultad.Dificil, 380, "Montaña"),
    Ruta("4", "Campus UTPL", 6.1, Dificultad.Moderado, 150, "Mía", autorId = "yo"),
)

private class GuardadasMemoria : RutasRepository {
    override val guardadas = MutableStateFlow<Set<String>>(emptySet())
    override suspend fun alternarGuardada(id: String) { guardadas.value = guardadas.value.let { if (id in it) it - id else it + id } }
}

private class ExplorarFalso(inicial: List<Ruta>?) : ExplorarRutas {
    override val rutas = MutableStateFlow(inicial)
    override val meGusta = MutableStateFlow<Set<String>>(emptySet())
    var refrescos = 0
    override suspend fun refrescar() { refrescos++ }
    override suspend fun cercanas(lat: Double, lng: Double) = mapOf("4" to 0.5, "1" to 2.0)
    override suspend fun resenas(id: String) = listOf(ResenaRuta("r", "u", "Ana", 5, "Buena"))
    override suspend fun fotos(id: String) = listOf("f.jpg")
    override suspend fun corredores(id: String) = listOf("Ana" to 2) to 1
    override suspend fun subirFoto(id: String, bytes: ByteArray, tipo: String) {}
    override suspend fun alternarMeGusta(id: String) {}
    override suspend fun resenar(id: String, estrellas: Int, comentario: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class RutasTest {
    @BeforeTest fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun despues() = Dispatchers.resetMain()

    @Test fun listaUsaEjemploSinServidorYGuarda() = runTest {
        val repo = GuardadasMemoria()
        val vm = ListaRutasViewModel(null, repo, "yo", ejemplo)
        assertEquals(3, vm.lista.value.size)
        vm.pestana(PestanaRutas.Guardadas)
        assertEquals(emptyList(), vm.lista.value)
        vm.alternarGuardada("3")
        assertEquals(listOf("3"), vm.lista.value.map { it.id })
    }

    @Test fun listaConServidorRefrescaYOrdenaCerca() = runTest {
        val ex = ExplorarFalso(null)
        val vm = ListaRutasViewModel(ex, GuardadasMemoria(), "yo", ejemplo)
        assertEquals(1, ex.refrescos)
        ex.rutas.value = ejemplo.take(2)
        assertEquals(listOf("1", "3"), vm.lista.value.map { it.id })
        vm.ordenarCerca(0.0, 0.0)
        assertEquals(mapOf("4" to 0.5, "1" to 2.0), vm.consulta.value.cerca)
        assertEquals(listOf("1", "3"), vm.lista.value.map { it.id })
        vm.quitarCercania()
        assertEquals(null, vm.consulta.value.cerca)
    }

    @Test fun detalleCargaDatosSociales() = runTest {
        val vm = DetalleRutaViewModel("1", ExplorarFalso(ejemplo))
        vm.cargar()
        assertEquals("Ana", vm.resenas.value.single().nombre)
        assertEquals(listOf("f.jpg"), vm.fotos.value)
        assertEquals(1, vm.corredores.value.second)
        val sinServidor = DetalleRutaViewModel("1", null).also { it.cargar() }
        assertEquals(emptyList(), sinServidor.fotos.value)
    }
}
