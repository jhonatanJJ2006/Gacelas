package com.gacelas.correr

import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.carreras.domain.modelo.Parcial
import com.gacelas.data.actualizacion.InfoVersion
import com.gacelas.carreras.domain.EstadoCarrera
import com.gacelas.carreras.domain.FaseCarrera
import com.gacelas.carreras.domain.modelo.PuntoGps
import kotlin.math.sin

/** Carrera fija para las capturas de paridad Android/iOS (los mismos valores que CarreraC4Test). */
object CarreraMuestra {
    val estado = EstadoCarrera(
        fase = FaseCarrera.Corriendo, segundos = 892, distanciaKm = 2.34, desnivelM = 18.0, ultimaPrecisionM = 9f, ritmoActualSegKm = 381,
        puntos = (0..40).map { PuntoGps(-4.0 + it * 0.0005, -79.2 + sin(it / 6.0) * 0.001, it * 1000L, 5f) },
        parciales = listOf(Parcial(1, 1.0, 378), Parcial(2, 1.0, 385), Parcial(1, 0.34, 129, manual = true)),
    )

    /** La misma carrera ya guardada (detalle de carrera). 12 oct 2026, 06:30 en Loja. */
    val guardada = Carrera(
        id = "muestra", titulo = "Carrera matutina", nota = "Buen ritmo por el Jipiro.", inicioMs = 1_791_811_800_000L,
        segundos = estado.segundos, distanciaKm = estado.distanciaKm, desnivelM = estado.desnivelM.toInt(),
        parciales = estado.parciales, puntos = estado.puntos,
    )

    /** Version nueva de ejemplo del modal de actualizacion (mismos datos que ParidadModalTest). */
    val version = InfoVersion(700, "0.7.0", "x", "https://example.com/a.apk",
        items = listOf("Nuevo diseño de bienvenida y login", "Mapa de Loja sin conexión en el acceso", "Tocar fuera cierra el teclado"), apkBytes = 49_283_072)
}
