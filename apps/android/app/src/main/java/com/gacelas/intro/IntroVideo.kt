package com.gacelas.intro

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.OptIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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

/**
 * Intro de marca a pantalla completa: reproduce res/raw/intro.mp4 con Media3 ExoPlayer (sin audio,
 * RESIZE_MODE_ZOOM) sobre el fondo de marca. [alPrimerCuadro] se llama cuando el video ya pinto
 * su primer cuadro (o al cerrarse sin llegar a pintarlo) para soltar el SplashScreen; [alTerminar]
 * una sola vez al acabar, al tocar, si el reproductor falla o si tarda mas de 1 s.
 */
@OptIn(UnstableApi::class)
@Composable
fun IntroVideo(alPrimerCuadro: () -> Unit, alTerminar: () -> Unit, modifier: Modifier = Modifier) {
    val listo by rememberUpdatedState(alPrimerCuadro)
    val fin by rememberUpdatedState(alTerminar)
    val control = remember { ControlIntro({ listo() }, { fin() }) }
    val contexto = LocalContext.current
    val jugador = remember { crearJugador(contexto, control) }
    DisposableEffect(jugador) { onDispose { jugador?.release() } }
    LaunchedEffect(Unit) {
        if (jugador == null) { control.terminar(); return@LaunchedEffect }
        delay(IntroArranque.ESPERA_PRIMER_CUADRO_MS)
        control.esperaAgotada()
        // Tope de seguridad por si STATE_ENDED nunca llega.
        delay((IntroArranque.DURACION_S * 1000).toLong() + 1_500 - IntroArranque.ESPERA_PRIMER_CUADRO_MS)
        control.terminar()
    }
    Box(
        modifier.fillMaxSize().background(FondoIntro).testTag("intro-animada")
            .semantics { contentDescription = "Intro de Runcity, toca para saltar" }
            .clickable(remember { MutableInteractionSource() }, indication = null) { control.terminar() },
    ) {
        if (jugador != null) AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(0xFF0C1322.toInt())
                    setBackgroundColor(0xFF0C1322.toInt())
                    // El toque lo recibe el Box de arriba.
                    isClickable = false
                    isFocusable = false
                    player = jugador
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(UnstableApi::class)
private fun crearJugador(contexto: Context, control: ControlIntro): ExoPlayer? = runCatching {
    ExoPlayer.Builder(contexto).build().apply {
        volume = 0f
        addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() = control.cuadroListo()
            override fun onPlaybackStateChanged(estado: Int) { if (estado == Player.STATE_ENDED) control.terminar() }
            override fun onPlayerError(error: PlaybackException) = control.terminar()
        })
        setMediaItem(MediaItem.fromUri("android.resource://${contexto.packageName}/${R.raw.intro}"))
        playWhenReady = true
        prepare()
    }
}.getOrNull()
