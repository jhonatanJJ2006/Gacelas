package com.gacelas.comunidad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gacelas.comunidad.data.EventoDeClub
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.presentation.ui.DetalleEventoContenido
import com.gacelas.comunidad.presentation.ui.EventosContenido
import com.gacelas.comunidad.presentation.ui.FeedContenido
import com.gacelas.data.mock.MockData
import com.gacelas.ui.aviso
import com.gacelas.ui.compartir
import com.gacelas.ui.contenedor
import kotlinx.coroutines.flow.combine

// Pantallas de comunidad: la UI vive en shared (comunidad/presentation/ui), aqui solo se conectan
// el contenedor, los avisos (Toast) y el menu de compartir de Android.

@Composable
fun FeedScreen(onEventos: () -> Unit, onEnVivo: () -> Unit = {}, onClubes: (() -> Unit)? = null) {
    val context = LocalContext.current
    val c = contenedor()
    val vm = viewModel { c.feedViewModel() }
    FeedContenido(vm, onEventos, onEnVivo, onCompartir = { context.compartir(it) }, onAviso = { context.aviso(it) }, onClubes = onClubes.takeIf { c.clubes != null })
}

/** Eventos de ejemplo mas los creados por el usuario. */
@Composable
fun eventosState(): Pair<List<Evento>, Set<String>> {
    val c = contenedor()
    val flujo = remember(c) { combine(c.eventos.creados, c.eventos.inscritos) { cr, ins -> com.gacelas.comunidad.domain.reglas.unirEventos(cr, MockData.eventos) to ins } }
    val estado by flujo.collectAsState(initial = MockData.eventos to emptySet())
    return estado
}

@Composable
fun EventosScreen(onEvento: (String) -> Unit, onClubes: () -> Unit = {}) {
    val c = contenedor()
    val context = LocalContext.current
    val (eventos, inscritos) = eventosState()
    val vm = viewModel { c.eventosViewModel() }
    val clubId = remember { EventoDeClub.id.also { EventoDeClub.id = null } }
    EventosContenido(vm, eventos, inscritos, conClubes = c.clubes != null, clubId = clubId, onEvento = onEvento, onClubes = onClubes, onAviso = { context.aviso(it) })
}

@Composable
fun DetalleEventoScreen(id: String, onAtras: () -> Unit, onEnVivo: () -> Unit = {}) {
    val c = contenedor()
    val context = LocalContext.current
    val vm = viewModel(key = "detalle-evento-$id") { c.detalleEventoViewModel(id) }
    DetalleEventoContenido(vm, onAtras, onEnVivo, onCompartir = { context.compartir(it) }, onAviso = { context.aviso(it) })
}
