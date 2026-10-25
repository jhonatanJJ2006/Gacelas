package com.gacelas.vivo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gacelas.ajustes.PantallaConEncabezado
import com.gacelas.legal.TipoConsentimiento
import com.gacelas.ui.components.CirculoMapa
import com.gacelas.ui.components.MapaLibre
import com.gacelas.ui.components.Mapas
import com.gacelas.ui.components.ModalC2
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.delay
import org.maplibre.android.geometry.LatLng

private fun iniciales(n: String) = n.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
private fun colorEstado(corriendo: Boolean) = if (corriendo) GColors.C2Menta else GColors.C2Cielo

/** Hace cuanto se actualizo la posicion, en texto corto. */
fun haceCuanto(ms: Long, ahora: Long): String {
    val s = ((ahora - ms) / 1000).coerceAtLeast(0)
    return if (s < 60) "hace $s s" else "hace ${s / 60} min"
}

/**
 * Mapa en vivo de corredores: solo se ven quienes activaron "Ubicación visible" y actualizaron
 * su posición en los últimos 2 minutos (lo garantiza la RLS de runner_positions). Sondea cada 10 s.
 */
@Composable
fun MapaVivoScreen(onAtras: () -> Unit, onPrivacidad: () -> Unit, inicial: List<CorredorEnMapa>? = null) {
    val c = contenedor()
    val repo = c.mapaVivo
    var corredores by remember { mutableStateOf(inicial.orEmpty()) }
    var visible by remember { mutableStateOf<Boolean?>(null) }
    var elegido by remember { mutableStateOf<CorredorEnMapa?>(null) }
    var ahora by remember { mutableStateOf(com.gacelas.shared.ahoraMs()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val incidentes = com.gacelas.seguridad.ui.incidentesRecientes(ejemplo = inicial != null)
    LaunchedEffect(Unit) { visible = c.consentimientos.otorgado(TipoConsentimiento.UbicacionVisible) }
    LaunchedEffect(repo) {
        while (inicial == null && repo != null) {
            corredores = repo.listar(); ahora = com.gacelas.shared.ahoraMs(); delay(10_000)
        }
    }
    PantallaConEncabezado("Mapa en vivo", onAtras) {
        if (visible == false) Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GColors.C2Card).border(1.dp, GColors.C2Linea, RoundedCornerShape(12.dp))
                .clickable(onClick = onPrivacidad).padding(12.dp).testTag("aviso-no-visible"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.LocationOff, null, tint = GColors.C2Muted)
            Text("  No estás visible. Actívalo en Privacidad si quieres que otros te vean.", style = GType.bodySm, color = GColors.C2Muted, modifier = Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(16.dp)).background(GColors.C2Card).testTag("mapa-vivo")) {
            if (Mapas.configurado() && !androidx.compose.ui.platform.LocalInspectionMode.current) {
                val centro = corredores.firstOrNull()?.let { LatLng(it.lat, it.lng) } ?: Mapas.CENTRO_LOJA
                MapaLibre(
                    Modifier.fillMaxWidth().height(380.dp),
                    circulos = incidentes.map { CirculoMapa(LatLng(it.lat, it.lng), 7f, com.gacelas.seguridad.ui.colorIncidente(it), GColors.C2Bg, 2f, id = "inc-" + it.id) } +
                        corredores.map { CirculoMapa(LatLng(it.lat, it.lng), 9f, colorEstado(it.corriendo), Color.White, 2f, id = it.userId) },
                    centro = centro, zoom = 13.0,
                    alTocarCirculo = { id -> elegido = corredores.firstOrNull { it.userId == id } },
                )
            } else EsquemaCorredores(corredores) { elegido = it }
            com.gacelas.seguridad.ui.BotonReportarIncidente(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                com.gacelas.rutas.ultimaUbicacion(context)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Leyenda(GColors.C2Menta, "Corriendo"); Leyenda(GColors.C2Cielo, "Visible, sin correr"); Leyenda(GColors.C2Peligro, "Incidente")
        }
        Text(if (corredores.isEmpty()) "Nadie está visible ahora mismo" else "${corredores.size} corredores visibles", style = GType.labelLg, color = GColors.TextPrimary)
        corredores.forEach { r -> FilaCorredor(r, ahora) { elegido = r } }
        com.gacelas.seguridad.ui.ListaIncidentes(incidentes)
    }
    elegido?.let { r ->
        ModalC2(
            icono = Icons.Filled.Person, titulo = r.nombre,
            texto = (if (r.corriendo) "Está corriendo" else "Visible, sin correr") + " · actualizado ${haceCuanto(r.actualizadoMs, ahora)}",
            primario = "Cerrar", onPrimario = { elegido = null }, onCerrar = { elegido = null }, tag = "perfil-corredor",
        )
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color)); Text("  $texto", style = GType.bodySm, color = GColors.C2Muted)
}

@Composable
private fun AvatarIniciales(nombre: String, corriendo: Boolean) =
    Box(Modifier.size(40.dp).clip(CircleShape).background(GColors.C2S2).border(2.dp, colorEstado(corriendo), CircleShape), contentAlignment = Alignment.Center) {
        Text(iniciales(nombre), style = GType.labelLg, color = GColors.TextPrimary)
    }

@Composable
private fun FilaCorredor(r: CorredorEnMapa, ahora: Long, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GColors.C2Card).clickable(onClick = onClick).padding(12.dp).testTag("corredor-${r.userId}"),
    verticalAlignment = Alignment.CenterVertically,
) {
    AvatarIniciales(r.nombre, r.corriendo)
    Column(Modifier.weight(1f).padding(start = 12.dp)) {
        Text(r.nombre, style = GType.labelLg, color = GColors.TextPrimary)
        Text(if (r.corriendo) "Corriendo" else "Sin correr", style = GType.bodySm, color = colorEstado(r.corriendo))
    }
    Text(haceCuanto(r.actualizadoMs, ahora), style = GType.bodySm, color = GColors.C2Muted)
}

/** Alternativa sin mapa (pruebas o sin clave): posiciones relativas sobre una rejilla. */
@Composable
private fun EsquemaCorredores(lista: List<CorredorEnMapa>, onTocar: (CorredorEnMapa) -> Unit) {
    Box(Modifier.fillMaxWidth().height(380.dp)) {
        Canvas(Modifier.fillMaxWidth().height(380.dp)) {
            val paso = 48.dp.toPx()
            var x = 0f; while (x < size.width) { drawLine(GColors.C2Linea, Offset(x, 0f), Offset(x, size.height)); x += paso }
            var y = 0f; while (y < size.height) { drawLine(GColors.C2Linea, Offset(0f, y), Offset(size.width, y)); y += paso }
        }
        if (lista.isEmpty()) Text("Sin corredores visibles", style = GType.bodyMd, color = GColors.C2Muted, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
        val minLat = lista.minOfOrNull { it.lat } ?: 0.0; val maxLat = lista.maxOfOrNull { it.lat } ?: 0.0
        val minLng = lista.minOfOrNull { it.lng } ?: 0.0; val maxLng = lista.maxOfOrNull { it.lng } ?: 0.0
        lista.forEach { r ->
            val fx = if (maxLng > minLng) ((r.lng - minLng) / (maxLng - minLng)).toFloat() else 0.5f
            val fy = if (maxLat > minLat) (1 - (r.lat - minLat) / (maxLat - minLat)).toFloat() else 0.5f
            Column(
                Modifier.align(androidx.compose.ui.BiasAlignment(fx * 1.6f - 0.8f, fy * 1.6f - 0.8f)).clickable { onTocar(r) }.testTag("marcador-${r.userId}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AvatarIniciales(r.nombre, r.corriendo)
                Text(r.nombre.substringBefore(" "), style = GType.labelMd, color = GColors.TextPrimary)
            }
        }
    }
}

/** Publica la posicion propia mientras la app esta abierta, solo con el consentimiento vigente. */
@Composable
fun PublicadorUbicacionVivo() {
    val c = contenedor()
    val repo = c.mapaVivo ?: return
    val context = LocalContext.current
    LaunchedEffect(repo) {
        while (true) {
            if (c.consentimientos.otorgado(TipoConsentimiento.UbicacionVisible)) {
                val e = c.tracker.estado.value
                val p = e.puntos.lastOrNull()?.takeIf { e.activa }?.let { it.lat to it.lng } ?: com.gacelas.rutas.ultimaUbicacion(context)
                if (p != null) runCatching { repo.publicar(p.first, p.second, e.fase == com.gacelas.carreras.domain.FaseCarrera.Corriendo) }
            }
            delay(5_000)
        }
    }
}
