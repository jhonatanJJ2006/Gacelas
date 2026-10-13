package com.gacelas.perfil

import com.gacelas.ui.components.TarjetaRacha
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.gacelas.data.ajustes.ConfigAvatar
import com.gacelas.carreras.domain.reglas.Estadisticas
import com.gacelas.tracking.Unidades
import com.gacelas.tracking.formatearHms
import com.gacelas.tracking.formatearMs
import com.gacelas.tracking.formatearRitmo
import com.gacelas.ui.ajustesState
import com.gacelas.ui.aviso
import com.gacelas.ui.carrerasState
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.contenedor
import com.gacelas.ui.sesionState
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gacelas.data.mock.MockData
import com.gacelas.ui.components.AvatarCirculo
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.EtiquetaGrupo
import com.gacelas.ui.components.FilaAjuste
import com.gacelas.ui.components.FilaInterruptor
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.gacelas.ui.three.ClipCorredor
import com.gacelas.ui.three.Runner3D
import io.github.sceneview.math.Position

/** Foto de perfil guardada en el dispositivo o avatar con degradado. */
@Composable
fun FotoPerfil(path: String?, tamano: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(path) { path?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() } }
    if (bitmap != null) {
        Image(bitmap, "Foto de perfil", Modifier.size(tamano).clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        AvatarCirculo(tamano, 0)
    }
}

@Composable
fun PerfilScreen(onEditar: () -> Unit, onAvatar: () -> Unit, onAjustes: () -> Unit, onCarrera: (String) -> Unit = {}) {
    val perfil by sesionState()
    val carreras by carrerasState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val mes = remember(carreras) { Estadisticas.mes(carreras, System.currentTimeMillis()) }
    val fecha = remember { SimpleDateFormat("d MMM, HH:mm", Locale("es", "EC")) }
    Pantalla {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FotoPerfil(perfil?.fotoPath, 80.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(perfil?.nombre ?: MockData.usuarioActual.nombre, style = GType.headlineMd, color = GColors.TextPrimary)
                Text("@${perfil?.username ?: MockData.usuarioActual.username}", style = GType.bodyMd, color = GColors.TextSecondary)
                perfil?.bio?.takeIf { it.isNotBlank() }?.let { Text(it, style = GType.bodySm, color = GColors.TextSecondary) }
            }
            IconButton(onClick = onEditar, modifier = Modifier.clip(CircleShape).background(GColors.SurfaceContainer)) {
                Icon(Icons.Filled.Edit, "Editar perfil", tint = GColors.Primary500)
            }
        }
        Tarjeta {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Metrica("${carreras.size}", "Carreras", color = GColors.Primary500)
                Metrica("${carreras.count { it.publica }}", "Públicas")
                Metrica(Unidades.texto(carreras.sumOf { it.distanciaKm }, m, 1), "${Unidades.etiquetaDistancia(m)} totales")
            }
        }
        TarjetaRacha(com.gacelas.ui.rachaState(), carreras)
        Tarjeta {
            Text("Estadísticas del mes", style = GType.titleMd, color = GColors.TextPrimary)
            FilaEstadistica(Icons.Filled.Straighten, "Distancia total", "${Unidades.texto(mes.km, m, 1)} ${Unidades.etiquetaDistancia(m)}")
            FilaEstadistica(Icons.Filled.Schedule, "Tiempo total", formatearHms(mes.segundos))
            FilaEstadistica(Icons.AutoMirrored.Filled.TrendingUp, "Elevación", "${mes.desnivelM} m")
        }
        Tarjeta(padding = 0.dp) {
            FilaAjuste(Icons.Filled.Person, "Personalizar avatar 3D", onClick = onAvatar)
            FilaAjuste(Icons.Filled.Settings, "Ajustes y preferencias", onClick = onAjustes)
        }
        Text("Actividad reciente", style = GType.titleMd, color = GColors.TextPrimary)
        if (carreras.isEmpty()) {
            Tarjeta { Text("Todavía no guardas carreras. Toca el botón Correr para grabar la primera.", style = GType.bodyMd, color = GColors.TextSecondary) }
        }
        carreras.take(20).forEach { c ->
            Tarjeta(Modifier.clickable { onCarrera(c.id) }.testTag("carrera-${c.id}")) {
                Row {
                    Text(c.titulo, style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text(fecha.format(Date(c.inicioMs)), style = GType.bodySm, color = GColors.TextSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${Unidades.texto(c.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}", style = GType.bodyMd, color = GColors.Primary500)
                    Text(formatearMs(c.segundos), style = GType.bodyMd, color = GColors.TextSecondary)
                    Text("${formatearRitmo(Unidades.ritmo(c.ritmoSegKm, m))} ${Unidades.etiquetaRitmo(m)}", style = GType.bodyMd, color = GColors.TextSecondary)
                    if (!c.publica) Text("Privada", style = GType.bodySm, color = GColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun FilaEstadistica(icono: ImageVector, etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = GColors.Primary500, modifier = Modifier.size(20.dp))
        Text("  $etiqueta", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(valor, style = GType.labelLg, color = GColors.TextPrimary)
    }
}

@Composable
fun EditarPerfilScreen(onAtras: () -> Unit, onGuardar: () -> Unit) {
    val c = contenedor()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val perfil by sesionState()
    val p = perfil
    if (p == null) {
        Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
            EncabezadoAtras("Editar perfil", onAtras)
            Text("Inicia sesión para editar tu perfil.", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.padding(16.dp))
        }
        return
    }
    var nombre by rememberSaveable(p.email) { mutableStateOf(p.nombre) }
    var username by rememberSaveable(p.email) { mutableStateOf(p.username) }
    var ubicacion by rememberSaveable(p.email) { mutableStateOf(p.ubicacion) }
    var bio by rememberSaveable(p.email) { mutableStateOf(p.bio) }
    var edad by rememberSaveable(p.email) { mutableStateOf(p.edad) }
    var genero by rememberSaveable(p.email) { mutableStateOf(p.genero) }
    var foto by rememberSaveable(p.email) { mutableStateOf(p.fotoPath) }
    var error by remember { mutableStateOf<String?>(null) }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Se copia la imagen a la carpeta privada de la app para no depender del permiso del URI.
            val destino = File(context.filesDir, "foto_perfil_${System.currentTimeMillis()}.jpg")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input -> destino.outputStream().use { input.copyTo(it) } }
                foto = destino.absolutePath
            }.onFailure { context.aviso("No se pudo cargar la foto") }
        }
    }
    val guardar: () -> Unit = {
        when {
            nombre.trim().length < 2 -> error = "Ingresa tu nombre"
            username.trim().removePrefix("@").length < 3 -> error = "El usuario debe tener al menos 3 caracteres"
            else -> scope.launch {
                c.auth.actualizarPerfil(p.copy(nombre = nombre.trim(), username = username.trim().removePrefix("@"), ubicacion = ubicacion.trim(), bio = bio, edad = edad, genero = genero, fotoPath = foto))
                context.aviso("Perfil actualizado")
                onGuardar()
            }
        }
    }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras("Editar perfil", onAtras) {
            Text("Guardar", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.clickable(onClick = guardar).padding(12.dp))
        }
        Pantalla(Modifier.weight(1f).navigationBarsPadding()) {
            Column(Modifier.fillMaxWidth().clickable { selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    FotoPerfil(foto, 104.dp)
                    Box(Modifier.align(Alignment.BottomEnd).size(34.dp).clip(CircleShape).background(GColors.Primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CameraAlt, null, tint = GColors.TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Text("Cambiar foto", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.padding(top = 8.dp))
            }
            CampoTexto("Nombre completo", nombre, { nombre = it; error = null })
            CampoTexto("Nombre de usuario", username, { username = it; error = null })
            CampoTexto("Ubicación", ubicacion, { ubicacion = it }, trailing = { Icon(Icons.Filled.LocationOn, null, tint = GColors.TextSecondary) })
            CampoTexto("Bio", bio, { if (it.length <= 150) bio = it }, "Cuéntanos sobre ti...", lineas = 4, ayuda = "${bio.length}/150 caracteres")
            EtiquetaGrupo("Información adicional")
            CampoTexto("Edad", edad, { edad = it.filter(Char::isDigit).take(3) }, "25", tipo = KeyboardType.Number)
            Text("Género", style = GType.labelLg, color = GColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Masculino", "Femenino", "Otro").forEach { Pastilla(it, it == genero, { genero = it }) }
            }
            Pastilla("Prefiero no decir", genero == "Prefiero no decir", { genero = "Prefiero no decir" })
            error?.let { Text(it, style = GType.bodyMd, color = GColors.Danger) }
            BotonPrimario("Guardar cambios", guardar, icono = null)
        }
    }
}

@Composable
fun Avatar3DScreen(onAtras: () -> Unit, onGuardar: () -> Unit) {
    val c = contenedor()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ajustes by ajustesState()
    val guardado = ajustes.avatar
    val equipaciones = listOf(GColors.Primary, GColors.Secondary, GColors.Info, GColors.Danger)
    var equipacion by remember(guardado) { mutableIntStateOf(guardado.equipacion) }
    var estilo by remember(guardado) { mutableStateOf(guardado.estilo) }
    var lentes by remember(guardado) { mutableStateOf(guardado.lentes) }
    var reloj by remember(guardado) { mutableStateOf(guardado.reloj) }
    var audifonos by remember(guardado) { mutableStateOf(guardado.audifonos) }
    val guardar: () -> Unit = {
        scope.launch {
            c.ajustes.actualizar { it.copy(avatar = ConfigAvatar(equipacion, estilo, lentes, reloj, audifonos)) }
            context.aviso("Avatar guardado")
            onGuardar()
        }
    }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras("Personalizar Avatar", onAtras) {
            Text("Guardar", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.clickable(onClick = guardar).padding(12.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(340.dp).background(Brush.verticalGradient(listOf(GColors.SurfaceLow, equipaciones[equipacion].copy(alpha = 0.18f))))) {
                Runner3D(ClipCorredor.Idle, autoRotar = true, camara = Position(0f, 0.3f, 4.4f))
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, null, tint = GColors.Primary500)
                        Text("  Equipación", style = GType.titleMd, color = GColors.TextPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        equipaciones.forEachIndexed { i, c ->
                            Box(Modifier.size(44.dp).clip(CircleShape).background(c)
                                .border(3.dp, if (i == equipacion) GColors.TextPrimary else Color.Transparent, CircleShape)
                                .clickable { equipacion = i })
                        }
                    }
                }
                EtiquetaGrupo("Estilo de carrera")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Dinámico" to Icons.AutoMirrored.Filled.DirectionsRun,
                        "Relajado" to Icons.Filled.SelfImprovement,
                        "Veloz" to Icons.Filled.Bolt,
                    ).forEach { (n, ic) ->
                        val activo = n == estilo
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (activo) GColors.Primary.copy(alpha = 0.2f) else GColors.SurfaceContainer)
                                .border(2.dp, if (activo) GColors.Primary else GColors.BorderSubtle, RoundedCornerShape(16.dp)).clickable { estilo = n }.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(ic, null, tint = if (activo) GColors.Primary500 else GColors.TextSecondary)
                            Text(n, style = GType.labelMd, color = GColors.TextPrimary)
                        }
                    }
                }
                EtiquetaGrupo("Accesorios")
                Tarjeta(padding = 0.dp) {
                    FilaInterruptor(Icons.Filled.Visibility, "Lentes deportivos", checked = lentes, onChange = { lentes = it })
                    FilaInterruptor(Icons.Filled.Watch, "Reloj GPS", checked = reloj, onChange = { reloj = it })
                    FilaInterruptor(Icons.Filled.Headphones, "Audífonos", checked = audifonos, onChange = { audifonos = it })
                }
            }
        }
    }
}
