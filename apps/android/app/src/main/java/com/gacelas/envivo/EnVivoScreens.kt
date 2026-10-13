package com.gacelas.envivo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.gacelas.correr.TrazadoRuta
import com.gacelas.carreras.domain.modelo.PuntoGps
import com.gacelas.comunidad.domain.modelo.SesionEnVivo
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearHms
import com.gacelas.tracking.formatearRitmo
import org.maplibre.android.geometry.LatLng
import com.gacelas.ui.ajustesState
import com.gacelas.ui.components.MapaGacelas
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.delay

@Composable
private fun Encabezado(titulo: String, onAtras: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = GColors.TextPrimary) }
        Text(titulo, style = GType.headlineMd, color = GColors.TextPrimary)
    }
}

/** Corredores en vivo que puedo ver. Se actualiza cada 10 s. */
@Composable
fun ListaEnVivoScreen(onAtras: () -> Unit, onSesion: (String) -> Unit) {
    val c = contenedor()
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { c.listaEnVivoViewModel() }
    val m by ajustesState()
    val sesiones by vm.sesiones.collectAsState()
    Pantalla {
        Encabezado("En vivo ahora", onAtras)
        when {
            sesiones == null -> Text("Buscando corredores…", style = GType.bodyMd, color = GColors.TextSecondary)
            sesiones!!.isEmpty() -> Tarjeta { Text("Nadie a quien sigues está corriendo en vivo ahora.", style = GType.bodyMd, color = GColors.TextSecondary) }
            else -> sesiones!!.forEach { s ->
                Tarjeta(onClick = { onSesion(s.id) }) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(GColors.Danger))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.nombre, style = GType.titleMd, color = GColors.TextPrimary)
                            Text("${Unidades.texto(s.distanciaKm, m.unidadesMetricas)} ${Unidades.etiquetaDistancia(m.unidadesMetricas)} · ${formatearHms(s.segundos)}", style = GType.bodySm, color = GColors.TextSecondary)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = GColors.TextSecondary)
                    }
                }
            }
        }
    }
}

/** Mapa en vivo de un corredor. La web usa Supabase Realtime; aqui se consulta cada 5 s. */
@Composable
fun DetalleEnVivoScreen(id: String, onAtras: () -> Unit) {
    val c = contenedor()
    val vm = androidx.lifecycle.viewmodel.compose.viewModel(key = "en-vivo-$id") { c.detalleEnVivoViewModel(id) }
    val a by ajustesState()
    val m = a.unidadesMetricas
    val s by vm.sesion.collectAsState()
    val cargado by vm.cargado.collectAsState()
    val x = s
    Pantalla {
        Encabezado(x?.nombre ?: "En vivo", onAtras)
        if (x == null) {
            Text(if (cargado) "Esta carrera en vivo no existe o no tienes permiso para verla." else "Cargando…", style = GType.bodyMd, color = GColors.TextSecondary)
            return@Pantalla
        }
        val activa = x.activa()
        Row(Modifier.clip(CircleShape).background(if (activa) GColors.Danger.copy(alpha = 0.2f) else GColors.SurfaceContainer).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (activa) GColors.Danger else GColors.TextSecondary))
            Text(if (activa) "  En vivo" else "  Carrera terminada", style = GType.labelMd, color = GColors.TextPrimary)
        }
        val puntos = x.puntos.mapIndexed { i, p -> PuntoGps(p.lat, p.lng, i.toLong()) }
        Tarjeta(padding = 8.dp) {
            MapaGacelas(x.puntos.map { LatLng(it.lat, it.lng) }, Modifier.fillMaxWidth().height(320.dp), seguir = true, oscuro = a.modoOscuro) {
                TrazadoRuta(puntos, Modifier.fillMaxWidth().height(320.dp))
            }
            if (x.oculta) Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Home, null, tint = GColors.Primary500)
                Text("  Ubicación oculta cerca de casa por privacidad", style = GType.bodySm, color = GColors.TextPrimary)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Metrica(Unidades.texto(x.distanciaKm, m), Unidades.etiquetaDistancia(m), color = GColors.Primary500)
            Metrica(formatearHms(x.segundos), "Tiempo")
            Metrica(formatearRitmo(Unidades.ritmo(if (x.distanciaKm > 0.01) (x.segundos / x.distanciaKm).toInt() else null, m)), Unidades.etiquetaRitmo(m), color = GColors.Secondary500)
        }
    }
}
