package com.gacelas.data.carreras

/*
 * Compatibilidad: el contexto carreras vive en com.gacelas.carreras (domain / data / presentation).
 * Estos alias mantienen los imports antiguos mientras se migran las pantallas.
 */
typealias PuntoGps = com.gacelas.carreras.domain.modelo.PuntoGps
typealias Parcial = com.gacelas.carreras.domain.modelo.Parcial
typealias CarreraGuardada = com.gacelas.carreras.domain.modelo.Carrera
typealias CarrerasRepository = com.gacelas.carreras.domain.repositorio.CarrerasRepository
typealias LocalCarrerasRepository = com.gacelas.carreras.data.LocalCarrerasRepository
typealias ResumenSemana = com.gacelas.carreras.domain.reglas.ResumenSemana
typealias ResumenPeriodo = com.gacelas.carreras.domain.reglas.ResumenPeriodo
typealias Estadisticas = com.gacelas.carreras.domain.reglas.Estadisticas
