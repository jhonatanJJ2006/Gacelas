package com.gacelas.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gacelas.acceso.SesionIos
import com.gacelas.comunidad.data.EventoDeClub
import com.gacelas.comunidad.presentation.ui.ClubesContenido
import com.gacelas.comunidad.presentation.ui.DetalleClubContenido
import com.gacelas.comunidad.presentation.ui.DetalleEnVivoContenido
import com.gacelas.comunidad.presentation.ui.DetalleEventoContenido
import com.gacelas.comunidad.presentation.ui.EventoEnVivoContenido
import com.gacelas.comunidad.presentation.ui.EventosContenido
import com.gacelas.comunidad.presentation.ui.FeedContenido
import com.gacelas.comunidad.presentation.ui.ListaEnVivoContenido
import com.gacelas.comunidad.presentation.ui.NotificacionesContenido
import com.gacelas.cuenta.domain.modelo.Ajustes
import com.gacelas.data.mock.MockData
import com.gacelas.mapa.MapaGacelasIos
import com.gacelas.mapa.MapaIos
import com.gacelas.mapa.MapaLibreIos
import com.gacelas.navigation.Destinos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIView

/** Aviso breve como el Toast de Android (iOS no trae uno propio). */
object AvisoIos {
    val texto = MutableStateFlow<String?>(null)
    fun mostrar(t: String) { texto.value = t }
}

/** Capa del aviso: misma posicion y duracion corta (2 s) que Toast.LENGTH_SHORT. */
@Composable
fun BoxScope.CapaAvisoIos() {
    val t by AvisoIos.texto.collectAsState()
    LaunchedEffect(t) { if (t != null) { delay(2000); AvisoIos.texto.value = null } }
    t?.let {
        Text(
            it, color = Color.White, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 64.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color(0xE6323232)).padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/** Hoja de compartir del sistema (equivalente al selector de Android). */
fun compartirIos(texto: String) {
    val raiz = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    val hoja = UIActivityViewController(activityItems = listOf(texto), applicationActivities = null)
    (raiz.presentedViewController ?: raiz).presentViewController(hoja, animated = true, completion = null)
}

/** Rutas de comunidad que se muestran dentro de la barra principal (como conBarra en Android). */
fun rutaConBarra(ruta: String) = ruta == Destinos.COMUNIDAD || ruta == Destinos.EVENTOS

private fun idDe(ruta: String, prefijo: String, sufijo: String = "") = ruta.removePrefix(prefijo).removeSuffix(sufijo)

/**
 * Pantallas de comunidad en iOS: las mismas pantallas compartidas (comunidad/presentation/ui) y los
 * mismos ViewModels del contenedor que usa Android. [ruta] sigue el formato de [Destinos].
 */
@Composable
fun ComunidadIos(ruta: String, navegar: (String) -> Unit, atras: () -> Unit, onCorrer: () -> Unit) {
    val c = SesionIos.contenedor
    val ajustes by SesionIos.ajustes.ajustes.collectAsState(Ajustes())
    when {
        ruta == Destinos.COMUNIDAD -> {
            val vm = viewModel { c.feedViewModel(SesionIos.ajustes) }
            FeedContenido(vm, onEventos = { navegar(Destinos.EVENTOS) }, onEnVivo = { navegar(Destinos.EN_VIVO) }, onCompartir = ::compartirIos, onAviso = AvisoIos::mostrar)
        }
        ruta == Destinos.EVENTOS -> {
            val flujo = remember(c) { combine(c.eventos.creados, c.eventos.inscritos) { cr, ins -> com.gacelas.comunidad.domain.reglas.unirEventos(cr, MockData.eventos) to ins } }
            val (eventos, inscritos) = flujo.collectAsState(MockData.eventos to emptySet()).value
            val vm = viewModel { c.eventosViewModel() }
            val clubId = remember { EventoDeClub.id.also { EventoDeClub.id = null } }
            EventosContenido(
                vm, eventos, inscritos, conClubes = c.clubes != null, clubId = clubId,
                onEvento = { navegar(Destinos.detalleEvento(it)) }, onClubes = { navegar(Destinos.CLUBES) }, onAviso = AvisoIos::mostrar,
            )
        }
        ruta == Destinos.NOTIFICACIONES -> {
            val vm = viewModel { c.notificacionesViewModel(MockData.notificaciones, SesionIos.ajustes) }
            NotificacionesContenido(vm, atras) {}
        }
        ruta == Destinos.CLUBES -> {
            val vm = viewModel { c.clubesViewModel() }
            ClubesContenido(vm, conServidor = c.clubes != null, onAtras = atras, onClub = { navegar(Destinos.detalleClub(it)) }, onAviso = AvisoIos::mostrar)
        }
        ruta.startsWith("comunidad/clubes/") -> {
            val id = idDe(ruta, "comunidad/clubes/")
            val vm = viewModel(key = "detalle-club-$id") { c.detalleClubViewModel(id) }
            DetalleClubContenido(
                id, vm, conServidor = c.clubes != null, onAtras = atras, onEvento = { navegar(Destinos.detalleEvento(it)) },
                onCrearEvento = { EventoDeClub.id = it; navegar(Destinos.EVENTOS) }, onAviso = AvisoIos::mostrar,
            )
        }
        ruta.startsWith("comunidad/eventos/") && ruta.endsWith("/en-vivo") -> {
            val id = idDe(ruta, "comunidad/eventos/", "/en-vivo")
            val vm = viewModel(key = "evento-en-vivo-$id") { c.eventoEnVivoViewModel(id) }
            EventoEnVivoContenido(
                vm, atras, { vm.correrEnEvento(); onCorrer() },
                mapa = if (!MapaIos.disponible()) null else { trazos, circulos, encuadre ->
                    var vista by remember { mutableStateOf<UIView?>(null) }
                    LaunchedEffect(encuadre.size, vista) {
                        val v = vista ?: return@LaunchedEffect
                        if (encuadre.size < 2) return@LaunchedEffect
                        MapaIos.puente?.encuadrar(v, encuadre.minOf { it.lat }, encuadre.minOf { it.lng }, encuadre.maxOf { it.lat }, encuadre.maxOf { it.lng }, 40.0)
                    }
                    MapaLibreIos(Modifier.fillMaxSize(), trazos = trazos, circulos = circulos, alListo = { vista = it })
                },
            )
        }
        ruta.startsWith("comunidad/eventos/") -> {
            val id = idDe(ruta, "comunidad/eventos/")
            val vm = viewModel(key = "detalle-evento-$id") { c.detalleEventoViewModel(id) }
            DetalleEventoContenido(vm, atras, onEnVivo = { navegar(Destinos.eventoEnVivo(id)) }, onCompartir = ::compartirIos, onAviso = AvisoIos::mostrar)
        }
        ruta == Destinos.EN_VIVO -> {
            val vm = viewModel { c.listaEnVivoViewModel() }
            ListaEnVivoContenido(vm, ajustes, atras) { navegar(Destinos.detalleEnVivo(it)) }
        }
        ruta.startsWith("comunidad/en-vivo/") -> {
            val id = idDe(ruta, "comunidad/en-vivo/")
            val vm = viewModel(key = "en-vivo-$id") { c.detalleEnVivoViewModel(id) }
            DetalleEnVivoContenido(vm, ajustes, atras) { puntos, modifier, oscuro, alternativa ->
                MapaGacelasIos(puntos, modifier, seguir = true, oscuro = oscuro) { alternativa() }
            }
        }
        else -> Box(Modifier.fillMaxSize())
    }
}
