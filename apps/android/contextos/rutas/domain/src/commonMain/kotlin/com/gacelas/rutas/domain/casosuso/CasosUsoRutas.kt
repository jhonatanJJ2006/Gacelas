package com.gacelas.rutas.domain.casosuso

import com.gacelas.rutas.domain.modelo.FiltrosRuta
import com.gacelas.rutas.domain.modelo.PestanaRutas
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.rutas.domain.reglas.aplicarFiltros
import com.gacelas.rutas.domain.reglas.filtrarRutas
import com.gacelas.rutas.domain.reglas.ordenarPorCercania

/** Criterios de la lista de rutas que elige el usuario. */
data class ConsultaRutas(
    val pestana: PestanaRutas = PestanaRutas.Explorar,
    val texto: String = "",
    val filtros: FiltrosRuta = FiltrosRuta(),
    val cerca: Map<String, Double>? = null,
)

/** Rutas visibles: pestana + busqueda + filtros y, si hay ubicacion, ordenadas por cercania. */
fun rutasVisibles(rutas: List<Ruta>, guardadas: Set<String>, usuarioId: String, c: ConsultaRutas): List<Ruta> {
    val filtradas = aplicarFiltros(filtrarRutas(rutas, c.pestana, guardadas, usuarioId, c.texto), c.filtros)
    return c.cerca?.let { ordenarPorCercania(filtradas, it) } ?: filtradas
}
