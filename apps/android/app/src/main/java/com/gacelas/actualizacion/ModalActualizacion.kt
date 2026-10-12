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
import com.gacelas.acceso.SpaceGrotesk
import com.gacelas.data.actualizacion.Aviso
import com.gacelas.data.actualizacion.InfoVersion
import com.gacelas.data.actualizacion.ReglasActualizacion
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.Inter
import kotlinx.coroutines.launch
import java.io.File

internal sealed interface Fase {
    data object Info : Fase
    data class Descargando(val porcentaje: Int, val hechos: Long, val total: Long) : Fase
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
    var bytes by remember { mutableStateOf(info.apkBytes) }
    LaunchedEffect(info.apkUrl) { if (bytes == null && !info.apkUrl.startsWith("https://example")) bytes = Instalador.tamano(info.apkUrl) }

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
        fase = Fase.Descargando(0, 0, bytes ?: 0)
        scope.launch {
            Instalador.descargar(context, info.apkUrl, info.versionName, info.sha256).collect { d ->
                when (d) {
                    is Descarga.Progreso -> fase = Fase.Descargando(d.porcentaje, d.hechos, if (d.total > 0) d.total else bytes ?: 0)
                    is Descarga.Error -> fase = Fase.Error(d.mensaje)
                    is Descarga.Lista -> if (Instalador.puedeInstalar(context)) {
                        fase = Fase.Info
                        Instalador.instalar(context, d.archivo)
                    } else fase = Fase.Permiso(d.archivo)
                }
            }
        }
    }

    val cerrable = !obligatoria && fase !is Fase.Descargando
    Dialog(
        onDismissRequest = { if (cerrable) onMasTarde() },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = !obligatoria, usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // El scrim es el del diseño (negro 60%); se quita el atenuado propio del Dialog para no oscurecer doble.
        (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setDimAmount(0f)
        ContenidoModal(
            versionActual = BuildConfig.VERSION_NAME, info = info, obligatoria = obligatoria, fase = fase, bytes = bytes,
            onActualizar = { actualizar() }, onMasTarde = onMasTarde, onPermiso = { Instalador.pedirPermiso(context) },
        )
    }
}

/** Colores y medidas del export de Stitch (update-modal V3 "Compacta"). */
internal object M3 {
    val Tarjeta = Color(0xFF131926)
    val Panel = Color(0xFF1C2028)
    val Secondary = Color(0xFFBCC7DE)
    val Naranja = Color(0xFFD97706)
    val Enlace = Color(0xFFF59E0B)
    val Magenta = Color(0xFFD6338A)
    val MagentaOscuro = Color(0xFFAB0660)
    val Error = Color(0xFFFFB4AB)
    fun blanco(a: Float) = Color.White.copy(alpha = a)
}

private val display = SpaceGrotesk

/** Contenido del modal (sin Dialog, para tests y previas). Sin scroll: en poco alto se compacta. */
@Composable
internal fun ContenidoModal(
    versionActual: String,
    info: InfoVersion,
    obligatoria: Boolean,
    fase: Fase,
    bytes: Long?,
    onActualizar: () -> Unit,
    onMasTarde: () -> Unit,
    onPermiso: () -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).systemBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val compacto = maxHeight < 560.dp
        val maxItems = if (maxHeight < 440.dp) 2 else if (compacto) 3 else 5
        val forma = RoundedCornerShape(28.dp)
        Box(
            Modifier.widthIn(max = 350.dp).fillMaxWidth()
                .shadow(20.dp, forma, ambientColor = Color.Black.copy(alpha = 0.65f), spotColor = Color.Black.copy(alpha = 0.65f))
                .clip(forma).background(M3.Tarjeta.copy(alpha = 0.95f)).border(1.dp, M3.blanco(0.10f), forma)
                .drawBehind {
                    // Brillo ambiental arriba a la derecha (-top-16 -right-16 w-36, #D6338A/10, blur-2xl).
                    drawCircle(Brush.radialGradient(listOf(M3.Magenta.copy(alpha = 0.12f), Color.Transparent), center = Offset(size.width + 8.dp.toPx(), 8.dp.toPx()), radius = 110.dp.toPx()),
                        radius = 110.dp.toPx(), center = Offset(size.width + 8.dp.toPx(), 8.dp.toPx()))
                }
                .testTag("modal-actualizacion"),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                .background(Brush.horizontalGradient(listOf(M3.blanco(0f), M3.blanco(0.22f), M3.blanco(0f)))))
            Column(Modifier.fillMaxWidth().padding(if (compacto) 20.dp else 24.dp)) {
                Cabecera(versionActual, info.versionName)
                Spacer(Modifier.height(if (compacto) 14.dp else 20.dp))
                if (obligatoria) {
                    Row(
                        Modifier.clip(CircleShape).background(M3.Magenta.copy(alpha = 0.15f)).border(1.dp, M3.Magenta.copy(alpha = 0.30f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 2.dp).testTag("badge-obligatoria"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(M3.Magenta))
                        Spacer(Modifier.width(6.dp))
                        Text("ACTUALIZACIÓN OBLIGATORIA", style = TextStyle(fontFamily = Inter, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.05.em), color = M3.Magenta)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text("Hay una nueva versión", style = TextStyle(fontFamily = display, fontSize = 22.sp, lineHeight = 26.4.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.025).em), color = Color.White)
                if (!compacto || obligatoria) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (obligatoria) "Esta versión es necesaria para seguir usando Gacelas." else "Mejoras listas para tu próxima carrera",
                        style = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, lineHeight = 18.6.sp), color = M3.Secondary,
                    )
                }
                Spacer(Modifier.height(16.dp))
                PanelNovedades(info.novedades().take(maxItems), bytes)
                Spacer(Modifier.height(if (compacto) 14.dp else 20.dp))
                Pie(fase, info.versionName, obligatoria, onActualizar, onMasTarde, onPermiso)
            }
        }
    }
}

@Composable
private fun Cabecera(actual: String, nueva: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).shadow(1.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(M3.Naranja), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("GACELAS", style = TextStyle(fontFamily = display, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.05.em), color = Color.White)
            Text("LOJA RUNNING CO.", style = TextStyle(fontFamily = Inter, fontSize = 8.5.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em), color = M3.Secondary.copy(alpha = 0.8f))
        }
        Row(
            Modifier.clip(CircleShape).background(M3.Panel).border(1.dp, M3.blanco(0.10f), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp).testTag("chip-version"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val base = TextStyle(fontFamily = Inter, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
            Text(actual, style = base.copy(textDecoration = TextDecoration.LineThrough), color = M3.Secondary.copy(alpha = 0.5f))
            Spacer(Modifier.width(6.dp))
            Text("→", style = base.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = M3.Magenta)
            Spacer(Modifier.width(6.dp))
            Text("v$nueva", style = base.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(M3.Magenta))
        }
    }
}

@Composable
private fun PanelNovedades(items: List<String>, bytes: Long?) {
    val forma = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(forma).background(M3.Panel).border(1.dp, M3.blanco(0.08f), forma).padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(M3.Naranja))
            Spacer(Modifier.width(6.dp))
            Text("NOVEDADES", style = TextStyle(fontFamily = display, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em), color = M3.blanco(0.9f), modifier = Modifier.weight(1f))
            if (bytes != null && bytes > 0) Row(
                Modifier.clip(RoundedCornerShape(6.dp)).background(M3.blanco(0.04f)).padding(horizontal = 8.dp, vertical = 2.dp).testTag("tamano-apk"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Inventory2, null, tint = M3.Secondary.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(ReglasActualizacion.textoMb(bytes), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Medium), color = M3.Secondary.copy(alpha = 0.7f))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(M3.blanco(0.06f)))
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { t ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.padding(top = 2.dp).size(20.dp).clip(CircleShape).background(M3.Magenta.copy(alpha = 0.15f)).border(1.dp, M3.Magenta.copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Check, null, tint = M3.Magenta, modifier = Modifier.size(13.dp)) }
                    Spacer(Modifier.width(10.dp))
                    Text(t, style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 16.25.sp), color = M3.Secondary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun BotonNaranja(texto: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp)
            .shadow(10.dp, CircleShape, ambientColor = M3.Naranja.copy(alpha = 0.42f), spotColor = M3.Naranja.copy(alpha = 0.42f))
            .clip(CircleShape).background(M3.Naranja).clickable(onClick = onClick).testTag("boton-actualizar"),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, style = TextStyle(fontFamily = display, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.025.em), color = Color.White)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Pie(fase: Fase, versionNueva: String, obligatoria: Boolean, onActualizar: () -> Unit, onMasTarde: () -> Unit, onPermiso: () -> Unit) {
    val masTarde: @Composable () -> Unit = {
        if (!obligatoria) Text(
            "Más tarde", style = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.025.em), color = M3.Enlace,
            modifier = Modifier.fillMaxWidth().clip(CircleShape).clickable(onClick = onMasTarde).padding(vertical = 8.dp).testTag("mas-tarde"),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
    when (fase) {
        Fase.Info -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonNaranja("Actualizar", onActualizar); masTarde()
        }
        is Fase.Descargando -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(M3.Magenta))
                    Spacer(Modifier.width(6.dp))
                    val st = TextStyle(fontFamily = display, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Descargando $versionNueva…", style = st, color = Color.White, modifier = Modifier.weight(1f))
                    Text("${fase.porcentaje}%", style = st.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.025).em), color = Color.White, modifier = Modifier.testTag("porcentaje"))
                }
                BarraProgreso(fase.porcentaje / 100f)
                Text(textoBytes(fase.hechos, fase.total), style = TextStyle(fontFamily = Inter, fontSize = 11.sp), color = M3.Secondary.copy(alpha = 0.8f), modifier = Modifier.testTag("bytes"))
            }
            Row(
                Modifier.fillMaxWidth().clip(CircleShape).background(M3.blanco(0.05f)).border(1.dp, M3.blanco(0.10f), CircleShape).padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(14.dp), color = M3.blanco(0.4f), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Instalando al terminar", style = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, fontWeight = FontWeight.Medium), color = M3.blanco(0.5f))
            }
        }
        is Fase.Permiso -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Para instalar, permite a Gacelas instalar apps desconocidas. Luego vuelve aquí.", style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp), color = M3.Secondary)
            BotonNaranja("Dar permiso", onPermiso); masTarde()
        }
        is Fase.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(fase.mensaje, style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp), color = M3.Error, modifier = Modifier.testTag("error-actualizacion"))
            BotonNaranja("Reintentar", onActualizar); masTarde()
        }
    }
}

/** Barra h8 r-full: pista #1C2028 + borde blanco 6% + p1; relleno #AB0660→#D6338A con brillo magenta. */
@Composable
private fun BarraProgreso(p: Float) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(M3.Panel).border(1.dp, M3.blanco(0.06f), CircleShape).padding(1.dp)) {
        if (p > 0f) Box(
            Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight()
                .shadow(5.dp, CircleShape, ambientColor = M3.Magenta.copy(alpha = 0.6f), spotColor = M3.Magenta.copy(alpha = 0.6f))
                .clip(CircleShape).background(Brush.horizontalGradient(listOf(M3.MagentaOscuro, M3.Magenta))).testTag("barra-progreso"),
        )
    }
}

internal fun textoBytes(hechos: Long, total: Long): String {
    val mb = { b: Long -> Math.round(b / 1048576.0) }
    return if (total > 0) "${mb(hechos)} de ${mb(total)} MB · no cierres la app" else "${mb(hechos)} MB · no cierres la app"
}
