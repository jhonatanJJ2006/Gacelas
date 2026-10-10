package com.gacelas.ui.three

import android.app.ActivityManager
import android.content.Context
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

/** Clips incluidos en gacelas-runner-animated.glb. */
enum class ClipCorredor(val nombre: String) { Idle("Idle"), Jog("Jog"), Run("Run") }

const val MODELO_CORREDOR = "models/gacelas-runner-animated.glb"

private const val TAG = "Runner3D"

/**
 * Indica si el dispositivo puede renderizar el corredor con Filament (OpenGL ES 3.0).
 * En previews, Robolectric o dispositivos sin GLES 3 se usa la imagen de reemplazo.
 */
fun soporta3D(context: Context): Boolean {
    if (Build.FINGERPRINT == "robolectric") return false
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    return am.deviceConfigurationInfo.reqGlEsVersion >= 0x30000
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
            ).apply { playAnimation(clip.nombre, loop = true) }
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
        onFrame = {
            if (autoRotar) {
                angulo = (angulo + 0.3f) % 360f
                modelo.rotation = Rotation(y = angulo)
            }
        },
    )
}

/** Imagen de reemplazo cuando el 3D no esta disponible. */
@Composable
fun Runner3DFallback(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(140.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(GColors.Primary.copy(alpha = 0.55f), GColors.Primary.copy(alpha = 0.05f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Corredor Gacelas", tint = GColors.Primary400, modifier = Modifier.size(88.dp))
            }
            Text("Corredor Gacelas", style = GType.labelMd, color = GColors.TextSecondary)
        }
    }
}
