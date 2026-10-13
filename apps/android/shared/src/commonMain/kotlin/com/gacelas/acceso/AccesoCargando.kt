package com.gacelas.acceso

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fases del inicio con Google que se muestran en el loading a pantalla completa. */
enum class FaseGoogle(val texto: String) { Cuenta("Elige tu cuenta de Google…"), Servidor("Iniciando sesión…"), Perfil("Preparando tu cuenta…") }

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
