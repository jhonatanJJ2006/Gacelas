package com.gacelas.administracion.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gacelas.administracion.domain.AccionModeracion
import com.gacelas.administracion.domain.AdminRepositorio
import com.gacelas.administracion.domain.AjusteApp
import com.gacelas.administracion.domain.CLAVE_VERSION_MINIMA
import com.gacelas.administracion.domain.EstadoReporte
import com.gacelas.administracion.domain.EventoAdmin
import com.gacelas.administracion.domain.ReporteAdmin
import com.gacelas.administracion.domain.RolAdmin
import com.gacelas.administracion.domain.UsuarioAdmin
import com.gacelas.administracion.domain.accionesPermitidas
import com.gacelas.administracion.domain.destructiva
import com.gacelas.administracion.domain.etiquetaAccion
import com.gacelas.administracion.domain.etiquetaTipo
import com.gacelas.ui.components.BotonChipC2
import com.gacelas.ui.components.BotonModalC2
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.DialogoC2
import com.gacelas.ui.components.EtiquetaC2
import com.gacelas.ui.components.EtiquetaGrupo
import com.gacelas.ui.components.FilaInterruptor
import com.gacelas.ui.components.MetricaC2
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

private fun km(v: Double): String = ((v * 10).toLong() / 10.0).toString()
private fun fecha(iso: String?): String = iso?.take(10).orEmpty()

/** Rejilla de metricas de dos columnas. */
@Composable
private fun Rejilla(pares: List<Pair<String, String>>) {
    pares.chunked(2).forEach { fila ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            fila.forEach { (valor, etiqueta) ->
                Tarjeta(Modifier.weight(1f)) { MetricaC2(valor, etiqueta) }
            }
            if (fila.size == 1) Box(Modifier.weight(1f))
        }
    }
}

@Composable
private fun <T> Marco(titulo: String, vm: SeccionViewModel<T>, onAtras: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val mensaje by vm.mensaje.collectAsState()
    MarcoAdmin(titulo, onAtras, mensaje, vm::limpiarMensaje, content = content)
}

/** Confirmacion C2 para acciones peligrosas o irreversibles. */
@Composable
private fun Confirmar(titulo: String, texto: String, confirmar: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    DialogoC2(
        onDismissRequest = onCancelar,
        confirmButton = { BotonModalC2(confirmar, { onConfirmar(); onCancelar() }, peligro = true) },
        dismissButton = { BotonModalC2("Cancelar", onCancelar, secundario = true) },
        title = { Text(titulo) },
        text = { Text(texto) },
    )
}

// ---------- Dashboard ----------

@Composable
fun DashboardAdmin(repo: AdminRepositorio, onAtras: () -> Unit) {
    val vm = viewModel { DashboardAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    Marco("Dashboard", vm, onAtras) {
        EtiquetaGrupo("Últimos 7 días")
        SegunCarga(carga, vm::recargar) { m ->
            Rejilla(
                listOf(
                    m.usuarios.toString() to "Usuarios", m.usuarios7d.toString() to "Nuevos 7d",
                    m.carreras7d.toString() to "Carreras 7d", km(m.km7d) to "Km 7d",
                    m.reportesPendientes.toString() to "Reportes abiertos", m.eventosProximos.toString() to "Eventos próximos",
                    m.clubes.toString() to "Clubes", m.rutas.toString() to "Rutas",
                ),
            )
        }
    }
}

// ---------- Usuarios ----------

@Composable
fun UsuariosAdmin(repo: AdminRepositorio, onAtras: () -> Unit) {
    val vm = viewModel { UsuariosAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    val busqueda by vm.busqueda.collectAsState()
    var detalle by remember { mutableStateOf<String?>(null) }
    Marco("Usuarios", vm, onAtras) {
        CampoTexto("Buscar", busqueda, vm::buscar, placeholder = "Nombre o usuario")
        SegunCarga(carga, vm::recargar, { vm.filtrar(it).isEmpty() }, "No hay usuarios con ese criterio") { todos ->
            vm.filtrar(todos).forEach { u ->
                Tarjeta(Modifier.testTag("admin-usuario-${u.id}"), onClick = { detalle = u.id }) {
                    Text(u.nombreVisible, style = GType.titleMd, color = GColors.TextPrimary)
                    Text("@${u.username} · ${u.carreras} carreras · ${km(u.km)} km", style = GType.bodySm, color = GColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        u.rol?.let { EtiquetaC2(it.etiqueta) }
                        if (u.suspendido) EtiquetaC2("Suspendido", color = GColors.C2Peligro)
                    }
                }
            }
            val sel = todos.firstOrNull { it.id == detalle }
            if (sel != null) DetalleUsuario(sel, vm, onCerrar = { detalle = null })
        }
    }
}

@Composable
private fun DetalleUsuario(u: UsuarioAdmin, vm: UsuariosAdminViewModel, onCerrar: () -> Unit) {
    var confirmar by remember { mutableStateOf(false) }
    DialogoC2(
        onDismissRequest = onCerrar,
        confirmButton = {
            BotonModalC2(if (u.suspendido) "Reactivar cuenta" else "Suspender cuenta", { confirmar = true }, peligro = !u.suspendido)
        },
        dismissButton = { BotonModalC2("Cerrar", onCerrar, secundario = true) },
        title = { Text(u.nombreVisible) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("@${u.username} · alta ${fecha(u.creadoEn)}")
                Text("${u.carreras} carreras · ${km(u.km)} km")
                if (u.suspendido) Text("Suspendido desde ${fecha(u.suspendidoEn)}", color = GColors.C2Peligro)
                EtiquetaC2("Rol")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf<RolAdmin?>(null, RolAdmin.Moderador, RolAdmin.Admin).forEach { r ->
                        BotonChipC2(r?.etiqueta ?: "Corredor", { if (r != u.rol) vm.cambiarRol(u, r) }, activo = r == u.rol)
                    }
                }
            }
        },
    )
    if (confirmar) {
        Confirmar(
            if (u.suspendido) "Reactivar cuenta" else "Suspender cuenta",
            if (u.suspendido) "${u.nombreVisible} podrá volver a usar la app." else "${u.nombreVisible} no podrá entrar ni publicar hasta que lo reactives.",
            if (u.suspendido) "Reactivar" else "Suspender",
            { vm.suspender(u) }, { confirmar = false },
        )
    }
}

// ---------- Moderacion ----------

private val tiposFiltro = listOf<String?>(null, "post", "usuario", "comentario", "evento", "incidente", "comunidad", "grupo")

@Composable
fun ModeracionAdmin(repo: AdminRepositorio, onAtras: () -> Unit) {
    val vm = viewModel { ModeracionAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    val estado by vm.filtroEstado.collectAsState()
    val tipo by vm.filtroTipo.collectAsState()
    val notas by vm.notas.collectAsState()
    val ocupado by vm.ocupado.collectAsState()
    var pendiente by remember { mutableStateOf<Pair<ReporteAdmin, AccionModeracion>?>(null) }
    Marco("Moderación", vm, onAtras) {
        Pestanas(EstadoReporte.entries, estado, { it.etiqueta }, vm::elegirEstado)
        Pestanas(tiposFiltro, tipo, { it?.let(::etiquetaTipo) ?: "Todos" }, vm::elegirTipo)
        SegunCarga(carga, vm::recargar, { it.isEmpty() }, "No hay casos ${estado.etiqueta.lowercase()}") { casos ->
            casos.forEach { r ->
                CasoModeracion(r, notas[r.id].orEmpty(), ocupado, { vm.escribirNota(r.id, it) }) { accion ->
                    if (accion.destructiva) pendiente = r to accion else vm.moderar(r, accion)
                }
            }
        }
    }
    pendiente?.let { (r, a) ->
        Confirmar(etiquetaAccion(a, r.tipo), "Se aplicará sobre \"${r.resumen}\" y quedará en el historial.", "Confirmar", { vm.moderar(r, a) }, { pendiente = null })
    }
}

@Composable
private fun CasoModeracion(r: ReporteAdmin, nota: String, ocupado: Boolean, onNota: (String) -> Unit, onAccion: (AccionModeracion) -> Unit) {
    Tarjeta(Modifier.testTag("admin-caso-${r.id}")) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EtiquetaC2(etiquetaTipo(r.tipo))
            if (r.totalReportes > 1) EtiquetaC2("${r.totalReportes} reportes", color = GColors.C2Peligro)
        }
        Text(r.resumen.ifBlank { r.objetivoId }, style = GType.titleMd, color = GColors.TextPrimary)
        Text("Motivo: ${r.motivo.replace('_', ' ')}", style = GType.bodySm, color = GColors.TextSecondary)
        if (r.detalle.isNotBlank()) Text(r.detalle, style = GType.bodyMd, color = GColors.TextPrimary)
        Text("${r.reportante.ifBlank { "Anónimo" }} · ${fecha(r.creadoEn)}", style = GType.bodySm, color = GColors.C2Muted)
        if (r.acciones.isNotEmpty()) {
            EtiquetaC2("Historial", Modifier.padding(top = 6.dp))
            r.acciones.forEach { a ->
                Text("${a.moderador}: ${a.texto}${if (a.nota.isNotBlank()) " · ${a.nota}" else ""} (${fecha(a.fecha)})", style = GType.bodySm, color = GColors.TextSecondary)
            }
        }
        CampoTexto("Nota", nota, onNota, placeholder = "Nota para el historial y el reportante")
        accionesPermitidas(r, nota.isNotBlank()).forEach { a ->
            BotonModalC2(etiquetaAccion(a, r.tipo), { onAccion(a) }, Modifier.padding(top = 6.dp), peligro = a.destructiva, secundario = a == AccionModeracion.Nota || a == AccionModeracion.Descartado, enabled = !ocupado)
        }
    }
}

// ---------- Estadisticas ----------

private val nombresEstadisticas = listOf(
    "usuarios_total" to "Usuarios totales", "usuarios_nuevos" to "Nuevos", "usuarios_activos" to "Activos",
    "carreras" to "Carreras", "km" to "Km", "eventos" to "Eventos", "inscripciones" to "Inscripciones", "reportes" to "Reportes",
)

@Composable
fun EstadisticasAdmin(repo: AdminRepositorio, onAtras: () -> Unit) {
    val vm = viewModel { EstadisticasAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    val rango by vm.rango.collectAsState()
    Marco("Estadísticas", vm, onAtras) {
        Pestanas(RangoEstadisticas.entries, rango, { it.etiqueta }, vm::elegir)
        SegunCarga(carga, vm::recargar, { it.totales.isEmpty() }, "Sin datos en el rango") { e ->
            Text("Del ${e.desde} al ${e.hasta}", style = GType.bodySm, color = GColors.TextSecondary)
            Rejilla(nombresEstadisticas.map { (k, n) -> (e.totales[k]?.let { if (k == "km") km(it) else it.toLong().toString() } ?: "0") to n })
        }
    }
}

// ---------- Eventos ----------

@Composable
fun EventosAdmin(repo: AdminRepositorio, onAtras: () -> Unit) {
    val vm = viewModel { EventosAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    var editando by remember { mutableStateOf<EventoAdmin?>(null) }
    Marco("Eventos", vm, onAtras) {
        BotonPrimario("Nuevo evento", { editando = EventoAdmin(null, "") })
        SegunCarga(carga, vm::recargar, { it.isEmpty() }, "Todavía no hay eventos") { lista ->
            lista.forEach { e ->
                Tarjeta(Modifier.testTag("admin-evento-${e.id}")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EtiquetaC2(if (e.publicado) "Publicado" else "Borrador", color = if (e.publicado) GColors.C2Menta else GColors.C2Muted)
                    }
                    Text(e.titulo, style = GType.titleMd, color = GColors.TextPrimary)
                    Text("${fecha(e.iniciaEn)} · ${e.lugar} · ${km(e.distanciaKm)} km", style = GType.bodySm, color = GColors.TextSecondary)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonChipC2(if (e.publicado) "Despublicar" else "Publicar", { vm.publicar(e) })
                        BotonChipC2("Editar", { editando = e })
                    }
                }
            }
        }
    }
    editando?.let { e -> FormularioEvento(e, { vm.guardar(it); editando = null }, { editando = null }) }
}

@Composable
private fun FormularioEvento(inicial: EventoAdmin, onGuardar: (EventoAdmin) -> Unit, onCancelar: () -> Unit) {
    var titulo by remember { mutableStateOf(inicial.titulo) }
    var descripcion by remember { mutableStateOf(inicial.descripcion) }
    var inicio by remember { mutableStateOf(inicial.iniciaEn.orEmpty().take(16)) }
    var lugar by remember { mutableStateOf(inicial.lugar) }
    var distancia by remember { mutableStateOf(if (inicial.distanciaKm > 0) km(inicial.distanciaKm) else "") }
    DialogoC2(
        onDismissRequest = onCancelar,
        confirmButton = {
            BotonModalC2("Guardar", {
                onGuardar(
                    inicial.copy(
                        titulo = titulo, descripcion = descripcion, lugar = lugar,
                        iniciaEn = inicio.trim().ifBlank { null }?.let { if (it.length == 16) "$it:00-05:00" else it },
                        distanciaKm = distancia.replace(',', '.').toDoubleOrNull() ?: 0.0,
                    ),
                )
            }, enabled = titulo.isNotBlank())
        },
        dismissButton = { BotonModalC2("Cancelar", onCancelar, secundario = true) },
        title = { Text(if (inicial.id == null) "Nuevo evento" else "Editar evento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto("Título", titulo, { titulo = it })
                CampoTexto("Descripción", descripcion, { descripcion = it }, lineas = 3)
                CampoTexto("Inicio", inicio, { inicio = it }, placeholder = "2026-11-15T07:00", ayuda = "Fecha y hora de Ecuador (AAAA-MM-DDTHH:MM)")
                CampoTexto("Lugar", lugar, { lugar = it })
                CampoTexto("Distancia (km)", distancia, { distancia = it }, tipo = KeyboardType.Decimal)
            }
        },
    )
}

// ---------- Rutas y clubes ----------

@Composable
fun RutasClubesAdmin(repo: AdminRepositorio, onAtras: () -> Unit) {
    val vm = viewModel { RutasClubesAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    var pestana by remember { mutableStateOf("Rutas") }
    var borrar by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }
    Marco("Rutas y clubes", vm, onAtras) {
        Pestanas(listOf("Rutas", "Clubes"), pestana, { it }) { pestana = it }
        SegunCarga(carga, vm::recargar) { d ->
            if (pestana == "Rutas") {
                if (d.rutas.isEmpty()) Vacio("No hay rutas")
                d.rutas.forEach { r ->
                    Fila(r.nombre, "${km(r.distanciaKm)} km · ${r.dificultad}") { borrar = Triple("ruta", r.nombre) { vm.borrarRuta(r) } }
                }
            } else {
                if (d.clubes.isEmpty()) Vacio("No hay clubes")
                d.clubes.forEach { c ->
                    Fila(c.nombre, c.descripcion) { borrar = Triple("club", c.nombre) { vm.borrarClub(c) } }
                }
            }
        }
    }
    borrar?.let { (tipo, nombre, accion) ->
        Confirmar("Eliminar $tipo", "Se eliminará \"$nombre\" para todos. No se puede deshacer.", "Eliminar", accion) { borrar = null }
    }
}

@Composable
private fun Fila(titulo: String, detalle: String, onBorrar: () -> Unit) {
    Tarjeta {
        Text(titulo, style = GType.titleMd, color = GColors.TextPrimary)
        if (detalle.isNotBlank()) Text(detalle, style = GType.bodySm, color = GColors.TextSecondary)
        Row(Modifier.padding(top = 6.dp)) { BotonChipC2("Eliminar", onBorrar) }
    }
}

// ---------- Versiones y ajustes ----------

@Composable
fun AjustesAdmin(repo: AdminRepositorio, onAtras: () -> Unit) {
    val vm = viewModel { AjustesAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    Marco("Versiones y ajustes", vm, onAtras) {
        SegunCarga(carga, vm::recargar, { it.isEmpty() }, "No hay ajustes") { lista ->
            val version = lista.firstOrNull { it.clave == CLAVE_VERSION_MINIMA }
            var texto by remember(version) { mutableStateOf((version?.valor as? JsonPrimitive)?.contentOrNull.orEmpty()) }
            EtiquetaGrupo("Versión mínima de Android")
            CampoTexto("Versión mínima", texto, { texto = it }, placeholder = "1.2.3", ayuda = "Las versiones anteriores deberán actualizar")
            BotonPrimario("Guardar versión", { vm.guardarVersionMinima(texto) }, icono = null)
            EtiquetaGrupo("Interruptores")
            lista.filter { it.esInterruptor() }.forEach { a ->
                FilaInterruptor(Icons.Filled.Settings, a.clave.replace('_', ' ').replaceFirstChar { it.uppercase() }, a.clave, a.activo(), { vm.alternar(a, it) })
            }
            val otros = lista.filter { !it.esInterruptor() && it.clave != CLAVE_VERSION_MINIMA }
            if (otros.isNotEmpty()) {
                EtiquetaGrupo("Otros (solo lectura)")
                otros.forEach { Text("${it.clave}: ${it.valor}", style = GType.bodySm, color = GColors.TextSecondary) }
            }
        }
    }
}

private fun AjusteApp.esInterruptor() = (valor as? JsonPrimitive)?.let { !it.isString && it.booleanOrNull != null } == true
private fun AjusteApp.activo() = (valor as? JsonPrimitive)?.booleanOrNull == true

