package com.gacelas.shared

actual object Plataforma {
    actual val nombre: String = "Android ${android.os.Build.VERSION.RELEASE}"
}
