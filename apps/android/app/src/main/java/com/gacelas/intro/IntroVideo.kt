package com.gacelas.intro

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.gacelas.R
import com.gacelas.ui.intro.EstadoIntro
import com.gacelas.ui.intro.IntroArranque
import kotlinx.coroutines.delay

/** Fondo de marca: igual al del SplashScreen (colors.xml fondo_marca) y al primer cuadro del video. */
val FondoIntro = Color(0xFF0C1322)

/**
 * Control de una reproduccion de la intro, sin Android: decide cuando avisar que hay primer cuadro
 * y cuando terminar (fin del video, toque, error o espera agotada), cada cosa una sola vez.
 */
class ControlIntro(private val alPrimerCuadro: () -> Unit, private val alTerminar: () -> Unit) {
    private val estado = EstadoIntro()
    var primerCuadro = false
        private set
    val terminada get() = estado.terminada

    fun cuadroListo() { if (!primerCuadro && !terminada) { primerCuadro = true; alPrimerCuadro() } }
    fun terminar() { if (estado.terminar()) { if (!primerCuadro) { primerCuadro = true; alPrimerCuadro() }; alTerminar() } }
    /** Fallback: sin primer cuadro tras [IntroArranque.ESPERA_PRIMER_CUADRO_MS] se cierra. */
    fun esperaAgotada() { if (!primerCuadro) terminar() }
}

/** Tamano del video (720 x 1280). */
private const val ANCHO_VIDEO = 720f
private const val ALTO_VIDEO = 1280f

/**
 * Intro de marca a pantalla completa: reproduce res/raw/intro.mp4 con el MediaPlayer del sistema en
 * un TextureView (transparente hasta el primer cuadro, asi se ve el fondo de marca y no negro),
 * escalado para cubrir la pantalla como RESIZE_MODE_ZOOM, sin audio. No usa Media3: el APK debug
 * debe quedar por debajo de los 50 MB de Supabase Storage (docs/MARCA.md).
 * [alPrimerCuadro] se llama cuando el video ya pinto su primer cuadro (o al cerrarse sin llegar a
 * pintarlo) para soltar el SplashScreen; [alTerminar] una sola vez al acabar, al tocar, si el
 * reproductor falla o si tarda mas de 1 s.
 */
@Composable
fun IntroVideo(alPrimerCuadro: () -> Unit, alTerminar: () -> Unit, modifier: Modifier = Modifier) {
    val listo by rememberUpdatedState(alPrimerCuadro)
    val fin by rememberUpdatedState(alTerminar)
    val control = remember { ControlIntro({ listo() }, { fin() }) }
    val contexto = LocalContext.current
    val uri = remember { Uri.parse("android.resource://${contexto.packageName}/${R.raw.intro}") }
    val jugador = remember { mutableListOf<MediaPlayer>() }
    DisposableEffect(Unit) { onDispose { jugador.forEach { runCatching { it.release() } }; jugador.clear() } }
    LaunchedEffect(Unit) {
        delay(IntroArranque.ESPERA_PRIMER_CUADRO_MS)
        control.esperaAgotada()
        // Tope de seguridad por si onCompletion nunca llega.
        delay((IntroArranque.DURACION_S * 1000).toLong() + 1_500 - IntroArranque.ESPERA_PRIMER_CUADRO_MS)
        control.terminar()
    }
    Box(
        modifier.fillMaxSize().clipToBounds().background(FondoIntro).testTag("intro-animada")
            .semantics { contentDescription = "Intro de Runcity, toca para saltar" }
            .clickable(remember { MutableInteractionSource() }, indication = null) { control.terminar() },
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    isOpaque = false
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            if (control.terminada) return
                            val mp = runCatching {
                                MediaPlayer().apply {
                                    setSurface(Surface(st))
                                    setDataSource(ctx, uri)
                                    setVolume(0f, 0f)
                                    setOnInfoListener { _, que, _ ->
                                        if (que == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) control.cuadroListo(); false
                                    }
                                    setOnCompletionListener { control.terminar() }
                                    setOnErrorListener { _, _, _ -> control.terminar(); true }
                                    setOnPreparedListener { it.start() }
                                    prepareAsync()
                                }
                            }.getOrNull()
                            if (mp == null) control.terminar() else jugador += mp
                        }
                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    }
                }
            },
            // Cubre la pantalla conservando la proporcion (como RESIZE_MODE_ZOOM), centrado.
            modifier = Modifier.layout { medible, limites ->
                val w = limites.maxWidth.toFloat(); val h = limites.maxHeight.toFloat()
                val k = maxOf(w / ANCHO_VIDEO, h / ALTO_VIDEO)
                val vw = (ANCHO_VIDEO * k).toInt(); val vh = (ALTO_VIDEO * k).toInt()
                val p = medible.measure(androidx.compose.ui.unit.Constraints.fixed(vw, vh))
                layout(limites.maxWidth, limites.maxHeight) { p.place((limites.maxWidth - vw) / 2, (limites.maxHeight - vh) / 2) }
            },
        )
    }
}
