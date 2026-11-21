package com.gacelas.compartir

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

/** Guarda el bitmap en cache/compartir y devuelve un content:// del FileProvider. */
fun guardarParaCompartir(context: Context, bmp: Bitmap, nombre: String): Uri {
    val dir = File(context.cacheDir, "compartir").apply { mkdirs() }
    val f = File(dir, "$nombre.png")
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return FileProvider.getUriForFile(context, "${context.packageName}.archivos", f)
}

/** Guarda el PNG en Fotos/Galería y devuelve su URI para compartir o copiar. */
fun guardarEnGaleria(context: Context, bmp: Bitmap): Uri {
    val nombre = "runcity-${System.currentTimeMillis()}.png"
    val valores = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, nombre)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Runcity")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Runcity")
            if (!dir.exists() && !dir.mkdirs()) throw IOException("No se pudo crear la carpeta de imágenes")
            put(MediaStore.Images.Media.DATA, File(dir, nombre).absolutePath)
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores)
        ?: throw IOException("No se pudo crear la imagen en la galería")
    try {
        val stream = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("No se pudo escribir la imagen")
        stream.use { if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("No se pudo codificar el PNG") }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        }
        return uri
    } catch (e: IOException) {
        context.contentResolver.delete(uri, null, null)
        throw e
    } catch (e: SecurityException) {
        context.contentResolver.delete(uri, null, null)
        throw e
    }
}

fun copiarImagen(context: Context, uri: Uri) {
    val clip = ClipData.newUri(context.contentResolver, "Imagen Runcity", uri)
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
}

fun copiarEnlace(context: Context, enlace: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText("Enlace Runcity", enlace))
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
    fun historiaInstagram(fondo: Uri?, sticker: Uri?, appId: String): Intent {
        require(appId.isNotBlank()) { "FACEBOOK_APP_ID no está configurado" }
        return Intent("com.instagram.share.ADD_TO_STORY").apply {
            putExtra("source_application", appId)
            // Sin fondo: Instagram deja elegir la foto al usuario y pone el sticker encima.
            if (fondo != null) setDataAndType(fondo, "image/png") else type = "image/png"
            if (sticker != null) putExtra("interactive_asset_uri", sticker)
            putExtra("top_background_color", "#0B1220")
            putExtra("bottom_background_color", "#111827")
            setPackage(INSTAGRAM)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

/** Hoja para elegir una plantilla de historia y compartirla. */
@Composable
fun CompartirDialogo(datos: DatosCompartir, onCerrar: () -> Unit) = ComponerFotoDialogo(datos, onCerrar)
