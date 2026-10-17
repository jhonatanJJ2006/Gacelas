package com.gacelas.actualizacion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gacelas.BuildConfig
import com.gacelas.ui.theme.SpaceGrotesk
import com.gacelas.data.actualizacion.Aviso
import com.gacelas.data.actualizacion.InfoVersion
import com.gacelas.data.actualizacion.ReglasActualizacion
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.Inter
import kotlinx.coroutines.launch
import java.io.File

/** Revisa al abrir la app (cache diaria) y muestra el modal si hay una version nueva. */
@Composable
fun AvisoActualizacion() {
    val repo = contenedor().actualizacion ?: return
    var aviso by remember { mutableStateOf<Aviso.Nueva?>(null) }
    val scope = rememberCoroutineScope()
    // Al abrir y cada vez que la app vuelve a primer plano (ON_START).
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_START) scope.launch { (repo.revisar() as? Aviso.Nueva)?.let { nueva -> if (aviso == null) aviso = nueva } }
        }
        ciclo.addObserver(obs)
        onDispose { ciclo.removeObserver(obs) }
    }
    aviso?.let { a ->
        ModalActualizacion(a.info, a.obligatoria, onMasTarde = {
            scope.launch { repo.posponer(a.info.versionCode) }
            aviso = null
        })
    }
}

@Composable
fun ModalActualizacion(info: InfoVersion, obligatoria: Boolean, onMasTarde: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fase by remember { mutableStateOf<Fase>(Fase.Info) }
    var bytes by remember { mutableStateOf(info.apkBytes) }
    LaunchedEffect(info.apkUrl) { if (bytes == null && !info.apkUrl.startsWith("https://example")) bytes = Instalador.tamano(info.apkUrl) }

    // Al volver de Ajustes ("Instalar apps desconocidas") se sigue con la instalacion.
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val obs = LifecycleEventObserver { _, e ->
            val f = fase
            if (e == Lifecycle.Event.ON_RESUME && f is Fase.Permiso && Instalador.puedeInstalar(context)) Instalador.instalar(context, java.io.File(f.ruta))
        }
        ciclo.addObserver(obs)
        onDispose { ciclo.removeObserver(obs) }
    }

    fun actualizar() {
        fase = Fase.Descargando(0, 0, bytes ?: 0)
        scope.launch {
            Instalador.descargar(context, info.apkUrl, info.versionName, info.sha256).collect { d ->
                when (d) {
                    is Descarga.Progreso -> fase = Fase.Descargando(d.porcentaje, d.hechos, if (d.total > 0) d.total else bytes ?: 0)
                    is Descarga.Error -> fase = Fase.Error(d.mensaje)
                    is Descarga.Lista -> if (Instalador.puedeInstalar(context)) {
                        fase = Fase.Info
                        Instalador.instalar(context, d.archivo)
                    } else fase = Fase.Permiso(d.archivo.path)
                }
            }
        }
    }

    val cerrable = !obligatoria && fase !is Fase.Descargando
    Dialog(
        onDismissRequest = { if (cerrable) onMasTarde() },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = !obligatoria, usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // Sin backdrop-blur en Compose: la tarjeta va al 98% (Stitch 95% + blur-xl) para que no se lea lo de atrás.
        // El scrim es el del diseño (negro 60%); se quita el atenuado propio del Dialog para no oscurecer doble.
        (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setDimAmount(0f)
        ContenidoModal(
            versionActual = BuildConfig.VERSION_NAME, info = info, obligatoria = obligatoria, fase = fase, bytes = bytes,
            onActualizar = { actualizar() }, onMasTarde = onMasTarde, onPermiso = { Instalador.pedirPermiso(context) },
        )
    }
}

/** "Versión 0.6.8 (608)", visible en Ajustes y en la bienvenida. */
fun textoVersion(nombre: String = BuildConfig.VERSION_NAME, codigo: Int = BuildConfig.VERSION_CODE) = "Versión $nombre ($codigo)"
