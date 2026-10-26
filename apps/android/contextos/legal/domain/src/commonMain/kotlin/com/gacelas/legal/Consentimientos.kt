package com.gacelas.legal

import kotlinx.serialization.Serializable

/** Tipos de consentimiento (columna consents.kind en Supabase). */
enum class TipoConsentimiento(val clave: String) {
    Terminos("terminos"), Privacidad("privacidad"), GpsCarrera("gps_carrera"), UbicacionVisible("ubicacion_visible"),
}

@Serializable
data class Consentimiento(val tipo: String, val version: String, val otorgado: Boolean, val fechaMs: Long)

/** Versiones vigentes de los documentos legales; si cambian se vuelven a pedir. */
object VersionesLegales {
    const val TERMINOS = "2026-10-25"
    const val PRIVACIDAD = "2026-10-25"
    const val UBICACION = "2026-10-25"

    fun de(t: TipoConsentimiento) = when (t) {
        TipoConsentimiento.Terminos -> TERMINOS
        TipoConsentimiento.Privacidad -> PRIVACIDAD
        else -> UBICACION
    }
}

/** Ultimo estado de cada tipo segun el historial (el mas reciente manda). */
fun vigente(historial: List<Consentimiento>, tipo: TipoConsentimiento): Consentimiento? =
    historial.filter { it.tipo == tipo.clave }.maxByOrNull { it.fechaMs }

/** True si hay que volver a pedir los terminos (nunca aceptados o version distinta). */
fun requiereAceptarTerminos(historial: List<Consentimiento>): Boolean {
    val t = vigente(historial, TipoConsentimiento.Terminos)
    return t == null || !t.otorgado || t.version != VersionesLegales.TERMINOS
}

fun otorgado(historial: List<Consentimiento>, tipo: TipoConsentimiento) = vigente(historial, tipo)?.otorgado == true
