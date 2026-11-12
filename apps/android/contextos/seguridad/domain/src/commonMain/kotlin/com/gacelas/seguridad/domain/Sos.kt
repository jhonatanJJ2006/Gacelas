package com.gacelas.seguridad.domain

/* Reglas del boton SOS (ticket 86e3uf097), iguales a la web (apps/web/src/contexts/seguridad/domain/sos.ts). */
object Sos {
    /** Numero unico de emergencias en Ecuador (ECU 911). */
    const val NUMERO_EMERGENCIA = "911"
    const val MAX_CONTACTOS = 3
    /** Hay que mantener pulsado el boton este tiempo para activarlo (evita toques accidentales). */
    const val MANTENER_MS = 3_000L
    /** Cada cuanto se manda la posicion mientras el SOS esta activo. */
    const val ENVIO_CADA_MS = 10_000L
    const val SITIO = "https://gacelas.vercel.app"

    /** Deja solo digitos y el + inicial. Moviles de Ecuador (09...) pasan a +5939... */
    fun normalizarTelefono(t: String): String {
        val limpio = t.trim().filterIndexed { i, ch -> ch.isDigit() || (ch == '+' && i == t.trim().indexOf('+') && i == 0) }
        return when {
            Regex("^09\\d{8}$").matches(limpio) -> "+593" + limpio.drop(1)
            Regex("^5939\\d{8}$").matches(limpio) -> "+$limpio"
            else -> limpio
        }
    }

    fun telefonoValido(t: String) = normalizarTelefono(t).removePrefix("+").let { it.length in 7..15 && it.all(Char::isDigit) }

    fun validar(nombre: String, telefono: String): String? = when {
        nombre.trim().length < 2 -> "Escribe el nombre del contacto"
        !telefonoValido(telefono) -> "Escribe un teléfono válido, por ejemplo 0991234567"
        else -> null
    }

    fun enlace(id: String) = "$SITIO/sos/$id"

    fun mensaje(nombre: String, id: String, lat: Double?, lng: Double?): String {
        val ubicacion = if (lat != null && lng != null) " Última posición: https://maps.google.com/?q=${fmt(lat)},${fmt(lng)}" else ""
        return "SOS de Runcity: $nombre necesita ayuda. Sigue su ubicación en vivo: ${enlace(id)}.$ubicacion Si no responde, llama al $NUMERO_EMERGENCIA."
    }

    private fun fmt(v: Double): String {
        val escala = 1_000_000.0
        val r = kotlin.math.round(v * escala) / escala
        return r.toString()
    }
}
