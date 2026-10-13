package com.gacelas.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.gacelas.Contenedor
import com.gacelas.GacelasApp
import com.gacelas.data.ajustes.Ajustes
import com.gacelas.data.auth.Perfil
import com.gacelas.carreras.domain.modelo.Carrera

/** Contenedor de dependencias de la app actual. */
@Composable
fun contenedor(): Contenedor {
    val app = LocalContext.current.applicationContext as GacelasApp
    return remember(app) { app.contenedor }
}

@Composable
fun ajustesState(): State<Ajustes> {
    val c = contenedor()
    return c.ajustes.ajustes.collectAsState(initial = Ajustes())
}

@Composable
fun sesionState(): State<Perfil?> = contenedor().auth.sesion.collectAsState(initial = null)

@Composable
fun carrerasState(): State<List<Carrera>> {
    // Historial de carreras desde su ViewModel compartido (contexto carreras, MVVM).
    val repo = contenedor().carreras
    val vm = androidx.lifecycle.viewmodel.compose.viewModel(key = "historial-carreras") { com.gacelas.carreras.presentation.HistorialCarrerasViewModel(repo) }
    return vm.carreras.collectAsState()
}

/** Racha vigente calculada con las carreras (misma logica que el servidor). */
@Composable
fun rachaState(): com.gacelas.carreras.domain.reglas.Racha {
    val carreras by carrerasState()
    val ajustes by ajustesState()
    return remember(carreras, ajustes.metaSemanal) {
        com.gacelas.carreras.domain.reglas.Rachas.calcular(carreras, System.currentTimeMillis(), ajustes.metaSemanal)
    }
}

fun Context.aviso(texto: String) = Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()

/** Abre el selector del sistema para compartir texto. */
fun Context.compartir(texto: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
    startActivity(Intent.createChooser(intent, "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
