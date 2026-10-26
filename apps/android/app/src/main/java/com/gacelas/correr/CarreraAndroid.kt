package com.gacelas.correr

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gacelas.ui.ajustesState
import com.gacelas.ui.components.MapaGacelas
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.components.aLatLng
import com.gacelas.ui.contenedor

/** Carrera V1 en Android: tracker, mapa MapLibre y avisos; el diseño es ContenidoCarreraC4 (compartido). */
@Composable
fun CarreraEnCursoC4(onCerrar: () -> Unit, onFinalizar: () -> Unit) {
    val context = LocalContext.current
    val c = contenedor()
    val tracker = c.tracker
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { c.carreraEnCursoViewModel(tracker) }
    val estado by vm.estado.collectAsState()
    val ajustes by ajustesState()
    var confirmar by remember { mutableStateOf(false) }
    val ajustesVm = com.gacelas.ui.ajustesViewModel()
    val guardarVoz: ((com.gacelas.identidad.domain.modelo.Ajustes) -> com.gacelas.identidad.domain.modelo.Ajustes) -> Unit = { t -> ajustesVm.actualizar(t) }
    MantenerPantallaEncendida()
    RelojCarrera(tracker, estado)
    PreferenciasCarrera(estado, ajustes) { if (!estado.pausada) vm.alternarPausa() }
    ContenidoCarreraC4(
        VistaCarrera(estado, ajustes.unidadesMetricas),
        onPausa = vm::alternarPausa,
        onStop = { terminarCarrera(context, tracker); onFinalizar() },
        onVuelta = vm::vuelta,
        onAtras = { confirmar = true },
        extraCabecera = {
            androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                com.gacelas.seguridad.ui.BotonReportarIncidente(compacto = true) {
                    estado.puntos.lastOrNull()?.let { it.lat to it.lng } ?: com.gacelas.rutas.ultimaUbicacion(context)
                }
                BotonCompartirEnVivo(tracker)
            }
        },
        vozActiva = ajustes.anunciosVoz,
        onVoz = { guardarVoz { it.copy(anunciosVoz = !it.anunciosVoz) } },
        autopausa = ajustes.autopausa,
        onDescartar = { confirmar = true },
        mapa = { vista3d, controles, panelAbierto ->
            if (Mapas.configurado()) MapaGacelas(
                estado.puntos.aLatLng(), Modifier.fillMaxSize(), seguir = true, zoom = 17f, modo3d = vista3d,
                ritmoSegKm = estado.ritmoActualSegKm, adelanteM = 60.0, rutaMagenta = true, avisoArriba = 210.dp,
                controlesPropios = controles, corredorVisible = !panelAbierto,
            ) { }
            else TrazadoRuta(estado.puntos, Modifier.fillMaxSize())
        },
    )
    if (confirmar) {
        DialogoDescartarCarrera(onConfirmar = {
            confirmar = false
            descartarCarrera(context, tracker)
            onCerrar()
        }, onCancelar = { confirmar = false })
    }
}

