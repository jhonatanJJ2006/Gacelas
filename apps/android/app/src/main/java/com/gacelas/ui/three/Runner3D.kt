package com.gacelas.ui.three

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader

/**
 * Clips incluidos en gacelas-runner-animated.glb. [alturaY] baja el modelo para que quede
 * centrado en el origen: cada clip deja la cadera a una altura distinta (medido en emulador).
 */
enum class ClipCorredor(val nombre: String, val alturaY: Float) { Idle("Idle", -1.8f), Jog("Jog", -0.9f), Run("Run", -0.9f) }

const val MODELO_CORREDOR = "models/gacelas-runner-animated.glb"

private const val TAG = "Runner3D"

/**
 * Indica si el dispositivo puede renderizar el corredor con Filament (OpenGL ES 3.0).
 * En previews, Robolectric o dispositivos sin GLES 3 se usa la imagen de reemplazo.
 */
fun soporta3D(context: Context): Boolean {
    if (Build.FINGERPRINT == "robolectric") return false
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    if (am.deviceConfigurationInfo.reqGlEsVersion < 0x30000) return false
    return !rendererProblematico(renderizadorGl)
}

/** Filament se cae (SIGSEGV en FEngine::loop) con el traductor SwiftShader del emulador. */
fun rendererProblematico(renderer: String?) = renderer?.contains("SwiftShader", ignoreCase = true) == true

/** GL_RENDERER del dispositivo, leido una vez con un contexto EGL temporal. */
private val renderizadorGl: String? by lazy {
    runCatching {
        val d = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val v = IntArray(2)
        EGL14.eglInitialize(d, v, 0, v, 1)
        val cfg = arrayOfNulls<EGLConfig>(1)
        val n = IntArray(1)
        EGL14.eglChooseConfig(d, intArrayOf(EGL14.EGL_RENDERABLE_TYPE, 0x40 /* EGL_OPENGL_ES3_BIT */, EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE), 0, cfg, 0, 1, n, 0)
        val ctx = EGL14.eglCreateContext(d, cfg[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0)
        val sup = EGL14.eglCreatePbufferSurface(d, cfg[0], intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
        EGL14.eglMakeCurrent(d, sup, sup, ctx)
        val r = GLES20.glGetString(GLES20.GL_RENDERER)
        EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(d, sup)
        EGL14.eglDestroyContext(d, ctx)
        r
    }.onSuccess { Log.i(TAG, "GL_RENDERER: $it") }.onFailure { Log.w(TAG, "No se pudo leer GL_RENDERER", it) }.getOrNull()
}

/**
 * Corredor 3D animado. Si el 3D no esta disponible o el modelo falla al cargar,
 * muestra [Runner3DFallback] en lugar de dejar la pantalla vacia.
 */
@Composable
fun Runner3D(
    clip: ClipCorredor,
    modifier: Modifier = Modifier,
    autoRotar: Boolean = false,
    camara: Position = Position(0f, 1.2f, 3.5f),
    /** Giro del corredor (grados, horario) respecto a "hacia adelante" en pantalla. */
    rumboRelativo: Float = 0f,
    /**
     * Fondo de Compose visible detras del corredor (Inicio). Desde 0.9.5 usa Filament en un TextureView
     * translucido (Runner3DFilamentTransparente); el SurfaceView de SceneView perforaba la ventana en negro y
     * el WebView transparente de 0.9.1-0.9.4 no se dibujaba en telefonos reales.
     */
    sobreFondo: Boolean = false,
) {
    val context = LocalContext.current
    val filament = !LocalInspectionMode.current && remember { soporta3D(context) }
    // Inicio (sobreFondo): Filament en un TextureView translucido. Desde 0.9.1 aqui se usaba el WebView
    // transparente y en telefonos reales el corredor no se dibujaba; 0.9.0 (Filament) si se veia.
    val motor = motorCorredor(sobreFondo, filament)
    if (motor == MotorCorredor.FilamentTranslucido) { Runner3DFilamentTransparente(clip, modifier, autoRotar, camara); return }
    if (motor == MotorCorredor.WebGL) {
        // Sin Filament (GPU emulada con SwiftShader, drivers problematicos) se dibuja el mismo GLB con WebGL.
        if (LocalInspectionMode.current || Build.FINGERPRINT == "robolectric") Runner3DFallback(modifier)
        else Runner3DWeb(clip, modifier, autoRotar, camara, rumboRelativo)
        return
    }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelo = remember(clip) {
        runCatching {
            ModelNode(
                modelInstance = modelLoader.createModelInstance(MODELO_CORREDOR),
                autoAnimate = false,
                scaleToUnits = 1.8f,
                centerOrigin = Position(0f, 0f, 0f),
            ).apply {
                // centerOrigin no centra este GLB animado (queda con los pies en el origen):
                // se baja medio cuerpo para que la camara, que mira al origen, lo encuadre entero.
                position = Position(0f, clip.alturaY, 0f)
                playAnimation(clip.nombre, loop = true)
            }
        }.onFailure { Log.w(TAG, "No se pudo cargar el corredor 3D", it) }.getOrNull()
    }
    if (modelo == null) {
        Runner3DFallback(modifier)
        return
    }
    val camaraNode = rememberCameraNode(engine) {
        position = camara
        lookAt(Position(0f, 0f, 0f))
    }
    var angulo = 0f
    Scene(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        isOpaque = false,
        cameraNode = camaraNode,
        cameraManipulator = null,
        childNodes = listOf(modelo),
        // Con isOpaque=false el SurfaceView se pone encima de toda la ventana (setZOrderOnTop) y tapaba
        // el panel del cronometro y la tarjeta. Como capa de medios queda sobre el mapa y bajo la UI.
        onViewCreated = { setZOrderOnTop(false); setZOrderMediaOverlay(true) },
        onFrame = {
            if (!autoRotar) modelo.rotation = Rotation(y = -rumboRelativo)
            if (autoRotar) {
                angulo = (angulo + 0.3f) % 360f
                modelo.rotation = Rotation(y = angulo)
            }
        },
    )
}


private const val HOST_ASSETS = "https://appassets.androidplatform.net/assets/"

/**
 * Corredor 3D con three.js en un WebView transparente (assets/runner3d). Mismo modelo y clips que
 * la version Filament; se usa cuando el dispositivo no soporta Filament.
 */
@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable
fun Runner3DWeb(
    clip: ClipCorredor,
    modifier: Modifier = Modifier,
    autoRotar: Boolean = false,
    camara: Position = Position(0f, 1.2f, 3.5f),
    rumboRelativo: Float = 0f,
) {
    val url = remember(autoRotar, camara) {
        "${HOST_ASSETS}runner3d/index.html?clip=${clip.nombre}&rotar=${if (autoRotar) 1 else 0}&cam=${camara.x},${camara.y},${camara.z}"
    }
    // El respaldo se dibuja DEBAJO del WebView hasta que runner3d.js confirma un cuadro con pixeles del
    // corredor (GacelasCorredor.primerCuadro). Que la pagina cargue no basta: un WebView puede cargar bien y
    // pintar transparente, y en ese caso el escenario quedaba vacio sin ningun error. El GLB va en assets.
    var visto by remember { androidx.compose.runtime.mutableStateOf(false) }
    var fallo by remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(modifier.fillMaxSize()) {
    if (!visto || fallo) Runner3DFallback(Modifier.testTag("corredor-3d-respaldo"))
    if (!fallo) androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier.fillMaxSize().then(Modifier.testTag("corredor-3d-webgl")),
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                setOnTouchListener { _, _ -> false }
                isClickable = false
                isFocusable = false
                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun primerCuadro() { post { visto = true } }
                }, "GacelasCorredor")
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun shouldInterceptRequest(view: android.webkit.WebView, request: android.webkit.WebResourceRequest): android.webkit.WebResourceResponse? {
                        val u = request.url.toString()
                        if (!u.startsWith(HOST_ASSETS)) return null
                        val ruta = request.url.path.orEmpty().removePrefix("/assets/")
                        val tipo = when {
                            ruta.endsWith(".html") -> "text/html"
                            ruta.endsWith(".js") -> "application/javascript"
                            ruta.endsWith(".glb") -> "model/gltf-binary"
                            else -> "application/octet-stream"
                        }
                        return runCatching { android.webkit.WebResourceResponse(tipo, "utf-8", ctx.assets.open(ruta)) }
                            .onFailure { Log.w(TAG, "Asset no encontrado: $ruta", it) }.getOrNull()
                    }

                    @androidx.annotation.RequiresApi(26)
                    override fun onRenderProcessGone(view: android.webkit.WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                        Log.w(TAG, "WebView del corredor 3D cerrado"); fallo = true; return true
                    }
                }
                loadUrl(url)
            }
        },
        update = { w ->
            w.evaluateJavascript("window.gacelasClip&&gacelasClip('${clip.nombre}');window.gacelasRumbo&&gacelasRumbo($rumboRelativo)", null)
        },
        onRelease = { it.destroy() },
    )
    }
}


enum class MotorCorredor { Filament, FilamentTranslucido, WebGL }

/**
 * Que render usa el corredor. Con Filament disponible nunca se usa el WebView: en 0.9.1-0.9.4 Inicio
 * (sobreFondo) pasaba al WebView transparente y el corredor no se dibujaba en telefonos reales.
 */
fun motorCorredor(sobreFondo: Boolean, filament: Boolean): MotorCorredor = when {
    !filament -> MotorCorredor.WebGL
    sobreFondo -> MotorCorredor.FilamentTranslucido
    else -> MotorCorredor.Filament
}

/**
 * Corredor de Inicio con Filament sobre un TextureView translucido: se compone con la UI de Compose
 * (escenario C2 detras, etiquetas y boton delante), a diferencia del SurfaceView de SceneView que perfora
 * la ventana en negro. Vista TRANSLUCENT, limpieza con alfa 0 y sin skybox. El GLB sale de los assets del
 * APK (sin red). El respaldo queda debajo hasta que el TextureView entrega un cuadro con pixeles.
 */
@Composable
fun Runner3DFilamentTransparente(
    clip: ClipCorredor,
    modifier: Modifier = Modifier,
    autoRotar: Boolean = false,
    camara: Position = Position(0f, 1.2f, 3.5f),
) {
    var visto by remember { androidx.compose.runtime.mutableStateOf(false) }
    var fallo by remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        if (!visto || fallo) Runner3DFallback(Modifier.testTag("corredor-3d-respaldo"))
        if (!fallo) androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize().testTag("corredor-3d-filament"),
            factory = { ctx ->
                val tv = android.view.TextureView(ctx).apply { isOpaque = false }
                runCatching { CorredorFilament(ctx, tv, clip.nombre, autoRotar, camara) { visto = true } }
                    .onSuccess { tv.tag = it }
                    .onFailure { Log.w(TAG, "Filament no pudo iniciar el corredor", it); fallo = true }
                tv
            },
            onRelease = { (it.tag as? CorredorFilament)?.cerrar() },
        )
    }
}

/** Bucle de render: Choreographer -> animacion + giro -> ModelViewer.render. */
internal class CorredorFilament(
    ctx: android.content.Context,
    private val tv: android.view.TextureView,
    clip: String,
    private val autoRotar: Boolean,
    camara: Position,
    private val onPrimerCuadro: () -> Unit,
) : android.view.Choreographer.FrameCallback {
    private val engine: com.google.android.filament.Engine
    private val uiHelper = com.google.android.filament.android.UiHelper(com.google.android.filament.android.UiHelper.ContextErrorPolicy.DONT_CHECK).apply { isOpaque = false }
    private val viewer: com.google.android.filament.utils.ModelViewer
    private val indice: Int
    private val escala: Float
    private val centro: com.google.android.filament.utils.Float3
    private var inicio = -1L
    private var cuadros = 0
    private var visto = false
    private var activo = true

    init {
        com.google.android.filament.utils.Utils.init()
        engine = com.google.android.filament.Engine.create()
        val manipulador = com.google.android.filament.utils.Manipulator.Builder()
            .targetPosition(0f, 0f, 0f)
            .orbitHomePosition(camara.x, camara.y, camara.z)
            .viewport(1, 1)
            .build(com.google.android.filament.utils.Manipulator.Mode.ORBIT)
        viewer = com.google.android.filament.utils.ModelViewer(tv, engine, uiHelper, manipulador)
        viewer.view.blendMode = com.google.android.filament.View.BlendMode.TRANSLUCENT
        viewer.scene.skybox = null
        viewer.renderer.clearOptions = viewer.renderer.clearOptions.apply { clear = true; clearColor = floatArrayOf(0f, 0f, 0f, 0f) }
        val bytes = ctx.assets.open(MODELO_CORREDOR).use { it.readBytes() }
        viewer.loadModelGlb(java.nio.ByteBuffer.allocateDirect(bytes.size).put(bytes).apply { rewind() })
        val asset = checkNotNull(viewer.asset) { "GLB sin escena" }
        // ModelViewer ya trae una luz direccional; se agrega un IBL plano para que el PBR no quede oscuro.
        viewer.scene.indirectLight = com.google.android.filament.IndirectLight.Builder()
            .irradiance(1, floatArrayOf(0.9f, 0.85f, 1f)).intensity(30_000f).build(engine)
        val animador = viewer.animator
        indice = (0 until (animador?.animationCount ?: 0)).firstOrNull { animador?.getAnimationName(it) == clip } ?: 0
        val caja = asset.boundingBox
        centro = com.google.android.filament.utils.Float3(caja.center[0], caja.center[1], caja.center[2])
        escala = 0.9f / caja.halfExtent[1].coerceAtLeast(0.001f)
        android.view.Choreographer.getInstance().postFrameCallback(this)
    }

    override fun doFrame(t: Long) {
        if (!activo) return
        android.view.Choreographer.getInstance().postFrameCallback(this)
        if (inicio < 0) inicio = t
        val s = (t - inicio) / 1e9f
        val asset = viewer.asset ?: return
        viewer.animator?.let { a ->
            if (a.animationCount > 0) { a.applyAnimation(indice, s % a.getAnimationDuration(indice)); a.updateBoneMatrices() }
        }
        val giro = if (autoRotar) (s * 18f) % 360f else 0f
        val m = com.google.android.filament.utils.rotation(com.google.android.filament.utils.Float3(0f, 1f, 0f), giro) *
            com.google.android.filament.utils.scale(com.google.android.filament.utils.Float3(escala)) *
            com.google.android.filament.utils.translation(-centro)
        val tm = engine.transformManager
        tm.setTransform(tm.getInstance(asset.root), com.google.android.filament.utils.transpose(m).toFloatArray())
        viewer.render(t)
        if (!visto && ++cuadros % 10 == 0 && tv.isAvailable) {
            val b = tv.getBitmap(48, 48)
            if (b != null && tienePixeles(b)) { visto = true; onPrimerCuadro() }
            b?.recycle()
        }
    }

    fun cerrar() {
        activo = false
        android.view.Choreographer.getInstance().removeFrameCallback(this)
        runCatching { viewer.destroyModel(); uiHelper.detach(); engine.destroy() }
    }
}

/** true si el cuadro tiene algun pixel no transparente (hay corredor dibujado). */
internal fun tienePixeles(b: android.graphics.Bitmap): Boolean {
    for (y in 0 until b.height step 2) for (x in 0 until b.width step 2) if (android.graphics.Color.alpha(b.getPixel(x, y)) > 0) return true
    return false
}
