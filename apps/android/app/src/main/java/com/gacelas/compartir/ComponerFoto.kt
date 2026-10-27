package com.gacelas.compartir

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gacelas.BuildConfig
import com.gacelas.ui.aviso
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Lee una foto de la galeria reducida a ~2160 px de lado mayor. */
fun leerFoto(context: Context, uri: Uri, lado: Int = 2160): Bitmap? = runCatching {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
    var muestra = 1
    while (maxOf(o.outWidth, o.outHeight) / (muestra * 2) >= lado) muestra *= 2
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra }) }
}.getOrNull()

/** Compositor estilo Strava: foto propia + plantilla transparente que se arrastra y escala. */
@Composable
fun ComponerFotoDialogo(datos: DatosCompartir, onCerrar: () -> Unit, onImagenMarca: () -> Unit) {
    val context = LocalContext.current
    var foto by remember { mutableStateOf<Bitmap?>(null) }
    var plantilla by remember { mutableStateOf(Plantilla.TrazoDatos) }
    var formato by remember { mutableStateOf(FormatoFoto.Historia) }
    var handle by remember { mutableStateOf(HANDLE_GACELAS) }
    var col by remember { mutableStateOf(Plantillas.inicial(Plantilla.TrazoDatos)) }
    var tam by remember { mutableStateOf(IntSize.Zero) }
    val h = Plantillas.normalizarHandle(handle)
    val capa = remember(datos, plantilla, h) { Plantillas.generar(datos, plantilla, h) }
    // Vista previa a media resolucion para que arrastrar sea fluido.
    val vista = remember(foto, capa, plantilla, col, formato) {
        val f = Plantillas.componer(foto, capa, plantilla, col, formato)
        Bitmap.createScaledBitmap(f, f.width / 2, f.height / 2, true)
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { foto = leerFoto(context, it) } }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { b -> if (b != null) foto = b }
    val texto = "${Compartir.texto(datos)}\n${datos.enlace}"
    fun lanzar(i: Intent, si: String) = try { context.startActivity(i) } catch (_: ActivityNotFoundException) { context.aviso(si) }
    fun exportar() = guardarParaCompartir(context, Plantillas.componer(foto, capa, plantilla, col, formato), "gacelas-foto-${formato.name.lowercase()}")

    com.gacelas.ui.components.DialogoC2(
        onDismissRequest = onCerrar,
        containerColor = GColors.Surface,
        title = { Text("Compartir sobre tu foto", style = GType.titleMd, color = GColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pastilla(if (foto == null) "Elegir foto" else "Cambiar foto", false, { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    Pastilla("Cámara", false, { runCatching { camara.launch(null) }.onFailure { context.aviso("No hay cámara disponible") } })
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Plantilla.entries.forEach { p -> Pastilla(p.etiqueta, p == plantilla, { plantilla = p; col = Plantillas.inicial(p) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FormatoFoto.entries.forEach { f -> Pastilla(f.etiqueta, f == formato, { formato = f }) } }
                Box(
                    Modifier.fillMaxWidth().heightIn(max = 360.dp).aspectRatio(formato.ancho / formato.alto.toFloat(), matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(16.dp)).background(GColors.SurfaceContainer).align(Alignment.CenterHorizontally)
                        .onSizeChanged { tam = it }.testTag("lienzo-componer")
                        .pointerInput(plantilla) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (tam.width > 0) col = col.copy(cx = col.cx + pan.x / tam.width, cy = col.cy + pan.y / tam.height, escala = col.escala * zoom).limitada()
                            }
                        },
                ) { Image(vista.asImageBitmap(), "Vista previa: arrastra y pellizca los datos", Modifier.fillMaxWidth(), contentScale = ContentScale.Fit) }
                Text("Arrastra para mover, pellizca para cambiar el tamaño.", style = GType.bodySm, color = GColors.TextSecondary)
                CampoTexto("Usuario a mostrar", handle, { handle = it.take(31) }, "@usuario (vacío para ocultar)")
                BotonPrimario("Compartir", { lanzar(Intenciones.selector(exportar(), texto), "No hay apps para compartir") }, icono = null)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        val sticker = guardarParaCompartir(context, capa, "ig-sticker-${plantilla.name.lowercase()}")
                        val fondo = foto?.let { guardarParaCompartir(context, Plantillas.componer(it, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), plantilla, col, FormatoFoto.Historia), "ig-fondo-foto") }
                        listOfNotNull(sticker, fondo).forEach { context.grantUriPermission(Intenciones.INSTAGRAM, it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        lanzar(Intenciones.historiaInstagram(fondo, sticker, BuildConfig.FACEBOOK_APP_ID, context.packageName), "Instagram no está instalado")
                    }, modifier = Modifier.weight(1f)) { Text("Instagram Historia", color = GColors.Primary400) }
                    TextButton(onClick = { lanzar(Intenciones.whatsapp(exportar(), texto), "WhatsApp no está instalado") }, modifier = Modifier.weight(1f)) { Text("WhatsApp", color = GColors.Secondary500) }
                }
                TextButton(onClick = onImagenMarca) { Text("Usar imagen Gacelas (sin foto)", color = GColors.TextSecondary) }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar", color = GColors.TextSecondary) } },
    )
}
