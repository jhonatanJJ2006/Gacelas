package com.gacelas.rutas

import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.horizontalScroll
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gacelas.carreras.domain.modelo.PuntoGps
import com.gacelas.data.mock.MockData
import com.gacelas.rutas.domain.modelo.ETIQUETAS_RUTA
import com.gacelas.rutas.domain.modelo.ResenaRuta
import com.gacelas.correr.TrazadoRuta
import com.gacelas.ui.ajustesState
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.MapaGacelas
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.aviso
import com.gacelas.ui.contenedor
import com.gacelas.ui.sesionState
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.gacelas.tracking.Unidades
import org.maplibre.android.geometry.LatLng
import kotlinx.coroutines.launch

/** Ultima ubicacion conocida (para "Cerca de mí"); null sin permiso. */
@SuppressLint("MissingPermission")
fun ultimaUbicacion(context: Context): Pair<Double, Double>? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    ) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return lm.getProviders(true).mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }?.let { it.latitude to it.longitude }
}

@Composable
fun Estrellas(valor: Double, tamano: Int = 18) {
    Row(Modifier.semantics { contentDescription = "%.1f de 5 estrellas".format(valor) }) {
        (1..5).forEach { i -> Icon(if (valor >= i - 0.25) Icons.Filled.Star else Icons.Filled.StarBorder, null, tint = GColors.Primary400, modifier = Modifier.size(tamano.dp)) }
    }
}

@Composable
fun DetalleRutaScreen(id: String, onAtras: () -> Unit, onCorrer: () -> Unit) {
    val c = contenedor()
    val repo = c.explorar
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remotas by (repo?.rutas ?: remember { kotlinx.coroutines.flow.MutableStateFlow(null) }).collectAsState()
    val meGusta by (repo?.meGusta ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptySet<String>()) }).collectAsState()
    val ruta = (remotas ?: MockData.rutas).firstOrNull { it.id == id }
    val perfil by sesionState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    var resenas by remember { mutableStateOf<List<ResenaRuta>>(emptyList()) }
    var estrellas by remember { mutableIntStateOf(0) }
    var comentario by remember { mutableStateOf("") }
    val sociales = repo != null && remotas != null
    var fotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var corredores by remember { mutableStateOf(emptyList<Pair<String, Int>>() to 0) }
    var subiendo by remember { mutableStateOf(false) }
    var compartiendo by remember { mutableStateOf(false) }
    val carrerasRacha by com.gacelas.ui.carrerasState()
    val rachaDias = remember(carrerasRacha, ajustes.metaSemanal) { com.gacelas.carreras.domain.reglas.Rachas.calcular(carrerasRacha, System.currentTimeMillis(), ajustes.metaSemanal).diaria }
    LaunchedEffect(id, sociales) {
        if (!sociales) return@LaunchedEffect
        resenas = repo!!.resenas(id)
        fotos = repo.fotos(id)
        corredores = repo.corredores(id)
    }
    val elegirFoto = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null || repo == null) return@rememberLauncherForActivityResult
        subiendo = true
        scope.launch {
            runCatching {
                val tipo = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.use { it.readBytes() } }
                repo.subirFoto(id, bytes, tipo)
                fotos = repo.fotos(id)
            }.onSuccess { context.aviso("Foto subida") }.onFailure { context.aviso(it.message ?: "No se pudo subir la foto") }
            subiendo = false
        }
    }

    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras(ruta?.nombre ?: "Ruta", onAtras)
        if (ruta == null) {
            Text("Ruta no encontrada", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.padding(16.dp))
            return
        }
        if (compartiendo) com.gacelas.compartir.CompartirDialogo(
            com.gacelas.compartir.DatosCompartir(
                com.gacelas.compartir.DatosCompartir.Tipo.Ruta, ruta.nombre, ruta.puntos, ruta.distanciaKm,
                desnivelM = ruta.elevacionM, rachaDias = rachaDias, enlace = com.gacelas.compartir.enlaceRuta(ruta.id),
            ),
        ) { compartiendo = false }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val puntos = ruta.puntos.map { PuntoGps(it.first, it.second, 0) }
            Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp)).background(GColors.SurfaceContainer)) {
                MapaGacelas(puntos.map { LatLng(it.lat, it.lng) }, Modifier.fillMaxSize(), zoom = 14f) { TrazadoRuta(puntos, Modifier.fillMaxSize()) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "${Unidades.texto(ruta.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)}" to "Distancia",
                    "${ruta.elevacionM} m" to "Desnivel",
                    ruta.dificultad.etiqueta to "Dificultad",
                ).forEach { (v, e) ->
                    Tarjeta(Modifier.weight(1f), padding = 10.dp) {
                        Text(v, style = GType.titleMd, color = GColors.TextPrimary, maxLines = 1)
                        Text(e, style = GType.bodySm, color = GColors.TextSecondary)
                    }
                }
            }
            if (ruta.perfilElevacion.size > 1) PerfilElevacion(ruta.perfilElevacion, ruta.distanciaKm)
            if (ruta.descripcion.isNotBlank()) Text(ruta.descripcion, style = GType.bodyMd, color = GColors.TextSecondary)
            if (ruta.etiquetas.isNotEmpty()) Text(ruta.etiquetas.joinToString("  ") { "#" + (ETIQUETAS_RUTA[it] ?: it) }, style = GType.labelLg, color = GColors.TextPrimary)
            ruta.autorNombre?.let { Text("Creada por $it", style = GType.bodySm, color = GColors.TextSecondary) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonPrimario("Correr esta ruta", { com.gacelas.rutas.data.RutaActiva.id = id; onCorrer() }, Modifier.weight(1f), icono = Icons.AutoMirrored.Filled.DirectionsRun)
                IconButton(onClick = { compartiendo = true }, modifier = Modifier.clip(CircleShape).background(GColors.SurfaceContainer)) { Icon(Icons.Filled.Share, "Compartir ruta", tint = GColors.TextPrimary) }
                if (sociales) {
                    val activo = id in meGusta
                    Row(
                        Modifier.clip(CircleShape).background(GColors.SurfaceContainer).clickable {
                            if (perfil == null) context.aviso("Inicia sesión para dar me gusta")
                            else scope.launch { runCatching { repo!!.alternarMeGusta(id) }.onFailure { context.aviso(it.message ?: "Error") } }
                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (activo) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (activo) "Quitar me gusta" else "Me gusta", tint = if (activo) GColors.Danger else GColors.TextPrimary)
                        Text(" ${ruta.meGusta}", style = GType.labelLg, color = GColors.TextPrimary)
                    }
                }
            }
            if (sociales) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fotos", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                    if (perfil != null) androidx.compose.material3.TextButton(onClick = {
                        elegirFoto.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, enabled = !subiendo) { Text(if (subiendo) "Subiendo..." else "Añadir foto", color = GColors.Primary400) }
                }
                if (fotos.isEmpty()) Text("Aún no hay fotos de esta ruta.", style = GType.bodySm, color = GColors.TextSecondary)
                else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { fotos.forEach { FotoRemota(it) } }
                Text("Quién la ha corrido", style = GType.titleMd, color = GColors.TextPrimary)
                Text(
                    if (corredores.second == 0) "Nadie la ha corrido todavía. ¡Sé el primero!" else "${corredores.second} ${if (corredores.second == 1) "persona" else "personas"}",
                    style = GType.bodySm, color = GColors.TextSecondary,
                )
                corredores.first.forEach { (n, v) ->
                    Row { Text(n, style = GType.bodyMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f)); Text("$v ${if (v == 1) "vez" else "veces"}", style = GType.bodySm, color = GColors.TextSecondary) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reseñas", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                    ruta.valoracion?.let { Estrellas(it); Text(" %.1f · %d".format(it, ruta.resenas), style = GType.labelMd, color = GColors.TextPrimary) }
                }
                if (perfil != null) {
                    Tarjeta {
                        Row {
                            (1..5).forEach { i ->
                                IconButton(onClick = { estrellas = i }) {
                                    Icon(if (i <= estrellas) Icons.Filled.Star else Icons.Filled.StarBorder, "$i ${if (i == 1) "estrella" else "estrellas"}", tint = GColors.Primary400, modifier = Modifier.size(30.dp))
                                }
                            }
                        }
                        CampoTexto("Comentario", comentario, { comentario = it.take(500) }, "¿Qué tal la ruta? (opcional)", lineas = 2)
                        BotonPrimario("Publicar reseña", {
                            if (estrellas == 0) context.aviso("Elige de 1 a 5 estrellas")
                            else scope.launch {
                                runCatching { repo!!.resenar(id, estrellas, comentario) }
                                    .onSuccess { comentario = ""; resenas = repo!!.resenas(id); context.aviso("¡Gracias por tu reseña!") }
                                    .onFailure { context.aviso(it.message ?: "No se pudo enviar") }
                            }
                        }, icono = null)
                    }
                } else Text("Inicia sesión para dejar una reseña.", style = GType.bodyMd, color = GColors.TextSecondary)
                if (resenas.isEmpty()) Text("Aún no hay reseñas.", style = GType.bodySm, color = GColors.TextSecondary)
                resenas.forEach { r ->
                    Tarjeta(padding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.nombre, style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                            Estrellas(r.estrellas.toDouble(), 16)
                        }
                        if (r.comentario.isNotBlank()) Text(r.comentario, style = GType.bodySm, color = GColors.TextSecondary)
                    }
                }
            }
        }
    }
}

/** Grafica del perfil de elevacion (Canvas). */
@Composable
fun PerfilElevacion(alturas: List<Int>, distanciaKm: Double) {
    val min = alturas.min(); val max = alturas.max(); val rango = maxOf(max - min, 10).toFloat()
    Tarjeta(padding = 12.dp) {
        Row {
            Text("Perfil de elevación", style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("$min–$max m · ${"%.1f".format(distanciaKm)} km", style = GType.bodySm, color = GColors.TextSecondary)
        }
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(110.dp).semantics { contentDescription = "Elevación entre $min y $max metros" }) {
            val pts = alturas.mapIndexed { i, h -> androidx.compose.ui.geometry.Offset(i * size.width / (alturas.size - 1), size.height - 4f - (h - min) / rango * (size.height - 12f)) }
            val linea = androidx.compose.ui.graphics.Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
            val area = androidx.compose.ui.graphics.Path().apply { addPath(linea); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            drawPath(area, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(GColors.Primary500.copy(alpha = 0.45f), androidx.compose.ui.graphics.Color.Transparent)))
            drawPath(linea, GColors.Primary500, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
        }
    }
}

/** Imagen remota sin dependencias extra (descarga y decodifica a tamano reducido). */
@Composable
private fun FotoRemota(url: String) {
    val img by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, url) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val bytes = java.net.URL(url).readBytes()
                val op = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, op)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(GColors.SurfaceContainer)) {
        img?.let { androidx.compose.foundation.Image(it, "Foto de la ruta", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
    }
}
