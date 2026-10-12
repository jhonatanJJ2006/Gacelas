package com.gacelas.rutas

import com.gacelas.data.mock.MockData
import com.gacelas.data.mock.Ruta
import com.gacelas.data.rutas.Guia
import com.gacelas.ui.components.Mapas

/** Ruta lista para navegar: con su trazo completo (sale de la copia local, sin red). */
data class RutaNavegable(
    val id: String,
    val nombre: String,
    val distanciaKm: Double,
    val minutos: Int,
    val elevacionM: Int,
    val trazo: List<Pair<Double, Double>>,
    val principal: Boolean = false,
    /** Ruta de la comunidad (id real): se registra la corrida al guardar la carrera. */
    val real: Boolean = true,
)

/** Minutos estimados a 6 min/km. */
private fun minutos(km: Double) = (km * 6).toInt().coerceAtLeast(1)

private fun Ruta.navegable(): RutaNavegable? = puntos.takeIf { it.size >= 2 }?.let { t ->
    // Se navega sobre el trazo: su largo manda. Antes Jipiro decia 5.2 km (dato de la ficha) y la guia "faltan 1.8 km".
    val km = Guia.longitudM(t) / 1000.0
    RutaNavegable(id, nombre, km, minutos(km), elevacionM, t)
}

/**
 * Rutas que se pueden navegar, en este orden: la elegida en el detalle, las guardadas, las propias,
 * el resto de la copia local o del servidor y, al final, las de ejemplo de Loja.
 */
fun rutasNavegables(remotas: List<Ruta>?, locales: List<Ruta>, guardadas: Set<String>, elegida: String? = null): List<RutaNavegable> {
    val todas = LinkedHashMap<String, Ruta>()
    (locales + remotas.orEmpty()).forEach { todas[it.id] = it }
    if (todas.isEmpty()) MockData.rutas.forEach { todas[it.id] = it }
    val ordenadas = todas.values.sortedWith(compareBy({ it.id != elegida }, { it.id !in guardadas }, { !it.propia }))
    val ejemplos = MockData.rutasSugeridas.mapNotNull { s ->
        Mapas.trazos[s.id]?.let { t -> Guia.longitudM(t.map { it.latitude to it.longitude }).div(1000.0).let { km -> RutaNavegable(s.id, s.nombre, km, minutos(km), s.elevacionM, t.map { it.latitude to it.longitude }, s.principal, real = false) } }
    }
    return (ordenadas.mapNotNull { it.navegable() } + ejemplos).distinctBy { it.id }
}
