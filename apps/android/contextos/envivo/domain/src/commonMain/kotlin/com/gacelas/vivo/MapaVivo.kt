package com.gacelas.vivo

/** Un corredor en el mapa en vivo. */
data class CorredorEnMapa(
    val userId: String,
    val nombre: String,
    val avatarUrl: String?,
    val lat: Double,
    val lng: Double,
    val corriendo: Boolean,
    val actualizadoMs: Long,
)

object MapaVivo {
    /** Posiciones con mas de 2 minutos se ocultan (tambien lo aplica el RLS). */
    const val CADUCIDAD_MS = 2 * 60_000L
    /** Publicar como mucho cada 15 s corriendo y cada 60 s sin correr. */
    const val INTERVALO_CORRIENDO_MS = 15_000L
    const val INTERVALO_QUIETO_MS = 60_000L

    fun visibles(lista: List<CorredorEnMapa>, ahoraMs: Long, yo: String?): List<CorredorEnMapa> =
        lista.filter { it.userId != yo && ahoraMs - it.actualizadoMs <= CADUCIDAD_MS }

    fun tocaPublicar(ultimoMs: Long?, ultimoCorriendo: Boolean?, ahoraMs: Long, corriendo: Boolean): Boolean {
        if (ultimoMs == null || ultimoCorriendo != corriendo) return true
        return ahoraMs - ultimoMs >= if (corriendo) INTERVALO_CORRIENDO_MS else INTERVALO_QUIETO_MS
    }
}

/** Puerto del mapa en vivo; la implementacion vive en infrastructure y la cablea ContenedorApp. */
interface MapaVivoRepositorio {
    /** Publica la posicion si toca segun los intervalos; true si se envio. */
    suspend fun publicar(lat: Double, lng: Double, corriendo: Boolean): Boolean
    suspend fun dejarDeCompartir()
    suspend fun listar(): List<CorredorEnMapa>
}
