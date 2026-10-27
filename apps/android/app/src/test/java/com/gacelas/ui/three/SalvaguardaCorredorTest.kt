package com.gacelas.ui.three

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SalvaguardaCorredorTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun limpiar() {
        ctx.getSharedPreferences(SalvaguardaCorredor.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        SalvaguardaCorredor.nuevoProceso()
    }

    @Test
    fun arranqueLimpioPermite3D() {
        assertTrue(SalvaguardaCorredor(ctx, 906).puedeUsar3D())
    }

    @Test
    fun salidaLimpiaSiguePermitiendo3D() {
        val s = SalvaguardaCorredor(ctx, 906)
        assertTrue(s.puedeUsar3D())
        s.empezarPaso(); s.terminarPaso()
        SalvaguardaCorredor.nuevoProceso()
        assertTrue(SalvaguardaCorredor(ctx, 906).puedeUsar3D())
    }

    @Test
    fun cierreDuranteEl3DUsaRespaldoEnElSiguienteArranque() {
        val s = SalvaguardaCorredor(ctx, 906)
        assertTrue(s.puedeUsar3D())
        s.empezarPaso() // el proceso muere aqui
        SalvaguardaCorredor.nuevoProceso()
        val siguiente = SalvaguardaCorredor(ctx, 906)
        assertFalse(siguiente.puedeUsar3D())
        // Sigue bloqueado en arranques posteriores de la misma version: no hay bucle de cierres.
        SalvaguardaCorredor.nuevoProceso()
        assertFalse(SalvaguardaCorredor(ctx, 906).puedeUsar3D())
    }

    @Test
    fun versionNuevaVuelveAIntentar() {
        SalvaguardaCorredor(ctx, 906).apply { puedeUsar3D(); empezarPaso() }
        SalvaguardaCorredor.nuevoProceso()
        assertFalse(SalvaguardaCorredor(ctx, 906).puedeUsar3D())
        SalvaguardaCorredor.nuevoProceso()
        assertTrue(SalvaguardaCorredor(ctx, 907).puedeUsar3D())
    }
}
