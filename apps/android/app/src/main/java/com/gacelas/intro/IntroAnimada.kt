package com.gacelas.intro

import android.app.Activity
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.gacelas.R
import com.gacelas.ui.intro.EstadoIntro
import com.gacelas.ui.intro.IntroArranque
import com.gacelas.ui.intro.IntroMarcaEAnimada

/** Fondo de marca: igual al del SplashScreen (colors.xml fondo_marca) y al primer cuadro de la intro. */
val FondoIntro = Color(0xFF0C1322)

/** Fundido con que se retira la intro (al terminar o al tocar): nunca un corte seco. */
const val FUNDIDO_SALIDA_MS = 160L

/**
 * Control de una reproduccion de la intro, sin Android: el fin (animacion completa, toque o tope de
 * seguridad) ocurre una sola vez. Que la app ya este lista no termina la intro.
 */
class ControlIntro(private val alTerminar: () -> Unit) {
    private val estado = EstadoIntro()
    val terminada get() = estado.terminada
    fun terminar() { if (estado.terminar()) alTerminar() }
}

/**
 * Intro de marca "Opcion E" (3,0 s): Compose Canvas con un solo reloj (IntroMarcaEAnimada, en
 * core/ui commonMain, comun con iOS). Es una vista encima del contenido de la actividad para que
 * aparezca en cuanto la ventana dibuja, sin esperar a que se monte la app (docs/MARCA.md).
 */
object IntroAnimada {
    const val ETIQUETA = "intro-animada"

    /** Momento (uptimeMillis) en que empezo a correr la ultima intro; 0 si aun no se dibujo. */
    @Volatile var iniciadaEn = 0L
        private set

    /** Pone la intro sobre [actividad] y la arranca; [alTerminar] se llama una vez, ya retirada. */
    fun mostrar(actividad: Activity, alTerminar: () -> Unit): View {
        var alPrimerDibujo: (() -> Unit)? = null
        // El reloj de la animacion arranca con el primer dibujo real de la ventana.
        val dibujada = androidx.compose.runtime.mutableStateOf(false)
        // El tope de seguridad empieza cuando la intro se dibuja de verdad por primera vez, no al
        // crearla: si el arranque tarda, la intro no se da por vencida antes de verse.
        val vista = object : android.widget.FrameLayout(actividad) {
            override fun dispatchDraw(canvas: android.graphics.Canvas) {
                super.dispatchDraw(canvas)
                alPrimerDibujo?.let { alPrimerDibujo = null; post(it) }
            }
        }
        val control = ControlIntro {
            vista.animate().cancel()
            vista.animate().alpha(0f).setDuration(FUNDIDO_SALIDA_MS).withEndAction {
                (vista.parent as? ViewGroup)?.removeView(vista)
                alTerminar()
            }.start()
        }
        val lienzo = ComposeView(actividad).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { IntroMarcaEAnimada(activa = dibujada.value, alTerminar = { control.terminar() }) }
        }
        vista.apply {
            tag = ETIQUETA
            contentDescription = "Intro de Runcity, toca para saltar"
            setBackgroundColor(actividad.getColor(R.color.fondo_marca))
            setWillNotDraw(false)
            addView(lienzo, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            isClickable = true
            setOnClickListener { control.terminar() }
        }
        alPrimerDibujo = {
            if (!control.terminada) {
                iniciadaEn = SystemClock.uptimeMillis()
                dibujada.value = true
                Log.i("Arranque", "intro empieza a ${iniciadaEn - Process.getStartUptimeMillis()} ms del inicio del proceso")
                // Tope por si el reloj de cuadros nunca llega al final (no corta una intro normal de 3,0 s).
                vista.postDelayed({ control.terminar() }, IntroArranque.TOPE_MS)
            }
        }
        actividad.addContentView(vista, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return vista
    }
}
