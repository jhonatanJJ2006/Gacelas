package com.gacelas.shared

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.HttpTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Utilidades multiplataforma que sustituyen java.util / java.net / android.util. */

fun ahoraMs(): Long = Clock.System.now().toEpochMilliseconds()

/** Reloj monotono (no cambia con la hora del sistema), en ms. */
expect fun relojMonotonoMs(): Long

expect fun logAviso(etiqueta: String, mensaje: String)

fun rad(grados: Double): Double = grados * PI / 180.0
fun grados(rad: Double): Double = rad * 180.0 / PI
/** Igual que Math.round(double). */
fun redondear(x: Double): Long = kotlin.math.floor(x + 0.5).toLong()

@OptIn(ExperimentalUuidApi::class)
fun nuevoUuid(): String = Uuid.random().toString()

/** Nombre simple de la clase (sustituye javaClass.simpleName). */
fun nombreClase(o: Any): String = o::class.simpleName ?: "?"

/**
 * printf minimo con Locale.US: %d %s %f %.Nf %0Nd %02x %%. Suficiente para los formatos de la app
 * (ritmos, distancias, coordenadas, hashes) sin String.format de Java.
 */
fun String.fmt(vararg args: Any?): String {
    val r = StringBuilder(); var i = 0; var n = 0
    while (i < length) {
        val c = this[i]
        if (c != '%') { r.append(c); i++; continue }
        i++
        if (this[i] == '%') { r.append('%'); i++; continue }
        var cero = false; var ancho = 0; var prec = -1
        if (this[i] == '0') { cero = true; i++ }
        while (this[i].isDigit()) { ancho = ancho * 10 + (this[i] - '0'); i++ }
        if (this[i] == '.') { i++; prec = 0; while (this[i].isDigit()) { prec = prec * 10 + (this[i] - '0'); i++ } }
        val tipo = this[i]; i++
        val a = args[n++]
        val s = when (tipo) {
            'd' -> (a as Number).toLong().toString()
            'x' -> ((a as Number).toInt() and 0xff).toString(16)
            'f' -> decimales((a as Number).toDouble(), if (prec < 0) 6 else prec)
            else -> a.toString()
        }
        val relleno = if (cero) '0' else ' '
        if (s.length < ancho) {
            if (cero && s.startsWith("-")) r.append('-').append(s.drop(1).padStart(ancho - 1, relleno))
            else r.append(s.padStart(ancho, relleno))
        } else r.append(s)
    }
    return r.toString()
}

/** Numero con [dec] decimales, redondeo HALF_UP como String.format. */
fun decimales(x: Double, dec: Int): String {
    if (x.isNaN()) return "NaN"
    var f = 1L; repeat(dec) { f *= 10 }
    val neg = x < 0
    val total = (abs(x) * f + 0.5 + 1e-9).let { kotlin.math.floor(it) }.toLong()
    val ent = total / f; val fr = total % f
    val txt = if (dec == 0) "$ent" else "$ent." + fr.toString().padStart(dec, '0')
    return if (neg && total != 0L) "-$txt" else txt
}

/** SHA-256 en hex (sustituye java.security.MessageDigest). */
fun sha256Hex(texto: String): String = sha256(texto.encodeToByteArray()).joinToString("") { "%02x".fmt(it) }

fun sha256(datos: ByteArray): ByteArray {
    val k = intArrayOf(
        0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b, 0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
        -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
        -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039, -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
        -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d, -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8, -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e,
    )
    val h = intArrayOf(0x6a09e667, -0x4498517b, 0x3c6ef372, -0x5ab00ac6, 0x510e527f, -0x64fa9774, 0x1f83d9ab, 0x5be0cd19)
    val bits = datos.size.toLong() * 8
    val largo = ((datos.size + 9 + 63) / 64) * 64
    val m = ByteArray(largo); datos.copyInto(m); m[datos.size] = 0x80.toByte()
    for (j in 0 until 8) m[largo - 1 - j] = (bits ushr (8 * j)).toByte()
    val w = IntArray(64)
    for (b in 0 until largo step 64) {
        for (t in 0 until 16) w[t] = ((m[b + 4 * t].toInt() and 0xff) shl 24) or ((m[b + 4 * t + 1].toInt() and 0xff) shl 16) or
            ((m[b + 4 * t + 2].toInt() and 0xff) shl 8) or (m[b + 4 * t + 3].toInt() and 0xff)
        for (t in 16 until 64) {
            val s0 = w[t - 15].rotateRight(7) xor w[t - 15].rotateRight(18) xor (w[t - 15] ushr 3)
            val s1 = w[t - 2].rotateRight(17) xor w[t - 2].rotateRight(19) xor (w[t - 2] ushr 10)
            w[t] = w[t - 16] + s0 + w[t - 7] + s1
        }
        var a = h[0]; var bb = h[1]; var c = h[2]; var d = h[3]; var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
        for (t in 0 until 64) {
            val t1 = hh + (e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)) + ((e and f) xor (e.inv() and g)) + k[t] + w[t]
            val t2 = (a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)) + ((a and bb) xor (a and c) xor (bb and c))
            hh = g; g = f; f = e; e = d + t1; d = c; c = bb; bb = a; a = t1 + t2
        }
        h[0] += a; h[1] += bb; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh
    }
    return ByteArray(32) { (h[it / 4] ushr (24 - 8 * (it % 4))).toByte() }
}

/** Calendario local (lunes = inicio de semana), sustituye java.util.Calendar. */
object Fechas {
    var zona: () -> TimeZone = { TimeZone.currentSystemDefault() }

    fun local(ms: Long, z: TimeZone = zona()): LocalDateTime = Instant.fromEpochMilliseconds(ms).toLocalDateTime(z)
    fun fecha(ms: Long, z: TimeZone = zona()): LocalDate = local(ms, z).date
    fun inicio(d: LocalDate, z: TimeZone = zona()): Long = d.atStartOfDayIn(z).toEpochMilliseconds()
    fun inicioDia(ms: Long, z: TimeZone = zona()): Long = inicio(fecha(ms, z), z)
    /** Mismo reloj de pared n dias despues (como Calendar.add(DAY_OF_MONTH)). */
    fun sumarDias(ms: Long, n: Int, z: TimeZone = zona()): Long {
        val l = local(ms, z)
        val d = l.date.plus(DatePeriod(days = n))
        return LocalDateTime(d, l.time).toInstant(z).toEpochMilliseconds()
    }
    /** 0 = lunes ... 6 = domingo. */
    fun diaSemana(ms: Long, z: TimeZone = zona()): Int = fecha(ms, z).dayOfWeek.ordinal
    fun lunes(d: LocalDate): LocalDate = d.plus(DatePeriod(days = -(d.dayOfWeek.ordinal)))
    fun hora(ms: Long, z: TimeZone = zona()): Int = local(ms, z).hour
}

/** ISO 8601 en UTC sin milisegundos, p. ej. 2026-10-10T10:00:00Z. */
fun isoUtc(ms: Long): String = Instant.fromEpochSeconds(ms.floorDiv(1000L)).toString()

/** Cliente HTTP compartido (motor OkHttp en Android, Darwin en iOS). */
object Red {
    val cliente by lazy { HttpClient { install(HttpTimeout) { requestTimeoutMillis = 8000; connectTimeoutMillis = 8000 } } }

    suspend fun texto(url: String, sinCache: Boolean = false): String {
        val r = cliente.get(url) {
            if (sinCache) { header("Cache-Control", "no-cache"); header("Pragma", "no-cache") }
        }
        if (r.status.value != 200) error("HTTP ${r.status.value}")
        return r.bodyAsText()
    }
}
