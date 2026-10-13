package com.gacelas.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.carreras.data.LocalCarrerasRepository
import com.gacelas.carreras.data.SupabaseCarrerasRepository
import com.gacelas.carreras.domain.SesionCarrera
import com.gacelas.carreras.domain.repositorio.CarrerasRepository
import com.gacelas.carreras.presentation.CarreraEnCursoViewModel
import com.gacelas.carreras.presentation.DetalleCarreraViewModel
import com.gacelas.carreras.presentation.HistorialCarrerasViewModel
import com.gacelas.comunidad.data.ClubesRepository
import com.gacelas.comunidad.data.EnVivoRepository
import com.gacelas.comunidad.data.LocalEventosRepository
import com.gacelas.comunidad.data.NotificacionesRepository
import com.gacelas.comunidad.data.SupabaseEventosRepository
import com.gacelas.comunidad.domain.modelo.Notificacion
import com.gacelas.comunidad.domain.repositorio.EventosRepository
import com.gacelas.comunidad.presentation.ClubesViewModel
import com.gacelas.comunidad.presentation.EventosViewModel
import com.gacelas.comunidad.presentation.NotificacionesViewModel
import com.gacelas.cuenta.data.LocalAjustesRepository
import com.gacelas.cuenta.data.LocalAuthRepository
import com.gacelas.cuenta.data.SupabaseAjustesRepository
import com.gacelas.cuenta.data.SupabaseAuthRepository
import com.gacelas.cuenta.domain.repositorio.AjustesRepository
import com.gacelas.cuenta.domain.repositorio.AuthRepository
import com.gacelas.cuenta.presentation.AccesoSocialViewModel
import com.gacelas.cuenta.presentation.AjustesViewModel
import com.gacelas.cuenta.presentation.LoginViewModel
import com.gacelas.cuenta.presentation.RecuperarViewModel
import com.gacelas.cuenta.presentation.RegistroViewModel
import com.gacelas.data.actualizacion.ActualizacionRepository
import com.gacelas.data.remoto.Backend
import com.gacelas.rutas.data.ExplorarRepository
import com.gacelas.rutas.data.LocalRutasRepository
import com.gacelas.rutas.data.RutasOffline
import com.gacelas.rutas.data.SupabaseRutasRepository
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.rutas.domain.repositorio.RutasRepository
import com.gacelas.rutas.presentation.DetalleRutaViewModel
import com.gacelas.rutas.presentation.ListaRutasViewModel

/**
 * Contenedor manual de dependencias compartido (Android e iOS), sin librerias de DI.
 *
 * Con [backend] usa los repositorios de Supabase (con DataStore como cache); sin el, todo queda
 * en el dispositivo. Las piezas de plataforma llegan por parametro: el [store] (DataStore),
 * el [backend] ya configurado, el aviso de [actualizacion] y [programarSincronizacion]
 * (WorkManager en Android). Tambien crea los ViewModels (fabricas `*ViewModel()`), asi las
 * pantallas no construyen dependencias por su cuenta.
 *
 * @param descargarAlEntrar si es true, al iniciar sesion en Supabase se descargan carreras,
 * rutas, ajustes y eventos (Android). iOS todavia no lo hace.
 */
class ContenedorApp(
    val store: DataStore<Preferences>,
    backend: Backend? = null,
    val actualizacion: ActualizacionRepository? = null,
    descargarAlEntrar: Boolean = true,
    programarSincronizacion: () -> Unit = {},
) {
    val auth: AuthRepository
    val carreras: CarrerasRepository
    val rutas: RutasRepository
    val ajustes: AjustesRepository
    val eventos: EventosRepository

    /** Ajustes solo en el dispositivo (iOS los guarda asi por ahora). */
    val ajustesLocales: AjustesRepository by lazy { LocalAjustesRepository(store) }

    /** Carrera en curso (una por app). */
    val tracker = SesionCarrera()

    /** Seguimiento en vivo; null sin servidor. */
    val enVivo: EnVivoRepository?

    /** Notificaciones reales; null sin servidor (se muestran las de ejemplo). */
    val notificaciones: NotificacionesRepository?

    /** Rutas de la comunidad (Fase 4); null sin servidor. */
    val explorar: ExplorarRepository?

    /** Clubes y eventos de Fase 5; null sin servidor. */
    val clubes: ClubesRepository?

    /** Descarga los datos de la cuenta y sube lo pendiente. No hace nada sin servidor. */
    val sincronizar: suspend () -> Unit

    /** Rutas guardadas en el celular y cambios pendientes. */
    val rutasOffline = RutasOffline(store)

    /** Sube carreras y rutas pendientes; devuelve cuantas operaciones quedan (lo usa SincronizacionWorker). */
    val subirPendientes: suspend () -> Int

    init {
        if (backend == null) {
            auth = LocalAuthRepository(store)
            carreras = LocalCarrerasRepository(store)
            rutas = LocalRutasRepository(store)
            ajustes = LocalAjustesRepository(store)
            eventos = LocalEventosRepository(store)
            sincronizar = {}
            subirPendientes = { 0 }
            enVivo = null
            notificaciones = null
            explorar = null
            clubes = null
        } else {
            lateinit var remotoAuth: SupabaseAuthRepository
            val uid: suspend () -> String? = { remotoAuth.userId() }
            val c = SupabaseCarrerasRepository(backend, store, uid, programarSincronizacion)
            val r = SupabaseRutasRepository(backend, store, uid, rutasOffline, programarSincronizacion)
            val a = SupabaseAjustesRepository(backend, store, uid)
            val e = SupabaseEventosRepository(backend, store, uid)
            val descargarTodo: suspend () -> Unit = { c.descargar(); r.descargar(); a.descargar(); e.descargar() }
            remotoAuth = if (descargarAlEntrar) SupabaseAuthRepository(backend, store) { descargarTodo() } else SupabaseAuthRepository(backend, store)
            auth = remotoAuth
            carreras = c
            rutas = r
            ajustes = a
            eventos = e
            sincronizar = { remotoAuth.restaurar() }
            enVivo = EnVivoRepository(backend, uid)
            notificaciones = NotificacionesRepository(backend, uid)
            val ex = ExplorarRepository(backend, rutasOffline, programarSincronizacion, uid)
            explorar = ex
            subirPendientes = { c.sincronizar() + ex.subirPendientes() }
            clubes = ClubesRepository(backend, uid)
        }
    }

    // Fabricas de ViewModels (presentation). Se usan dentro de viewModel { ... } de lifecycle.

    fun loginViewModel(a: AuthRepository = auth) = LoginViewModel(a)
    fun registroViewModel(a: AuthRepository = auth) = RegistroViewModel(a)
    fun recuperarViewModel(enviadoInicial: Boolean = false, a: AuthRepository = auth) = RecuperarViewModel(a, enviadoInicial)
    fun accesoSocialViewModel(a: AuthRepository = auth) = AccesoSocialViewModel(a)
    fun ajustesViewModel(repo: AjustesRepository = ajustes) = AjustesViewModel(repo)
    fun historialCarrerasViewModel() = HistorialCarrerasViewModel(carreras)
    fun detalleCarreraViewModel(id: String) = DetalleCarreraViewModel(id, carreras)
    fun carreraEnCursoViewModel(sesion: SesionCarrera = tracker) = CarreraEnCursoViewModel(sesion)
    fun listaRutasViewModel(idUsuario: String, ejemplo: List<Ruta>) = ListaRutasViewModel(explorar, rutas, idUsuario, ejemplo)
    fun detalleRutaViewModel(id: String) = DetalleRutaViewModel(id, explorar)
    fun eventosViewModel() = EventosViewModel(eventos)
    fun clubesViewModel() = ClubesViewModel(clubes)
    fun feedViewModel() = com.gacelas.comunidad.presentation.FeedViewModel(carreras, auth, ajustes)
    fun detalleEventoViewModel(id: String) = com.gacelas.comunidad.presentation.DetalleEventoViewModel(id, eventos, clubes)
    fun detalleClubViewModel(id: String) = com.gacelas.comunidad.presentation.DetalleClubViewModel(id, clubes)
    fun eventoEnVivoViewModel(id: String) = com.gacelas.comunidad.presentation.EventoEnVivoViewModel(id, clubes, enVivo)
    fun listaEnVivoViewModel() = com.gacelas.comunidad.presentation.ListaEnVivoViewModel(enVivo)
    fun detalleEnVivoViewModel(id: String) = com.gacelas.comunidad.presentation.DetalleEnVivoViewModel(id, enVivo)
    fun notificacionesViewModel(ejemplo: List<Notificacion>) = NotificacionesViewModel(notificaciones, ajustes, ejemplo)
}
