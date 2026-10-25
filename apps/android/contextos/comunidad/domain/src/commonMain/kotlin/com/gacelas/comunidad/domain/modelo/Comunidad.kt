package com.gacelas.comunidad.domain.modelo

import com.gacelas.carreras.domain.modelo.PuntoGps
import com.gacelas.shared.ahoraMs
import kotlinx.serialization.Serializable

@Serializable
data class Evento(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    val hora: String,
    val ubicacion: String,
    val participantes: Int,
    val distanciaKm: Int,
    val pasado: Boolean = false,
    val creadoPorUsuario: Boolean = false,
    val inicioMs: Long? = null,
    val cupo: Int? = null,
    val tipo: String = "grupal",
    val clubId: String? = null,
)

enum class TipoNotificacion { Like, Comentario, Seguidor, Evento, EnVivo, Racha, Reporte }

data class Notificacion(val id: String, val tipo: TipoNotificacion, val usuario: String, val mensaje: String, val tiempo: String, val leida: Boolean)

enum class RolClub(val etiqueta: String) { owner("Fundador"), admin("Admin"), member("Miembro") }

data class Club(val id: String, val nombre: String, val descripcion: String, val ubicacion: String, val conAprobacion: Boolean, val miembros: Int)

data class MiembroClub(val usuarioId: String, val nombre: String, val rol: RolClub, val activo: Boolean)

data class PostClub(val id: String, val usuarioId: String, val nombre: String, val texto: String)

data class EstadoEvento(val inscritos: Int, val enEspera: Int, val cupo: Int?, val miEstado: String?, val miPosicion: Int?)

data class CorredorEvento(
    val usuarioId: String, val nombre: String, val distanciaM: Double, val segundos: Int,
    val lat: Double?, val lng: Double?, val oculto: Boolean, val terminado: Boolean, val puntos: List<Pair<Double, Double>>,
)

data class DiaCalendario(val dia: Int, val delMes: Boolean, val eventos: List<Evento>, val inicioMs: Long)

@Serializable
enum class PestanaEventos(val etiqueta: String) { Proximos("Próximos"), MisEventos("Mis eventos"), Pasados("Pasados") }

data class Coordenada(val lat: Double, val lng: Double)

/** Sesion en vivo tal como la ve un espectador (misma forma que la web, lib/enVivo.ts). */
data class SesionEnVivo(
    val id: String,
    val userId: String,
    val nombre: String,
    val publica: Boolean,
    val inicioMs: Long,
    val finMs: Long?,
    /** Posicion oculta mientras esta cerca de casa. */
    val oculta: Boolean,
    val ultimaMs: Long?,
    val posicion: Coordenada?,
    val puntos: List<Coordenada>,
    val distanciaKm: Double,
    val segundos: Int,
) {
    fun activa(ahoraMs: Long = ahoraMs()) = finMs == null && ahoraMs - (ultimaMs ?: inicioMs) < 15 * 60_000L
}

/** Publicacion del feed: actividad de la comunidad o carrera publica propia. */
data class Publicacion(
    val id: String,
    val autor: String,
    val propia: Boolean,
    val indice: Int,
    val cuando: String,
    val titulo: String,
    val distanciaKm: Double,
    val segundos: Int,
    val ritmoSegKm: Int?,
    val likesBase: Int,
    val comentariosBase: Int,
    val puntos: List<PuntoGps> = emptyList(),
    /** Id del autor en Supabase (publicaciones reales). */
    val autorId: String? = null,
    /** true si viene de la base: likes y comentarios se guardan en post_likes/post_comments. */
    val remota: Boolean = false,
    /** Me gusta del usuario actual (solo remotas). */
    val meGusta: Boolean = false,
    val inicioMs: Long = 0L,
    val clienteId: String? = null,
    /** Desnivel positivo (m). */
    val desnivelM: Int = 0,
    /** URLs publicas de las fotos. */
    val imagenes: List<String> = emptyList(),
    val compartidos: Int = 0,
    val compartida: Boolean = false,
    /** Km publicos acumulados del autor (nivel); null si no viene del servidor. */
    val autorKm: Double? = null,
    /** Ultimo comentario (autor, texto) para la vista previa. */
    val ultimoComentario: Pair<String, String>? = null,
)

/** Nivel del corredor segun km acumulados (misma regla que la web nivelCorredor). */
fun nivelCorredor(km: Double): String = when {
    km >= 500 -> "Gacela Élite"
    km >= 150 -> "Gacela Avanzada"
    km >= 30 -> "Gacela Intermedia"
    else -> "Gacela Principiante"
}

/** Comentario de una publicacion del feed (post_comments). */
data class ComentarioFeed(val id: String, val autorId: String, val autor: String, val texto: String)

enum class FiltroFeed(val etiqueta: String) { Todos("Para ti"), Siguiendo("Siguiendo"), Comunidad("Comunidad"), Mias("Mis carreras") }
