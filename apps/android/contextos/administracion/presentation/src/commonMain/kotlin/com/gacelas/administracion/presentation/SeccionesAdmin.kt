package com.gacelas.administracion.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.gacelas.administracion.domain.idCorto
import com.gacelas.administracion.domain.siguienteCaso
import com.gacelas.ui.components.BotonModalC2
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.DialogoC2
import com.gacelas.ui.components.FilaInterruptor
import com.gacelas.ui.theme.GType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

private fun km(v: Double): String = ((v * 10).toLong() / 10.0).toString()
private fun fecha(iso: String?): String = iso?.take(10).orEmpty()

@Composable
private fun <T> Marco(titulo: String, vm: SeccionViewModel<T>, onSalir: () -> Unit, eyebrow: String = "Administración", content: @Composable ColumnScope.() -> Unit) {
    val mensaje by vm.mensaje.collectAsState()
    MarcoAdmin(titulo, onSalir, mensaje, vm::limpiarMensaje, eyebrow, content)
}

/** Mosaicos de cifras en filas de [porFila]. */
@Composable
private fun Mosaicos(items: List<Triple<String, String, Color>>, porFila: Int = 2) {
    items.chunked(porFila).forEach { fila ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            fila.forEach { (valor, etiqueta, color) -> KpiEsc(valor, etiqueta, color, Modifier.weight(1f)) }
            repeat(porFila - fila.size) { Box(Modifier.weight(1f)) }
        }
    }
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

// ---------- Resumen ----------

/**
 * Resumen: lienzo del mapa de Loja, accion ambar para atender el siguiente caso y cifras reales de
 * admin_metricas. La posicion de corredores e incidentes en vivo no tiene fuente en la app movil del
 * panel: el mapa lo dice y no dibuja puntos inventados.
 */
@Composable
fun ResumenAdmin(repo: AdminRepositorio, onSalir: () -> Unit, onAtender: (String?) -> Unit) {
    val vm = viewModel { DashboardAdminViewModel(repo) }
    val cola = viewModel { PendientesAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    val pendientes by cola.estado.collectAsState()
    Marco("Resumen", vm, onSalir, eyebrow = "Loja, Ecuador") {
        SegunCarga(carga, { vm.recargar(); cola.recargar() }) { m ->
            MapaResumen(
                m.reportesPendientes.toString(), m.eventosProximos.toString(), m.usuarios7d.toString(),
            )
            val siguiente = (pendientes as? Carga.Listo)?.datos?.siguienteCaso()
            AtenderSiguiente(siguiente) { onAtender(siguiente?.id) }
            Mosaicos(
                listOf(
                    Triple(m.usuarios.toString(), "Usuarios", Esc.Violeta),
                    Triple(m.rutas.toString(), "Rutas", Esc.Menta),
                    Triple(m.eventosProximos.toString(), "Eventos", Esc.Ambar),
                ),
                porFila = 3,
            )
            Mosaicos(
                listOf(
                    Triple(m.carreras7d.toString(), "Carreras 7 d", Esc.Texto),
                    Triple(km(m.km7d), "Km 7 d", Esc.Texto),
                ),
            )
            val lista = (pendientes as? Carga.Listo)?.datos.orEmpty().take(3)
            if (lista.isNotEmpty()) {
                Text("Cola de moderación", style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto)
                lista.forEach { r ->
                    TarjetaEsc(Modifier.testTag("resumen-caso-${r.id}"), onClick = { onAtender(r.id) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            ChipEsc(etiquetaTipo(r.tipo), Tono.Violeta)
                            Text(haceCuanto(r.creadoEn), style = GType.bodySm, color = Esc.Texto3)
                        }
                        Text(r.resumen.ifBlank { r.objetivoId }, style = GType.bodyMd.copy(fontWeight = FontWeight.Bold), color = Esc.Texto, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun MapaResumen(cola: String, eventos: String, nuevos: String) {
    val lineas = Esc.Borde
    val rio = Esc.Cielo
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Esc.Card).testTag("resumen-mapa")) {
        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Esc.CardAlta, Esc.Card)))) {
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                for (i in 1..5) drawLine(lineas, Offset(size.width * i / 6f, 0f), Offset(size.width * i / 6f, size.height), 2f)
                for (i in 1..3) drawLine(lineas, Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), 2f)
                val p = Path().apply {
                    moveTo(0f, size.height * 0.75f)
                    cubicTo(size.width * 0.3f, size.height * 0.35f, size.width * 0.6f, size.height * 1.0f, size.width, size.height * 0.45f)
                }
                drawPath(p, rio.copy(alpha = 0.55f), style = Stroke(10f))
            }
            ChipEsc("Mapa en vivo: fuente pendiente", Tono.Ambar, Modifier.align(Alignment.TopStart).padding(10.dp).testTag("mapa-fuente-pendiente"))
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            CifraMapa(cola, "Casos abiertos", Esc.Violeta)
            CifraMapa(eventos, "Eventos próximos", Esc.Ambar)
            CifraMapa(nuevos, "Nuevos 7 d", Esc.Menta)
        }
    }
}

@Composable
private fun CifraMapa(valor: String, etiqueta: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = GType.bodyMd.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold), color = color)
        Text(etiqueta, style = GType.bodySm, color = Esc.Texto2)
    }
}

@Composable
private fun AtenderSiguiente(siguiente: ReporteAdmin?, onClick: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    val habil = siguiente != null
    Row(
        Modifier.fillMaxWidth().clip(forma)
            .background(if (habil) Brush.linearGradient(listOf(Esc.Ambar1, Esc.Ambar2)) else Brush.linearGradient(listOf(Esc.Card, Esc.Card)))
            .let { if (habil) it.clickable(onClick = onClick) else it }
            .padding(18.dp).testTag("atender-siguiente"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Acción rápida", style = GType.labelMd.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp), color = (if (habil) Color.White else Esc.Texto3).copy(alpha = 0.8f))
            Text(if (habil) "Atender siguiente caso" else "Sin casos pendientes", style = GType.bodyMd.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = if (habil) Color.White else Esc.Texto)
            if (siguiente != null) {
                Text("${idCorto(siguiente.id)} · ${etiquetaTipo(siguiente.tipo)} · ${haceCuanto(siguiente.creadoEn)}", style = GType.bodySm, color = Color.White.copy(alpha = 0.85f))
            }
        }
        if (habil) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
            }
        }
    }
}

// ---------- Usuarios ----------

private enum class FiltroUsuarios(val etiqueta: String) { Todos("Todos"), Suspendidos("Suspendidos") }

@Composable
fun UsuariosAdmin(repo: AdminRepositorio, onSalir: () -> Unit) {
    val vm = viewModel { UsuariosAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    val busqueda by vm.busqueda.collectAsState()
    var detalle by remember { mutableStateOf<String?>(null) }
    var filtro by rememberSaveable { mutableStateOf(FiltroUsuarios.Todos) }
    Marco("Usuarios", vm, onSalir, eyebrow = "Comunidad Runcity") {
        CampoTexto("Buscar", busqueda, vm::buscar, placeholder = "Nombre o usuario")
        PestanasEsc(FiltroUsuarios.entries, filtro, { it.etiqueta }, { filtro = it })
        SegunCarga(carga, vm::recargar, { visibles(vm, it, filtro).isEmpty() }, "No hay usuarios con ese criterio") { todos ->
            visibles(vm, todos, filtro).forEach { u ->
                TarjetaEsc(Modifier.testTag("admin-usuario-${u.id}"), onClick = { detalle = u.id }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarEsc(u.nombreVisible)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(u.nombreVisible, style = GType.bodyMd.copy(fontWeight = FontWeight.Bold), color = Esc.Texto, maxLines = 1)
                            Text("@${u.username} · ${u.carreras} carreras · ${km(u.km)} km", style = GType.bodySm, color = Esc.Texto2, maxLines = 1)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            u.rol?.let { ChipEsc(it.etiqueta, Tono.Violeta) }
                            if (u.suspendido) ChipEsc("Suspendido", Tono.Peligro) else ChipEsc("Activo", Tono.Menta)
                        }
                    }
                }
            }
            val sel = todos.firstOrNull { it.id == detalle }
            if (sel != null) DetalleUsuario(sel, vm, onCerrar = { detalle = null })
        }
    }
}

private fun visibles(vm: UsuariosAdminViewModel, todos: List<UsuarioAdmin>, f: FiltroUsuarios) =
    vm.filtrar(todos).filter { f == FiltroUsuarios.Todos || it.suspendido }

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
                if (u.suspendido) Text("Suspendido desde ${fecha(u.suspendidoEn)}", color = Esc.Peligro)
                Text("Rol")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf<RolAdmin?>(null, RolAdmin.Moderador, RolAdmin.Admin).forEach { r ->
                        com.gacelas.ui.components.BotonChipC2(r?.etiqueta ?: "Corredor", { if (r != u.rol) vm.cambiarRol(u, r) }, activo = r == u.rol)
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
fun ModeracionAdmin(repo: AdminRepositorio, onSalir: () -> Unit, casoFoco: String? = null) {
    val vm = viewModel { ModeracionAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    val estado by vm.filtroEstado.collectAsState()
    val tipo by vm.filtroTipo.collectAsState()
    val notas by vm.notas.collectAsState()
    val ocupado by vm.ocupado.collectAsState()
    var pendiente by remember { mutableStateOf<Pair<ReporteAdmin, AccionModeracion>?>(null) }
    var elegido by rememberSaveable { mutableStateOf(casoFoco) }
    Marco("Moderación", vm, onSalir, eyebrow = "Cola de reportes") {
        PestanasEsc(EstadoReporte.entries, estado, { it.etiqueta }, vm::elegirEstado)
        FiltrosEsc(tiposFiltro, tipo, { it?.let(::etiquetaTipo) ?: "Todos" }, vm::elegirTipo)
        SegunCarga(carga, vm::recargar, { it.isEmpty() }, "No hay casos ${estado.etiqueta.lowercase()}") { casos ->
            val siguiente = casos.siguienteCaso()
            if (siguiente != null) {
                TarjetaEsc(Modifier.testTag("asignarme-siguiente-tarjeta"), fondo = Esc.Violeta.copy(alpha = 0.10f), borde = Esc.Violeta.copy(alpha = 0.4f)) {
                    ChipEsc("Nuevo", Tono.Violeta)
                    Text("Asignarme el siguiente caso", style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto)
                    Text("Toma el pendiente más antiguo y queda en tu historial.", style = GType.bodySm, color = Esc.Texto2)
                    BotonEsc("Tomar ${idCorto(siguiente.id)}", { elegido = siguiente.id; vm.asignarmeSiguiente() }, Modifier.fillMaxWidth().testTag("asignarme-siguiente"), TipoBoton.Secundario, enabled = !ocupado)
                }
            }
            val activo = casos.firstOrNull { it.id == elegido } ?: casos.first()
            casos.forEach { r ->
                CasoModeracion(r, r.id == activo.id, notas[r.id].orEmpty(), ocupado, { elegido = r.id }, { vm.escribirNota(r.id, it) }) { accion ->
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
private fun CasoModeracion(
    r: ReporteAdmin, activo: Boolean, nota: String, ocupado: Boolean,
    onElegir: () -> Unit, onNota: (String) -> Unit, onAccion: (AccionModeracion) -> Unit,
) {
    TarjetaEsc(
        Modifier.testTag("admin-caso-${r.id}"),
        borde = if (activo) Esc.Violeta.copy(alpha = 0.6f) else Esc.Borde,
        fondo = if (activo) Esc.CardAlta else Esc.Card,
        onClick = if (activo) null else onElegir,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(idCorto(r.id), style = GType.labelMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto3)
            ChipEsc(etiquetaTipo(r.tipo), Tono.Violeta)
            if (r.totalReportes > 1) ChipEsc("${r.totalReportes} reportes", Tono.Peligro)
            Box(Modifier.weight(1f))
            Text(haceCuanto(r.creadoEn), style = GType.bodySm, color = Esc.Texto3)
        }
        Text(r.resumen.ifBlank { r.objetivoId }, style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto, maxLines = if (activo) 4 else 1)
        Text("Motivo: ${r.motivo.replace('_', ' ')}", style = GType.bodySm, color = Esc.Texto2)
        if (!activo) return@TarjetaEsc
        if (r.detalle.isNotBlank()) Text(r.detalle, style = GType.bodyMd, color = Esc.Texto)
        Text("${r.reportante.ifBlank { "Anónimo" }} · ${fecha(r.creadoEn)}", style = GType.bodySm, color = Esc.Texto3)
        if (r.acciones.isNotEmpty()) {
            Text("Historial", style = GType.labelMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Violeta)
            r.acciones.forEach { a ->
                Text("${a.moderador}: ${a.texto}${if (a.nota.isNotBlank()) " · ${a.nota}" else ""} (${fecha(a.fecha)})", style = GType.bodySm, color = Esc.Texto2)
            }
        }
        CampoTexto("Nota", nota, onNota, placeholder = "Nota para el historial y el reportante")
        accionesPermitidas(r, nota.isNotBlank()).forEach { a ->
            val tipo = when {
                a.destructiva -> TipoBoton.Peligro
                a == AccionModeracion.Nota || a == AccionModeracion.Descartado -> TipoBoton.Secundario
                a == AccionModeracion.EnRevision -> TipoBoton.Ok
                else -> TipoBoton.Primario
            }
            BotonEsc(etiquetaAccion(a, r.tipo), { onAccion(a) }, Modifier.fillMaxWidth(), tipo, enabled = !ocupado)
        }
    }
}

// ---------- Reportes (estadisticas) ----------

private val nombresEstadisticas = listOf(
    "usuarios_total" to "Usuarios totales", "usuarios_nuevos" to "Nuevos", "usuarios_activos" to "Activos",
    "carreras" to "Carreras", "km" to "Km", "eventos" to "Eventos", "inscripciones" to "Inscripciones", "reportes" to "Reportes",
)

@Composable
fun EstadisticasAdmin(repo: AdminRepositorio, onSalir: () -> Unit) {
    val vm = viewModel { EstadisticasAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    val rango by vm.rango.collectAsState()
    Marco("Reportes", vm, onSalir, eyebrow = "Estadísticas") {
        PestanasEsc(RangoEstadisticas.entries, rango, { it.etiqueta }, vm::elegir)
        SegunCarga(carga, vm::recargar, { it.totales.isEmpty() }, "Sin datos en el rango") { e ->
            Text("Del ${e.desde} al ${e.hasta}", style = GType.bodySm, color = Esc.Texto2)
            Mosaicos(nombresEstadisticas.map { (k, n) ->
                Triple(e.totales[k]?.let { if (k == "km") km(it) else it.toLong().toString() } ?: "0", n, if (k == "reportes") Esc.Peligro else Esc.Violeta)
            })
        }
    }
}

// ---------- Rutas y eventos ----------

enum class PestanaRutas(val etiqueta: String) { Rutas("Rutas"), Eventos("Eventos"), Clubes("Clubes") }

@Composable
fun RutasEventosAdmin(repo: AdminRepositorio, onSalir: () -> Unit, inicial: PestanaRutas = PestanaRutas.Rutas) {
    val rutasVm = viewModel { RutasClubesAdminViewModel(repo) }
    val eventosVm = viewModel { EventosAdminViewModel(repo) }
    var pestana by rememberSaveable { mutableStateOf(inicial) }
    var editando by remember { mutableStateOf<EventoAdmin?>(null) }
    var borrar by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }
    val msgRutas by rutasVm.mensaje.collectAsState()
    val msgEventos by eventosVm.mensaje.collectAsState()
    val cargaRutas by rutasVm.estado.collectAsState()
    val cargaEventos by eventosVm.estado.collectAsState()
    MarcoAdmin(
        "Rutas y eventos", onSalir, msgRutas ?: msgEventos, { rutasVm.limpiarMensaje(); eventosVm.limpiarMensaje() }, "Contenido",
    ) {
        PestanasEsc(PestanaRutas.entries, pestana, { it.etiqueta }, { pestana = it })
        if (pestana == PestanaRutas.Eventos) {
            BotonEsc("Nuevo evento", { editando = EventoAdmin(null, "") }, Modifier.fillMaxWidth())
            SegunCarga(cargaEventos, eventosVm::recargar, { it.isEmpty() }, "Todavía no hay eventos") { lista ->
                lista.forEach { e ->
                    TarjetaEsc(Modifier.testTag("admin-evento-${e.id}")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChipEsc(if (e.publicado) "Publicado" else "Borrador", if (e.publicado) Tono.Menta else Tono.Neutro)
                        }
                        Text(e.titulo, style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto)
                        Text("${fecha(e.iniciaEn)} · ${e.lugar} · ${km(e.distanciaKm)} km", style = GType.bodySm, color = Esc.Texto2)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BotonEsc(if (e.publicado) "Despublicar" else "Publicar", { eventosVm.publicar(e) }, Modifier.weight(1f), TipoBoton.Secundario)
                            BotonEsc("Editar", { editando = e }, Modifier.weight(1f), TipoBoton.Secundario)
                        }
                    }
                }
            }
        } else {
            SegunCarga(cargaRutas, rutasVm::recargar) { d ->
                if (pestana == PestanaRutas.Rutas) {
                    if (d.rutas.isEmpty()) Vacio("No hay rutas")
                    d.rutas.forEach { r ->
                        Fila(r.nombre, "${km(r.distanciaKm)} km", r.dificultad) { borrar = Triple("ruta", r.nombre) { rutasVm.borrarRuta(r) } }
                    }
                } else {
                    if (d.clubes.isEmpty()) Vacio("No hay clubes")
                    d.clubes.forEach { c ->
                        Fila(c.nombre, c.descripcion, null) { borrar = Triple("club", c.nombre) { rutasVm.borrarClub(c) } }
                    }
                }
            }
        }
    }
    editando?.let { e -> FormularioEvento(e, { eventosVm.guardar(it); editando = null }, { editando = null }) }
    borrar?.let { (tipo, nombre, accion) ->
        Confirmar("Eliminar $tipo", "Se eliminará \"$nombre\" para todos. No se puede deshacer.", "Eliminar", accion) { borrar = null }
    }
}

@Composable
private fun Fila(titulo: String, detalle: String, chip: String?, onBorrar: () -> Unit) {
    TarjetaEsc {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = GType.bodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Esc.Texto, modifier = Modifier.weight(1f))
            if (!chip.isNullOrBlank()) ChipEsc(chip, Tono.Violeta)
        }
        if (detalle.isNotBlank()) Text(detalle, style = GType.bodySm, color = Esc.Texto2)
        BotonEsc("Eliminar", onBorrar, Modifier.fillMaxWidth(), TipoBoton.Secundario)
    }
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

// ---------- Ajustes ----------

@Composable
fun AjustesAdmin(repo: AdminRepositorio, onSalir: () -> Unit) {
    val vm = viewModel { AjustesAdminViewModel(repo) }
    val carga by vm.estado.collectAsState()
    Marco("Ajustes", vm, onSalir) {
        SegunCarga(carga, vm::recargar, { it.isEmpty() }, "No hay ajustes") { lista ->
            val version = lista.firstOrNull { it.clave == CLAVE_VERSION_MINIMA }
            var texto by remember(version) { mutableStateOf((version?.valor as? JsonPrimitive)?.contentOrNull.orEmpty()) }
            Etiqueta("Versión mínima de Android")
            TarjetaEsc {
                CampoTexto("Versión mínima", texto, { texto = it }, placeholder = "1.2.3", ayuda = "Las versiones anteriores deberán actualizar")
                BotonEsc("Guardar versión", { vm.guardarVersionMinima(texto) }, Modifier.fillMaxWidth())
            }
            Etiqueta("Interruptores")
            lista.filter { it.esInterruptor() }.forEach { a ->
                FilaInterruptor(Icons.Filled.Settings, etiquetaAjuste(a.clave), a.clave, a.activo(), { vm.alternar(a, it) })
            }
            val otros = lista.filter { !it.esInterruptor() && it.clave != CLAVE_VERSION_MINIMA }
            if (otros.isNotEmpty()) {
                Etiqueta("Otros (solo lectura)")
                otros.forEach { Text("${it.clave}: ${it.valor}", style = GType.bodySm, color = Esc.Texto2) }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String) =
    Text(texto.uppercase(), style = GType.labelMd.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp), color = Esc.Texto3)

/** Clave de ajuste en frase: "registro_abierto" -> "Registro abierto". */
internal fun etiquetaAjuste(clave: String) = clave.replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun AjusteApp.esInterruptor() = (valor as? JsonPrimitive)?.let { !it.isString && it.booleanOrNull != null } == true
private fun AjusteApp.activo() = (valor as? JsonPrimitive)?.booleanOrNull == true
