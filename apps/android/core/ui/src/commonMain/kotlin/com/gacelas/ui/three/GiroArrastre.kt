package com.gacelas.ui.three

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs
import kotlin.math.exp

/**
 * Giro del corredor sobre su eje vertical con arrastre horizontal (Inicio). Mientras se arrastra el giro
 * automatico se pausa; al soltar queda una inercia ligera que se frena sola y luego sigue el giro
 * automatico desde donde quedo. Sin estado de Compose: el bucle de render llama a [avanzar] cada cuadro.
 */
class GiroArrastre(
    /** Grados por segundo del giro automatico (0 = sin giro automatico). */
    private val gradosAuto: Float = 18f,
    /** Grados por pixel arrastrado. */
    private val gradosPorPx: Float = 0.4f,
    /** Frenado de la inercia (1/s). */
    private val frenado: Float = 4f,
    /** Inercia maxima en grados/s. */
    private val inerciaMax: Float = 720f,
) {
    var angulo = 0f
        private set
    var arrastrando = false
        private set
    private var inercia = 0f

    fun empezar() { arrastrando = true; inercia = 0f }

    fun arrastrar(dxPx: Float) { angulo = normalizar(angulo + dxPx * gradosPorPx) }

    /** [velocidadPx] en px/s al soltar. */
    fun soltar(velocidadPx: Float) {
        arrastrando = false
        inercia = (velocidadPx * gradosPorPx).coerceIn(-inerciaMax, inerciaMax)
    }

    /** Avanza [dt] segundos y devuelve el angulo (grados). */
    fun avanzar(dt: Float): Float {
        if (arrastrando || dt <= 0f) return angulo
        angulo = normalizar(angulo + (gradosAuto + inercia) * dt)
        inercia *= exp(-frenado * dt)
        if (abs(inercia) < 1f) inercia = 0f
        return angulo
    }

    val inerciaActual: Float get() = inercia

    /** true mientras el arrastre o su inercia mueven el modelo. */
    val enMovimiento: Boolean get() = arrastrando || inercia != 0f

    /** Vuelve a 0 grados y sin inercia (para giros relativos, como en el WebView). */
    fun reiniciar() { angulo = 0f; inercia = 0f; arrastrando = false }

    private fun normalizar(g: Float) = ((g % 360f) + 360f) % 360f
}

/**
 * Arrastre horizontal sobre el corredor. Solo reclama el gesto pasado el umbral horizontal, asi el
 * scroll vertical del Inicio sigue funcionando.
 */
@Composable
fun Modifier.giroPorArrastre(giro: GiroArrastre, alEmpezar: () -> Unit = {}): Modifier {
    val tracker = remember { VelocityTracker() }
    return this.pointerInput(giro) {
        detectHorizontalDragGestures(
            onDragStart = { tracker.resetTracking(); alEmpezar(); giro.empezar() },
            onDragEnd = { giro.soltar(tracker.calculateVelocity().x) },
            onDragCancel = { giro.soltar(0f) },
            onHorizontalDrag = { cambio, dx ->
                tracker.addPosition(cambio.uptimeMillis, cambio.position)
                cambio.consume()
                giro.arrastrar(dx)
            },
        )
    }
}
