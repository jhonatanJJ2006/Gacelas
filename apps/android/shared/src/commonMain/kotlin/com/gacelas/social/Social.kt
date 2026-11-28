package com.gacelas.social

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.reglas.Estadisticas
import com.gacelas.data.remoto.Backend
import com.gacelas.moderacion.domain.ObjetivoDenuncia
import com.gacelas.moderacion.domain.tablaDenuncia
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Categorias de la pantalla de logros. */
enum class CategoriaLogro(val titulo: String) {
    Distancia("Distancia"), Constancia("Constancia"), Velocidad("Velocidad"), Exploracion("Exploración"), Horario("Horario"),
}

/**
 * Catalogo unico de logros (reemplaza al de 20 logros y a las 12 medallas). Misma lista que
 * packages/shared-types/logros-catalogo.json, la tabla logros.catalogo_logros (migracion 049) y la web;
 * ParidadLogrosTest y src/__tests__/paridadLogros.test.ts de la web lo comprueban.
 * [meta] es lo que hay que alcanzar en la unidad de [progresoLogro] (y [unidad] la nombra) y
 * [comoConseguirlo] lo que se muestra en su estado vacio. El orden es el de las categorias.
 */
enum class Logro(
    val codigo: String, val titulo: String, val descripcion: String, val comoConseguirlo: String,
    val categoria: CategoriaLogro, val meta: Int, val unidad: String,
) {
    Km5("km_5", "5 km", "Corriste 5 km en una carrera.", "Completa 5 km en una sola carrera.", CategoriaLogro.Distancia, 5, "km"),
    Km10("km_10", "10 km", "Corriste 10 km en una carrera.", "Completa 10 km en una sola carrera.", CategoriaLogro.Distancia, 10, "km"),
    Km15("km_15", "15 km", "Corriste 15 km en una carrera.", "Completa 15 km en una sola carrera.", CategoriaLogro.Distancia, 15, "km"),
    Km21("km_21", "Media maratón", "Corriste 21,1 km en una carrera.", "Completa los 21,1 km de una media maratón en una sola carrera.", CategoriaLogro.Distancia, 21, "km"),
    Km42("km_42", "Maratón", "Corriste 42,2 km en una carrera.", "Completa los 42,2 km de un maratón en una sola carrera.", CategoriaLogro.Distancia, 42, "km"),
    Total50("total_50", "50 km acumulados", "Sumaste 50 km entre todas tus carreras.", "Suma 50 km entre todas tus carreras guardadas.", CategoriaLogro.Distancia, 50, "km"),
    Total100("total_100", "Centenario", "Sumaste 100 km entre todas tus carreras.", "Suma 100 km entre todas tus carreras guardadas.", CategoriaLogro.Distancia, 100, "km"),
    Total500("total_500", "Quinientos", "Sumaste 500 km entre todas tus carreras.", "Suma 500 km entre todas tus carreras guardadas.", CategoriaLogro.Distancia, 500, "km"),
    Total1000("total_1000", "Mil kilómetros", "Sumaste 1000 km entre todas tus carreras.", "Suma 1000 km entre todas tus carreras guardadas.", CategoriaLogro.Distancia, 1000, "km"),
    PrimeraCarrera("primera_carrera", "Primera carrera", "Grabaste tu primera carrera.", "Graba y guarda tu primera carrera con el botón Correr.", CategoriaLogro.Constancia, 1, "carrera"),
    Racha3("racha_3", "Racha de 3 días", "Corriste 3 días seguidos.", "Corre tres días seguidos.", CategoriaLogro.Constancia, 3, "días"),
    Racha7("racha_7", "Racha de 7 días", "Corriste 7 días seguidos.", "Corre al menos una vez cada día durante 7 días seguidos.", CategoriaLogro.Constancia, 7, "días"),
    Racha30("racha_30", "Racha de 30 días", "Corriste 30 días seguidos.", "Corre al menos una vez cada día durante un mes.", CategoriaLogro.Constancia, 30, "días"),
    Carreras10("carreras_10", "10 carreras", "Guardaste 10 carreras.", "Guarda 10 carreras en tu historial.", CategoriaLogro.Constancia, 10, "carreras"),
    Carreras50("carreras_50", "50 carreras", "Guardaste 50 carreras.", "Guarda 50 carreras en tu historial.", CategoriaLogro.Constancia, 50, "carreras"),
    SemanasMeta4("semanas_meta_4", "Mes constante", "Cumpliste tu meta semanal 4 semanas seguidas.", "Cumple tu meta de carreras por semana durante 4 semanas seguidas.", CategoriaLogro.Constancia, 4, "semanas"),
    Ritmo6("ritmo_6", "Bajo 6:00", "Corriste 3 km o más a menos de 6:00 por km.", "Corre al menos 3 km con un ritmo medio menor a 6:00 por km.", CategoriaLogro.Velocidad, 1, ""),
    Ritmo5("ritmo_5", "Bajo 5:00", "Corriste 3 km o más a menos de 5:00 por km.", "Corre al menos 3 km con un ritmo medio menor a 5:00 por km.", CategoriaLogro.Velocidad, 1, ""),
    Ritmo430("ritmo_430", "Bajo 4:30", "Corriste 5 km o más a menos de 4:30 por km.", "Corre al menos 5 km con un ritmo medio menor a 4:30 por km.", CategoriaLogro.Velocidad, 1, ""),
    Escalador("escalador", "Escalador", "Subiste 100 m en una carrera.", "Acumula 100 m de desnivel positivo en una sola carrera.", CategoriaLogro.Exploracion, 100, "m"),
    Explorador("explorador", "Explorador", "Empezaste carreras en 5 lugares distintos.", "Sal a correr desde 5 lugares distintos de la ciudad.", CategoriaLogro.Exploracion, 5, "lugares"),
    Desnivel1000("desnivel_1000", "Andino", "Acumulaste 1000 m de subida entre todas tus carreras.", "Acumula 1000 m de desnivel positivo entre tus carreras.", CategoriaLogro.Exploracion, 1000, "m"),
    Desnivel5000("desnivel_5000", "Cumbre", "Acumulaste 5000 m de subida entre todas tus carreras.", "Acumula 5000 m de desnivel positivo entre tus carreras.", CategoriaLogro.Exploracion, 5000, "m"),
    Madrugador("madrugador", "Madrugador", "Saliste a correr 3 veces antes de las 7:00.", "Empieza 3 carreras antes de las 7:00 de la mañana.", CategoriaLogro.Horario, 3, "salidas"),
    Nocturno("nocturno", "Corredor nocturno", "Saliste a correr 3 veces después de las 19:00.", "Empieza 3 carreras después de las 19:00.", CategoriaLogro.Horario, 3, "salidas"),
    FinDeSemana("fin_de_semana", "Guerrero de fin de semana", "Corriste 5 sábados o domingos.", "Corre 5 veces en sábado o domingo.", CategoriaLogro.Horario, 5, "salidas"),
}

/** Progreso hacia el logro, en su unidad (km, dias, carreras...), sin pasar de la meta. */
fun progresoLogro(l: Logro, carreras: List<Carrera>, ahoraMs: Long): Int {
    val max = carreras.maxOfOrNull { it.distanciaKm } ?: 0.0
    val total = carreras.sumOf { it.distanciaKm }
    val subida = carreras.sumOf { it.desnivelM }
    fun conRitmo(km: Double, seg: Int) = if (carreras.any { it.distanciaKm >= km && it.segundos / it.distanciaKm < seg }) 1 else 0
    val racha by lazy { com.gacelas.actividad.domain.reglas.Rachas.calcular(carreras, ahoraMs) }
    val v = when (l) {
        Logro.PrimeraCarrera, Logro.Carreras10, Logro.Carreras50 -> carreras.size
        Logro.Km5, Logro.Km10, Logro.Km15 -> max.toInt()
        Logro.Km21 -> if (max >= 21.0975) 21 else minOf(max.toInt(), 20)
        Logro.Km42 -> if (max >= 42.195) 42 else minOf(max.toInt(), 41)
        Logro.Total50, Logro.Total100, Logro.Total500, Logro.Total1000 -> total.toInt()
        // Las rachas cuentan la mejor racha, como el servidor: un logro ganado no se pierde al romperla.
        Logro.Racha3, Logro.Racha7, Logro.Racha30 -> racha.mejorDiaria
        Logro.SemanasMeta4 -> racha.mejorSemanal
        Logro.Ritmo6 -> conRitmo(3.0, 360)
        Logro.Ritmo5 -> conRitmo(3.0, 300)
        Logro.Ritmo430 -> conRitmo(5.0, 270)
        Logro.Escalador -> carreras.maxOfOrNull { it.desnivelM } ?: 0
        Logro.Desnivel1000, Logro.Desnivel5000 -> subida
        Logro.Explorador -> carreras.mapNotNull { c -> c.puntos.firstOrNull()?.let { (it.lat * 100).toInt() to (it.lng * 100).toInt() } }.toSet().size
        Logro.Madrugador -> carreras.count { Estadisticas.hora(it.inicioMs) < 7 }
        Logro.Nocturno -> carreras.count { Estadisticas.hora(it.inicioMs) >= 19 }
        Logro.FinDeSemana -> carreras.count { Estadisticas.diaSemana(it.inicioMs) >= 5 }
    }
    return v.coerceIn(0, l.meta)
}

/** Reglas de logros a partir del historial de carreras. */
fun logrosCumplidos(carreras: List<Carrera>, ahoraMs: Long): List<Logro> =
    Logro.entries.filter { progresoLogro(it, carreras, ahoraMs) >= it.meta }

// Compatibilidad: el modelo de denuncias vive en el contexto Moderacion.
typealias TipoContenido = com.gacelas.moderacion.domain.TipoContenido
val MOTIVOS_REPORTE = com.gacelas.moderacion.domain.MOTIVOS_REPORTE
fun motivosReporte(tipo: TipoContenido) = com.gacelas.moderacion.domain.motivosReporte(tipo)

/** Logros desbloqueados (dispositivo y tabla user_achievements) y reportes (tabla post_reports). */
class SocialRepository(
    private val store: DataStore<Preferences>,
    private val backend: Backend? = null,
    private val uid: suspend () -> String? = { null },
    private val reloj: () -> Long = { com.gacelas.shared.ahoraMs() },
) {
    private val clave = stringPreferencesKey("logros")
    /** El dispositivo ya paso al catalogo unico (logros y medallas juntos, migracion 049). */
    private val claveUnificado = booleanPreferencesKey("logros_unificado")

    /** Codigo a fecha (ms) de cada logro desbloqueado. */
    suspend fun desbloqueados(): Map<String, Long> =
        store.data.first()[clave].orEmpty().split(';').mapNotNull { e ->
            val p = e.split('='); if (p.size == 2) p[0] to (p[1].toLongOrNull() ?: 0L) else null
        }.toMap()

    /**
     * Guarda los logros nuevos y devuelve los que se acaban de ganar. La primera vez que se llama
     * (sin registro previo) y la primera vez con el catalogo unico (lo ganado antes, incluidas las
     * antiguas medallas) los guarda sin devolverlos, para no saturar con modales.
     */
    suspend fun desbloquearNuevos(carreras: List<Carrera>): List<Logro> {
        val prefs = store.data.first()
        val previo = prefs[clave]
        val unificado = prefs[claveUnificado] == true
        val ya = desbloqueados()
        val ahora = reloj()
        val nuevos = logrosCumplidos(carreras, ahora).filter { it.codigo !in ya }
        // La lista de carreras llega vacia mientras carga: no marcar el paso al catalogo unico con ella.
        val marcar = carreras.isNotEmpty() || previo == null
        if (nuevos.isEmpty() && previo != null && (unificado || !marcar)) return emptyList()
        val todo = ya + nuevos.associate { it.codigo to ahora }
        store.edit {
            it[clave] = todo.entries.joinToString(";") { (k, v) -> "$k=$v" }
            if (marcar) it[claveUnificado] = true
        }
        val id = uid()
        // Uno por uno: el servidor valida cada codigo con las carreras verificadas y un rechazo no debe tirar el resto.
        if (id != null) nuevos.forEach { l ->
            runCatching {
                backend?.upsert("user_achievements", listOf(
                    JsonObject(mapOf("user_id" to JsonPrimitive(id), "code" to JsonPrimitive(l.codigo))),
                ), "user_id,code")
            }
        }
        return if (previo == null || !unificado) emptyList() else nuevos
    }

    /** Envia el reporte; false sin servidor o sin sesion. */
    suspend fun reportar(postId: String, motivo: String, detalle: String, tipo: TipoContenido = TipoContenido.Post): Boolean =
        denunciar(tipo.objetivo(postId), motivo, detalle)

    /** Envia una denuncia sobre cualquier objetivo registrado en report_target_types. */
    suspend fun denunciar(o: ObjetivoDenuncia, motivo: String, detalle: String): Boolean {
        val b = backend ?: return false
        val id = uid() ?: return false
        return runCatching {
            val tabla = tablaDenuncia(o)
            val objetivo = if (tabla == "post_reports") mapOf("post_id" to JsonPrimitive(o.id))
            else mapOf("target_type" to JsonPrimitive(o.tipo), "target_id" to JsonPrimitive(o.id))
            b.insert(tabla, listOf(JsonObject(objetivo + mapOf(
                "reporter_id" to JsonPrimitive(id), "reason" to JsonPrimitive(motivo), "detail" to JsonPrimitive(detalle.trim()),
            ))))
            true
        }.getOrElse { it.message?.contains("23505") == true }
    }
}
