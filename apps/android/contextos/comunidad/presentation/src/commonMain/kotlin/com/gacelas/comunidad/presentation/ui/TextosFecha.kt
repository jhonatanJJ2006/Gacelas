package com.gacelas.comunidad.presentation.ui

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private val DIAS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
private val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

/** Nombre del mes en español (mes base 0), como "LLLL" de Java con es-EC. */
fun nombreMesEs(mes: Int): String = MESES[mes]

/** "Sábado 18 de octubre", como "EEEE d 'de' MMMM" (es-EC) con la primera letra en mayúscula. */
fun etiquetaFechaEvento(ms: Long, zona: TimeZone = TimeZone.currentSystemDefault()): String {
    val f = Instant.fromEpochMilliseconds(ms).toLocalDateTime(zona)
    return "${DIAS[f.dayOfWeek.ordinal]} ${f.dayOfMonth} de ${MESES[f.monthNumber - 1]}".replaceFirstChar { it.uppercase() }
}

/** "AAAA-MM-DD" y "HH:MM" a milisegundos en la zona del teléfono; null si no son válidos. */
fun inicioEvento(fecha: String, hora: String, zona: TimeZone = TimeZone.currentSystemDefault()): Long? =
    runCatching { LocalDateTime.parse("${fecha.trim()}T${hora.trim()}").toInstant(zona).toEpochMilliseconds() }.getOrNull()
