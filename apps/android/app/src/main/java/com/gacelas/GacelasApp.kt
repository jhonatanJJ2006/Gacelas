package com.gacelas

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.gacelas.data.ajustes.AjustesRepository
import com.gacelas.data.ajustes.LocalAjustesRepository
import com.gacelas.data.auth.AuthRepository
import com.gacelas.data.auth.LocalAuthRepository
import com.gacelas.data.carreras.CarrerasRepository
import com.gacelas.data.carreras.LocalCarrerasRepository
import com.gacelas.data.eventos.EventosRepository
import com.gacelas.data.eventos.LocalEventosRepository
import com.gacelas.data.rutas.LocalRutasRepository
import com.gacelas.data.rutas.RutasRepository
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.SupabaseAjustesRepository
import com.gacelas.data.remoto.SupabaseAuthRepository
import com.gacelas.data.remoto.SupabaseBackend
import com.gacelas.data.remoto.SupabaseCarrerasRepository
import com.gacelas.data.remoto.SupabaseEventosRepository
import com.gacelas.data.remoto.SupabaseRutasRepository
import com.gacelas.tracking.RunTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.almacenGacelas: DataStore<Preferences> by preferencesDataStore(name = "gacelas")

/**
 * Contenedor simple de dependencias. Con Supabase configurado usa los repositorios remotos
 * (con DataStore como cache); si no, todo queda en el dispositivo.
 */
class Contenedor(store: DataStore<Preferences>, backend: Backend? = null, programarSincronizacion: () -> Unit = {}) {
    val auth: AuthRepository
    val carreras: CarrerasRepository
    val rutas: RutasRepository
    val ajustes: AjustesRepository
    val eventos: EventosRepository
    val tracker = RunTracker()
    /** Seguimiento en vivo; null sin servidor. */
    val enVivo: com.gacelas.data.envivo.EnVivoRepository?
    /** Notificaciones reales; null sin servidor (se muestran las de ejemplo). */
    val notificaciones: com.gacelas.data.notificaciones.NotificacionesRepository?
    /** Rutas de la comunidad (Fase 4); null sin servidor. */
    val explorar: com.gacelas.data.rutas.ExplorarRepository?
    /** Clubes y eventos de Fase 5; null sin servidor. */
    val clubes: com.gacelas.data.clubes.ClubesRepository?

    /** Descarga los datos de la cuenta y sube lo pendiente. No hace nada sin servidor. */
    val sincronizar: suspend () -> Unit
    /** Rutas guardadas en el celular y cambios pendientes. */
    val rutasOffline = com.gacelas.data.rutas.RutasOffline(store)
    /** Aviso de version nueva (manifiesto publico en Supabase Storage); null en pruebas o sin servidor. */
    val actualizacion: com.gacelas.data.actualizacion.ActualizacionRepository? =
        (if (android.os.Build.FINGERPRINT == "robolectric") null else com.gacelas.data.actualizacion.ReglasActualizacion.urlManifiesto(BuildConfig.SUPABASE_URL))
            ?.let { com.gacelas.data.actualizacion.ActualizacionRepository(store, it, BuildConfig.VERSION_CODE) }
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
            remotoAuth = SupabaseAuthRepository(backend, store) { descargarTodo() }
            auth = remotoAuth
            carreras = c
            rutas = r
            ajustes = a
            eventos = e
            sincronizar = { remotoAuth.restaurar() }
            enVivo = com.gacelas.data.envivo.EnVivoRepository(backend, uid)
            notificaciones = com.gacelas.data.notificaciones.NotificacionesRepository(backend, uid)
            val ex = com.gacelas.data.rutas.ExplorarRepository(backend, rutasOffline, programarSincronizacion, uid)
            explorar = ex
            subirPendientes = { c.sincronizar() + ex.subirPendientes() }
            clubes = com.gacelas.data.clubes.ClubesRepository(backend, uid)
        }
    }

    companion object {
        /** Supabase solo se usa si local.properties trae la URL y la clave. */
        fun backendConfigurado(): Backend? =
            // En las pruebas (Robolectric) nunca se usa el proyecto real.
            if (android.os.Build.FINGERPRINT != "robolectric" && BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank())
                runCatching { SupabaseBackend(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) }.getOrNull()
            else null
    }
}

class GacelasApp : Application() {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val contenedor: Contenedor by lazy {
        Contenedor(applicationContext.almacenGacelas, Contenedor.backendConfigurado()) { com.gacelas.core.sync.Sincronizacion.programar(this) }
    }

    override fun onCreate() {
        super.onCreate()
        alcance.launch { runCatching { contenedor.sincronizar() }; runCatching { contenedor.explorar?.refrescar() } }
        // Si quedo algo pendiente de otra sesion, se sube en cuanto haya red.
        com.gacelas.core.sync.Sincronizacion.programar(this)
    }
}
