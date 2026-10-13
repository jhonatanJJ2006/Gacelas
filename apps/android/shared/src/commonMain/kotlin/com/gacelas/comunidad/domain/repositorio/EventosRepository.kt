package com.gacelas.comunidad.domain.repositorio

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.shared.ahoraMs
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import com.gacelas.comunidad.domain.modelo.*
import com.gacelas.comunidad.domain.reglas.*

/** Eventos creados por el usuario e inscripciones. Hoy local; preparado para Supabase. */
interface EventosRepository {
    val creados: Flow<List<Evento>>
    val inscritos: Flow<Set<String>>
    suspend fun crear(evento: Evento)
    suspend fun alternarInscripcion(id: String)
}

/** Notificaciones de la cuenta (servidor). La implementa data.NotificacionesRepository. */
interface FuenteNotificaciones {
    val lista: kotlinx.coroutines.flow.StateFlow<List<Notificacion>>
    suspend fun refrescar()
    suspend fun marcarLeidas(ids: Collection<String>)
}

/** Clubes: lista con los mios. La implementa data.ClubesRepository. */
interface FuenteClubes {
    suspend fun listar(): Pair<List<Club>, Set<String>>
    /** Id del usuario con sesion; null sin sesion. */
    suspend fun miId(): String? = null
    suspend fun crear(nombre: String, descripcion: String, ubicacion: String, conAprobacion: Boolean): String = throw UnsupportedOperationException()
    suspend fun detalle(id: String): Club? = null
    suspend fun miembros(id: String): List<MiembroClub> = emptyList()
    suspend fun unirse(id: String) {}
    suspend fun salir(id: String, usuario: String? = null) {}
    suspend fun aprobar(id: String, usuario: String) {}
    suspend fun cambiarRol(id: String, usuario: String, rol: RolClub) {}
    suspend fun posts(id: String): List<PostClub> = emptyList()
    suspend fun publicar(id: String, texto: String) {}
    suspend fun eventos(id: String): List<Evento> = emptyList()
    suspend fun evento(id: String): Evento? = null
    suspend fun estadoEvento(id: String): EstadoEvento? = null
    suspend fun corredores(id: String): List<CorredorEvento> = emptyList()
}

/** Carreras en vivo que puedo ver (lectura). */
interface FuenteEnVivo {
    /** Evento elegido para correr en vivo: la proxima sesion se asocia a el. */
    var eventoId: String?
    suspend fun listar(): List<SesionEnVivo>
    suspend fun leer(id: String): SesionEnVivo?
}
