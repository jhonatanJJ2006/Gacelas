package com.gacelas

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.SupabaseBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.almacenGacelas: DataStore<Preferences> by preferencesDataStore(name = "gacelas")

/** El contenedor de dependencias es el compartido (shared commonMain, com.gacelas.di). */
typealias Contenedor = com.gacelas.di.ContenedorApp

/** Supabase solo se usa si local.properties trae la URL y la clave. */
fun backendConfigurado(): Backend? =
    // En las pruebas (Robolectric) nunca se usa el proyecto real.
    if (android.os.Build.FINGERPRINT != "robolectric" && BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank())
        runCatching { SupabaseBackend(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) }.getOrNull()
    else null

/** Aviso de version nueva (manifiesto publico en Supabase Storage); null en pruebas o sin servidor. */
private fun actualizacionAndroid(store: DataStore<Preferences>): com.gacelas.data.actualizacion.ActualizacionRepository? =
    (if (android.os.Build.FINGERPRINT == "robolectric") null else com.gacelas.data.actualizacion.ReglasActualizacion.urlManifiesto(BuildConfig.SUPABASE_URL))
        ?.let { com.gacelas.data.actualizacion.ActualizacionRepository(store, it, BuildConfig.VERSION_CODE) }

class GacelasApp : Application() {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val contenedor: Contenedor by lazy {
        val store = applicationContext.almacenGacelas
        Contenedor(
            store, backendConfigurado(), actualizacion = actualizacionAndroid(store),
            ubicacionAproximada = { com.gacelas.inicio.ubicacionParaClima(this) },
            nombreLugar = { com.gacelas.inicio.nombreLugarParaClima(this, it) },
        ) { com.gacelas.core.sync.Sincronizacion.programar(this) }
    }

    private val contenedorListo = kotlinx.coroutines.CompletableDeferred<Unit>()

    /** Espera (sin bloquear el hilo principal) a que el contenedor este construido en segundo plano. */
    suspend fun esperarContenedor() = contenedorListo.await()

    override fun onCreate() {
        android.os.Trace.beginSection("GacelasApp.onCreate")
        super.onCreate()
        alcance.launch { runCatching { contenedor }; contenedorListo.complete(Unit); runCatching { contenedor.sincronizar() }; runCatching { contenedor.explorar?.refrescar() } }
        // Si quedo algo pendiente de otra sesion, se sube en cuanto haya red.
        com.gacelas.core.sync.Sincronizacion.programar(this)
        android.os.Trace.endSection()
    }
}
