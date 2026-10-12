package com.gacelas.correr

import com.gacelas.data.carreras.Parcial
import com.gacelas.tracking.EstadoCarrera
import com.gacelas.tracking.FaseCarrera
import com.gacelas.data.carreras.PuntoGps
import kotlin.math.sin

/** Carrera fija para las capturas de paridad Android/iOS (los mismos valores que CarreraC4Test). */
object CarreraMuestra {
    val estado = EstadoCarrera(
        fase = FaseCarrera.Corriendo, segundos = 892, distanciaKm = 2.34, desnivelM = 18.0, ultimaPrecisionM = 9f, ritmoActualSegKm = 381,
        puntos = (0..40).map { PuntoGps(-4.0 + it * 0.0005, -79.2 + sin(it / 6.0) * 0.001, it * 1000L, 5f) },
        parciales = listOf(Parcial(1, 1.0, 378), Parcial(2, 1.0, 385), Parcial(1, 0.34, 129, manual = true)),
    )
}
