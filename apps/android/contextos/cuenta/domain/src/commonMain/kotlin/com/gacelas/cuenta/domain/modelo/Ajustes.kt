package com.gacelas.cuenta.domain.modelo

import kotlinx.serialization.Serializable

@Serializable
enum class Visibilidad { Publico, Seguidores, Privado }

@Serializable
data class ConfigIntervalos(val trabajoSeg: Int = 60, val descansoSeg: Int = 30, val rondas: Int = 6, val vibracion: Boolean = true, val sonido: Boolean = true)

@Serializable
data class ConfigAvatar(val equipacion: Int = 0, val estilo: String = "Dinámico", val lentes: Boolean = false, val reloj: Boolean = true, val audifonos: Boolean = true)

/** Preferencias del usuario guardadas en el dispositivo. */
@Serializable
data class Ajustes(
    val modoOscuro: Boolean = true,
    val unidadesMetricas: Boolean = true,
    val visibilidad: Visibilidad = Visibilidad.Seguidores,
    val ubicacionPrecisa: Boolean = true,
    val ocultarHogar: Boolean = true,
    val radioHogarM: Int = 300,
    val notifLikes: Boolean = true,
    val notifComentarios: Boolean = true,
    val notifSeguidores: Boolean = true,
    val notifEnVivo: Boolean = true,
    val notifEventos: Boolean = true,
    val notifEmail: Boolean = false,
    val intervalos: ConfigIntervalos = ConfigIntervalos(),
    val metronomoBpm: Int = 168,
    val avatar: ConfigAvatar = ConfigAvatar(),
    val notificacionesLeidas: Set<String> = emptySet(),
    val likes: Set<String> = emptySet(),
    val comentarios: Map<String, List<String>> = emptyMap(),
    val ocultas: Set<String> = emptySet(),
    /** Carreras por semana para mantener la racha semanal. */
    val metaSemanal: Int = 3,
)

