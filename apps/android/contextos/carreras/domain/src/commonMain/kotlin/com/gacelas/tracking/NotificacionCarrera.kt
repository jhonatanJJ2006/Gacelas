package com.gacelas.tracking

import com.gacelas.carreras.domain.EstadoCarrera

import com.gacelas.shared.fmt

import kotlinx.coroutines.flow.MutableStateFlow

/** Texto de la notificacion de la carrera (logica pura, probada en unit tests). */
data class ContenidoNotificacion(val titulo: String, val texto: String, val accionPausa: String, val cronometroCorre: Boolean)

object NotificacionCarrera {
    fun contenido(e: EstadoCarrera): ContenidoNotificacion = ContenidoNotificacion(
        titulo = if (e.pausada) "Carrera en pausa" else "Carrera en curso",
        texto = "%.2f km  ·  %s  ·  %s /km".fmt(e.distanciaKm, formatearHms(e.segundos), formatearRitmo(e.ritmoActualSegKm ?: e.ritmoPromedioSegKm)),
        accionPausa = if (e.pausada) "Reanudar" else "Pausar",
        cronometroCorre = !e.pausada,
    )
}

/** Pantalla a abrir cuando se toca la notificacion o su boton Terminar (la consume el NavHost). */
object DestinoPendiente {
    const val EXTRA = "com.gacelas.DESTINO"
    /** Boton Terminar: se abre la app (Android 14 no deja abrir actividades desde un servicio). */
    const val TERMINAR = "com.gacelas.TERMINAR"
    val valor = MutableStateFlow<String?>(null)
}
