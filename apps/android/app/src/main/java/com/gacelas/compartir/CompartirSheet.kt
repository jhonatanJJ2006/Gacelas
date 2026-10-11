package com.gacelas.compartir

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.gacelas.BuildConfig
import com.gacelas.ui.aviso
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Guarda el bitmap en cache/compartir y devuelve un content:// del FileProvider. */
fun guardarParaCompartir(context: Context, bmp: Bitmap, nombre: String): Uri {
    val dir = File(context.cacheDir, "compartir").apply { mkdirs() }
    val f = File(dir, "$nombre.png")
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return FileProvider.getUriForFile(context, "${context.packageName}.archivos", f)
}

object Intenciones {
    const val INSTAGRAM = "com.instagram.android"
    const val WHATSAPP = "com.whatsapp"

    fun selector(uri: Uri, texto: String): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, texto)
            clipData = ClipData.newRawUri("gacelas", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
        "Compartir con",
    )

    fun whatsapp(uri: Uri, texto: String) = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        setPackage(WHATSAPP)
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, texto)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /**
     * Historia de Instagram: fondo = historia 1080x1920 y, si hay, sticker encima.
     * https://developers.facebook.com/docs/instagram-platform/sharing-to-stories
     */
    fun historiaInstagram(fondo: Uri, sticker: Uri?, appId: String, paquete: String) = Intent("com.instagram.share.ADD_TO_STORY").apply {
        putExtra("source_application", appId.ifBlank { paquete })
        setDataAndType(fondo, "image/png")
        if (sticker != null) putExtra("interactive_asset_uri", sticker)
        putExtra("top_background_color", "#0B1220")
        putExtra("bottom_background_color", "#111827")
        setPackage(INSTAGRAM)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

/** Dialogo para compartir una ruta o carrera como historia, post o sticker. */
@Composable
fun CompartirDialogo(datos: DatosCompartir, onCerrar: () -> Unit) {
    val context = LocalContext.current
    var formato by remember { mutableStateOf(FormatoCompartir.Historia) }
    val bmp by produceState<Bitmap?>(null, datos, formato) {
        value = null
        value = withContext(Dispatchers.Default) { ImagenCompartir.generar(context, datos, formato) }
    }
    val texto = "${Compartir.texto(datos)}\n${datos.enlace}"
    fun lanzar(i: Intent, si: String) = try { context.startActivity(i) } catch (_: ActivityNotFoundException) { context.aviso(si) }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = GColors.Surface,
        title = { Text("Compartir", style = GType.titleMd, color = GColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FormatoCompartir.entries.forEach { f -> Pastilla(f.etiqueta, f == formato, { formato = f }) } }
                Box(
                    Modifier.fillMaxWidth().heightIn(max = 340.dp).aspectRatio(formato.ancho / formato.alto.toFloat(), matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(16.dp)).background(GColors.SurfaceContainer).align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center,
                ) {
                    bmp?.let { Image(it.asImageBitmap(), "Vista previa ${formato.ancho}×${formato.alto}", Modifier.fillMaxWidth(), contentScale = ContentScale.Fit) }
                        ?: Text("Generando...", color = GColors.TextSecondary)
                }
                BotonPrimario("Compartir imagen", {
                    bmp?.let { lanzar(Intenciones.selector(guardarParaCompartir(context, it, "gacelas-${formato.name.lowercase()}"), texto), "No hay apps para compartir") }
                }, enabled = bmp != null, icono = null)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val fondo = guardarParaCompartir(context, ImagenCompartir.generar(context, datos, FormatoCompartir.Historia), "ig-fondo")
                        val sticker = guardarParaCompartir(context, ImagenCompartir.generar(context, datos, FormatoCompartir.Sticker), "ig-sticker")
                        context.grantUriPermission(Intenciones.INSTAGRAM, fondo, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.grantUriPermission(Intenciones.INSTAGRAM, sticker, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        // El fondo ya incluye todo; el sticker se ofrece solo si se eligio ese formato.
                        lanzar(Intenciones.historiaInstagram(fondo, if (formato == FormatoCompartir.Sticker) sticker else null, BuildConfig.FACEBOOK_APP_ID, context.packageName), "Instagram no está instalado")
                    }, modifier = Modifier.weight(1f)) { Text("Instagram Historia", color = GColors.Primary400) }
                    TextButton(onClick = {
                        bmp?.let { lanzar(Intenciones.whatsapp(guardarParaCompartir(context, it, "wa"), texto), "WhatsApp no está instalado") }
                    }, modifier = Modifier.weight(1f)) { Text("WhatsApp", color = GColors.Secondary500) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar", color = GColors.TextSecondary) } },
        modifier = Modifier.padding(8.dp),
    )
}
