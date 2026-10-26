package com.gacelas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.gacelas.tracking.DestinoPendiente
import com.gacelas.GacelasApp
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.gacelas.identidad.domain.modelo.Perfil
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import androidx.compose.runtime.SideEffect
import com.gacelas.ui.ajustesState
import com.gacelas.ui.theme.GacelasTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerDestino(intent)
    }

    private fun leerDestino(i: Intent?) {
        if (i?.getBooleanExtra(DestinoPendiente.TERMINAR, false) == true) {
            (application as GacelasApp).contenedor.tracker.let { if (it.estado.value.activa) it.finalizar() }
            i.removeExtra(DestinoPendiente.TERMINAR)
        }
        i?.getStringExtra(DestinoPendiente.EXTRA)?.let { DestinoPendiente.valor.value = it }
        i?.removeExtra(DestinoPendiente.EXTRA)
    }

    /** Android 13+: sin este permiso la notificacion de la carrera (con sus botones) no se ve. */
    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        leerDestino(intent)
        pedirPermisoNotificaciones()
        // Mapa sin conexion: Loja se descarga sola con wifi y se reanuda si quedo a medias.
        if (com.gacelas.ui.components.Mapas.configurado()) {
            runCatching { com.gacelas.ui.components.MapaOffline.asegurar(this) } // cache ambiental + estado
            com.gacelas.ui.components.MapaOfflineWorker.programar(this)         // descarga con internet (reintenta)
        }
        setContent {
            GacelasTheme {
                // Tocar fuera de un campo oculta el teclado. Los botones, listas y mapas consumen
                // sus propios toques, asi que este detector solo recibe toques en zonas vacias.
                val foco = LocalFocusManager.current
                val teclado = LocalSoftwareKeyboardController.current
                Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { foco.clearFocus(); teclado?.hide() } }) {
                    GacelasRaiz()
                    com.gacelas.actualizacion.AvisoActualizacion()
                }
            }
        }
    }
}

private sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data class Lista(val perfil: Perfil?) : EstadoSesion
}

/** Decide la pantalla inicial segun la sesion guardada y protege las rutas privadas. */
@Composable
fun GacelasRaiz() {
    val c = contenedor()
    val ajustes by ajustesState()
    SideEffect { GColors.claro = !ajustes.modoOscuro }
    val flujo = remember(c) { c.auth.sesion.map { EstadoSesion.Lista(it) } }
    val estado by flujo.collectAsState(initial = EstadoSesion.Cargando)
    when (val e = estado) {
        EstadoSesion.Cargando -> Box(Modifier.fillMaxSize().background(GColors.BgPrimary), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GColors.Primary)
        }
        is EstadoSesion.Lista -> {
            val inicio = remember { if (e.perfil != null) Destinos.INICIO else Destinos.BIENVENIDA }
            Box(Modifier.fillMaxSize()) {
                GacelasNavHost(inicio = inicio, sesionActiva = e.perfil != null)
                if (e.perfil != null) {
                    var doc by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<Boolean?>(null) }
                    com.gacelas.vivo.PublicadorUbicacionVivo()
                    doc?.let { t -> Box(Modifier.fillMaxSize().background(GColors.BgPrimary)) { com.gacelas.legal.DocumentoLegalScreen(t) { doc = null } } }
                    if (doc == null) com.gacelas.legal.ReaceptarTerminos { doc = it }
                }
                com.gacelas.ui.components.AnfitrionAvisosC2()
                com.gacelas.social.ModalSinConexion()
            }
        }
    }
}
