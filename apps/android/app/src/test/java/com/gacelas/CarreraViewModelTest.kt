package com.gacelas

import com.gacelas.correr.CarreraViewModel
import com.gacelas.correr.formatearHms
import com.gacelas.correr.formatearMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarreraViewModelTest {
    @Test
    fun tickAvanzaTiempoYDistancia() {
        val vm = CarreraViewModel { 0.005 }
        repeat(200) { vm.tick() }
        val e = vm.estado.value
        assertEquals(200, e.segundos)
        assertEquals(1.0, e.distanciaKm, 1e-9)
        assertEquals(3, e.ritmoMin)
        assertEquals("00:03:20", e.tiempoTexto)
        assertEquals("1.00", e.distanciaTexto)
    }

    @Test
    fun pausaDetieneElConteo() {
        val vm = CarreraViewModel { 0.01 }
        vm.tick()
        vm.alternarPausa()
        repeat(10) { vm.tick() }
        assertEquals(1, vm.estado.value.segundos)
        assertTrue(vm.estado.value.pausada)
        vm.alternarPausa()
        vm.tick()
        assertEquals(2, vm.estado.value.segundos)
        vm.reiniciar()
        assertEquals(0, vm.estado.value.segundos)
    }

    @Test
    fun ritmoEsCeroSinDistancia() {
        assertEquals(0, CarreraViewModel { 0.0 }.estado.value.ritmoMin)
    }

    @Test
    fun formatos() {
        assertEquals("01:01:01", formatearHms(3661))
        assertEquals("29:13", formatearMs(1753))
    }
}
