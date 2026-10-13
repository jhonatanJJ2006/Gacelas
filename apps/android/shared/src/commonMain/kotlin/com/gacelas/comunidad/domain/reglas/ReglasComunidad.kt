package com.gacelas.comunidad.domain.reglas

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.ErrorRemoto
import com.gacelas.data.remoto.FechaIso
import com.gacelas.comunidad.data.eventoDesdeFila
import com.gacelas.shared.Fechas
import com.gacelas.shared.ahoraMs
import com.gacelas.shared.nuevoUuid
import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import com.gacelas.comunidad.domain.modelo.*
import com.gacelas.comunidad.domain.repositorio.*

fun puedeGestionar(rol: RolClub?) = rol == RolClub.owner || rol == RolClub.admin

/** Texto del boton principal segun mi relacion con el club (igual que la web). */
fun accionClub(yo: MiembroClub?, conAprobacion: Boolean): String? = when {
    yo == null -> if (conAprobacion) "Solicitar unirme" else "Unirme"
    !yo.activo -> "Solicitud enviada"
    yo.rol == RolClub.owner -> null
    else -> "Salir del club"
}

fun validarClub(nombre: String, descripcion: String): String? = when {
    nombre.trim().length < 3 -> "El nombre debe tener al menos 3 letras"
    nombre.trim().length > 60 -> "El nombre es demasiado largo"
    descripcion.length > 500 -> "La descripción es demasiado larga"
    else -> null
}

fun textoCupo(e: EstadoEvento): String {
    val cupo = e.cupo ?: return "${e.inscritos} inscritos"
    val libres = maxOf(cupo - e.inscritos, 0)
    return if (libres > 0) "${e.inscritos}/$cupo · $libres ${if (libres == 1) "cupo libre" else "cupos libres"}" else "Lleno · ${e.enEspera} en espera"
}

fun clasificacion(c: List<CorredorEvento>) = c.sortedWith(compareByDescending<CorredorEvento> { it.distanciaM }.thenBy { it.segundos })

fun posicionRepeticion(c: CorredorEvento, t: Float): Pair<Double, Double>? =
    if (c.puntos.isEmpty()) null else c.puntos[minOf(c.puntos.lastIndex, (t * c.puntos.lastIndex).toInt())]

/** Semanas (lunes a domingo) del mes con los eventos de cada dia. */
fun calendarioMes(anio: Int, mes: Int, eventos: List<Evento>, zona: kotlinx.datetime.TimeZone = kotlinx.datetime.TimeZone.currentSystemDefault()): List<List<DiaCalendario>> {
    // mes base 0 (como Calendar.MONTH).
    val porDia = eventos.filter { it.inicioMs != null }.groupBy { e -> Fechas.fecha(e.inicioMs!!, zona) }
    var d = Fechas.lunes(kotlinx.datetime.LocalDate(anio, mes + 1, 1))
    val semanas = mutableListOf<List<DiaCalendario>>()
    do {
        semanas += (0 until 7).map {
            DiaCalendario(d.dayOfMonth, d.monthNumber == mes + 1, porDia[d].orEmpty(), Fechas.inicio(d, zona))
                .also { d = d.plus(kotlinx.datetime.DatePeriod(days = 1)) }
        }
    } while (d.monthNumber == mes + 1)
    return semanas
}

/**
 * Si el evento ya empezo, con la hora de ahora. `pasado` venia fijo (datos de ejemplo) o calculado al
 * descargar, asi que el evento Jipiro del sabado 11 de octubre seguia en "Proximos". inicioMs es un
 * instante absoluto (America/Guayaquil = UTC-5 en el servidor), no depende de la zona del telefono.
 */
fun Evento.yaPaso(ahoraMs: Long = ahoraMs()): Boolean = inicioMs?.let { it < ahoraMs } ?: pasado

fun filtrarEventos(eventos: List<Evento>, pestana: PestanaEventos, inscritos: Set<String>, ahoraMs: Long = ahoraMs()): List<Evento> = when (pestana) {
    PestanaEventos.Proximos -> eventos.filter { !it.yaPaso(ahoraMs) }
    PestanaEventos.MisEventos -> eventos.filter { it.id in inscritos || it.creadoPorUsuario }
    PestanaEventos.Pasados -> eventos.filter { it.yaPaso(ahoraMs) }
}

/** "hace 5 min", "hace 3h", "hace 2d" (igual que la web). */
fun haceCuanto(ms: Long?, ahora: Long = ahoraMs()): String {
    if (ms == null) return ""
    val min = ((ahora - ms) / 60_000).coerceAtLeast(0)
    return when {
        min < 1 -> "ahora"
        min < 60 -> "hace $min min"
        min < 60 * 24 -> "hace ${min / 60}h"
        else -> "hace ${min / (60 * 24)}d"
    }
}
