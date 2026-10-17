package com.gacelas.notificaciones

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.gacelas.comunidad.domain.modelo.Notificacion
import com.gacelas.comunidad.presentation.ui.NotificacionesContenido
import com.gacelas.data.mock.MockData
import com.gacelas.ui.contenedor

fun iconoNotificacion(tipo: com.gacelas.comunidad.domain.modelo.TipoNotificacion) = com.gacelas.comunidad.presentation.ui.iconoNotificacion(tipo)

@Composable
fun NotificacionesScreen(onAtras: () -> Unit, onAbrir: (Notificacion) -> Unit = {}) {
    val c = contenedor()
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { c.notificacionesViewModel(MockData.notificaciones) }
    NotificacionesContenido(vm, onAtras, onAbrir)
}

/** Notificaciones de la cuenta si hay servidor; si no, las de ejemplo. Se refrescan al mostrarse. */
@Composable
fun notificacionesBase(): androidx.compose.runtime.State<List<Notificacion>> {
    val repo = contenedor().notificaciones ?: return androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(MockData.notificaciones) }
    androidx.compose.runtime.LaunchedEffect(repo) { repo.refrescar() }
    return repo.lista.collectAsState()
}
