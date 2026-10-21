package com.gacelas.comunidad.domain.repositorio

import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.ComentarioFeed
import com.gacelas.comunidad.domain.modelo.Publicacion
import com.gacelas.comunidad.domain.modelo.CorredorEvento
import com.gacelas.comunidad.domain.modelo.EstadoEvento
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.MiembroClub
import com.gacelas.comunidad.domain.modelo.Notificacion
import com.gacelas.comunidad.domain.modelo.PostClub
import com.gacelas.comunidad.domain.modelo.RolClub
import com.gacelas.comunidad.domain.modelo.SesionEnVivo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Eventos creados por el usuario e inscripciones. Hoy local; preparado para Supabase. */
interface EventosRepository {
    val creados: Flow<List<Evento>>
    val inscritos: Flow<Set<String>>
    suspend fun crear(evento: Evento)
    suspend fun alternarInscripcion(id: String)
}

/** Notificaciones de la cuenta (servidor). La implementa data.SupabaseNotificacionesRepository. */
interface NotificacionesRepository {
    val lista: StateFlow<List<Notificacion>>
    suspend fun refrescar()
    suspend fun marcarLeidas(ids: Collection<String>)
}

/** Clubes: lista con los mios. La implementa data.SupabaseClubesRepository. */
interface ClubesRepository {
    suspend fun listar(): Pair<List<Club>, Set<String>>
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

/** Carreras en vivo: compartir posicion y ver sesiones de otros. */
interface EnVivoRepository {
    var eventoId: String?
    val sesionId: String?
    /** Ultimo envio: true visible, false oculto cerca de casa, null sin envios. */
    val visible: Boolean?
    suspend fun iniciar(): String?
    suspend fun enviar(lat: Double, lng: Double, distanciaKm: Double, segundos: Int): Boolean
    suspend fun terminar()
    suspend fun listar(): List<SesionEnVivo>
    suspend fun leer(id: String): SesionEnVivo?
}

/** Feed social desde Supabase (RPC feed_comunidad, post_likes, post_comments, follows). */
interface FeedRepository {
    /** Publicaciones visibles para el usuario; null si no hay sesion o red. */
    suspend fun publicaciones(): List<Publicacion>?
    suspend fun alternarMeGusta(id: String, activo: Boolean)
    suspend fun comentarios(id: String): List<ComentarioFeed>
    suspend fun comentar(id: String, texto: String)
    suspend fun siguiendo(): Set<String>
    suspend fun alternarSeguir(id: String): Set<String>
    /** Numero de corredores que siguen al usuario. */
    suspend fun seguidores(): Int = 0
}
