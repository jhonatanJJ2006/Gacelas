package com.gacelas.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.data.carreras.CarreraGuardada
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.ajustesState
import com.gacelas.ui.carrerasState
import com.gacelas.ui.components.BotonSecundario
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.MapaGacelas
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Tarjeta
import com.gacelas.correr.TrazadoRuta
import com.gacelas.ui.components.aLatLng
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Detalle de una carrera guardada: mapa del recorrido, metricas, parciales, notas y borrar. */
@Composable
fun DetalleCarreraScreen(id: String, onAtras: () -> Unit) {
    val carreras by carrerasState()
    val c: CarreraGuardada? = carreras.firstOrNull { it.id == id }
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val repo = contenedor().carreras
    val scope = rememberCoroutineScope()
    DetalleCarreraContenido(
        c = c, fecha = c?.let { SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", Locale("es", "EC")).format(Date(it.inicioMs)) }.orEmpty(), m = m,
        onAtras = onAtras, onEliminar = { scope.launch { repo.eliminar(id) }; onAtras() },
        mapa = { carrera ->
            MapaGacelas(carrera.puntos.aLatLng(), Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp))) {
                TrazadoRuta(carrera.puntos, Modifier.fillMaxWidth().height(240.dp))
            }
        },
    )
}
