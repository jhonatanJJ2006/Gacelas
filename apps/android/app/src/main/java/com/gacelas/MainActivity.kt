package com.gacelas

import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.gacelas.ui.intro.IntroAnimada
import com.gacelas.ui.intro.IntroArranque
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
    private val intro = mutableStateOf(false)
    private var decidida = false

    private fun decidirIntro(): Boolean {
        decidida = true
        val escala = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return IntroArranque.alEntrar(SystemClock.elapsedRealtime(), escala)
    }

    override fun onStart() {
        super.onStart()
        // onCreate ya decidio en este ciclo; en las vueltas siguientes se decide aqui.
        if (!decidida && decidirIntro()) intro.value = true
        decidida = false
    }

    override fun onStop() {
        super.onStop()
        IntroArranque.alSalir(SystemClock.elapsedRealtime())
    }

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
        // Arranque con el fondo de marca (Theme.Gacelas.Arranque); se cierra en el primer cuadro.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // La intro se decide en onStart (cada vez que la app pasa a primer plano, docs/MARCA.md).
        intro.value = decidirIntro()
        enableEdgeToEdge()
        leerDestino(intent)
        pedirPermisoNotificaciones()
        // Mapa sin conexion: Loja se descarga sola con wifi y se reanuda si quedo a medias.
        // Se aplaza hasta despues de la intro para no competir con su primer cuadro.
        if (com.gacelas.ui.components.Mapas.configurado()) window.decorView.postDelayed({
            if (isDestroyed) return@postDelayed
            runCatching { com.gacelas.ui.components.MapaOffline.asegurar(this) } // cache ambiental + estado
            com.gacelas.ui.components.MapaOfflineWorker.programar(this)         // descarga con internet (reintenta)
        }, 3_000)
        setContent {
            GacelasTheme {
                // Tocar fuera de un campo oculta el teclado. Los botones, listas y mapas consumen
                // sus propios toques, asi que este detector solo recibe toques en zonas vacias.
                val foco = LocalFocusManager.current
                val teclado = LocalSoftwareKeyboardController.current
                Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { foco.clearFocus(); teclado?.hide() } }) {
                    // Con intro, la app (sesion, Supabase, NavHost, mapas) se monta despues del primer
                    // cuadro de la intro y cuando el contenedor ya esta construido, para que la intro
                    // arranque sin hueco y sin tirones (docs/MARCA.md).
                    var montarApp by remember { mutableStateOf(!intro.value) }
                    if (!montarApp) androidx.compose.runtime.LaunchedEffect(Unit) {
                        androidx.compose.runtime.withFrameNanos { }
                        androidx.compose.runtime.withFrameNanos { }
                        (application as GacelasApp).esperarContenedor()
                        montarApp = true
                    }
                    if (montarApp) {
                        GacelasRaiz()
                        com.gacelas.actualizacion.AvisoActualizacion()
                    }
                    if (intro.value) IntroAnimada(alTerminar = { intro.value = false; montarApp = true })
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
