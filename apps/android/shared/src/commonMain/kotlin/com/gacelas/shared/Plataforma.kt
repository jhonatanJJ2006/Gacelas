package com.gacelas.shared

/** Nombre y version de la plataforma (Android / iOS). */
expect object Plataforma {
    val nombre: String
}

/** Version comun de la app (Android la sobrescribe con BuildConfig). */
object VersionGacelas {
    const val NOMBRE = "0.7.0"
    /** 0.7.0 -> 700, igual que build.gradle.kts / ReglasActualizacion.codigoDe. */
    fun codigo(nombre: String = NOMBRE): Int {
        val p = nombre.split('.', '-').take(3).map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
        return p[0] * 10000 + p[1] * 100 + p[2]
    }
}
