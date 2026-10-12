package com.gacelas.acceso

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.res.painterResource
import com.gacelas.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import android.util.Log
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.gacelas.BuildConfig
import com.gacelas.data.auth.ResultadoAuth
import com.gacelas.ui.contenedor
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

/** Resultado de pedir la cuenta de Google al sistema. */
sealed interface TokenGoogle {
    data class Ok(val idToken: String, val nonce: String) : TokenGoogle
    data class Error(val mensaje: String?) : TokenGoogle
}

object GoogleAcceso {
    const val NO_CONFIGURADO = "El inicio con Google aún no está configurado en esta versión de la app"

    fun configurado(clientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID) = clientId.isNotBlank()

    fun sha256(texto: String): String =
        MessageDigest.getInstance("SHA-256").digest(texto.toByteArray()).joinToString("") { "%02x".format(it) }

    /**
     * Pide una cuenta de Google con Credential Manager. Google firma el token con el nonce
     * en SHA-256 y Supabase recibe el nonce original para verificarlo.
     */
    suspend fun pedirToken(context: Context, clientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID): TokenGoogle {
        if (!configurado(clientId)) return TokenGoogle.Error(NO_CONFIGURADO)
        val nonce = UUID.randomUUID().toString()
        val gestor = CredentialManager.create(context)
        // 1) Hoja de cuentas (todas las cuentas del telefono, sin seleccion automatica).
        val hoja = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setNonce(sha256(nonce))
            .setAutoSelectEnabled(false)
            .build()
        // 2) Si no hay credenciales para esa hoja, flujo del boton "Sign in with Google".
        val boton = GetSignInWithGoogleOption.Builder(clientId).setNonce(sha256(nonce)).build()
        return try {
            leer(gestor.getCredential(context, GetCredentialRequest.Builder().addCredentialOption(hoja).build()), nonce)
        } catch (e: NoCredentialException) {
            Log.w(TAG, "Hoja de Google sin credenciales (${e.javaClass.simpleName}: ${e.type}); probando Sign in with Google")
            try {
                leer(gestor.getCredential(context, GetCredentialRequest.Builder().addCredentialOption(boton).build()), nonce)
            } catch (e2: Exception) {
                Log.w(TAG, "Sign in with Google fallo: ${e2.javaClass.simpleName}", e2)
                TokenGoogle.Error(mensajeError(e2))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google fallo: ${e.javaClass.simpleName}", e)
            TokenGoogle.Error(mensajeError(e))
        }
    }

    private fun leer(r: GetCredentialResponse, nonce: String): TokenGoogle {
        val c = r.credential
        return if (c is CustomCredential && c.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
            TokenGoogle.Ok(GoogleIdTokenCredential.createFrom(c.data).idToken, nonce)
        else TokenGoogle.Error("Google no devolvió una cuenta válida")
    }

    /** Mensaje en español segun el tipo de error (null = el usuario cancelo, no se muestra nada). */
    fun mensajeError(e: Throwable): String? = when (e) {
        is GetCredentialCancellationException -> null
        is NoCredentialException -> "Google no aceptó la app (falta registrarla en Google Cloud con su huella SHA-1) o no hay ninguna cuenta de Google en el teléfono"
        is GetCredentialInterruptedException -> "Se interrumpió el inicio con Google. Inténtalo de nuevo."
        is GetCredentialProviderConfigurationException -> "Este teléfono necesita actualizar los Servicios de Google Play para iniciar con Google"
        is GetCredentialUnsupportedException -> "Este teléfono no permite iniciar con Google desde apps"
        is GoogleIdTokenParsingException -> "Google devolvió una respuesta que no se pudo leer"
        is GetCredentialException -> mensajeCredencial(e.type, e.message)
        else -> "No se pudo usar Google en este teléfono (${e.javaClass.simpleName})"
    }

    /**
     * Errores que Google devuelve DESPUES de elegir la cuenta. "[10]"/"[16] Account reauth failed"/28444
     * significan que el cliente OAuth Android (paquete + SHA-1 de la firma) no existe en el proyecto
     * del web client id.
     */
    fun mensajeCredencial(tipo: String, detalle: String?): String {
        val d = detalle.orEmpty()
        return when {
            "[10]" in d || "[16]" in d || "reauth" in d.lowercase() || "28444" in d || "developer console" in d.lowercase() ->
                "Google rechazó la app: su firma (SHA-1) no está registrada en el cliente OAuth Android de com.gacelas. ($d)"
            else -> "No se pudo usar Google ($tipo${if (d.isNotBlank()) ": $d" else ""})"
        }
    }

    private const val TAG = "GoogleAcceso"
}

/** Fases del inicio con Google que se muestran en el loading a pantalla completa. */
enum class FaseGoogle(val texto: String) { Cuenta("Elige tu cuenta de Google…"), Servidor("Iniciando sesión…"), Perfil("Preparando tu cuenta…") }

/** Boton "Continuar con Google": Credential Manager + Supabase signInWith(IDToken). */
@Composable
fun BotonGoogle(
    onEntrar: () -> Unit,
    modifier: Modifier = Modifier,
    /** Estilo de la portada C4 (Stitch): fondo #1C2028, borde blanco 10%, Inter 15 medium. */
    portada: Boolean = false,
    alto: androidx.compose.ui.unit.Dp = 52.dp,
) {
    val auth = contenedor().auth
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var fase by remember { mutableStateOf<FaseGoogle?>(null) }
    val cargando = fase != null
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BotonGoogleVisual(
            onClick = {
                error = null
                if (!auth.googleDisponible) {
                    error = "El inicio con Google no está disponible en esta versión de la app. Usa tu correo por ahora."
                    return@BotonGoogleVisual
                }
                fase = FaseGoogle.Cuenta
                scope.launch {
                    try {
                        when (val t = GoogleAcceso.pedirToken(context)) {
                            is TokenGoogle.Error -> error = t.mensaje
                            is TokenGoogle.Ok -> {
                                fase = FaseGoogle.Servidor
                                when (val r = auth.entrarConGoogle(t.idToken, t.nonce)) {
                                    is ResultadoAuth.Ok -> { fase = FaseGoogle.Perfil; onEntrar() }
                                    is ResultadoAuth.Error -> error = r.mensaje.ifBlank { "No se pudo iniciar sesión con Google" }
                                }
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e("GoogleAcceso", "Fallo inesperado", e)
                        error = "No se pudo iniciar sesión con Google (${e.javaClass.simpleName}: ${e.message})"
                    } finally { fase = null }
                }
            },
            cargando = cargando, portada = portada, alto = alto,
        )
    }
    fase?.let { CargandoGoogle(it) }
    error?.let { ErrorGoogle(it) { error = null } }
}

/** Loading a pantalla completa, sin poder cerrarse, desde que se toca el boton hasta la sesion o el error. */
@Composable
fun CargandoGoogle(fase: FaseGoogle) {
    androidx.compose.ui.window.Dialog(onDismissRequest = {}, properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Column(
            Modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xFF131926)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(horizontal = 28.dp, vertical = 24.dp).testTag("cargando-google"),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(Modifier.size(36.dp), color = Color(0xFFD97706), strokeWidth = 3.dp)
            Text(fase.texto, fontFamily = com.gacelas.ui.theme.Inter, fontSize = 15.sp, color = Color.White)
        }
    }
}

/** El error se muestra en un dialogo (no se pierde fuera de la tarjeta). */
@Composable
fun ErrorGoogle(mensaje: String, onCerrar: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = { androidx.compose.material3.TextButton(onClick = onCerrar) { Text("Entendido", color = Color(0xFFF59E0B)) } },
        title = { Text("No se pudo entrar con Google") },
        text = { Text(mensaje, modifier = Modifier.testTag("error-google")) },
        containerColor = Color(0xFF131926), titleContentColor = Color.White, textContentColor = Color(0xFFBCC7DE),
    )
}
