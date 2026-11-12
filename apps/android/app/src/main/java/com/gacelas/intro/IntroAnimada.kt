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
        var liberar: () -> Unit = {}
        val control = ControlIntro {
            liberar()
            vista.animate().cancel()
            vista.animate().alpha(0f).setDuration(FUNDIDO_SALIDA_MS).withEndAction {
                (vista.parent as? ViewGroup)?.removeView(vista)
                alTerminar()
            }.start()
        }
        // Como Netflix: video local (res/raw/intro.mp4, H.264 High 4.2, 1080 x 1920, 60 fps) que decodifica
        // el hardware (MediaCodec) y se compone directo en un SurfaceView, fuera del hilo de la UI. Si el
        // reproductor no se puede crear, se usa la intro dibujada en Compose Canvas.
        val video = IntroVideo.crear(actividad, alTerminar = { control.terminar() })
        val lienzo: View = video?.vista ?: ComposeView(actividad).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { IntroMarcaEAnimada(activa = dibujada.value, alTerminar = { control.terminar() }) }
        }
        vista.apply {
            tag = ETIQUETA
            contentDescription = "Intro de Runcity, toca para saltar"
            setBackgroundColor(actividad.getColor(R.color.fondo_marca))
            setWillNotDraw(false)
            addView(lienzo, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            // Tapa del color de marca hasta que el video pinta su primer cuadro (el SurfaceView vacio es negro).
            video?.tapa?.let { addView(it, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) }
            isClickable = true
            setOnClickListener { control.terminar() }
        }
        alPrimerDibujo = {
            if (!control.terminada) {
                iniciadaEn = SystemClock.uptimeMillis()
                dibujada.value = true
                video?.reproducir()
                Log.i("Arranque", "intro empieza a ${iniciadaEn - Process.getStartUptimeMillis()} ms del inicio del proceso")
                // Tope por si el reloj de cuadros nunca llega al final (no corta una intro normal de 3,0 s).
                vista.postDelayed({ control.terminar() }, IntroArranque.TOPE_MS)
            }
        }
        liberar = { video?.liberar() }
        actividad.addContentView(vista, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return vista
    }
}

/**
 * Reproductor de la intro: MediaPlayer del sistema (MediaCodec, decodificacion por hardware) sobre un
 * SurfaceView, preparado al crear la vista para que el primer cuadro este listo antes de mostrarse;
 * sin audio, sin bucle y sin dependencias que agranden el APK. El SurfaceView se agranda para cubrir
 * la pantalla sin deformar.
 */
class IntroVideo private constructor(
    val vista: View,
    val tapa: View,
    private val player: android.media.MediaPlayer,
) {
    private var preparado = false
    private var superficieLista = false
    private var pedida = false
    private var liberado = false

    fun reproducir() { pedida = true; arrancarSiPuede() }

    private fun arrancarSiPuede() {
        if (pedida && preparado && superficieLista && !liberado) runCatching { player.start() }
    }

    fun liberar() { if (!liberado) { liberado = true; runCatching { player.release() } } }

    companion object {
        fun crear(actividad: Activity, alTerminar: () -> Unit): IntroVideo? = runCatching {
            val player = android.media.MediaPlayer()
            val superficie = android.view.SurfaceView(actividad)
            val tapa = View(actividad).apply { setBackgroundColor(actividad.getColor(R.color.fondo_marca)) }
            val marco = object : android.widget.FrameLayout(actividad) {
                override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                    // Cubrir: el video es 9:16; en pantallas mas altas se recortan los lados (fondo liso).
                    val w = r - l; val h = b - t
                    val (vw, vh) = if (w * 16 > h * 9) w to (w * 16 / 9) else (h * 9 / 16) to h
                    val x = (w - vw) / 2; val y = (h - vh) / 2
                    superficie.layout(x, y, x + vw, y + vh)
                }
            }.apply { setBackgroundColor(actividad.getColor(R.color.fondo_marca)); addView(superficie) }
            actividad.resources.openRawResourceFd(R.raw.intro).use { fd ->
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            player.setVolume(0f, 0f)
            player.isLooping = false
            val intro = IntroVideo(marco, tapa, player)
            superficie.holder.addCallback(object : android.view.SurfaceHolder.Callback {
                override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                    if (intro.liberado) return
                    player.setDisplay(holder)
                    intro.superficieLista = true
                    intro.arrancarSiPuede()
                }
                override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                override fun surfaceDestroyed(holder: android.view.SurfaceHolder) { intro.superficieLista = false }
            })
            player.setOnPreparedListener { intro.preparado = true; intro.arrancarSiPuede() }
            player.setOnInfoListener { _, que, _ ->
                if (que == android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    tapa.animate().alpha(0f).setDuration(80).withEndAction { tapa.visibility = View.GONE }.start()
                }
                false
            }
            player.setOnCompletionListener { alTerminar() }
            player.setOnErrorListener { _, que, extra ->
                Log.w("Arranque", "intro en video fallo: $que/$extra")
                alTerminar()
                true
            }
            player.prepareAsync()
            intro
        }.onFailure { Log.w("Arranque", "sin reproductor de video, intro en Canvas", it) }.getOrNull()
    }
}
