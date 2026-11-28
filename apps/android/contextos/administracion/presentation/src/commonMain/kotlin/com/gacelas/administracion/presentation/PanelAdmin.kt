package com.gacelas.administracion.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gacelas.administracion.application.ConsultarAccesoAdmin
import com.gacelas.administracion.domain.AccesoAdmin
import com.gacelas.administracion.domain.AdminRepositorio
import com.gacelas.administracion.domain.EstadoReporte
import com.gacelas.administracion.domain.SeccionAdmin
import com.gacelas.ui.components.EsqueletoTarjetas
import com.gacelas.ui.navegacion.BarraNavColapsable
import com.gacelas.ui.navegacion.ColoresNavColapsable
import com.gacelas.ui.navegacion.ItemNavColapsable
import com.gacelas.ui.theme.GType
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color

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
    val acceso = consultarAcceso(repo)
    PanelAdminContenido(acceso, repo, onAtras)
}

@Composable
fun PanelAdminContenido(acceso: AccesoAdmin?, repo: AdminRepositorio, onAtras: () -> Unit, inicial: SeccionAdmin? = null) {
    var seccion by rememberSaveable { mutableStateOf(inicial?.name) }
    // Caso a mostrar al abrir Moderacion desde el Resumen ("Atender siguiente caso").
    var casoFoco by rememberSaveable { mutableStateOf<String?>(null) }
    when {
        acceso == null -> MarcoAdmin("Administración", onAtras) { Cargando() }
        !acceso.puedeEntrar -> MarcoAdmin("Administración", onAtras) { AccesoDenegado(onAtras) }
        else -> {
            // La seccion pedida solo cuenta si el rol la permite; si no, se abre la primera permitida.
            val actual = seccion?.let { s -> SeccionAdmin.entries.firstOrNull { it.name == s } }?.takeIf { it in acceso.secciones }
                ?: acceso.secciones.first()
            // Conteo real de casos pendientes para la insignia de Moderacion; se vuelve a pedir al cambiar de
            // seccion. Si falla, no se muestra insignia (nada inventado).
            val pendientes by produceState<Int?>(null, repo, actual) {
                value = try { repo.reportes(EstadoReporte.Pendiente, null).size } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { null }
            }
            val items = acceso.secciones.map { it.itemNav() }.distinctBy { it.id }
                .sortedBy { ordenBarra.indexOf(it.id) }
                .map { if (it.id == "moderacion") it.copy(insignia = pendientes) else it }
            Column(Modifier.fillMaxSize().background(Esc.Fondo).testTag("panel-admin")) {
                Box(Modifier.weight(1f)) {
                    when (actual) {
                        SeccionAdmin.Dashboard -> ResumenAdmin(repo, onAtras) { id ->
                            casoFoco = id
                            seccion = SeccionAdmin.Moderacion.name
                        }
                        SeccionAdmin.Usuarios -> UsuariosAdmin(repo, onAtras)
                        SeccionAdmin.Moderacion -> ModeracionAdmin(repo, onAtras, casoFoco)
                        SeccionAdmin.Estadisticas -> EstadisticasAdmin(repo, onAtras)
                        SeccionAdmin.Eventos -> RutasEventosAdmin(repo, onAtras, PestanaRutas.Eventos)
                        SeccionAdmin.RutasYClubes -> RutasEventosAdmin(repo, onAtras, PestanaRutas.Rutas)
                        SeccionAdmin.Ajustes -> AjustesAdmin(repo, onAtras)
                    }
                }
                // Con un solo destino (moderador) no hay a donde saltar: no se muestra la barra.
                if (items.size > 1) {
                    BarraNavColapsable(
                        items = items,
                        seleccionado = actual.itemNav().id,
                        onElegir = { id ->
                            casoFoco = null
                            seccion = (acceso.secciones.firstOrNull { it.itemNav().id == id } ?: actual).name
                        },
                        colores = coloresNavAdmin(),
                        prefijoTag = "admin-nav",
                    )
                }
            }
        }
    }
}

private val ordenBarra = listOf("resumen", "moderacion", "usuarios", "rutas", "reportes", "ajustes")

/** Colores de la barra con los tokens Escenario del panel. */
@Composable
private fun coloresNavAdmin() = ColoresNavColapsable(
    fondo = Esc.Fondo, barra = Esc.Card, borde = Esc.Borde, texto = Esc.Texto3, activo = Esc.Violeta,
    burbujaDesde = Esc.Morado, burbujaHasta = Color(0xFF5E1781), sobreBurbuja = Color.White,
    insignia = Color(0xFFEF4444), sobreInsignia = Color.White,
)

/** Destino de la barra de cada seccion: Eventos comparte ranura con Rutas. */
private fun SeccionAdmin.itemNav(): ItemNavColapsable = when (this) {
    SeccionAdmin.Dashboard -> ItemNavColapsable("resumen", "Resumen", Icons.Filled.SpaceDashboard)
    SeccionAdmin.Moderacion -> ItemNavColapsable("moderacion", "Moderación", Icons.Filled.Flag)
    SeccionAdmin.Usuarios -> ItemNavColapsable("usuarios", "Usuarios", Icons.Filled.Group)
    SeccionAdmin.RutasYClubes, SeccionAdmin.Eventos -> ItemNavColapsable("rutas", "Rutas", Icons.Filled.Map)
    SeccionAdmin.Estadisticas -> ItemNavColapsable("reportes", "Reportes", Icons.Filled.Insights)
    SeccionAdmin.Ajustes -> ItemNavColapsable("ajustes", "Ajustes", Icons.Filled.Tune)
}

/** Icono de la entrada "Administración" en Ajustes. */
val IconoAdmin = Icons.Filled.AdminPanelSettings

/** Marco comun: titulo Escenario con salida, contenido con scroll y toast opcional. */
@Composable
internal fun MarcoAdmin(
    titulo: String,
    onSalir: () -> Unit,
    mensaje: String? = null,
    onMensajeVisto: () -> Unit = {},
    eyebrow: String = "Administración",
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Esc.Fondo)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TituloEsc(eyebrow, titulo, Modifier.weight(1f))
                IconButton(onClick = onSalir, modifier = Modifier.testTag("admin-salir")) {
                    Icon(Icons.Filled.Close, contentDescription = "Salir del panel", tint = Esc.Texto2)
                }
            }
            content()
        }
        if (mensaje != null) {
            val error = !mensajeExito(mensaje)
            Box(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                com.gacelas.ui.components.ToastC2(mensaje, error)
            }
            LaunchedEffect(mensaje) { delay(2500); onMensajeVisto() }
        }
    }
}

private val exitos = setOf(
    "Cuenta reactivada", "Cuenta suspendida", "Rol actualizado", "Caso actualizado", "Evento oculto", "Evento publicado",
    "Evento guardado", "Ruta eliminada", "Club eliminado", "Ajuste guardado", "Versión mínima guardada",
    "Prioridad actualizada", "Plazos guardados", "Respuesta guardada", "Respuesta eliminada", "CSV listo para compartir",
)

internal fun mensajeExito(m: String) = m in exitos

@Composable
internal fun Cargando() {
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("admin-cargando")) {
        EsqueletoTarjetas(3, etiqueta = "Cargando datos del panel")
    }
}

@Composable
internal fun ErrorCarga(mensaje: String, onReintentar: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp).testTag("admin-error"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("No se pudo cargar", style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp), color = Esc.Texto)
        Text(mensaje, style = GType.bodySm, color = Esc.Texto2)
        BotonEsc("Reintentar", onReintentar)
    }
}

@Composable
internal fun Vacio(texto: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp).testTag("admin-vacio"), contentAlignment = Alignment.Center) {
        Text(texto, style = GType.bodyMd, color = Esc.Texto2)
    }
}

@Composable
private fun AccesoDenegado(onAtras: () -> Unit, detalle: String = "Esta sección es solo para administradores y moderadores.") {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp).testTag("admin-denegado"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.Lock, null, tint = Esc.Peligro, modifier = Modifier.size(40.dp))
        Text("Acceso denegado", style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp), color = Esc.Texto)
        Text(detalle, style = GType.bodyMd, color = Esc.Texto2)
        Spacer(Modifier.height(8.dp))
        BotonEsc("Volver", onAtras)
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

/**
 * Rol de la sesion para decidir si Ajustes muestra la entrada al panel. Se consulta al servidor cada
 * vez que se abre la pantalla (sin cache); null mientras responde o sin servidor.
 */
@Composable
fun accesoAdminActual(repo: AdminRepositorio?): AccesoAdmin? {
    if (repo == null) return null
    return consultarAcceso(repo)
}

/** Consulta nueva cada vez que la pantalla entra en composicion (sin ViewModel compartido ni cache). */
@Composable
private fun consultarAcceso(repo: AdminRepositorio): AccesoAdmin? {
    val acceso by produceState<AccesoAdmin?>(null, repo) { value = ConsultarAccesoAdmin(repo)() }
    return acceso
}
