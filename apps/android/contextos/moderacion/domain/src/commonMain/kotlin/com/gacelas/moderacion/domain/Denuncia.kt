package com.gacelas.moderacion.domain

/**
 * Objetivo de una denuncia. El tipo es una clave de report_target_types (migracion 023), asi que
 * otro contexto (por ejemplo incidentes) se suma registrando su clave sin cambiar este modulo.
 */
data class ObjetivoDenuncia(val tipo: String, val id: String)

val MOTIVOS_REPORTE = listOf(
    "spam" to "Spam o publicidad", "ofensivo" to "Contenido ofensivo", "acoso" to "Acoso o intimidación",
    "falso" to "Carrera falsa o trampa", "otro" to "Otro motivo",
)

/** Motivos para tipos que la app no conoce todavia. */
val MOTIVOS_GENERICOS = listOf(
    "spam" to "Spam o publicidad", "ofensivo" to "Contenido ofensivo", "falso" to "Información falsa", "otro" to "Otro motivo",
)

/** Tipos que registran las migraciones 023 y 031. Post usa post_reports por compatibilidad; el resto, content_reports. */
enum class TipoContenido(val clave: String, val titulo: String) {
    Post("post", "Reportar publicación"), Usuario("usuario", "Reportar corredor"),
    Comentario("comentario", "Reportar comentario"), Evento("evento", "Reportar evento"),
    /** Incidentes de corredores (contexto Seguridad de rutas, migraciones 030 y 031). */
    Incidente("incidente", "Reportar incidente"),
    ;

    fun objetivo(id: String) = ObjetivoDenuncia(clave, id)

    companion object {
        fun desde(clave: String): TipoContenido? = entries.firstOrNull { it.clave == clave }
    }
}

/** Motivos validos por tipo, igual que la web (contexts/moderacion/domain/denuncia.ts). */
fun motivosReporte(tipo: TipoContenido): List<Pair<String, String>> = when (tipo) {
    TipoContenido.Post -> MOTIVOS_REPORTE
    TipoContenido.Usuario -> listOf(
        "suplantacion" to "Suplantación de identidad", "acoso" to "Acoso o intimidación", "spam" to "Spam o cuenta falsa",
        "ofensivo" to "Perfil ofensivo", "otro" to "Otro motivo",
    )
    TipoContenido.Comentario -> listOf(
        "ofensivo" to "Contenido ofensivo", "acoso" to "Acoso o intimidación", "spam" to "Spam o publicidad", "otro" to "Otro motivo",
    )
    TipoContenido.Evento -> listOf(
        "peligroso" to "Ruta o evento peligroso", "falso" to "Evento falso o engañoso", "spam" to "Spam o publicidad",
        "ofensivo" to "Contenido ofensivo", "otro" to "Otro motivo",
    )
    TipoContenido.Incidente -> listOf(
        "falso" to "Incidente falso o exagerado", "ofensivo" to "Descripción o fotos ofensivas",
        "spam" to "Spam o publicidad", "otro" to "Otro motivo",
    )
}

/** Motivos para cualquier clave, conocida o no. */
fun motivosReporte(clave: String): List<Pair<String, String>> =
    TipoContenido.desde(clave)?.let { motivosReporte(it) } ?: MOTIVOS_GENERICOS

/** Tabla donde se guarda la denuncia; las publicaciones siguen en post_reports para las apps publicadas. */
fun tablaDenuncia(o: ObjetivoDenuncia) = if (o.tipo == TipoContenido.Post.clave) "post_reports" else "content_reports"
