package com.gacelas.rutas.domain.reglas

/** Una sugerencia del buscador: el texto, si es una busqueda reciente y donde coincide (para resaltarlo). */
data class Sugerencia(val texto: String, val reciente: Boolean, val desde: Int, val largo: Int)

/** Sugerencias al escribir, como un buscador de video: recientes primero, luego lo que empieza igual. */
object SugerenciasBusqueda {
    private val TILDES = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')

    /** Minusculas y sin tildes, misma longitud que el original (para resaltar sobre el texto real). */
    fun normalizar(t: String): String = t.lowercase().map { TILDES[it] ?: it }.joinToString("")

    /**
     * Sin texto: las busquedas recientes. Con texto: recientes que coinciden, luego candidatos cuyo
     * nombre o alguna palabra empieza por el texto, luego los que lo contienen. Sin repetidos.
     */
    fun sugerir(texto: String, candidatos: List<String>, recientes: List<String>, max: Int = 6): List<Sugerencia> {
        val q = normalizar(texto.trim())
        if (q.isEmpty()) return recientes.distinctBy(::normalizar).take(max).map { Sugerencia(it, true, 0, 0) }
        fun posicion(t: String): Int? = normalizar(t).indexOf(q).takeIf { it >= 0 }
        fun alInicio(t: String): Boolean = normalizar(t).let { n -> n.startsWith(q) || n.split(' ', '-').any { it.startsWith(q) } }
        val vistos = HashSet<String>()
        val salida = mutableListOf<Sugerencia>()
        fun agregar(t: String, reciente: Boolean) {
            val p = posicion(t) ?: return
            if (vistos.add(normalizar(t))) salida += Sugerencia(t, reciente, p, q.length)
        }
        recientes.forEach { agregar(it, true) }
        candidatos.filter(::alInicio).sortedBy { it.length }.forEach { agregar(it, false) }
        candidatos.forEach { agregar(it, false) }
        return salida.take(max)
    }
}
