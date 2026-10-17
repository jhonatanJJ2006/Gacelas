package com.gacelas.ui.three

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
) {
    val context = LocalContext.current
    val disponible = !LocalInspectionMode.current && remember { soporta3D(context) }
    if (!disponible) {
        Runner3DFallback(modifier)
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

