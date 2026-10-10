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
import com.gacelas.tracking.RunTracker

private val Context.almacenGacelas: DataStore<Preferences> by preferencesDataStore(name = "gacelas")

/**
 * Contenedor simple de dependencias. Para usar Supabase basta con cambiar
 * las implementaciones Local* por otras con la misma interfaz.
 */
class Contenedor(store: DataStore<Preferences>) {
    val auth: AuthRepository = LocalAuthRepository(store)
    val carreras: CarrerasRepository = LocalCarrerasRepository(store)
    val rutas: RutasRepository = LocalRutasRepository(store)
    val ajustes: AjustesRepository = LocalAjustesRepository(store)
    val eventos: EventosRepository = LocalEventosRepository(store)
    val tracker = RunTracker()
}

class GacelasApp : Application() {
    val contenedor: Contenedor by lazy { Contenedor(applicationContext.almacenGacelas) }
}
