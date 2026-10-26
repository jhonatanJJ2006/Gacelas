package com.gacelas.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.gacelas.Contenedor
import com.gacelas.GacelasApp
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.identidad.domain.modelo.Perfil
import com.gacelas.actividad.domain.modelo.Carrera

/** Contenedor de dependencias de la app actual. */
@Composable
fun contenedor(): Contenedor {
    val app = LocalContext.current.applicationContext as GacelasApp
    return remember(app) { app.contenedor }
}

@Composable
fun ajustesState(): State<Ajustes> {
    val repo = contenedor().ajustes
    return ajustesViewModel(repo).ajustes.collectAsState()
}

@Composable
fun sesionState(): State<Perfil?> = contenedor().auth.sesion.collectAsState(initial = null)

@Composable
fun carrerasState(): State<List<Carrera>> {
    // Historial de carreras desde su ViewModel compartido (contexto carreras, MVVM).
    val c = contenedor()
    val vm = androidx.lifecycle.viewmodel.compose.viewModel(key = "historial-carreras") { c.historialCarrerasViewModel() }
    return vm.carreras.collectAsState()
}

/** Racha vigente calculada con las carreras (misma logica que el servidor). */
@Composable
fun rachaState(): com.gacelas.actividad.domain.reglas.Racha {
    val carreras by carrerasState()
    val ajustes by ajustesState()
    return remember(carreras, ajustes.metaSemanal) {
        com.gacelas.actividad.domain.reglas.Rachas.calcular(carreras, System.currentTimeMillis(), ajustes.metaSemanal)
    }
}

@Suppress("UnusedReceiverParameter")
fun Context.aviso(texto: String) = com.gacelas.ui.components.AvisosC2.mostrar(texto)

/** Abre el selector del sistema para compartir texto. */
fun Context.compartir(texto: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
    startActivity(Intent.createChooser(intent, "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** ViewModel de ajustes compartido (contexto cuenta). */
@Composable
fun ajustesViewModel(repo: com.gacelas.identidad.domain.repositorio.AjustesRepository = contenedor().ajustes): com.gacelas.identidad.presentation.AjustesViewModel {
    val c = contenedor()
    return androidx.lifecycle.viewmodel.compose.viewModel(key = "ajustes") { c.ajustesViewModel(repo) }
}

/** Datos sociales reales de Supabase para Inicio y Perfil (amigos en ruta, seguidores, siguiendo). */
data class Social(
    val amigos: List<com.gacelas.comunidad.domain.modelo.Publicacion> = emptyList(),
    val seguidores: Int = 0,
    val siguiendo: Int = 0,
)

@Composable
fun socialState(): State<Social> {
    val feed = contenedor().feed
    val perfil by sesionState()
    return androidx.compose.runtime.produceState(Social(), feed, perfil?.email) {
        val f = feed ?: return@produceState
        val sig = f.siguiendo()
        val pubs = f.publicaciones().orEmpty()
        value = Social(pubs.filter { !it.propia && it.autorId != null && it.autorId in sig }.take(3), f.seguidores(), sig.size)
    }
}
