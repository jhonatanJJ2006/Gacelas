package com.gacelas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.gacelas.data.auth.Perfil
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import androidx.compose.runtime.SideEffect
import com.gacelas.ui.ajustesState
import com.gacelas.ui.theme.GacelasTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GacelasTheme {
                // Tocar fuera de un campo oculta el teclado. Los botones, listas y mapas consumen
                // sus propios toques, asi que este detector solo recibe toques en zonas vacias.
                val foco = LocalFocusManager.current
                val teclado = LocalSoftwareKeyboardController.current
                Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { foco.clearFocus(); teclado?.hide() } }) { GacelasRaiz() }
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
            GacelasNavHost(inicio = inicio, sesionActiva = e.perfil != null)
        }
    }
}
