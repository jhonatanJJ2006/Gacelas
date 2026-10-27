package com.gacelas.intro

import android.app.Activity
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.graphics.Color
import com.gacelas.R
import com.gacelas.ui.intro.EstadoIntro
import com.gacelas.ui.intro.IntroArranque

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
 * Intro de marca "Ruta y destello v2b" como AnimatedVectorDrawable (res/drawable/intro_marca.xml,
 * generado por tools/marca/generar_intro_avd.py). Es la tecnica del splash animado de Android:
 * desde Android 7.1 el sistema anima el vector en el RenderThread, sin reproductor, sin
 * decodificador de video, sin red y sin depender del hilo principal, asi que sigue a 60 fps
 * mientras la app se monta debajo. Es una vista nativa encima de Compose para que arranque en el
 * primer cuadro de la ventana, antes de que Compose componga nada (docs/MARCA.md).
 */
object IntroAnimada {
    const val ETIQUETA = "intro-animada"

    /** Momento (uptimeMillis) en que empezo a correr la ultima intro; 0 si aun no se dibujo. */
    @Volatile var iniciadaEn = 0L
        private set

    /** Pone la intro sobre [actividad] y la arranca; [alTerminar] se llama una vez, ya retirada. */
    fun mostrar(actividad: Activity, alTerminar: () -> Unit): View {
        var alPrimerDibujo: (() -> Unit)? = null
        // La animacion y el tope de seguridad empiezan cuando la intro se dibuja de verdad por primera
        // vez, no al crearla: si el arranque tarda, la intro no se da por vencida antes de verse.
        val vista = object : ImageView(actividad) {
            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
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
        val avd = actividad.getDrawable(R.drawable.intro_marca) as AnimatedVectorDrawable
        vista.apply {
            tag = ETIQUETA
            contentDescription = "Intro de Runcity, toca para saltar"
            setBackgroundColor(actividad.getColor(R.color.fondo_marca))
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageDrawable(avd)
            isClickable = true
            setOnClickListener { control.terminar() }
        }
        avd.registerAnimationCallback(object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) { control.terminar() }
        })
        alPrimerDibujo = {
            if (!control.terminada) {
                iniciadaEn = SystemClock.uptimeMillis()
                Log.i("Arranque", "intro empieza a ${iniciadaEn - Process.getStartUptimeMillis()} ms del inicio del proceso")
                avd.start()
                // Tope por si onAnimationEnd nunca llega (no corta una intro normal: dura 2,2 s).
                vista.postDelayed({ control.terminar() }, IntroArranque.TOPE_MS)
            }
        }
        actividad.addContentView(vista, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return vista
    }
}
