package com.gacelas.tracking

/* Compatibilidad: la carrera en curso es SesionCarrera (com.gacelas.carreras.domain). */
typealias FaseCarrera = com.gacelas.carreras.domain.FaseCarrera
typealias EstadoCarrera = com.gacelas.carreras.domain.EstadoCarrera
typealias RunTracker = com.gacelas.carreras.domain.SesionCarrera

fun tituloPorHora(ms: Long): String = com.gacelas.carreras.domain.reglas.tituloPorHora(ms)
