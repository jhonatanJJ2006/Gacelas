package com.gacelas.administracion.domain

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/*
 * Partes del panel que necesitaban datos nuevos (migracion 050): prioridad y SLA de los casos,
 * respuestas rapidas, SOS en vivo, ultimo acceso, versiones de la app e importaciones del ECU 911.
 */

/** Prioridad de un caso (content_reports.priority). Al crearse la decide el motivo en el servidor. */
enum class Prioridad(val clave: String, val etiqueta: String, val orden: Int) {
    Alta("alta", "Alta", 0),
    Media("media", "Media", 1),
    Baja("baja", "Baja", 2),
    ;

    companion object {
        fun desde(clave: String?): Prioridad = entries.firstOrNull { it.clave == clave } ?: Media
    }
}

/** Misma regla que moderacion.prioridad_por_motivo (050): sirve para la vista previa sin servidor. */
fun prioridadPorMotivo(motivo: String): Prioridad = when (motivo) {
    "acoso", "peligroso", "suplantacion" -> Prioridad.Alta
    "spam" -> Prioridad.Baja
    else -> Prioridad.Media
}

/** Horas objetivo de respuesta por prioridad (app_settings.sla_horas_moderacion). */
data class SlaHoras(val alta: Int = 4, val media: Int = 24, val baja: Int = 72) {
    fun horas(p: Prioridad): Int = when (p) {
        Prioridad.Alta -> alta
        Prioridad.Media -> media
        Prioridad.Baja -> baja
    }

    fun json(): JsonObject = JsonObject(mapOf("alta" to JsonPrimitive(alta), "media" to JsonPrimitive(media), "baja" to JsonPrimitive(baja)))

    val valido: Boolean get() = listOf(alta, media, baja).all { it in 1..720 }

    companion object {
        /** Lee el ajuste; si falta o esta mal formado usa los valores de la migracion 050. */
        fun desde(ajustes: List<AjusteApp>): SlaHoras {
            val o = ajustes.firstOrNull { it.clave == CLAVE_SLA }?.valor as? JsonObject ?: return SlaHoras()
            fun h(k: String, d: Int) = (o[k] as? JsonPrimitive)?.intOrNull ?: d
            return SlaHoras(h("alta", 4), h("media", 24), h("baja", 72))
        }
    }
}

const val CLAVE_SLA = "sla_horas_moderacion"

/** Estado del plazo de un caso abierto. [minutos] es lo que falta (positivo) o lo que se paso (negativo). */
sealed interface EstadoSla {
    val minutos: Long

    data class EnPlazo(override val minutos: Long) : EstadoSla
    data class PorVencer(override val minutos: Long) : EstadoSla
    data class Vencido(override val minutos: Long) : EstadoSla
}

/**
 * Plazo de un caso: vencido si ya paso [venceEnMs]; por vencer si queda menos de una cuarta parte
 * del plazo total (o menos de una hora). Los casos cerrados o sin vencimiento no tienen SLA.
 */
fun estadoSla(creadoEnMs: Long?, venceEnMs: Long?, ahoraMs: Long, abierto: Boolean): EstadoSla? {
    if (!abierto || venceEnMs == null) return null
    val restante = (venceEnMs - ahoraMs) / 60_000
    if (restante < 0) return EstadoSla.Vencido(restante)
    val total = if (creadoEnMs != null) (venceEnMs - creadoEnMs) / 60_000 else 0
    val margen = maxOf(60L, total / 4)
    return if (restante <= margen) EstadoSla.PorVencer(restante) else EstadoSla.EnPlazo(restante)
}

/** "3 h", "45 min", "2 d" para un numero de minutos (sin signo). */
fun duracionCorta(minutos: Long): String {
    val m = kotlin.math.abs(minutos)
    return when {
        m < 60 -> "$m min"
        m < 60 * 48 -> "${m / 60} h"
        else -> "${m / (60 * 24)} d"
    }
}

fun EstadoSla.texto(): String = when (this) {
    is EstadoSla.Vencido -> "Vencido hace ${duracionCorta(minutos)}"
    is EstadoSla.PorVencer -> "Vence en ${duracionCorta(minutos)}"
    is EstadoSla.EnPlazo -> "Vence en ${duracionCorta(minutos)}"
}

/**
 * Orden de la cola: primero lo abierto, luego lo que vence antes (sin vencimiento al final) y a
 * igualdad la prioridad mas alta. Los cerrados quedan como llegan (mas recientes primero).
 */
fun List<ReporteAdmin>.ordenarPorSla(): List<ReporteAdmin> =
    sortedWith(
        compareBy<ReporteAdmin> { if (it.estado.abierto) 0 else 1 }
            .thenBy { if (it.estado.abierto) it.venceEn ?: "9999" else "" }
            .thenBy { it.prioridad.orden },
    )

/** Plantilla de nota para el moderador (moderacion.respuestas_rapidas). */
data class RespuestaRapida(val id: String?, val titulo: String, val texto: String)

fun respuestaValida(titulo: String, texto: String): String? = when {
    titulo.isBlank() -> "Escribe un título"
    titulo.trim().length > 60 -> "El título admite hasta 60 caracteres"
    texto.isBlank() -> "Escribe el texto de la respuesta"
    texto.trim().length > 500 -> "El texto admite hasta 500 caracteres"
    else -> null
}

/** Punto geografico (lat, lng en grados). */
data class PuntoGeo(val lat: Double, val lng: Double)

/** Alerta SOS vista por el panel (admin_sos_alertas). La posicion solo llega mientras esta activa. */
data class AlertaSos(
    val id: String,
    val nombre: String,
    val activa: Boolean,
    val iniciadaEn: String,
    val terminadaEn: String?,
    val ultimaEn: String?,
    val posicion: PuntoGeo?,
    val puntos: Int,
)

/** Lo que dibuja el mapa del Resumen: corredores visibles, incidentes sin verificar y SOS activos. */
data class EscenarioVivo(
    val corredores: List<PuntoGeo>,
    val incidentes: List<PuntoGeo>,
    val sos: List<AlertaSos>,
) {
    val sosActivas: List<AlertaSos> get() = sos.filter { it.activa }
}

/** Recuadro de la ciudad de Loja (mismo que MapaEscenario.tsx de la web). */
object RecuadroLoja {
    const val NORTE = -3.93
    const val SUR = -4.05
    const val OESTE = -79.3
    const val ESTE = -79.11

    /** Posicion relativa (0..1, 0..1) dentro del recuadro, o null si cae fuera. */
    fun proyectar(p: PuntoGeo): Pair<Float, Float>? {
        val x = (p.lng - OESTE) / (ESTE - OESTE)
        val y = (NORTE - p.lat) / (NORTE - SUR)
        if (x !in 0.0..1.0 || y !in 0.0..1.0) return null
        return x.toFloat() to y.toFloat()
    }
}

/** Ultimo acceso de una cuenta (admin_ultimo_acceso). */
data class AccesoUsuario(val id: String, val ultimoAcceso: String?, val plataforma: String?, val version: String?)

/** Version publicada de la app (admin.versiones_app). */
data class VersionPublicada(
    val versionName: String,
    val versionCode: Int,
    val canal: String,
    val publicadaEn: String,
    val apkBytes: Long?,
) {
    val estable: Boolean get() = canal == "estable"
}

/** Usuarios por version en los ultimos dias (admin_versiones_en_uso). */
data class VersionEnUso(val plataforma: String, val versionName: String, val versionCode: Int?, val usuarios: Long)

/** Carga del ECU 911 (importaciones_ecu911). */
data class ImportacionEcu(
    val periodo: String,
    val archivo: String,
    val filas: Long,
    val filasLoja: Long,
    val ok: Boolean,
    val mensaje: String,
    val fecha: String,
)

/** "39,9 MB" para un tamano en bytes (MB de 1 000 000, como el limite de 52,4 MB). */
fun megas(bytes: Long?): String {
    if (bytes == null) return "-"
    val decimas = (bytes + 50_000) / 100_000
    return "${decimas / 10},${decimas % 10} MB"
}

/** Etiqueta de plataforma para el panel. */
fun etiquetaPlataforma(p: String?): String = when (p) {
    "android" -> "Android"
    "ios" -> "iOS"
    "web" -> "Web"
    null, "" -> ""
    else -> p
}

/**
 * CSV de Reportes, igual que csvEstadisticas de la web (apps/web/.../Estadisticas.tsx y
 * analitica/application/exportar.ts): serie diaria, separador punto y coma, decimales con coma,
 * BOM UTF-8 y fin de linea CRLF para que Excel lo abra con tildes.
 */
fun csvEstadisticas(e: EstadisticasAdmin): String {
    fun num(v: Double): String = (if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()).replace('.', ',')
    fun esc(s: String): String = if (s.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    val filas = listOf(listOf("Día", "Usuarios nuevos", "Corredores activos", "Carreras", "Km")) +
        e.serie.map { listOf(it.dia, num(it.nuevos), num(it.activos), num(it.carreras), num(it.km)) }
    return "\uFEFF" + filas.joinToString("\r\n") { f -> f.joinToString(";") { esc(it) } } + "\r\n"
}

fun nombreCsvEstadisticas(e: EstadisticasAdmin): String = "gacelas-estadisticas-${e.desde}-a-${e.hasta}.csv"
