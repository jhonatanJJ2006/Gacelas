package com.gacelas

import android.view.View
import com.gacelas.intro.IntroAnimada
import com.gacelas.ui.intro.IntroArranque
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/**
 * La intro sale en CADA apertura (docs/MARCA.md), tambien al cerrar y reabrir a los pocos segundos
 * con el proceso vivo (antes solo se veia el icono del splash si no pasaban 30 s).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntroCadaAperturaTest {
    @Before fun limpiar() = IntroArranque.reiniciarParaPruebas()

    private fun conIntro(a: MainActivity) = a.window.decorView.findViewWithTag<View>(IntroAnimada.ETIQUETA) != null
    private fun esperar(ms: Long) = ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)

    /** En Robolectric no hay dibujo real (la animacion no arranca): se salta con un toque. */
    private fun terminarIntro(a: MainActivity) {
        a.window.decorView.findViewWithTag<View>(IntroAnimada.ETIQUETA)?.performClick()
        esperar(1_000)
    }

    @Test fun cerrarYReabrirTreintaVecesSiempreMuestraLaIntro() {
        val n = 30
        var vistas = 0
        repeat(n) {
            val c = Robolectric.buildActivity(MainActivity::class.java).setup()
            if (conIntro(c.get())) vistas++
            terminarIntro(c.get())
            c.pause().stop().destroy()             // atras o cerrar desde recientes (proceso vivo)
            esperar(1_500)                          // reabre al poco
        }
        println("INTRO_APERTURAS $vistas/$n")
        assertEquals(n, vistas)
    }

    @Test fun traerDeSegundoPlanoRapidoNoLaRepiteYTras30sSi() {
        val c = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertTrue("arranque en frio", conIntro(c.get()))
        terminarIntro(c.get())
        assertFalse("la intro se retira", conIntro(c.get()))
        c.pause().stop(); esperar(5_000); c.restart().start().resume()
        assertFalse("cambio rapido de app", conIntro(c.get()))
        c.pause().stop(); esperar(IntroArranque.UMBRAL_AUSENCIA_MS + 1_000); c.restart().start().resume()
        assertTrue("vuelve tras 30 s", conIntro(c.get()))
    }

    @Test fun rotarNoRepiteLaIntro() {
        val c = Robolectric.buildActivity(MainActivity::class.java).setup()
        terminarIntro(c.get())
        val nueva = c.recreate()
        assertFalse(conIntro(nueva.get()))
    }
}
