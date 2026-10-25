package com.gacelas.rutas.domain.modelo

import kotlinx.serialization.Serializable

enum class Dificultad(val etiqueta: String) { Facil("Fácil"), Moderado("Moderado"), Dificil("Difícil") }

@Serializable
data class Ruta(
    val id: String,
    val nombre: String,
    val distanciaKm: Double,
    val dificultad: Dificultad,
    val elevacionM: Int,
    val descripcion: String,
    val autorId: String = "gacelas",
    val etiquetas: List<String> = emptyList(),
    val puntos: List<Pair<Double, Double>> = emptyList(),
    val meGusta: Int = 0,
    val valoracion: Double? = null,
    val resenas: Int = 0,
    val autorNombre: String? = null,
    val propia: Boolean = false,
    val perfilElevacion: List<Int> = emptyList(),
    /** Ruta oficial de Gacelas (catalogo de Loja, migracion 022). */
    val oficial: Boolean = false,
    val categoria: CategoriaRuta? = null,
    /** pavimento, tierra, mixta o tartan. */
    val superficie: String? = null,
    /** Fuente y licencia del trazado. */
    val fuente: String? = null,
)

/** Categorias del catalogo (routes.category). */
enum class CategoriaRuta(val clave: String, val etiqueta: String) {
    Parque("parque", "Parques"), Sendero("sendero", "Senderos"), Montana("montana", "Montaña"),
    Urbana("urbana", "Urbanas"), Pista("pista", "Pista");

    companion object {
        fun desdeClave(clave: String?): CategoriaRuta? = entries.firstOrNull { it.clave == clave }
    }
}

data class ResenaRuta(val id: String, val usuarioId: String, val nombre: String, val estrellas: Int, val comentario: String)

enum class PestanaRutas(val etiqueta: String) { Explorar("Explorar"), MisRutas("Mis Rutas"), Guardadas("Guardadas") }

/** Etiquetas permitidas (mismas que la web y el CHECK de routes). */
val ETIQUETAS_RUTA = linkedMapOf(
    "panoramica" to "Panorámica", "plana" to "Plana", "urbana" to "Urbana", "sendero" to "Sendero",
    "montana" to "Montaña", "parque" to "Parque", "rio" to "Río",
)

enum class FiltroDistancia(val etiqueta: String) { Todas("Toda distancia"), Hasta5("Hasta 5 km"), De5a10("5-10 km"), Mas10("Más de 10 km") }

data class FiltrosRuta(
    val dificultad: Dificultad? = null,
    val distancia: FiltroDistancia = FiltroDistancia.Todas,
    val etiqueta: String? = null,
    val categoria: CategoriaRuta? = null,
)
