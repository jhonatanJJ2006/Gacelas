package com.gacelas.correr

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.gacelas.carreras.domain.EstadoCarrera
import com.gacelas.carreras.domain.FaseCarrera
import com.gacelas.cuenta.domain.modelo.Ajustes
import kotlinx.coroutines.delay
import java.util.Locale

/** Anuncios de voz cada km/mi y autopausa sin puntos GPS nuevos en 15 s, segun los ajustes. */
@Composable
fun PreferenciasCarrera(estado: EstadoCarrera, ajustes: Ajustes, onPausar: () -> Unit) {
    val context = LocalContext.current
    val tts = remember { arrayOfNulls<TextToSpeech>(1) }
    DisposableEffect(ajustes.anunciosVoz) {
        if (ajustes.anunciosVoz) tts[0] = TextToSpeech(context) { ok -> if (ok == TextToSpeech.SUCCESS) tts[0]?.language = Locale("es", "EC") }
        onDispose { tts[0]?.shutdown(); tts[0] = null }
    }
    val m = ajustes.unidadesMetricas
    val tramo = kotlin.math.floor(if (m) estado.distanciaKm else estado.distanciaKm / 1.609).toInt()
    LaunchedEffect(tramo) {
        if (!ajustes.anunciosVoz || tramo < 1) return@LaunchedEffect
        val t = estado.segundos
        val texto = "$tramo ${if (m) "kilómetros" else "millas"}. Tiempo ${t / 60} minutos ${t % 60} segundos."
        tts[0]?.speak(texto, TextToSpeech.QUEUE_ADD, null, "tramo-$tramo")
    }
    LaunchedEffect(ajustes.autopausa, estado.fase, estado.puntos.size) {
        if (!ajustes.autopausa || estado.fase != FaseCarrera.Corriendo) return@LaunchedEffect
        delay(15_000)
        onPausar()
    }
}
