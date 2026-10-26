package com.gacelas.kernel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Kernel compartido de los bounded contexts (docs/ARQUITECTURA-DDD.md, seccion 4).
 * Solo tipos sin dependencias de plataforma ni de infraestructura.
 */

/** Resultado de un caso de uso: exito con valor o fallo con un error de dominio. */
sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>
    data class Fallo(val error: ErrorDominio) : Resultado<Nothing>
}

/** Error de negocio con un codigo estable y un mensaje para mostrar. */
data class ErrorDominio(val codigo: String, val mensaje: String)

/** Evento de dominio publicado por un contexto. */
interface EventoDominio {
    /** Nombre en pasado, por ejemplo "actividad.CarreraTerminada". */
    val nombre: String
    val ocurridoEnMs: Long
}

/** Bus de eventos en memoria para comunicar contextos dentro de la app. */
class BusEventos {
    private val flujo = MutableSharedFlow<EventoDominio>(extraBufferCapacity = 64)
    val eventos: SharedFlow<EventoDominio> = flujo.asSharedFlow()

    fun publicar(evento: EventoDominio): Boolean = flujo.tryEmit(evento)
}
