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
    val tracker = contenedor().tracker
    val estado by tracker.estado.collectAsState()
    val ajustes by ajustesState()
    var confirmar by remember { mutableStateOf(false) }
    MantenerPantallaEncendida()
    RelojCarrera(tracker, estado)
    ContenidoCarreraC4(
        VistaCarrera(estado, ajustes.unidadesMetricas),
        onPausa = tracker::alternarPausa,
        onStop = { terminarCarrera(context, tracker); onFinalizar() },
        onVuelta = tracker::vuelta,
        onAtras = { confirmar = true },
        extraCabecera = { BotonCompartirEnVivo(tracker) },
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

