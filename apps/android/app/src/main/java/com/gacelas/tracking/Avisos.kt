package com.gacelas.tracking

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Sonidos y vibraciones para intervalos y metronomo. */
class Avisos(context: Context) {
    private val tono: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull()
    private val vibrador: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun tick() { tono?.startTone(ToneGenerator.TONE_PROP_BEEP, 40) }

    fun cambioFase(sonido: Boolean, vibracion: Boolean) {
        if (sonido) tono?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        if (vibracion) vibrar(400)
    }

    private fun vibrar(ms: Long) {
        val v = vibrador ?: return
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(ms)
    }

    fun liberar() { tono?.release() }
}
