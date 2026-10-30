package com.gacelas

import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.gacelas.intro.IntroAnimada
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

    private fun decidirIntro(aperturaNueva: Boolean = false): Boolean {
        decidida = true
        val escala = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return IntroArranque.alEntrar(SystemClock.elapsedRealtime(), escala, aperturaNueva)
    }

    override fun onStart() {
        super.onStart()
        // onCreate ya decidio en este ciclo; en las vueltas siguientes se decide aqui.
        if (!decidida && decidirIntro()) iniciarIntro()
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
        // Arranque con el fondo de marca (Theme.Gacelas.Arranque). El SplashScreen no se retiene: en
        // el primer cuadro de la ventana ya corre la intro (vector animado en el RenderThread) sobre
        // el mismo #0C1322 y la gacela del splash se desvanece encima (docs/MARCA.md).
        android.os.Trace.beginSection("MainActivity.onCreate")
        val t0 = SystemClock.uptimeMillis()
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // La intro se decide en onStart (cada vez que la app pasa a primer plano, docs/MARCA.md).
        // Actividad nueva (sin estado guardado) = la app se abrio de nuevo: intro siempre, aunque el
        // proceso siguiera vivo tras cerrarla con atras o desde recientes hace pocos segundos.
        val conIntro = decidirIntro(aperturaNueva = savedInstanceState == null)
        splash.setOnExitAnimationListener { vista ->
            vista.iconView.animate().alpha(0f).setDuration(180).withEndAction { vista.remove() }.start()
        }
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
                    // El primer cuadro es solo el fondo de marca (y la intro encima): asi el splash se suelta
                    // al instante. La app se monta despues, cuando el contenedor (Supabase, datos) ya se
                    // construyo en segundo plano; tocarlo antes bloqueaba el hilo principal ~5 s y el splash
                    // se quedaba fijo (v0.8.9). La intro corre en el RenderThread y no se entrecorta.
                    var montarApp by remember { mutableStateOf(false) }
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        androidx.compose.runtime.withFrameNanos { }
                        (application as GacelasApp).esperarContenedor()
                        android.util.Log.i("Arranque", "app lista a ${SystemClock.uptimeMillis() - android.os.Process.getStartUptimeMillis()} ms del inicio del proceso")
                        montarApp = true
                    }
                    if (montarApp) {
                        // Hojas C2 en la ventana de la actividad (no en una ventana de Dialog): v0.9.1.
                        val hojas = remember { com.gacelas.ui.components.HojasC2() }
                        androidx.compose.runtime.CompositionLocalProvider(com.gacelas.ui.components.LocalHojasC2 provides hojas) {
                            GacelasRaiz()
                            com.gacelas.actualizacion.AvisoActualizacion()
                            com.gacelas.ui.components.AnfitrionHojasC2(hojas)
                        }
                    } else Box(Modifier.fillMaxSize().background(com.gacelas.intro.FondoIntro))
                }
            }
        }
        if (conIntro) iniciarIntro()
        duracionOnCreateMs = SystemClock.uptimeMillis() - t0
        android.util.Log.i("Arranque", "onCreate ${duracionOnCreateMs} ms, intro=$conIntro")
        android.os.Trace.endSection()
    }

    /** Lo que tardo el ultimo onCreate (debe ser poco: nada pesado en el hilo principal). */
    var duracionOnCreateMs = 0L
        private set

    private var vistaIntro: android.view.View? = null

    /** Intro sobre todo; se retira sola al completar la animacion (o al tocarla) con un fundido. */
    private fun iniciarIntro() {
        if (vistaIntro != null) return
        intro.value = true
        vistaIntro = IntroAnimada.mostrar(this) { vistaIntro = null; intro.value = false }
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
