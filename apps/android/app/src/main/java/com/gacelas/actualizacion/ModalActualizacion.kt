package com.gacelas.actualizacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gacelas.BuildConfig
import com.gacelas.data.actualizacion.Aviso
import com.gacelas.data.actualizacion.InfoVersion
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.BotonSecundario
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.launch
import java.io.File

private sealed interface Fase {
    data object Info : Fase
    data class Descargando(val porcentaje: Int) : Fase
    data class Permiso(val archivo: File) : Fase
    data class Error(val mensaje: String) : Fase
}

/** Revisa al abrir la app (cache diaria) y muestra el modal si hay una version nueva. */
@Composable
fun AvisoActualizacion() {
    val repo = contenedor().actualizacion ?: return
    var aviso by remember { mutableStateOf<Aviso.Nueva?>(null) }
    LaunchedEffect(Unit) { aviso = repo.revisar() as? Aviso.Nueva }
    val scope = rememberCoroutineScope()
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

    // Al volver de Ajustes ("Instalar apps desconocidas") se sigue con la instalacion.
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val obs = LifecycleEventObserver { _, e ->
            val f = fase
            if (e == Lifecycle.Event.ON_RESUME && f is Fase.Permiso && Instalador.puedeInstalar(context)) Instalador.instalar(context, f.archivo)
        }
        ciclo.addObserver(obs)
        onDispose { ciclo.removeObserver(obs) }
    }

    fun actualizar() {
        scope.launch {
            Instalador.descargar(context, info.apkUrl, info.versionName, info.sha256).collect { d ->
                when (d) {
                    is Descarga.Progreso -> fase = Fase.Descargando(d.porcentaje)
                    is Descarga.Error -> fase = Fase.Error(d.mensaje)
                    is Descarga.Lista -> if (Instalador.puedeInstalar(context)) {
                        fase = Fase.Info
                        Instalador.instalar(context, d.archivo)
                    } else fase = Fase.Permiso(d.archivo)
                }
            }
        }
    }

    Dialog(onDismissRequest = { if (!obligatoria && fase !is Fase.Descargando) onMasTarde() }, properties = DialogProperties(dismissOnClickOutside = false)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(GColors.BgSecondary).padding(20.dp).testTag("modal-actualizacion"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Hay una nueva versión", style = GType.headlineSm, color = GColors.TextPrimary)
            Text("Gacelas ${info.versionName}  ·  tienes ${BuildConfig.VERSION_NAME}", style = GType.labelMd, color = GColors.Primary400)
            if (info.notes.isNotBlank()) Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                Text(info.notes, style = GType.bodyMd, color = GColors.TextSecondary)
            }
            when (val f = fase) {
                is Fase.Descargando -> {
                    Text("Descargando ${f.porcentaje}%", style = GType.bodyMd, color = GColors.TextPrimary)
                    LinearProgressIndicator(progress = { f.porcentaje / 100f }, modifier = Modifier.fillMaxWidth(), color = GColors.Primary)
                }
                is Fase.Permiso -> {
                    Text("Para instalar, permite a Gacelas instalar apps desconocidas. Luego vuelve aquí.", style = GType.bodyMd, color = GColors.TextPrimary)
                    BotonPrimario("Dar permiso", { Instalador.pedirPermiso(context) })
                }
                is Fase.Error -> {
                    Text(f.mensaje, style = GType.bodyMd, color = GColors.Danger)
                    BotonPrimario("Reintentar", { actualizar() })
                }
                Fase.Info -> {
                    if (obligatoria) Text("Esta actualización es necesaria para seguir usando la app.", style = GType.bodySm, color = GColors.TextSecondary)
                    BotonPrimario("Actualizar", { actualizar() })
                }
            }
            if (!obligatoria && fase !is Fase.Descargando) BotonSecundario("Más tarde", onMasTarde, Modifier.fillMaxWidth())
        }
    }
}
