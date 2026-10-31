package com.gacelas.administracion.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gacelas.administracion.application.ConsultarAccesoAdmin
import com.gacelas.administracion.domain.AccesoAdmin
import com.gacelas.administracion.domain.AdminRepositorio
import com.gacelas.administracion.domain.SeccionAdmin
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.FilaAjuste
import com.gacelas.ui.components.GrupoC2
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.components.ToastC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.delay

/**
 * Entrada del panel en movil (Android e iOS). Vuelve a preguntar el rol al servidor; si la sesion
 * no es admin ni moderador muestra "Acceso denegado" aunque se llegue por ruta directa.
 */
@Composable
fun PanelAdmin(repo: AdminRepositorio?, onAtras: () -> Unit) {
    if (repo == null) {
        MarcoAdmin("Administración", onAtras) { AccesoDenegado(onAtras, "El panel necesita conexión con el servidor.") }
        return
    }
    val vm = viewModel { AccesoAdminViewModel(ConsultarAccesoAdmin(repo)) }
    val acceso by vm.acceso.collectAsState()
    PanelAdminContenido(acceso, repo, onAtras)
}

@Composable
fun PanelAdminContenido(acceso: AccesoAdmin?, repo: AdminRepositorio, onAtras: () -> Unit, inicial: SeccionAdmin? = null) {
    var seccion by rememberSaveable { mutableStateOf(inicial?.name) }
    val actual = seccion?.let { s -> SeccionAdmin.entries.firstOrNull { it.name == s } }
    when {
        acceso == null -> MarcoAdmin("Administración", onAtras) { Cargando() }
        !acceso.puedeEntrar -> MarcoAdmin("Administración", onAtras) { AccesoDenegado(onAtras) }
        actual == null || actual !in acceso.secciones -> MarcoAdmin("Administración", onAtras) {
            MenuAdmin(acceso) { seccion = it.name }
        }
        else -> {
            val volver = { seccion = null }
            when (actual) {
                SeccionAdmin.Dashboard -> DashboardAdmin(repo, volver)
                SeccionAdmin.Usuarios -> UsuariosAdmin(repo, volver)
                SeccionAdmin.Moderacion -> ModeracionAdmin(repo, volver)
                SeccionAdmin.Estadisticas -> EstadisticasAdmin(repo, volver)
                SeccionAdmin.Eventos -> EventosAdmin(repo, volver)
                SeccionAdmin.RutasYClubes -> RutasClubesAdmin(repo, volver)
                SeccionAdmin.Ajustes -> AjustesAdmin(repo, volver)
            }
        }
    }
}

@Composable
private fun MenuAdmin(acceso: AccesoAdmin, onElegir: (SeccionAdmin) -> Unit) {
    Text(
        "Sesión con rol ${acceso.rol?.etiqueta?.lowercase()}. Cada acción se valida de nuevo en el servidor.",
        style = GType.bodySm, color = GColors.TextSecondary,
    )
    GrupoC2 {
        acceso.secciones.forEach { s ->
            Box(Modifier.testTag("admin-seccion-${s.name}")) {
                FilaAjuste(iconoSeccion(s), s.titulo, s.descripcion, onClick = { onElegir(s) })
            }
        }
    }
}

private fun iconoSeccion(s: SeccionAdmin) = when (s) {
    SeccionAdmin.Dashboard -> Icons.Filled.SpaceDashboard
    SeccionAdmin.Usuarios -> Icons.Filled.Group
    SeccionAdmin.Moderacion -> Icons.Filled.Flag
    SeccionAdmin.Estadisticas -> Icons.Filled.BarChart
    SeccionAdmin.Eventos -> Icons.Filled.Event
    SeccionAdmin.RutasYClubes -> Icons.Filled.Map
    SeccionAdmin.Ajustes -> Icons.Filled.Settings
}

/** Icono de la entrada "Administración" en Ajustes. */
val IconoAdmin = Icons.Filled.AdminPanelSettings

/** Marco comun: encabezado C2 con atras, contenido con scroll y toast opcional. */
@Composable
internal fun MarcoAdmin(
    titulo: String,
    onAtras: () -> Unit,
    mensaje: String? = null,
    onMensajeVisto: () -> Unit = {},
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary).testTag("panel-admin")) {
        EncabezadoAtras(titulo, onAtras)
        Box(Modifier.weight(1f).navigationBarsPadding()) {
            Pantalla(scroll = scroll) { content() }
            if (mensaje != null) {
                val error = !mensajeExito(mensaje)
                ToastC2(mensaje, error, Modifier.align(Alignment.BottomCenter).padding(16.dp))
                LaunchedEffect(mensaje) { delay(2500); onMensajeVisto() }
            }
        }
    }
}

private val exitos = setOf(
    "Cuenta reactivada", "Cuenta suspendida", "Rol actualizado", "Caso actualizado", "Evento oculto", "Evento publicado",
    "Evento guardado", "Ruta eliminada", "Club eliminado", "Ajuste guardado", "Versión mínima guardada",
)

internal fun mensajeExito(m: String) = m in exitos

@Composable
internal fun Cargando() {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp).testTag("admin-cargando"), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = GColors.C2Violeta)
    }
}

@Composable
internal fun ErrorCarga(mensaje: String, onReintentar: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp).testTag("admin-error"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("No se pudo cargar", style = GType.titleMd, color = GColors.TextPrimary)
        Text(mensaje, style = GType.bodySm, color = GColors.TextSecondary)
        BotonPrimario("Reintentar", onReintentar, icono = null)
    }
}

@Composable
internal fun Vacio(texto: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp).testTag("admin-vacio"), contentAlignment = Alignment.Center) {
        Text(texto, style = GType.bodyMd, color = GColors.TextSecondary)
    }
}

@Composable
private fun AccesoDenegado(onAtras: () -> Unit, detalle: String = "Esta sección es solo para administradores y moderadores.") {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp).testTag("admin-denegado"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.Lock, null, tint = GColors.C2Peligro, modifier = Modifier.size(40.dp))
        Text("Acceso denegado", style = GType.headlineSm, color = GColors.TextPrimary)
        Text(detalle, style = GType.bodyMd, color = GColors.TextSecondary)
        Spacer(Modifier.height(8.dp))
        BotonPrimario("Volver", onAtras, icono = null)
    }
}

/** Pestanas C2 con scroll horizontal. */
@Composable
internal fun <T> Pestanas(opciones: List<T>, activa: T, etiqueta: (T) -> String, onElegir: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { o -> Pastilla(etiqueta(o), o == activa, onClick = { onElegir(o) }) }
    }
}

/** Contenido segun el estado de carga, con vacio y error con reintento. */
@Composable
internal fun <T> SegunCarga(carga: Carga<T>, onReintentar: () -> Unit, vacio: (T) -> Boolean = { false }, textoVacio: String = "Sin datos", listo: @Composable (T) -> Unit) {
    when (carga) {
        Carga.Cargando -> Cargando()
        is Carga.Error -> ErrorCarga(carga.mensaje, onReintentar)
        is Carga.Listo -> if (vacio(carga.datos)) Vacio(textoVacio) else listo(carga.datos)
    }
}

