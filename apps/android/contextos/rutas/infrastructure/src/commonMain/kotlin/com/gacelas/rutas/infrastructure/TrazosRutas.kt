package com.gacelas.rutas.infrastructure

/** Trazos (lat, lng) de las rutas de ejemplo; Android los dibuja con MapLibre (Mapas.trazos) y la portada C4 mide el "1". */
object TrazosRutas {
    val puntos: Map<String, List<Pair<Double, Double>>> = mapOf(
        "1" to listOf(-3.9746 to -79.2017, -3.9712 to -79.2001, -3.969 to -79.203, -3.9718 to -79.2058, -3.9746 to -79.2017),
        "2" to listOf(-3.9931 to -79.2005, -3.999 to -79.1985, -4.006 to -79.197, -4.0125 to -79.195),
        "3" to listOf(-3.995 to -79.225, -3.999 to -79.24, -4.001 to -79.255, -4.0002 to -79.264),
        "primary" to listOf(-3.9746 to -79.2017, -3.9845 to -79.2035, -3.9931 to -79.2005, -4.0045 to -79.1958),
        "alt" to listOf(-3.9746 to -79.2017, -3.988 to -79.2075, -3.999 to -79.201),
    )
}
