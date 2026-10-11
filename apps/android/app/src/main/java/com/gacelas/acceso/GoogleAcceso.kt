package com.gacelas.acceso

import android.content.Context
import androidx.compose.foundation.background
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
        val opcion = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setNonce(sha256(nonce))
            .setAutoSelectEnabled(false)
            .build()
        return try {
            val r = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(opcion).build())
            val c = r.credential
            if (c is CustomCredential && c.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                TokenGoogle.Ok(GoogleIdTokenCredential.createFrom(c.data).idToken, nonce)
            else TokenGoogle.Error("Google no devolvió una cuenta válida")
        } catch (_: GetCredentialCancellationException) {
            TokenGoogle.Error(null)
        } catch (_: NoCredentialException) {
            TokenGoogle.Error("No hay cuentas de Google disponibles en este teléfono")
        } catch (e: GetCredentialException) {
            TokenGoogle.Error("No se pudo usar Google: revisa que la app esté registrada en Google Cloud (${e.type})")
        } catch (e: Exception) {
            TokenGoogle.Error("No se pudo usar Google en este teléfono")
        }
    }
}

/** Boton "Continuar con Google": Credential Manager + Supabase signInWith(IDToken). */
@Composable
fun BotonGoogle(onEntrar: () -> Unit, modifier: Modifier = Modifier) {
    val auth = contenedor().auth
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            onClick = {
                error = null
                if (!auth.googleDisponible) {
                    error = "El inicio con Google necesita conexión con el servidor. Usa tu correo por ahora."
                    return@Button
                }
                cargando = true
                scope.launch {
                    when (val t = GoogleAcceso.pedirToken(context)) {
                        is TokenGoogle.Error -> error = t.mensaje
                        is TokenGoogle.Ok -> when (val r = auth.entrarConGoogle(t.idToken, t.nonce)) {
                            is ResultadoAuth.Ok -> onEntrar()
                            is ResultadoAuth.Error -> error = r.mensaje
                        }
                    }
                    cargando = false
                }
            },
            enabled = !cargando,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1F2937)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("boton-google"),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(22.dp).background(Color(0xFF4285F4), CircleShape), contentAlignment = Alignment.Center) {
                    Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(if (cargando) "Abriendo Google..." else "Continuar con Google", style = GType.labelLg)
            }
        }
        error?.let { Text(it, style = GType.bodyMd, color = GColors.Danger, modifier = Modifier.testTag("error-google")) }
    }
}
