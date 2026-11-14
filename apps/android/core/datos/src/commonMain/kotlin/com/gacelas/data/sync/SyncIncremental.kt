package com.gacelas.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.data.ValorJson
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.FechaIso
import com.gacelas.data.remoto.texto
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Arranque incremental: solo bajan filas posteriores a la ultima sync que termino bien.
 *
 * La marca es el reloj del servidor menos [MARGEN_SYNC_MS]. Ese margen vuelve a leer lo
 * recien escrito, por si una fila se confirmo mientras se leia, y no usa el reloj del
 * telefono. La primera vez, otro usuario, el cierre de sesion o la recuperacion manual
 * no tienen marca: ahi la descarga es completa.
 *
 * La cache del historial sigue siendo la de siempre (hallazgo A6). Aqui solo se agrega
 * la marca, una clave chica.
 */

/** Filas de tablas chicas por pagina. Por debajo del max_rows = 1000 de PostgREST. */
const val PAGINA_SYNC = 200

/** Los recorridos GPS pesan: paginas mas chicas para no armar una respuesta enorme. */
const val PAGINA_RECORRIDOS = 20

/** Se resta al reloj del servidor al guardar la marca. */
const val MARGEN_SYNC_MS = 5 * 60 * 1000L

enum class ModoDescarga { COMPLETA, INCREMENTAL }

/** Ultima sync exitosa de una cuenta. [marca] es un instante ISO exclusivo (se pide lo posterior). */
@Serializable
data class MarcaSync(val userId: String, val marca: String)

/** Una pagina de PostgREST ordenada por (marca, clave), con la clave de la pagina anterior. */
data class ConsultaPagina(
    val tabla: String,
    val igual: Map<String, String> = emptyMap(),
    /** Columnas que tienen que ser NULL (el catalogo de eventos borrados). */
    val nulos: Set<String> = emptySet(),
    val columnaMarca: String = "updated_at",
    val marcaExclusiva: String? = null,
    val columnaClave: String = "id",
    val claveExclusiva: String? = null,
    val limite: Int = PAGINA_SYNC,
)

/** DELETE anotado en sync_borrados. */
data class BorradoSync(val tabla: String, val clave: String, val marcaMs: Long)

/** Alta o cambio, con la marca en milisegundos para desempatar contra un borrado. */
data class CambioClave(val clave: String, val marcaMs: Long)

class MarcasSync(store: DataStore<Preferences>) {
    private val valor = ValorJson(store, "marca_sync", MarcaSync.serializer().nullable, null)
    suspend fun leer(): MarcaSync? = valor.flujo.first()
    suspend fun guardar(marca: MarcaSync) { valor.actualizar { marca } }
    suspend fun borrar() { valor.actualizar { null } }
}

/** Completa si no hay marca, si la marca es de otra cuenta, o si se pide a proposito. */
fun modoDescarga(marca: MarcaSync?, userId: String, forzarCompleta: Boolean = false): ModoDescarga =
    if (forzarCompleta || marca == null || marca.userId != userId) ModoDescarga.COMPLETA else ModoDescarga.INCREMENTAL

/** Reloj del servidor menos el margen, en ISO. Null si el reloj no se pudo leer. */
fun marcaSiguiente(relojIso: String?, margenMs: Long = MARGEN_SYNC_MS): String? {
    val ms = relojIso?.let(FechaIso::leer) ?: return null
    return FechaIso.formatear(ms - margenMs)
}

fun comparaClave(a: String, b: String): Int {
    val na = a.toLongOrNull()
    val nb = b.toLongOrNull()
    return if (na != null && nb != null) na.compareTo(nb) else a.compareTo(b)
}

/** Instantes ISO se comparan por tiempo; el resto (ids, created_at de prueba) como texto o numero. */
fun comparaMarca(a: String, b: String): Int {
    val ia = FechaIso.leer(a)
    val ib = FechaIso.leer(b)
    return if (ia != null && ib != null) ia.compareTo(ib) else comparaClave(a, b)
}

/** True si (marca, clave) queda estrictamente despues del cursor de la pagina. */
fun esPosterior(marca: String?, clave: String?, desdeMarca: String?, desdeClave: String?): Boolean {
    if (desdeMarca == null) return true
    if (marca == null) return false
    val cmp = comparaMarca(marca, desdeMarca)
    if (cmp > 0) return true
    if (cmp < 0 || desdeClave == null || clave == null) return false
    return comparaClave(clave, desdeClave) > 0
}

/**
 * Lee todas las paginas. La primera usa [ConsultaPagina.marcaExclusiva]; las siguientes
 * avanzan con la ultima fila para no saltarse empates de la misma marca.
 */
suspend fun bajarTodo(leer: suspend (ConsultaPagina) -> List<JsonObject>, base: ConsultaPagina): List<JsonObject> {
    val salida = mutableListOf<JsonObject>()
    var marca = base.marcaExclusiva
    var clave = base.claveExclusiva
    repeat(10_000) {
        val pagina = leer(base.copy(marcaExclusiva = marca, claveExclusiva = clave))
        if (pagina.isEmpty()) return salida
        salida += pagina
        if (pagina.size < base.limite) return salida
        val ultima = pagina.last()
        val nuevaMarca = ultima.texto(base.columnaMarca) ?: return salida
        val nuevaClave = ultima.texto(base.columnaClave) ?: return salida
        if (nuevaMarca == marca && nuevaClave == clave) return salida
        marca = nuevaMarca
        clave = nuevaClave
    }
    return salida
}

fun marcaMs(f: JsonObject, columna: String = "updated_at"): Long =
    f.texto(columna)?.let(FechaIso::leer) ?: 0L

fun borradoDesdeFila(f: JsonObject): BorradoSync? {
    val tabla = f.texto("tabla") ?: return null
    val clave = f.texto("clave") ?: return null
    val ms = f.texto("deleted_at")?.let(FechaIso::leer) ?: return null
    return BorradoSync(tabla, clave, ms)
}

fun borradosDe(lista: List<BorradoSync>, tabla: String): List<CambioClave> =
    lista.filter { it.tabla == tabla }.map { CambioClave(it.clave, it.marcaMs) }

/** Borrados propios y los del catalogo (events) posteriores a la marca. Null si la red fallo. */
suspend fun leerBorrados(backend: Backend, userId: String, desde: String): List<BorradoSync>? = runCatching {
    val propios = bajarTodo(backend::selectPagina, ConsultaPagina(
        tabla = "sync_borrados", igual = mapOf("user_id" to userId),
        columnaMarca = "deleted_at", marcaExclusiva = desde, columnaClave = "id",
    ))
    val catalogo = bajarTodo(backend::selectPagina, ConsultaPagina(
        tabla = "sync_borrados", igual = mapOf("tabla" to "events"), nulos = setOf("user_id"),
        columnaMarca = "deleted_at", marcaExclusiva = desde, columnaClave = "id",
    ))
    (propios + catalogo).mapNotNull(::borradoDesdeFila)
}.getOrNull()

/**
 * Une altas y bajas. En completa, el servidor manda el conjunto entero.
 * Si la misma clave se actualizo y se borro, gana el evento mas nuevo; empate, el borrado.
 */
fun fusionarClaves(local: Set<String>, altas: List<CambioClave>, bajas: List<CambioClave>, completa: Boolean): Set<String> {
    if (completa) return altas.map { it.clave }.toSet()
    val ganador = mutableMapOf<String, Pair<Long, Boolean>>()
    for (a in altas) {
        val prev = ganador[a.clave]
        if (prev == null || a.marcaMs > prev.first) ganador[a.clave] = a.marcaMs to false
    }
    for (b in bajas) {
        val prev = ganador[b.clave]
        if (prev == null || b.marcaMs >= prev.first) ganador[b.clave] = b.marcaMs to true
    }
    val salida = local.toMutableSet()
    for ((clave, ev) in ganador) if (ev.second) salida.remove(clave) else salida.add(clave)
    return salida
}

/**
 * Lista con id (eventos). Completa reemplaza. Incremental aplica cambios y borrados
 * sin tirar lo que no vino en esta pagina.
 */
fun <T> fusionarFilas(
    local: List<T>,
    idDe: (T) -> String,
    cambios: List<Pair<T, Long>>,
    borrados: List<CambioClave>,
    completa: Boolean,
): List<T> {
    if (completa) return cambios.map { it.first }
    val orden = local.map(idDe).toMutableList()
    val map = local.associateBy(idDe).toMutableMap()
    val marcas = mutableMapOf<String, Long>()
    for ((item, ms) in cambios) {
        val id = idDe(item)
        val baja = borrados.filter { it.clave == id }.maxOfOrNull { it.marcaMs }
        if (baja != null && baja >= ms) continue
        if (id !in map) orden += id
        map[id] = item
        marcas[id] = ms
    }
    for (b in borrados) {
        val ms = marcas[b.clave]
        if (ms == null || b.marcaMs >= ms) {
            map.remove(b.clave)
            orden.remove(b.clave)
        }
    }
    return orden.mapNotNull { map[it] }
}

/** Cambio de carrera. [traePuntos] false conserva el recorrido que ya estaba en el telefono. */
data class CambioCarrera(val carrera: Carrera, val marcaMs: Long, val traePuntos: Boolean)

/**
 * Historial local mas lo que cambio. Los pendientes de subir ganan sobre el servidor:
 * una carrera recien guardada sin red no se pisa, y una eliminada en el telefono no vuelve.
 */
fun fusionarCarreras(
    local: List<Carrera>,
    cambios: List<CambioCarrera>,
    recorridosSueltos: Map<String, List<PuntoGps>>,
    borrados: List<CambioClave>,
    completa: Boolean,
    pendientesGuardar: List<Carrera>,
    pendientesBorrar: Set<String>,
): List<Carrera> {
    val map = linkedMapOf<String, Carrera>()
    if (!completa) local.forEach { map[it.id] = it }
    val marcas = mutableMapOf<String, Long>()
    for (c in cambios) {
        val baja = borrados.filter { it.clave == c.carrera.id }.maxOfOrNull { it.marcaMs }
        if (!completa && baja != null && baja >= c.marcaMs) continue
        val previa = map[c.carrera.id]
        val puntos = when {
            c.traePuntos -> c.carrera.puntos
            c.carrera.id in recorridosSueltos -> recorridosSueltos.getValue(c.carrera.id)
            previa != null -> previa.puntos
            else -> c.carrera.puntos
        }
        map[c.carrera.id] = c.carrera.copy(puntos = puntos)
        marcas[c.carrera.id] = c.marcaMs
    }
    if (!completa) {
        for ((id, pts) in recorridosSueltos) {
            if (cambios.none { it.carrera.id == id }) map[id]?.let { map[id] = it.copy(puntos = pts) }
        }
        for (b in borrados) {
            val ms = marcas[b.clave]
            if (ms == null || b.marcaMs >= ms) map.remove(b.clave)
        }
    }
    for (c in pendientesGuardar) map[c.id] = c
    for (id in pendientesBorrar) map.remove(id)
    return map.values.sortedByDescending { it.inicioMs }
}

/** Lee el reloj de sync_reloj(). Null si la funcion no esta o no hay red. */
suspend fun leerReloj(backend: Backend): String? = runCatching {
    val el = backend.rpc("sync_reloj", JsonObject(emptyMap()))
    (el as? JsonPrimitive)?.content
}.getOrNull()
