package com.gacelas.ui.three

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.gacelas.identidad.domain.modelo.ConfigAvatar
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Colores del avatar por zona sobre la textura unica del GLB. La mascara (assets/models/runner-mascara.png,
 * generada con tools/avatar/generar_mascara.py) marca cada texel: 1 piel, 2 camiseta, 3 shorts, 4 zapatillas.
 * Cada zona se tine con el color elegido conservando el sombreado (luminancia) de la textura original.
 */
object RecolorAvatar {
    const val PIEL = 1
    const val CAMISETA = 2
    const val SHORTS = 3
    const val ZAPATILLAS = 4
    private const val MASCARA = "models/runner-mascara.png"
    private const val LADO = 1024

    val EQUIPACIONES = listOf<Int?>(null, 0xFF059669.toInt(), 0xFF2563EB.toInt(), 0xFFDC2626.toInt(), 0xFF111827.toInt())
    val TONOS_PIEL = listOf(0xFFF5D0B5.toInt(), 0xFFE0AC85.toInt(), 0xFFC68863.toInt(), 0xFF8D5A3B.toInt(), 0xFF5C3A24.toInt())
    val COLORES_SHORTS = listOf(0xFF111827.toInt(), 0xFF7B1FA2.toInt(), 0xFF1E3A8A.toInt(), 0xFFE5E7EB.toInt())
    val COLORES_ZAPATILLAS = listOf(0xFFF3F4F6.toInt(), 0xFF111827.toInt(), 0xFFF97316.toInt(), 0xFF22C55E.toInt())

    /** Color por zona; null deja la zona como en el modelo original. */
    fun colores(c: ConfigAvatar): Map<Int, Int> = buildMap {
        EQUIPACIONES.getOrNull(c.equipacion)?.let { put(CAMISETA, it) }
        c.tonoPiel?.let { TONOS_PIEL.getOrNull(it) }?.let { put(PIEL, it) }
        c.colorShorts?.let { COLORES_SHORTS.getOrNull(it) }?.let { put(SHORTS, it) }
        c.colorZapatillas?.let { COLORES_ZAPATILLAS.getOrNull(it) }?.let { put(ZAPATILLAS, it) }
    }

    /** Clip segun el estilo elegido: Relajado trota, Veloz corre y Dinamico queda listo para salir. */
    fun clip(c: ConfigAvatar?, porDefecto: ClipCorredor): ClipCorredor = when (c?.estilo) {
        "Relajado" -> ClipCorredor.Jog
        "Veloz" -> ClipCorredor.Run
        else -> porDefecto
    }

    private val cache = HashMap<Map<Int, Int>, Bitmap>()
    private var base: Bitmap? = null
    private var mascara: ByteArray? = null

    /**
     * Textura recoloreada (1024 px); null si no hay nada que cambiar o falla. En cache de memoria por
     * combinacion y, desde 0.9.21, en disco: tenir cuesta 1 M de texeles en el hilo principal, asi que
     * solo se hace la primera vez y despues (incluso tras cerrar la app) se lee el JPEG guardado.
     */
    @Synchronized
    fun textura(context: Context, colores: Map<Int, Int>): Bitmap? {
        if (colores.isEmpty()) return null
        cache[colores]?.let { return it }
        val archivo = archivoCache(context, colores)
        val salida = runCatching { BitmapFactory.decodeFile(archivo.path, BitmapFactory.Options().apply { inScaled = false }) }.getOrNull()
            ?: tenida(context, colores)?.also { guardar(it, archivo) } ?: return null
        if (cache.size > 6) cache.clear()
        cache[colores] = salida
        return salida
    }

    private fun tenida(context: Context, colores: Map<Int, Int>): Bitmap? {
        val b = base ?: texturaBase(context)?.also { base = it } ?: return null
        val m = mascara ?: leerMascara(context)?.also { mascara = it } ?: return null
        val px = IntArray(LADO * LADO)
        b.getPixels(px, 0, LADO, 0, 0, LADO, LADO)
        tenir(px, m, colores)
        return Bitmap.createBitmap(px, LADO, LADO, Bitmap.Config.ARGB_8888)
    }

    /** Nombre por version de la app (un modelo nuevo invalida lo guardado) y por combinacion de colores. */
    internal fun archivoCache(context: Context, colores: Map<Int, Int>): File =
        File(File(context.filesDir, "avatar3d"), "v${com.gacelas.BuildConfig.VERSION_CODE}-" +
            colores.toSortedMap().entries.joinToString("_") { "${it.key}-${Integer.toHexString(it.value)}" } + ".jpg")

    /** Escribe en otro hilo (no retrasa el primer cuadro) y borra lo de versiones anteriores. */
    private fun guardar(bmp: Bitmap, archivo: File) = Thread {
        runCatching {
            val dir = archivo.parentFile!!.apply { mkdirs() }
            dir.listFiles()?.filter { !it.name.startsWith("v${com.gacelas.BuildConfig.VERSION_CODE}-") }?.forEach { it.delete() }
            val tmp = File(dir, archivo.name + ".tmp")
            tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            tmp.renameTo(archivo)
        }
    }.start()

    /**
     * Tine en sitio: por zona se calcula la luminancia media y cada texel toma el color objetivo
     * multiplicado por su luminancia relativa (sombras y pliegues se mantienen).
     */
    fun tenir(px: IntArray, mascara: ByteArray, colores: Map<Int, Int>) {
        val suma = DoubleArray(5); val n = IntArray(5)
        for (i in px.indices) {
            val z = mascara[i].toInt()
            if (z in colores) { suma[z] += luminancia(px[i]); n[z]++ }
        }
        for (i in px.indices) {
            val z = mascara[i].toInt()
            val objetivo = colores[z] ?: continue
            val media = if (n[z] > 0) suma[z] / n[z] else 128.0
            val f = (luminancia(px[i]) / media).coerceIn(0.0, 1.8)
            fun canal(c: Int) = (c * f).toInt().coerceIn(0, 255)
            px[i] = (0xFF shl 24) or (canal((objetivo shr 16) and 0xFF) shl 16) or (canal((objetivo shr 8) and 0xFF) shl 8) or canal(objetivo and 0xFF)
        }
    }

    private fun luminancia(c: Int) = 0.299 * ((c shr 16) and 0xFF) + 0.587 * ((c shr 8) and 0xFF) + 0.114 * (c and 0xFF)

    /** Mascara 512 px escalada (vecino mas cercano) al lado de la textura. */
    private fun leerMascara(context: Context): ByteArray? = runCatching {
        val bmp = context.assets.open(MASCARA).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inScaled = false }) }!!
        val escalada = Bitmap.createScaledBitmap(bmp, LADO, LADO, false)
        val px = IntArray(LADO * LADO)
        escalada.getPixels(px, 0, LADO, 0, 0, LADO, LADO)
        // Escala de grises: el valor de la zona esta en cualquier canal.
        ByteArray(px.size) { (px[it] and 0xFF).toByte() }
    }.getOrNull()

    /** Textura base del GLB (imagen del baseColorTexture), a 1024 px (el GLB ya la trae a ese tamano desde 0.9.21). */
    private fun texturaBase(context: Context): Bitmap? = runCatching {
        val d = context.assets.open(MODELO_CORREDOR).use { it.readBytes() }
        val bb = ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN)
        val largo = bb.getInt(12)
        val json = JSONObject(String(d, 20, largo, Charsets.UTF_8))
        val ini = 20 + largo + 8
        val tex = json.getJSONArray("materials").getJSONObject(0).getJSONObject("pbrMetallicRoughness").getJSONObject("baseColorTexture").getInt("index")
        val img = json.getJSONArray("images").getJSONObject(json.getJSONArray("textures").getJSONObject(tex).getInt("source"))
        val vista = json.getJSONArray("bufferViews").getJSONObject(img.getInt("bufferView"))
        val o = ini + vista.optInt("byteOffset", 0)
        val bmp = BitmapFactory.decodeByteArray(d, o, vista.getInt("byteLength"))!!
        Bitmap.createScaledBitmap(bmp, LADO, LADO, true)
    }.getOrNull()
}
