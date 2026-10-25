package com.gacelas.perfil

import androidx.compose.material.icons.filled.BarChart
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
import com.gacelas.cuenta.domain.modelo.ConfigAvatar
import com.gacelas.carreras.domain.reglas.Estadisticas
import com.gacelas.carreras.domain.reglas.EstadisticasPerfil
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
import androidx.compose.material.icons.filled.ViewInAr
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
fun PerfilScreen(onEditar: () -> Unit, onAvatar: () -> Unit, onAjustes: () -> Unit, onCarrera: (String) -> Unit = {}, onReportes: () -> Unit = {}) {
    val perfil by sesionState()
    val carreras by carrerasState()
    val ajustes by ajustesState()
    val m = ajustes.unidadesMetricas
    val mes = remember(carreras) { Estadisticas.mes(carreras, System.currentTimeMillis()) }
    val fecha = remember { SimpleDateFormat("d MMM, HH:mm", Locale("es", "EC")) }
    val social by com.gacelas.ui.socialState()
    val hist = remember(carreras) { EstadisticasPerfil.historico(carreras) }
    val meses = remember(carreras) { EstadisticasPerfil.kmMensual(carreras, System.currentTimeMillis()) }
    val recs = remember(carreras) { EstadisticasPerfil.records(carreras) }
    val meds = remember(carreras) { EstadisticasPerfil.medallas(carreras) }
    Pantalla {
        // C2: cabecera en fila con foto, nombre en Anton cursiva y datos en una linea.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FotoPerfil(perfil?.fotoPath, 80.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(perfil?.nombre ?: MockData.usuarioActual.nombre, style = GType.headlineMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black), color = GColors.TextPrimary, maxLines = 1)
                Text(
                    listOfNotNull("@${perfil?.username ?: MockData.usuarioActual.username}", perfil?.ubicacion?.takeIf { it.isNotBlank() }).joinToString(" · "),
                    style = GType.bodySm, color = GColors.C2Muted, maxLines = 1,
                )
                perfil?.bio?.takeIf { it.isNotBlank() }?.let { Text(it, style = GType.bodySm, color = GColors.C2Muted, maxLines = 2) }
                com.gacelas.ui.components.EtiquetaC2("Club Gacelas Loja", Modifier.padding(top = 4.dp))
            }
            IconButton(onClick = onEditar, modifier = Modifier.align(Alignment.Top).clip(RoundedCornerShape(4.dp)).background(GColors.C2S2)) {
                Icon(Icons.Filled.Edit, "Editar perfil", tint = GColors.C2Violeta)
            }
        }
        Contadores(carreras.size, social.seguidores, social.siguiendo)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.weight(1f).clip(com.gacelas.ui.components.FormaBotonC2).background(GColors.C2Morado).clickable(onClick = onAvatar).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Person, null, tint = GColors.OnPrimary, modifier = Modifier.size(18.dp))
                Text(" Editar avatar 3D", style = GType.labelLg, color = GColors.OnPrimary)
            }
            Row(
                Modifier.weight(1f).clip(com.gacelas.ui.components.FormaBotonC2).background(GColors.C2S2).clickable(onClick = onAjustes).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Settings, null, tint = GColors.TextPrimary, modifier = Modifier.size(18.dp))
                Text(" Configuración", style = GType.labelLg, color = GColors.TextPrimary)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(com.gacelas.ui.components.FormaBotonC2).background(GColors.C2S2).clickable(onClick = onReportes)
                .padding(vertical = 12.dp).testTag("ir-reportes"),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.BarChart, null, tint = GColors.C2Violeta, modifier = Modifier.size(18.dp))
            Text(" Mis reportes", style = GType.labelLg, color = GColors.TextPrimary)
        }
        TarjetaRacha(com.gacelas.ui.rachaState(), carreras)
        com.gacelas.social.ListaLogros(carreras)
        EstadisticasHistoricas(hist, m)
        KilometrajeMensual(meses, m)
        Tarjeta {
            Text("Este mes", style = GType.titleMd, color = GColors.TextPrimary)
            Row(Modifier.fillMaxWidth()) {
                Metrica("${Unidades.texto(mes.km, m, 1)} ${Unidades.etiquetaDistancia(m)}", "Distancia", Modifier.weight(1f), color = GColors.Primary500)
                Metrica(formatearHms(mes.segundos), "Tiempo", Modifier.weight(1f), color = GColors.Secondary500)
                Metrica("${mes.desnivelM} m", "Elevación", Modifier.weight(1f))
            }
        }
        Records(recs)
        Medallero(meds)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Actividades recientes", style = GType.titleMd, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("${carreras.size} en total", style = GType.labelMd, color = GColors.TextSecondary)
        }
        if (carreras.isEmpty()) {
            Tarjeta { Text("Todavía no guardas carreras. Toca el botón Correr para grabar la primera.", style = GType.bodyMd, color = GColors.TextSecondary) }
        }
        carreras.take(20).forEach { c ->
            Tarjeta(Modifier.clickable { onCarrera(c.id) }.testTag("carrera-${c.id}")) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(c.titulo, style = GType.labelLg, color = GColors.TextPrimary)
                        Text(fecha.format(Date(c.inicioMs)), style = GType.bodySm, color = GColors.TextSecondary)
                    }
                    if (!c.publica) Text("Privada", style = GType.bodySm, color = GColors.TextSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("${Unidades.texto(c.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}", style = GType.labelLg, color = GColors.C2Violeta)
                    Text("${formatearRitmo(Unidades.ritmo(c.ritmoSegKm, m))} ${Unidades.etiquetaRitmo(m)}", style = GType.labelLg, color = GColors.C2Menta)
                    Text(formatearMs(c.segundos), style = GType.labelLg, color = GColors.TextPrimary)
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
fun EditarPerfilScreen(onAtras: () -> Unit, onGuardar: () -> Unit, onAvatar: () -> Unit = {}) {
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
    val ajustes by ajustesState()
    var peso by rememberSaveable(p.email) { mutableStateOf(ajustes.pesoKg?.let { it.toInt().toString() }.orEmpty()) }
    var altura by rememberSaveable(p.email) { mutableStateOf(ajustes.alturaCm?.toString().orEmpty()) }
    var nivel by rememberSaveable(p.email) { mutableStateOf(ajustes.nivelDeclarado) }
    var favorita by rememberSaveable(p.email) { mutableStateOf(ajustes.distanciaFavorita) }
    var metaKm by rememberSaveable(p.email) { mutableIntStateOf(ajustes.metaKmSemana) }
    var dias by rememberSaveable(p.email) { mutableIntStateOf(ajustes.metaSemanal) }
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
                c.ajustes.actualizar {
                    it.copy(
                        pesoKg = peso.toDoubleOrNull()?.takeIf { v -> v in 25.0..250.0 }?.let { v -> kotlin.math.round(v) },
                        alturaCm = altura.toIntOrNull()?.takeIf { v -> v in 100..230 },
                        nivelDeclarado = nivel, distanciaFavorita = favorita, metaKmSemana = metaKm, metaSemanal = dias,
                    )
                }
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
            Tarjeta(Modifier.clickable(onClick = onAvatar)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ViewInAr, null, tint = GColors.Primary500)
                    Text("  Personalizar avatar 3D", style = GType.labelLg, color = GColors.TextPrimary)
                }
            }
            EtiquetaGrupo("Datos fisiológicos (Privado)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { CampoTexto("Peso (kg)", peso, { peso = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) }, "70", tipo = KeyboardType.Decimal) }
                Box(Modifier.weight(1f)) { CampoTexto("Altura (cm)", altura, { altura = it.filter(Char::isDigit).take(3) }, "170", tipo = KeyboardType.Number) }
            }
            EtiquetaGrupo("Perfil atlético")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("principiante" to "Principiante", "intermedio" to "Intermedio", "avanzado" to "Avanzado").forEach { (k, t) -> Pastilla(t, k == nivel, { nivel = k }) }
            }
            Text("Distancia favorita", style = GType.labelLg, color = GColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("5K", "10K", "21K", "42K").forEach { d -> Pastilla(d, d == favorita, { favorita = d }) }
            }
            EtiquetaGrupo("Objetivos")
            Text("Meta semanal: $metaKm km", style = GType.labelLg, color = GColors.TextPrimary)
            androidx.compose.material3.Slider(metaKm.toFloat(), { metaKm = it.toInt() }, valueRange = 5f..150f, modifier = Modifier.testTag("meta-km"))
            Text("Días por semana", style = GType.labelLg, color = GColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..7).forEach { d ->
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(if (d == dias) GColors.Primary else GColors.SurfaceContainer).clickable { dias = d },
                        contentAlignment = Alignment.Center,
                    ) { Text("$d", style = GType.labelLg, color = GColors.TextPrimary) }
                }
            }
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
    var complexion by remember(guardado) { mutableStateOf(guardado.complexion ?: "hombre") }
    var tonoPiel by remember(guardado) { mutableIntStateOf(guardado.tonoPiel ?: 0) }
    var peinado by remember(guardado) { mutableStateOf(guardado.peinado ?: "corto") }
    var colorShorts by remember(guardado) { mutableIntStateOf(guardado.colorShorts ?: 0) }
    val guardar: () -> Unit = {
        scope.launch {
            c.ajustes.actualizar { it.copy(avatar = ConfigAvatar(equipacion, estilo, lentes, reloj, audifonos, complexion, tonoPiel, peinado, colorShorts)) }
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
                // Rasgos guardados; el modelo GLB actual tiene una sola textura y aun no los pinta.
                EtiquetaGrupo("Género y complexión")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("hombre" to "Hombre", "mujer" to "Mujer").forEach { (k, t) -> Pastilla(t, k == complexion, { complexion = k }) }
                }
                EtiquetaGrupo("Tono de piel")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TONOS_PIEL.forEachIndexed { i, t ->
                        Box(Modifier.size(40.dp).clip(CircleShape).background(t).border(3.dp, if (i == tonoPiel) GColors.Primary else Color.Transparent, CircleShape).clickable { tonoPiel = i })
                    }
                }
                EtiquetaGrupo("Cabello y peinado")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("corto" to "Corto runner", "coleta" to "Coleta", "rapado" to "Rapado").forEach { (k, t) -> Pastilla(t, k == peinado, { peinado = k }) }
                }
                EtiquetaGrupo("Shorts")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    COLORES_SHORTS.forEachIndexed { i, (col, n) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(col).border(3.dp, if (i == colorShorts) GColors.Primary else Color.Transparent, CircleShape).clickable { colorShorts = i })
                            Text(n, style = GType.labelMd, color = GColors.TextSecondary)
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

private val TONOS_PIEL = listOf(Color(0xFFF5D0B5), Color(0xFFE0AC85), Color(0xFFC68863), Color(0xFF8D5A3B), Color(0xFF5C3A24))
private val COLORES_SHORTS = listOf(Color(0xFF111827) to "Negro", Color(0xFF7B1FA2) to "Morado", Color(0xFF1E3A8A) to "Azul", Color(0xFFE5E7EB) to "Blanco")
