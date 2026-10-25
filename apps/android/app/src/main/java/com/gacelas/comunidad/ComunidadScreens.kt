package com.gacelas.comunidad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.asImageBitmap
import com.gacelas.comunidad.domain.reglas.yaPaso
import kotlinx.coroutines.launch
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
fun FeedScreen(onEventos: () -> Unit, onEnVivo: () -> Unit = {}, onClubes: (() -> Unit)? = null, onEvento: (String) -> Unit = { onEventos() }) {
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val c = contenedor()
    val vm = viewModel { c.feedViewModel() }
    var fotosPara by remember { mutableStateOf<String?>(null) }
    val selector = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(6),
    ) { uris ->
        val id = fotosPara ?: return@rememberLauncherForActivityResult
        fotosPara = null
        val fotos = uris.mapNotNull { u ->
            runCatching { context.contentResolver.openInputStream(u)?.use { it.readBytes() }?.let { it to (context.contentResolver.getType(u) ?: "image/jpeg") } }.getOrNull()
        }
        if (fotos.isNotEmpty()) vm.subirFotos(id, fotos) { context.aviso(it) }
    }
    val (eventos, inscritos) = eventosState()
    val creados by c.eventos.creados.collectAsState(initial = emptyList())
    val club = (if (c.feed != null) creados else eventos).filter { !it.yaPaso() }.minByOrNull { it.inicioMs ?: Long.MAX_VALUE }
    var reportando by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    reportando?.let { id -> com.gacelas.social.ModalReporte(id, c.social) { reportando = null } }
    FeedContenido(
        vm, onEventos, onEnVivo, onCompartir = { context.compartir(it) }, onAviso = { context.aviso(it) }, onClubes = onClubes.takeIf { c.clubes != null },
        imagen = { url, mod -> FotoFeed(url, mod) },
        onAgregarFotos = { id ->
            fotosPara = id
            selector.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onReportar = { reportando = it },
        eventoClub = club,
        inscritoClub = club?.id in inscritos,
        onAsistir = { club?.let { e -> alcance.launch { c.eventos.alternarInscripcion(e.id) } } },
        onEventoClub = { club?.let { onEvento(it.id) } },
    )
}

/** Foto remota del feed (descarga y decodifica reducida, sin dependencias extra). */
@Composable
private fun FotoFeed(url: String, modifier: Modifier) {
    val img by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, url) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val bytes = java.net.URL(url).readBytes()
                val op = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, op)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(modifier.background(com.gacelas.ui.theme.GColors.SurfaceHigh)) {
        img?.let { androidx.compose.foundation.Image(it, "Foto de la publicación", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
    }
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
    val (unidos, inscritos) = eventosState()
    val creados by c.eventos.creados.collectAsState(initial = emptyList())
    // Con Supabase la lista sale solo de la base; los de ejemplo quedan para el modo sin backend.
    val eventos = if (c.feed != null) creados else unidos
    val vm = viewModel { c.eventosViewModel() }
    val clubId = remember { EventoDeClub.id.also { EventoDeClub.id = null } }
    EventosContenido(vm, eventos, inscritos, conClubes = c.clubes != null, clubId = clubId, onEvento = onEvento, onClubes = onClubes, onAviso = { context.aviso(it) })
}

@Composable
fun DetalleEventoScreen(id: String, onAtras: () -> Unit, onEnVivo: () -> Unit = {}) {
    val c = contenedor()
    val context = LocalContext.current
    val vm = viewModel(key = "detalle-evento-$id") { c.detalleEventoViewModel(id) }
    var reportando by remember { mutableStateOf<String?>(null) }
    DetalleEventoContenido(vm, onAtras, onEnVivo, onCompartir = { context.compartir(it) }, onAviso = { context.aviso(it) }, onReportar = { reportando = it })
    reportando?.let { eid -> com.gacelas.social.ModalReporte(eid, c.social, com.gacelas.social.TipoContenido.Evento) { reportando = null } }
}
