package com.gacelas.comunidad.domain.reglas

import com.gacelas.comunidad.domain.modelo.CorredorEvento
import com.gacelas.comunidad.domain.modelo.DiaCalendario
import com.gacelas.comunidad.domain.modelo.EstadoEvento
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.FiltroFeed
import com.gacelas.comunidad.domain.modelo.MiembroClub
import com.gacelas.comunidad.domain.modelo.Notificacion
import com.gacelas.comunidad.domain.modelo.PestanaEventos
import com.gacelas.comunidad.domain.modelo.Publicacion
import com.gacelas.comunidad.domain.modelo.RolClub
import com.gacelas.comunidad.domain.modelo.TipoNotificacion
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.shared.Fechas
import com.gacelas.shared.ahoraMs
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

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
fun calendarioMes(anio: Int, mes: Int, eventos: List<Evento>, zona: TimeZone = TimeZone.currentSystemDefault()): List<List<DiaCalendario>> {
    val porDia = eventos.filter { it.inicioMs != null }.groupBy { e -> Fechas.fecha(e.inicioMs!!, zona) }
    var d = Fechas.lunes(LocalDate(anio, mes + 1, 1))
    val semanas = mutableListOf<List<DiaCalendario>>()
    do {
        semanas += (0 until 7).map {
            DiaCalendario(d.dayOfMonth, d.monthNumber == mes + 1, porDia[d].orEmpty(), Fechas.inicio(d, zona))
                .also { d = d.plus(DatePeriod(days = 1)) }
        }
    } while (d.monthNumber == mes + 1)
    return semanas
}

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

/** Notificaciones visibles segun los ajustes de notificaciones y las ya leidas. */
fun notificacionesVisibles(a: Ajustes, base: List<Notificacion>): List<Notificacion> = base
    .filter {
        when (it.tipo) {
            TipoNotificacion.Like -> a.notifLikes
            TipoNotificacion.Comentario -> a.notifComentarios
            TipoNotificacion.Seguidor -> a.notifSeguidores
            TipoNotificacion.Evento -> a.notifEventos
            TipoNotificacion.EnVivo -> a.notifEnVivo
            TipoNotificacion.Racha, TipoNotificacion.Reporte -> true
        }
    }
    .map { if (it.id in a.notificacionesLeidas) it.copy(leida = true) else it }

/** Filtra notificaciones segun la pestana activa. */
fun filtrarNotificaciones(lista: List<Notificacion>, filtro: String): List<Notificacion> = when (filtro) {
    "Sin leer" -> lista.filter { !it.leida }
    "En vivo" -> lista.filter { it.tipo == TipoNotificacion.EnVivo }
    else -> lista
}

fun filtrarFeed(lista: List<Publicacion>, filtro: FiltroFeed, ocultas: Set<String>, siguiendo: Set<String> = emptySet()): List<Publicacion> = lista
    .filter { it.id !in ocultas }
    .filter {
        when (filtro) {
            FiltroFeed.Todos -> true
            FiltroFeed.Siguiendo -> it.autorId != null && it.autorId in siguiendo
            FiltroFeed.Comunidad -> !it.propia
            FiltroFeed.Mias -> it.propia
        }
    }

/** Une eventos de la base con los de ejemplo sin duplicar ids (gana la base). */
fun unirEventos(base: List<Evento>, ejemplo: List<Evento>): List<Evento> {
    val ids = base.map { it.id }.toSet()
    return base + ejemplo.filter { it.id !in ids }
}
