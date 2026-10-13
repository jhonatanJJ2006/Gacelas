@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.gacelas.acceso

import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.gacelas.cuenta.domain.repositorio.AuthRepository
import com.gacelas.cuenta.data.LocalAuthRepository
import com.gacelas.cuenta.domain.modelo.ResultadoAuth
import com.gacelas.cuenta.data.SupabaseAuthRepository
import com.gacelas.data.remoto.SupabaseBackend
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIView
import kotlin.coroutines.resume

/** Resultado del proveedor nativo (GoogleSignIn / AuthenticationServices). mensaje null = cancelado. */
interface AlTokenAcceso {
    fun ok(idToken: String)
    fun error(mensaje: String?)
}

interface AlTocarBoton {
    fun tocar()
}

/** Implementado en Swift (iosApp/AccesoNativo.swift). */
interface PuenteAcceso {
    /** GoogleSignIn con el cliente iOS (GIDClientID) y el nonce en SHA-256. */
    fun google(nonceSha256: String, alTerminar: AlTokenAcceso)
    /** Sign in with Apple (ASAuthorizationAppleIDProvider) con el nonce en SHA-256. */
    fun apple(nonceSha256: String, alTerminar: AlTokenAcceso)
    fun googleConfigurado(): Boolean
    /** Boton oficial ASAuthorizationAppleIDButton (Apple exige su propio diseño). */
    fun botonApple(alTocar: AlTocarBoton): UIView
}

/** Claves inyectadas en el build (Secrets.xcconfig -> Info.plist). Vacias sin configurar. */
fun claveIos(nombre: String): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey(nombre) as? String)?.trim()?.takeUnless { it.startsWith("$(") }.orEmpty()

/** Sesion de iOS: Supabase si el build trae SUPABASE_URL y SUPABASE_ANON_KEY; si no, cuentas locales (igual que Android). */
object SesionIos {
    var puente: PuenteAcceso? = null
        internal set

    val store: DataStore<Preferences> by lazy {
        val docs = NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null) as NSURL
        PreferenceDataStoreFactory.createWithPath(produceFile = { (docs.path + "/gacelas.preferences_pb").toPath() })
    }

    val backend: SupabaseBackend? by lazy {
        val url = claveIos("SUPABASE_URL"); val clave = claveIos("SUPABASE_ANON_KEY")
        if (url.isBlank() || clave.isBlank()) null else runCatching { SupabaseBackend(url, clave) }.getOrNull()
    }

    val auth: AuthRepository by lazy { backend?.let { SupabaseAuthRepository(it, store) } ?: LocalAuthRepository(store) }

    val ajustes: com.gacelas.cuenta.domain.repositorio.AjustesRepository by lazy { com.gacelas.cuenta.data.LocalAjustesRepository(store) }
}

/** Llamado desde Swift al arrancar. */
fun registrarAccesoIos(puente: PuenteAcceso) {
    SesionIos.puente = puente
}

private suspend fun pedir(bloque: (String, AlTokenAcceso) -> Unit): Triple<String?, String, String?> {
    val nonce = NSUUID().UUIDString
    return suspendCancellableCoroutine { c ->
        bloque(com.gacelas.shared.sha256Hex(nonce), object : AlTokenAcceso {
            override fun ok(idToken: String) { if (c.isActive) c.resume(Triple(idToken, nonce, null)) }
            override fun error(mensaje: String?) { if (c.isActive) c.resume(Triple(null, nonce, mensaje)) }
        })
    }
}

/**
 * "Continuar con Google" (mismo BotonGoogleVisual, loading y error que Android) y debajo Sign in with Apple,
 * obligatorio en iOS cuando se ofrece Google (App Store 4.8), con el boton oficial de Apple.
 */
@Composable
fun BotonesSocialesIos(onEntrar: () -> Unit, portada: Boolean = false, alto: Dp = 52.dp) {
    val auth = SesionIos.auth
    val puente = SesionIos.puente
    val vm = androidx.lifecycle.viewmodel.compose.viewModel(key = "acceso-social") { com.gacelas.cuenta.presentation.AccesoSocialViewModel(auth) }
    val fase by vm.fase.collectAsState()
    val error by vm.error.collectAsState()
    val entrar by rememberUpdatedState(onEntrar)
    fun flujo(apple: Boolean) {
        val p = if (apple) com.gacelas.cuenta.presentation.Proveedor.Apple else com.gacelas.cuenta.presentation.Proveedor.Google
        val disponible = auth.googleDisponible && puente != null && (apple || puente.googleConfigurado())
        vm.entrar(p, disponible, pedirToken = {
            val (token, nonce, msj) = pedir { h, cb -> if (apple) puente!!.apple(h, cb) else puente!!.google(h, cb) }
            if (token != null) com.gacelas.cuenta.presentation.TokenSocial.Ok(token, nonce) else com.gacelas.cuenta.presentation.TokenSocial.Error(msj)
        }, alEntrar = { entrar() })
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BotonGoogleVisual(onClick = { flujo(apple = false) }, cargando = fase != null, portada = portada, alto = alto)
        if (puente != null) {
            val tocar = remember { object : AlTocarBoton { override fun tocar() = flujo(apple = true) } }
            UIKitView(factory = { puente.botonApple(tocar) }, modifier = Modifier.fillMaxWidth().height(alto))
        }
    }
    fase?.let { CargandoGoogle(it) }
    error?.let { ErrorGoogle(it.mensaje, titulo = it.titulo) { vm.cerrarError() } }
}
