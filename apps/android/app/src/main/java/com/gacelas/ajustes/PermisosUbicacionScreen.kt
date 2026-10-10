package com.gacelas.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gacelas.ui.components.BotonPrimario
import com.gacelas.ui.components.BotonSecundario
import com.gacelas.ui.components.IconoCirculo
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Permiso de ubicacion. TODO: pedir ACCESS_FINE_LOCATION real con rememberLauncherForActivityResult. */
@Composable
fun PermisosUbicacionScreen(onPermitir: () -> Unit, onMasTarde: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(GColors.BgPrimary).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.padding(top = 24.dp))
        IconoCirculo(Icons.Filled.LocationOn, GColors.Primary.copy(alpha = 0.2f), GColors.Primary500, 112.dp)
        Text("Activa tu ubicación", style = GType.headlineLg, color = GColors.TextPrimary, textAlign = TextAlign.Center)
        Text("Necesitamos acceso a tu ubicación para grabar tus carreras y mostrarte rutas cercanas",
            style = GType.bodyLg, color = GColors.TextSecondary, textAlign = TextAlign.Center)
        listOf(
            "Graba tus rutas con precisión" to "GPS activo durante tus carreras",
            "Descubre rutas cercanas" to "Encuentra las mejores rutas de Loja",
            "Tu privacidad está protegida" to "Controlas qué compartes y con quién",
        ).forEach { (t, d) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconoCirculo(Icons.Filled.Check, GColors.Secondary.copy(alpha = 0.2f), GColors.Secondary500, 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(t, style = GType.bodyLg, color = GColors.TextPrimary)
                    Text(d, style = GType.bodySm, color = GColors.TextSecondary)
                }
            }
        }
        BotonPrimario("Permitir ubicación", onPermitir)
        BotonSecundario("Configurar más tarde", onMasTarde, color = GColors.TextSecondary)
    }
}
