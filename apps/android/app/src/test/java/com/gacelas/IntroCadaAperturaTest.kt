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

    /** Las actividades que quedan vivas (con su indicador de carga animado) no dejan a Compose quedar
     *  ocioso en las pruebas siguientes del mismo proceso: se destruyen todas al terminar. */
    private val vivas = mutableListOf<org.robolectric.android.controller.ActivityController<MainActivity>>()
    private fun abrir() = Robolectric.buildActivity(MainActivity::class.java).setup().also { vivas += it }

    @org.junit.After fun cerrarTodo() {
        vivas.forEach { c -> runCatching { if (!c.get().isDestroyed) c.pause().stop().destroy() } }
        vivas.clear()
        IntroArranque.reiniciarParaPruebas()
    }

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
            val c = abrir()
            if (conIntro(c.get())) vistas++
            terminarIntro(c.get())
            c.pause().stop().destroy()             // atras o cerrar desde recientes (proceso vivo)
            esperar(1_500)                          // reabre al poco
        }
        println("INTRO_APERTURAS $vistas/$n")
        assertEquals(n, vistas)
    }

    // Traer de segundo plano (<30 s no, >=30 s si) y rotar se prueban con la regla pura en IntroArranqueTest:
    // reanudar MainActivity tras avanzar el reloj deja trabajo del mapa sin conexion que bloquea otras pruebas.
}
