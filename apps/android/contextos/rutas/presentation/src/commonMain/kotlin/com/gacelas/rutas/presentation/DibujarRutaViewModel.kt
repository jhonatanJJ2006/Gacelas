package com.gacelas.rutas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.repositorio.PublicadorRutas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Ruta que se esta dibujando punto a punto (lat, lng). */
data class DibujoRuta(
    val puntos: List<Pair<Double, Double>> = emptyList(),
    val nombre: String = "",
    val descripcion: String = "",
    val dificultad: Dificultad = Dificultad.Moderado,
    val etiquetas: Set<String> = emptySet(),
    val guardando: Boolean = false,
) {
    val km: Double get() = puntos.zipWithNext().sumOf { (a, b) -> haversineKm(a, b) }
}

/** Mismo calculo que tenia DibujarRuta en Android (radio 6371 km). */
fun haversineKm(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
    fun rad(g: Double) = g * PI / 180.0
    val dLat = rad(b.first - a.first); val dLng = rad(b.second - a.second)
    val h = sin(dLat / 2).let { it * it } + cos(rad(a.first)) * cos(rad(b.first)) * sin(dLng / 2).let { it * it }
    return 2 * 6371.0 * asin(sqrt(h))
}

/**
 * Dibujar y publicar una ruta. [publicador] es null sin sesion/servidor; [perfil] calcula la elevacion
 * de los puntos (data.Elevacion) y [desnivel] el desnivel positivo del perfil.
 */
class DibujarRutaViewModel(
    private val publicador: PublicadorRutas?,
    private val perfil: suspend (List<Pair<Double, Double>>) -> List<Int>,
    private val desnivel: (List<Int>) -> Int,
) : ViewModel() {
    private val _estado = MutableStateFlow(DibujoRuta())
    val estado: StateFlow<DibujoRuta> = _estado.asStateFlow()
    private inline fun cambiar(f: (DibujoRuta) -> DibujoRuta) { _estado.value = f(_estado.value) }

    fun agregar(lat: Double, lng: Double) = cambiar { it.copy(puntos = it.puntos + (lat to lng)) }
    fun deshacer() = cambiar { it.copy(puntos = it.puntos.dropLast(1)) }
    fun borrar() = cambiar { it.copy(puntos = emptyList()) }
    fun nombre(t: String) = cambiar { it.copy(nombre = t.take(80)) }
    fun descripcion(t: String) = cambiar { it.copy(descripcion = t.take(500)) }
    fun dificultad(d: Dificultad) = cambiar { it.copy(dificultad = d) }
    fun alternarEtiqueta(k: String) = cambiar { it.copy(etiquetas = if (k in it.etiquetas) it.etiquetas - k else it.etiquetas + k) }

    /** Valida y publica; [onAviso] recibe el mensaje y [onCreada] el id de la ruta nueva. */
    fun publicar(onAviso: (String) -> Unit, onCreada: (String) -> Unit) {
        val e = _estado.value
        val p = publicador
        when {
            p == null -> onAviso("Inicia sesión para publicar rutas")
            e.nombre.trim().length < 3 -> onAviso("Ponle un nombre de al menos 3 letras")
            e.puntos.size < 2 -> onAviso("Toca el mapa al menos dos veces")
            else -> {
                cambiar { it.copy(guardando = true) }
                viewModelScope.launch {
                    val alturas = perfil(e.puntos)
                    runCatching { p.crear(e.nombre.trim(), e.descripcion.trim(), e.dificultad, e.etiquetas.toList(), e.puntos, desnivel(alturas), null, alturas, "dibujada") }
                        .onSuccess { onAviso(if (p.pendiente(it)) "Sin conexión: la ruta quedó en el celular y se publicará al volver la red" else "Ruta publicada"); onCreada(it) }
                        .onFailure { onAviso(it.message ?: "No se pudo publicar"); cambiar { s -> s.copy(guardando = false) } }
                }
            }
        }
    }
}
