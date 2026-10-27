package com.gacelas.ui.three

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType

/** Imagen de reemplazo cuando el 3D no esta disponible. */
@Composable
fun Runner3DFallback(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(140.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(GColors.Primary.copy(alpha = 0.55f), GColors.Primary.copy(alpha = 0.05f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Corredor Runcity", tint = GColors.Primary400, modifier = Modifier.size(88.dp))
            }
            Text("Corredor Runcity", style = GType.labelMd, color = GColors.TextSecondary)
        }
    }
}
