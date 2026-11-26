package com.gacelas.compartir

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.gacelas.BuildConfig
import com.gacelas.ui.aviso
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.propiedadesHojaC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private sealed interface Opcion {
    val id: String
    val etiqueta: String

    data class Sobre(val plantilla: Plantilla) : Opcion {
        override val id = plantilla.name
        override val etiqueta = plantilla.etiqueta
    }

    data object Marca : Opcion {
        override val id = "runcity"
        override val etiqueta = "Runcity"
    }
}

private val OPCIONES: List<Opcion> = Plantilla.entries.map { Opcion.Sobre(it) } + Opcion.Marca

private fun Modifier.damero(): Modifier = drawBehind {
    val celda = 14.dp.toPx()
    val oscuro = Color(0xFF111827)
    val claro = Color(0xFF1F2937)
    var fila = 0
    var y = 0f
    while (y < size.height) {
        var columna = 0
        var x = 0f
        while (x < size.width) {
            drawRect(if ((fila + columna) % 2 == 0) oscuro else claro, Offset(x, y), Size(celda, celda))
            x += celda
            columna++
        }
        y += celda
        fila++
    }
}

private fun reducir(full: Bitmap, ancho: Int): Bitmap {
    val alto = ancho * full.height / full.width
    val mini = Bitmap.createScaledBitmap(full, ancho, alto, true)
    if (mini != full) full.recycle()
    return mini
}

/** Lee una foto de la galeria reducida a ~2160 px de lado mayor. */
fun leerFoto(context: Context, uri: Uri, lado: Int = 2160): Bitmap? = runCatching {
    val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, o) }
    var muestra = 1
    while (maxOf(o.outWidth, o.outHeight) / (muestra * 2) >= lado) muestra *= 2
    context.contentResolver.openInputStream(uri)?.use {
        android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = muestra })
    }
}.getOrNull()

private fun guardarGaleriaConAviso(context: Context, bitmap: Bitmap) {
    try {
        guardarEnGaleria(context, bitmap)
        context.aviso("Imagen guardada en la galería")
    } catch (e: IOException) {
        context.aviso("No se pudo guardar la imagen en la galería")
    } catch (e: SecurityException) {
        context.aviso("Permiso denegado para guardar la imagen")
    }
}

/** Galería de historias: la foto queda detrás y cada plantilla se abre para ajustarla. */
@Composable
fun ComponerFotoDialogo(datos: DatosCompartir, onCerrar: () -> Unit) {
    val context = LocalContext.current
    var foto by remember { mutableStateOf<Bitmap?>(null) }
    var elegida by remember { mutableStateOf<Opcion>(Opcion.Sobre(Plantilla.Estadisticas)) }
    var editando by remember { mutableStateOf(false) }
    var handle by remember { mutableStateOf(datos.handle) }
    var ajuste by remember { mutableStateOf(AjusteFoto()) }
    var cols by remember { mutableStateOf(Plantilla.entries.associateWith { Plantillas.inicial(it) }) }
    val h = Plantillas.normalizarHandle(handle)
    val texto = "${Compartir.texto(datos)}\n${datos.enlace}"
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { foto = leerFoto(context, it) }
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { b ->
        if (b != null) foto = b
    }
    var imagenPendiente by remember { mutableStateOf<Bitmap?>(null) }
    val permisoGuardar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        val pendiente = imagenPendiente
        imagenPendiente = null
        if (concedido && pendiente != null) guardarGaleriaConAviso(context, pendiente)
        else if (!concedido) context.aviso("Se necesita permiso para guardar la imagen en la galería")
    }
    fun lanzar(i: Intent, si: String) = try {
        context.startActivity(i)
    } catch (_: ActivityNotFoundException) {
        context.aviso(si)
    }
    fun elegirFoto() = galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun tomarFoto() = runCatching { camara.launch(null) }.onFailure { context.aviso("No hay cámara disponible") }
    fun exportar(): Bitmap = when (val op = elegida) {
        is Opcion.Sobre -> {
            val capa = Plantillas.generar(datos, op.plantilla, h)
            Plantillas.componer(foto, capa, op.plantilla, cols.getValue(op.plantilla), FormatoFoto.Historia, ajuste)
        }
        Opcion.Marca -> ImagenCompartir.generar(context, datos.copy(handle = handle), FormatoCompartir.Historia)
    }
    fun uriDe(nombre: String) = guardarParaCompartir(context, exportar(), nombre)
    fun guardar() {
        val imagen = exportar()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            imagenPendiente = imagen
            permisoGuardar.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else guardarGaleriaConAviso(context, imagen)
    }
    fun historia() {
        if (BuildConfig.FACEBOOK_APP_ID.isBlank()) {
            context.aviso("Instagram Stories requiere configurar FACEBOOK_APP_ID")
            return
        }
        val fondo = uriDe("ig-fondo")
        context.grantUriPermission(Intenciones.INSTAGRAM, fondo, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        lanzar(Intenciones.historiaInstagram(fondo, null, BuildConfig.FACEBOOK_APP_ID), "Instagram no está instalado")
    }

    Dialog(onDismissRequest = { if (editando) editando = false else onCerrar() }, properties = propiedadesHojaC2()) {
        Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
            if (editando) {
                Editor(
                    datos, elegida, foto, handle, ajuste, cols,
                    onAjuste = { ajuste = it },
                    onCols = { cols = it },
                    onHandle = { handle = it.take(31) },
                    onFoto = { elegirFoto() },
                    onCamara = { tomarFoto() },
                    onVolver = { editando = false },
                )
            } else {
                Galeria(
                    datos, foto, h, ajuste, cols, elegida,
                    onElegir = { elegida = it; editando = true },
                    onFoto = { elegirFoto() },
                    onCamara = { tomarFoto() },
                    onCerrar = onCerrar,
                )
                Barra(
                    onHistoria = { historia() },
                    onCopiar = {
                        try {
                            copiarImagen(context, uriDe("copia"))
                            context.aviso("Imagen copiada")
                        } catch (e: SecurityException) {
                            context.aviso("No se pudo copiar la imagen")
                        }
                    },
                    onGuardar = { guardar() },
                    onEnlace = {
                        copiarEnlace(context, datos.enlace)
                        context.aviso("Enlace copiado")
                    },
                    onWhatsapp = { lanzar(Intenciones.whatsapp(uriDe("wa"), texto), "WhatsApp no está instalado") },
                    onMas = { lanzar(Intenciones.selector(uriDe("gacelas-historia"), texto), "No hay apps para compartir") },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.Galeria(
    datos: DatosCompartir,
    foto: Bitmap?,
    handle: String,
    ajuste: AjusteFoto,
    cols: Map<Plantilla, Colocacion>,
    elegida: Opcion,
    onElegir: (Opcion) -> Unit,
    onFoto: () -> Unit,
    onCamara: () -> Unit,
    onCerrar: () -> Unit,
) {
    val context = LocalContext.current
    val miniaturas by produceState(emptyMap<String, Bitmap>(), foto, ajuste, cols, handle, datos) {
        val fotoActual = foto
        val ajusteActual = ajuste
        val colsActual = cols
        value = withContext(Dispatchers.Default) {
            buildMap {
                Plantilla.entries.forEach { p ->
                    val capa = Plantillas.generar(datos, p, handle)
                    val full = Plantillas.componer(fotoActual, capa, p, colsActual.getValue(p), FormatoFoto.Historia, ajusteActual)
                    capa.recycle()
                    put(p.name, reducir(full, 270))
                }
                val marca = ImagenCompartir.generar(context, datos.copy(handle = handle), FormatoCompartir.Historia)
                put(Opcion.Marca.id, reducir(marca, 270))
            }
        }
    }
    Column(Modifier.weight(1f)) {
        Cabecera(if (foto == null) "Elegir foto" else "Cambiar foto", onCerrar, Icons.Filled.Close, onFoto, onCamara)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).testTag("galeria-historias"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OPCIONES.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    fila.forEach { op ->
                        Miniatura(op, miniaturas[op.id], op == elegida, Modifier.weight(1f)) { onElegir(op) }
                    }
                    if (fila.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Editor(
    datos: DatosCompartir,
    opcion: Opcion,
    foto: Bitmap?,
    handle: String,
    ajuste: AjusteFoto,
    cols: Map<Plantilla, Colocacion>,
    onAjuste: (AjusteFoto) -> Unit,
    onCols: (Map<Plantilla, Colocacion>) -> Unit,
    onHandle: (String) -> Unit,
    onFoto: () -> Unit,
    onCamara: () -> Unit,
    onVolver: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.weight(1f).fillMaxWidth()) {
        Cabecera(opcion.etiqueta, onVolver, Icons.AutoMirrored.Filled.ArrowBack, onFoto, onCamara, "Volver")
        when (opcion) {
            is Opcion.Sobre -> {
                val plantilla = opcion.plantilla
                val dibujo = Plantillas.normalizarHandle(handle)
                var col by remember(plantilla) { mutableStateOf(cols.getValue(plantilla)) }
                val capa = remember(datos, plantilla, dibujo) { Plantillas.generar(datos, plantilla, dibujo) }
                val vista = remember(foto, capa, col, ajuste) {
                    reducir(
                        Plantillas.componer(foto, capa, plantilla, col, FormatoFoto.Historia, ajuste),
                        FormatoFoto.Historia.ancho / 2,
                    ).asImageBitmap()
                }
                var tam by remember { mutableStateOf(IntSize.Zero) }
                Vista(vista, "Vista previa: arrastra y pellizca los datos", Modifier.pointerInput(plantilla) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (tam.width == 0) return@detectTransformGestures
                        col = col.copy(
                            cx = col.cx + pan.x / tam.width,
                            cy = col.cy + pan.y / tam.height,
                            escala = col.escala * zoom,
                        ).limitada()
                        onCols(cols + (plantilla to col))
                    }
                }.onSizeChanged { tam = it })
                Text(
                    "Arrastra para mover, pellizca para cambiar el tamaño.",
                    style = GType.bodySm,
                    color = GColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Column(
                    Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Control("Brillo", ajuste.brillo * 100f, -100f..100f) { onAjuste(ajuste.copy(brillo = it / 100f)) }
                    Control("Contraste", ajuste.contraste * 100f, 0f..200f) { onAjuste(ajuste.copy(contraste = it / 100f)) }
                    Control("Saturación", ajuste.saturacion * 100f, 0f..200f) { onAjuste(ajuste.copy(saturacion = it / 100f)) }
                    Control("Opacidad", ajuste.opacidad * 100f, 0f..100f) { onAjuste(ajuste.copy(opacidad = it / 100f)) }
                    CampoTexto("Usuario a mostrar", handle, onHandle, "@usuario (respaldo @runcity)")
                }
            }
            Opcion.Marca -> {
                val vista by produceState<Bitmap?>(null, datos, handle) {
                    value = withContext(Dispatchers.Default) {
                        reducir(
                            ImagenCompartir.generar(context, datos.copy(handle = handle), FormatoCompartir.Historia),
                            FormatoFoto.Historia.ancho / 2,
                        )
                    }
                }
                val lista = vista
                if (lista != null) Vista(lista.asImageBitmap(), "Historia Runcity", Modifier)
                else Box(Modifier.fillMaxWidth().weight(1f))
                Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
                    CampoTexto("Usuario a mostrar", handle, onHandle, "@usuario (respaldo @runcity)")
                }
            }
        }
    }
}

@Composable
private fun Cabecera(
    titulo: String,
    onCerrar: () -> Unit,
    iconoCerrar: ImageVector,
    onFoto: () -> Unit,
    onCamara: () -> Unit,
    cerrar: String = "Cerrar",
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCerrar) { Icon(iconoCerrar, cerrar, tint = GColors.TextPrimary) }
        Text(titulo, color = GColors.TextPrimary, style = GType.titleMd, modifier = Modifier.weight(1f), maxLines = 1)
        IconButton(onClick = onFoto) { Icon(Icons.Outlined.AddPhotoAlternate, "Elegir foto", tint = GColors.TextPrimary) }
        IconButton(onClick = onCamara) { Icon(Icons.Filled.PhotoCamera, "Tomar foto", tint = GColors.TextPrimary) }
    }
}

@Composable
private fun ColumnScope.Vista(bitmap: ImageBitmap, descripcion: String, gestos: Modifier) {
    Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 36.dp), contentAlignment = Alignment.Center) {
        Box(
            gestos.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(18.dp)).damero().testTag("lienzo-componer"),
        ) {
            Image(bitmap, descripcion, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun Miniatura(opcion: Opcion, bitmap: Bitmap?, elegida: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(18.dp)
    Box(
        modifier.aspectRatio(9f / 16f).clip(forma).border(2.dp, if (elegida) GColors.C2Violeta else Color.Transparent, forma)
            .damero().clickable(onClick = onClick).testTag("plantilla-${opcion.id}"),
    ) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), opcion.etiqueta, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun Control(etiqueta: String, valor: Float, rango: ClosedFloatingPointRange<Float>, onCambio: (Float) -> Unit) {
    Column {
        Text("$etiqueta ${valor.toInt()}", color = GColors.TextSecondary, style = GType.bodySm)
        Slider(
            valor,
            onCambio,
            valueRange = rango,
            colors = SliderDefaults.colors(
                thumbColor = GColors.C2Violeta,
                activeTrackColor = GColors.C2Violeta,
                inactiveTrackColor = GColors.C2Linea,
            ),
        )
    }
}

@Composable
private fun Barra(
    onHistoria: () -> Unit,
    onCopiar: () -> Unit,
    onGuardar: () -> Unit,
    onEnlace: () -> Unit,
    onWhatsapp: () -> Unit,
    onMas: () -> Unit,
) {
    var mas by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp, top = 4.dp)) {
        Accion("Historia", Icons.Outlined.AutoStories, "historia-instagram", Modifier.weight(1f), onHistoria)
        Accion("Copiar", Icons.Filled.ContentCopy, "copiar-imagen", Modifier.weight(1f), onCopiar)
        Accion("Guardar", Icons.Filled.Download, "guardar-imagen", Modifier.weight(1f), onGuardar)
        Accion("Copiar enlace", Icons.Filled.Link, "copiar-enlace", Modifier.weight(1f), onEnlace)
        Box(Modifier.weight(1f)) {
            Accion("Más", Icons.Filled.MoreHoriz, "mas-compartir", Modifier.fillMaxWidth()) { mas = true }
            DropdownMenu(expanded = mas, onDismissRequest = { mas = false }) {
                DropdownMenuItem(text = { Text("WhatsApp") }, onClick = { mas = false; onWhatsapp() })
                DropdownMenuItem(text = { Text("Compartir") }, onClick = { mas = false; onMas() })
            }
        }
    }
}

@Composable
private fun Accion(etiqueta: String, icono: ImageVector, tag: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(onClick = onClick).testTag(tag).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, etiqueta, tint = GColors.TextPrimary, modifier = Modifier.size(22.dp))
        Text(etiqueta, color = GColors.TextPrimary, style = GType.bodySm, textAlign = TextAlign.Center, maxLines = 2)
    }
}
