package com.gacelas.comunidad.domain.reglas

import com.gacelas.comunidad.domain.modelo.BorradorClub
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.CorredorEvento
import com.gacelas.comunidad.domain.modelo.NivelGrupo
import com.gacelas.comunidad.domain.modelo.TipoClub
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
fun accionClub(yo: MiembroClub?, conAprobacion: Boolean, tipo: TipoClub = TipoClub.grupo, lleno: Boolean = false): String? = when {
    yo == null -> if (lleno) "Grupo lleno" else if (conAprobacion) "Solicitar unirme" else "Unirme"
    !yo.activo -> "Solicitud enviada"
    yo.rol == RolClub.owner -> null
    else -> if (tipo == TipoClub.comunidad) "Salir de la comunidad" else "Salir del grupo"
}

/** Limites de creacion por usuario (los mismos que aplica el trigger club_proteger). */
val LIMITE_CREACION = mapOf(TipoClub.comunidad to 3, TipoClub.grupo to 5)
const val LIMITE_DIARIO = 3
val DIAS_CORTOS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

fun estaLleno(c: Club) = c.cupo != null && c.miembros >= c.cupo

/** Nombre comparable: sin espacios repetidos ni mayusculas (indice clubs_nombre_unico). */
fun normalizarNombre(n: String) = n.trim().replace(Regex("\\s+"), " ").lowercase()

/** "6:30" o "6:30 min/km" a segundos; null si no es valido. */
fun leerRitmo(t: String): Int? {
    val m = Regex("^(\\d{1,2}):([0-5]\\d)").find(t.trim()) ?: return null
    return m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
}

fun textoRitmo(s: Int?) = s?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')} min/km" }

/** "Mar y Sáb · 06:00" con los dias ISO 1..7. */
fun textoHorario(dias: List<Int>, hora: String?): String {
    val d = dias.sorted().mapNotNull { DIAS_CORTOS.getOrNull(it - 1) }
    val ds = when (d.size) { 0 -> ""; 1 -> d[0]; else -> d.dropLast(1).joinToString(", ") + " y " + d.last() }
    val h = hora?.take(5)?.ifBlank { null }
    return listOfNotNull(ds.ifBlank { null }, h).joinToString(" · ").ifBlank { "Sin horario fijo" }
}

fun validarClub(nombre: String, descripcion: String): String? = when {
    nombre.trim().length < 3 -> "El nombre debe tener al menos 3 letras"
    nombre.trim().length > 60 -> "El nombre es demasiado largo"
    descripcion.length > 500 -> "La descripción es demasiado larga"
    else -> null
}

/** Valida el borrador como la web y la base: nombre unico por tipo, ritmo, cupo y hora. */
fun validarClub(b: BorradorClub, existentes: List<Club>, editandoId: String? = null): String? {
    validarClub(b.nombre, b.descripcion)?.let { return it }
    if (b.ciudad.trim().isEmpty()) return "Indica la ciudad o zona"
    val n = normalizarNombre(b.nombre)
    if (existentes.any { it.tipo == b.tipo && it.id != editandoId && normalizarNombre(it.nombre) == n })
        return "Ya existe ${if (b.tipo == TipoClub.comunidad) "una comunidad" else "un grupo"} con ese nombre"
    if (b.tipo == TipoClub.grupo) {
        if (b.ritmo.isNotBlank()) { val r = leerRitmo(b.ritmo) ?: return "Escribe el ritmo como 6:30"; if (r !in 150..900) return "El ritmo debe estar entre 2:30 y 15:00 min/km" }
        if (b.cupo.isNotBlank()) { val c = b.cupo.trim().toIntOrNull() ?: return "El cupo debe ser un número"; if (c !in 2..1000) return "El cupo debe estar entre 2 y 1000" }
        if (b.hora.isNotBlank() && !Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(b.hora.trim())) return "Escribe la hora como 06:00"
    }
    return null
}

/** Filtra por tipo, texto (nombre, descripcion, ciudad, punto) y nivel. */
fun filtrarClubes(c: List<Club>, tipo: TipoClub, q: String, nivel: NivelGrupo? = null): List<Club> {
    val t = q.trim().lowercase()
    return c.filter { it.tipo == tipo }
        .filter { nivel == null || it.nivel == nivel }
        .filter { t.isEmpty() || listOf(it.nombre, it.descripcion, it.ciudad, it.lugarEncuentro).any { s -> s.lowercase().contains(t) } }
}

fun borradorDesdeClub(c: Club) = BorradorClub(
    c.tipo, c.nombre, c.descripcion, c.ciudad, c.contacto, c.conAprobacion, c.comunidadId, c.lugarEncuentro, c.lat, c.lng,
    c.dias, c.hora?.take(5).orEmpty(), c.nivel, c.ritmoSegKm?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" }.orEmpty(), c.cupo?.toString().orEmpty(),
)

/** Traduce los errores de la base (trigger club_proteger e indice unico) a texto para el usuario. */
fun mensajeErrorClub(m: String?): String = when {
    m == null -> "No se pudo guardar"
    m.contains("clubs_nombre_unico") || m.contains("23505") -> "Ya existe uno con ese nombre"
    m.contains("cupo", ignoreCase = true) -> "El grupo está lleno"
    else -> m
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
