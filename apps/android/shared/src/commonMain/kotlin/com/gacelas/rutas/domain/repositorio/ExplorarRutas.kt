package com.gacelas.rutas.domain.repositorio

import com.gacelas.rutas.domain.modelo.ResenaRuta
import com.gacelas.rutas.domain.modelo.Ruta
import kotlinx.coroutines.flow.StateFlow

/** Rutas de la comunidad (lectura y acciones sociales). La implementa data.ExplorarRepository. */
interface ExplorarRutas {
    /** null hasta cargar o si el servidor falla. */
    val rutas: StateFlow<List<Ruta>?>
    val meGusta: StateFlow<Set<String>>
    suspend fun refrescar()
    suspend fun cercanas(lat: Double, lng: Double): Map<String, Double>
    suspend fun resenas(id: String): List<ResenaRuta>
    suspend fun fotos(id: String): List<String>
    /** (nombre, veces) visibles y total de personas. */
    suspend fun corredores(id: String): Pair<List<Pair<String, Int>>, Int>
}

/** Publicar rutas dibujadas (o de una actividad). La implementa data.ExplorarRepository. */
interface PublicadorRutas {
    suspend fun crear(
        nombre: String, descripcion: String, dificultad: com.gacelas.rutas.domain.modelo.Dificultad, etiquetas: List<String>,
        puntos: List<Pair<Double, Double>>, elevacionM: Int, actividadId: String?, perfil: List<Int> = emptyList(), origen: String = "actividad",
    ): String
    /** true si la ruta quedo en el celular esperando red. */
    suspend fun pendiente(id: String): Boolean
}
