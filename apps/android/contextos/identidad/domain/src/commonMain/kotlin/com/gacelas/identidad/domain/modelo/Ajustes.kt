package com.gacelas.identidad.domain.modelo

import kotlinx.serialization.Serializable

@Serializable
enum class Visibilidad { Publico, Seguidores, Privado }

@Serializable
data class ConfigIntervalos(val trabajoSeg: Int = 60, val descansoSeg: Int = 30, val rondas: Int = 6, val vibracion: Boolean = true, val sonido: Boolean = true)

@Serializable
data class ConfigAvatar(val equipacion: Int = 0, val estilo: String = "Dinámico", val lentes: Boolean = false, val reloj: Boolean = true, val audifonos: Boolean = true,
    /** Rasgos guardados; el modelo GLB actual tiene una sola textura y aun no los pinta. */
    val complexion: String? = null, val tonoPiel: Int? = null, val peinado: String? = null, val colorShorts: Int? = null,
)

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
    /** Ritmo objetivo del pacer en segundos por km. */
    val ritmoObjetivoSeg: Int = 330,
    val autopausa: Boolean = true,
    val anunciosVoz: Boolean = true,
    /** Segundos de cuenta regresiva antes de arrancar el GPS (0 = sin cuenta). */
    val cuentaRegresivaSeg: Int = 3,
    val idioma: String = "es-EC",
    val ocultarBiometricos: Boolean = false,
    val aparecerRankings: Boolean = true,
    /** Nivel que el corredor declaro al registrarse. */
    val nivelDeclarado: String = "principiante",
    /** Datos fisiologicos privados (solo los lee el dueno). */
    val pesoKg: Double? = null,
    val alturaCm: Int? = null,
    val distanciaFavorita: String = "10K",
    val metaKmSemana: Int = 25,
    /** Compartir la ubicacion en vivo al correr: null = aun no se pregunto (modal la primera vez). */
    val compartirEnVivo: Boolean? = null,
)

