package com.gacelas.shared

actual fun relojMonotonoMs(): Long = android.os.SystemClock.elapsedRealtime()

actual fun logAviso(etiqueta: String, mensaje: String) {
    runCatching { android.util.Log.w(etiqueta, mensaje) }
}
