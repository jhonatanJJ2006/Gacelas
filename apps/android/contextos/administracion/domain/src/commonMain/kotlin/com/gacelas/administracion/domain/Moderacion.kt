package com.gacelas.administracion.domain

import com.gacelas.moderacion.domain.TipoContenido

/** Estados de un caso (content_reports.status, migracion 023). */
enum class EstadoReporte(val clave: String, val etiqueta: String) {
    Pendiente("pendiente", "Pendientes"),
    EnRevision("en_revision", "En revisión"),
    Resuelto("resuelto", "Resueltos"),
    Descartado("descartado", "Descartados"),
    ;

    val abierto: Boolean get() = this == Pendiente || this == EnRevision

    companion object {
        fun desde(clave: String): EstadoReporte = entries.firstOrNull { it.clave == clave } ?: Pendiente
    }
}

/** Acciones que acepta public.moderar_reporte (mismos valores que la web). */
enum class AccionModeracion(val clave: String, val historial: String) {
    EnRevision("en_revision", "Tomó el caso"),
    Resuelto("resuelto", "Resolvió"),
    Descartado("descartado", "Descartó"),
    Reabierto("reabierto", "Reabrió"),
    OcultarContenido("ocultar_contenido", "Ocultó el contenido"),
    SuspenderUsuario("suspender_usuario", "Suspendió al autor"),
    Nota("nota", "Nota"),
    ;

    companion object {
        fun desde(clave: String): AccionModeracion? = entries.firstOrNull { it.clave == clave }
    }
}

data class AccionReporte(val accion: String, val nota: String, val fecha: String, val moderador: String) {
    val texto: String get() = AccionModeracion.desde(accion)?.historial ?: accion
}

/** Caso de la cola (public.admin_cola_reportes). */
data class ReporteAdmin(
    val id: String,
    val tipo: String,
    val objetivoId: String,
    val motivo: String,
    val detalle: String,
    val estado: EstadoReporte,
    val creadoEn: String,
    val reportante: String,
    val resumen: String,
    val totalReportes: Long,
    val acciones: List<AccionReporte>,
)

/** Etiqueta del tipo de objetivo, igual que la web (definicionTipo). */
fun etiquetaTipo(tipo: String): String = when (tipo) {
    "post" -> "Publicación"
    "usuario" -> "Usuario"
    "comentario" -> "Comentario"
    "evento" -> "Evento"
    "incidente" -> "Incidente"
    "comunidad" -> "Comunidad"
    "grupo" -> "Grupo"
    else -> tipo.replaceFirstChar { it.uppercase() }.replace('_', ' ')
}

/** Texto del boton para ocultar segun el tipo (en usuarios se suspende al autor). */
fun textoOcultar(tipo: String): String = when (TipoContenido.desde(tipo)) {
    TipoContenido.Post -> "Ocultar publicación"
    TipoContenido.Comentario -> "Ocultar comentario"
    TipoContenido.Evento -> "Ocultar evento"
    TipoContenido.Usuario -> "Suspender cuenta"
    else -> "Ocultar contenido"
}

/**
 * Acciones disponibles para un caso, en el mismo orden que la web: abierto -> revisar (solo
 * pendiente), ocultar o suspender, descartar; cerrado -> reabrir. "Solo nota" si hay nota.
 */
fun accionesPermitidas(r: ReporteAdmin, hayNota: Boolean): List<AccionModeracion> = buildList {
    if (r.estado.abierto) {
        if (r.estado == EstadoReporte.Pendiente) add(AccionModeracion.EnRevision)
        add(if (r.tipo == "usuario") AccionModeracion.SuspenderUsuario else AccionModeracion.OcultarContenido)
        add(AccionModeracion.Descartado)
    } else {
        add(AccionModeracion.Reabierto)
    }
    if (hayNota) add(AccionModeracion.Nota)
}

/** Etiqueta de un boton de accion. */
fun etiquetaAccion(a: AccionModeracion, tipo: String): String = when (a) {
    AccionModeracion.EnRevision -> "Revisar"
    AccionModeracion.OcultarContenido, AccionModeracion.SuspenderUsuario -> textoOcultar(tipo)
    AccionModeracion.Descartado -> "Descartar"
    AccionModeracion.Reabierto -> "Reabrir"
    AccionModeracion.Nota -> "Solo nota"
    AccionModeracion.Resuelto -> "Resolver"
}

val AccionModeracion.destructiva: Boolean get() = this == AccionModeracion.OcultarContenido || this == AccionModeracion.SuspenderUsuario
