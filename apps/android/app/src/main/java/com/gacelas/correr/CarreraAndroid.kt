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
        onStop = { com.gacelas.seguridad.EstadoSos.terminar(c.sos); terminarCarrera(context, tracker); onFinalizar() },
        onVuelta = vm::vuelta,
        onAtras = { confirmar = true },
        extraCabecera = {
            // Reportar va al costado del mapa, debajo de los grupos; el SOS queda aparte (abajo a la izquierda).
            com.gacelas.seguridad.ui.BotonReportarIncidente(compacto = true) {
                estado.puntos.lastOrNull()?.let { it.lat to it.lng } ?: com.gacelas.rutas.ultimaUbicacion(context)
            }
        },
        sos = { com.gacelas.seguridad.BotonSOS(estado.puntos.lastOrNull()?.let { it.lat to it.lng }) },
        objetivo = ObjetivoCarrera.de(ajustes.objetivoTipo, ajustes.objetivoValor),
        enVivo = ajustes.compartirEnVivo == true && c.enVivo != null,
        vista3dInicial = ajustes.vistaMapa3d,
        onVista3d = { v -> ajustesVm.actualizar { it.copy(vistaMapa3d = v) } },
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
            // Sin mapa configurado se ve el trazo, pero con los mismos controles al costado.
            else androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                com.gacelas.ui.components.MapaEsquemaC2(estado.puntos.map { it.lat to it.lng }, Modifier.fillMaxSize(), yo = estado.puntos.lastOrNull()?.let { it.lat to it.lng }, grosor = 5.dp,
                    margenArriba = 70.dp, margenAbajo = 400.dp)
                controles({}, {}, 0f, true)
            }
        },
    )
    CompartirEnVivoAutomatico(tracker)
    AlertasZonaCarrera(tracker)
    if (confirmar) {
        DialogoDescartarCarrera(onConfirmar = {
            confirmar = false
            com.gacelas.seguridad.EstadoSos.terminar(c.sos)
            descartarCarrera(context, tracker)
            onCerrar()
        }, onCancelar = { confirmar = false })
    }
}

