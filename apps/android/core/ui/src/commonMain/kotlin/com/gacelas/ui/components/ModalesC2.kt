package com.gacelas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.delay

/**
 * Modal C2 especifico (hoja inferior): bloque de icono, titulo, texto, tarjeta de contenido opcional,
 * boton principal sesgado y boton secundario.
 */
@Composable
fun ModalC2(
    icono: ImageVector,
    titulo: String,
    texto: String,
    primario: String,
    onPrimario: () -> Unit,
    onCerrar: () -> Unit,
    secundario: String? = null,
    onSecundario: (() -> Unit)? = null,
    peligro: Boolean = false,
    tag: String = "modal-c2",
    contenido: (@Composable () -> Unit)? = null,
) {
    DialogoC2(
        onDismissRequest = onCerrar,
        modifier = Modifier.testTag(tag),
        icon = { Icon(icono, null, tint = if (peligro) GColors.C2Peligro else GColors.C2Violeta, modifier = Modifier.size(26.dp)) },
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(texto)
                contenido?.invoke()
            }
        },
        confirmButton = { BotonModalC2(primario, onPrimario, Modifier.fillMaxWidth(), peligro = peligro) },
        dismissButton = secundario?.let { s -> { BotonModalC2(s, onSecundario ?: onCerrar, Modifier.fillMaxWidth(), secundario = true) } },
    )
}

/** Tarjeta de datos dentro de un modal: pares valor / etiqueta en fila. */
@Composable
fun ResumenModalC2(datos: List<Pair<String, String>>) {
    Row(
        Modifier.fillMaxWidth().clip(FormaC2).background(GColors.C2S2).border(1.dp, GColors.C2Linea, FormaC2).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        datos.forEachIndexed { i, (valor, etiqueta) ->
            MetricaC2(valor, etiqueta, Modifier.weight(1f), color = if (i == 0) GColors.C2Violeta else Color.White)
        }
    }
}

/** Aviso breve C2 (toast) mostrado como banner superior. */
object AvisosC2 {
    var actual by mutableStateOf<Pair<String, Boolean>?>(null)
        private set
    private var serie = 0
    var id by mutableStateOf(0)
        private set

    fun mostrar(texto: String, error: Boolean = esError(texto)) {
        actual = texto to error
        id = ++serie
    }

    fun ocultar() { actual = null }

    internal fun esError(texto: String): Boolean {
        val t = texto.lowercase()
        return listOf("error", "no se pudo", "no pudimos", "falló", "fallo", "sin conexión", "inválid", "permiso").any { it in t }
    }
}

/** Anfitrion del aviso C2; colocar encima del contenido de la app. */
@Composable
fun AnfitrionAvisosC2(modifier: Modifier = Modifier) {
    val a = AvisosC2.actual
    val id = AvisosC2.id
    LaunchedEffect(id) { if (a != null) { delay(2800); AvisosC2.ocultar() } }
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(a != null, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
            val (texto, error) = a ?: ("" to false)
            ToastC2(texto, error)
        }
    }
}

@Composable
fun ToastC2(texto: String, error: Boolean, modifier: Modifier = Modifier) {
    val acento = if (error) GColors.C2Peligro else GColors.C2Menta
    Row(
        modifier.fillMaxWidth().clip(FormaC2).background(GColors.C2Card).border(1.dp, acento.copy(alpha = 0.6f), FormaC2)
            .padding(horizontal = 14.dp, vertical = 12.dp).testTag(if (error) "toast-error" else "toast-exito"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(30.dp).clip(FormaBotonC2).background(acento.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(if (error) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle, null, tint = acento, modifier = Modifier.size(18.dp))
        }
        Text(texto, style = GType.bodyMd.copy(fontWeight = FontWeight.SemiBold), color = Color.White, modifier = Modifier.weight(1f))
    }
}
