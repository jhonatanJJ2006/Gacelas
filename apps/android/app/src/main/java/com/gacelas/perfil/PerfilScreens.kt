package com.gacelas.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gacelas.data.mock.MockData
import com.gacelas.ui.components.AvatarCirculo
import com.gacelas.ui.components.CampoTexto
import com.gacelas.ui.components.EncabezadoAtras
import com.gacelas.ui.components.EtiquetaGrupo
import com.gacelas.ui.components.FilaAjuste
import com.gacelas.ui.components.FilaInterruptor
import com.gacelas.ui.components.Metrica
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.components.Pastilla
import com.gacelas.ui.components.Tarjeta
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GType
import com.gacelas.ui.three.ClipCorredor
import com.gacelas.ui.three.Runner3D
import io.github.sceneview.math.Position

@Composable
fun PerfilScreen(onEditar: () -> Unit, onAvatar: () -> Unit, onAjustes: () -> Unit) {
    val u = MockData.usuarioActual
    Pantalla {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarCirculo(80.dp, 0)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(u.nombre, style = GType.headlineMd, color = GColors.TextPrimary)
                Text("@${u.username}", style = GType.bodyMd, color = GColors.TextSecondary)
            }
            IconButton(onClick = onEditar, modifier = Modifier.clip(CircleShape).background(GColors.SurfaceContainer)) {
                Icon(Icons.Filled.Edit, "Editar perfil", tint = GColors.Primary500)
            }
        }
        Tarjeta {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Metrica("142", "Carreras", color = GColors.Primary500)
                Metrica("248", "Seguidores")
                Metrica("189", "Siguiendo")
            }
        }
        Tarjeta {
            Text("Estadísticas del mes", style = GType.titleMd, color = GColors.TextPrimary)
            FilaEstadistica(Icons.Filled.Straighten, "Distancia total", "42.5 km")
            FilaEstadistica(Icons.Filled.Schedule, "Tiempo total", "4:25:30")
            FilaEstadistica(Icons.AutoMirrored.Filled.TrendingUp, "Elevación", "520 m")
        }
        Tarjeta(padding = 0.dp) {
            FilaAjuste(Icons.Filled.Person, "Personalizar avatar 3D", onClick = onAvatar)
            FilaAjuste(Icons.Filled.Settings, "Ajustes y preferencias", onClick = onAjustes)
        }
        Text("Actividad reciente", style = GType.titleMd, color = GColors.TextPrimary)
        (1..3).forEach { i ->
            Tarjeta {
                Row {
                    Text("Carrera matutina", style = GType.labelLg, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text("hace $i día${if (i > 1) "s" else ""}", style = GType.bodySm, color = GColors.TextSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("5.2 km", style = GType.bodyMd, color = GColors.Primary500)
                    Text("28:34", style = GType.bodyMd, color = GColors.TextSecondary)
                    Text("5:30 /km", style = GType.bodyMd, color = GColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun FilaEstadistica(icono: ImageVector, etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = GColors.Primary500, modifier = Modifier.size(20.dp))
        Text("  $etiqueta", style = GType.bodyMd, color = GColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(valor, style = GType.labelLg, color = GColors.TextPrimary)
    }
}

@Composable
fun EditarPerfilScreen(onAtras: () -> Unit, onGuardar: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("Jhonatan Runner") }
    var username by rememberSaveable { mutableStateOf("jhonatan_runs") }
    var ubicacion by rememberSaveable { mutableStateOf("Loja, Ecuador") }
    var bio by rememberSaveable { mutableStateOf("Corredor de Loja. Me encanta trail running.") }
    var edad by rememberSaveable { mutableStateOf("") }
    var genero by rememberSaveable { mutableStateOf("Masculino") }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras("Editar perfil", onAtras) {
            Text("Guardar", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.clickable(onClick = onGuardar).padding(12.dp))
        }
        Pantalla(Modifier.weight(1f).navigationBarsPadding()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    AvatarCirculo(104.dp, 0)
                    Box(Modifier.align(Alignment.BottomEnd).size(34.dp).clip(CircleShape).background(GColors.Primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CameraAlt, null, tint = GColors.TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Text("Cambiar foto", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.padding(top = 8.dp))
            }
            CampoTexto("Nombre completo", nombre, { nombre = it })
            CampoTexto("Nombre de usuario", username, { username = it })
            CampoTexto("Ubicación", ubicacion, { ubicacion = it }, trailing = { Icon(Icons.Filled.LocationOn, null, tint = GColors.TextSecondary) })
            CampoTexto("Bio", bio, { if (it.length <= 150) bio = it }, "Cuéntanos sobre ti...", lineas = 4, ayuda = "${bio.length}/150 caracteres")
            EtiquetaGrupo("Información adicional")
            CampoTexto("Edad", edad, { edad = it.filter(Char::isDigit) }, "25", tipo = KeyboardType.Number)
            Text("Género", style = GType.labelLg, color = GColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Masculino", "Femenino", "Otro").forEach { Pastilla(it, it == genero, { genero = it }) }
            }
            Pastilla("Prefiero no decir", genero == "Prefiero no decir", { genero = "Prefiero no decir" })
        }
    }
}

@Composable
fun Avatar3DScreen(onAtras: () -> Unit, onGuardar: () -> Unit) {
    val equipaciones = listOf(GColors.Primary, GColors.Secondary, GColors.Info, GColors.Danger)
    var equipacion by rememberSaveable { mutableIntStateOf(0) }
    var estilo by rememberSaveable { mutableStateOf("Dinámico") }
    var lentes by rememberSaveable { mutableStateOf(false) }
    var reloj by rememberSaveable { mutableStateOf(true) }
    var audifonos by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().background(GColors.BgPrimary)) {
        EncabezadoAtras("Personalizar Avatar", onAtras) {
            Text("Guardar", style = GType.labelLg, color = GColors.Primary500, modifier = Modifier.clickable(onClick = onGuardar).padding(12.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(340.dp).background(Brush.verticalGradient(listOf(GColors.SurfaceLow, equipaciones[equipacion].copy(alpha = 0.18f))))) {
                Runner3D(ClipCorredor.Idle, autoRotar = true, camara = Position(0f, 0.5f, 3.4f))
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, null, tint = GColors.Primary500)
                        Text("  Equipación", style = GType.titleMd, color = GColors.TextPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        equipaciones.forEachIndexed { i, c ->
                            Box(Modifier.size(44.dp).clip(CircleShape).background(c)
                                .border(3.dp, if (i == equipacion) GColors.TextPrimary else Color.Transparent, CircleShape)
                                .clickable { equipacion = i })
                        }
                    }
                }
                EtiquetaGrupo("Estilo de carrera")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Dinámico" to Icons.AutoMirrored.Filled.DirectionsRun,
                        "Relajado" to Icons.Filled.SelfImprovement,
                        "Veloz" to Icons.Filled.Bolt,
                    ).forEach { (n, ic) ->
                        val activo = n == estilo
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (activo) GColors.Primary.copy(alpha = 0.2f) else GColors.SurfaceContainer)
                                .border(2.dp, if (activo) GColors.Primary else GColors.BorderSubtle, RoundedCornerShape(16.dp)).clickable { estilo = n }.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(ic, null, tint = if (activo) GColors.Primary500 else GColors.TextSecondary)
                            Text(n, style = GType.labelMd, color = GColors.TextPrimary)
                        }
                    }
                }
                EtiquetaGrupo("Accesorios")
                Tarjeta(padding = 0.dp) {
                    FilaInterruptor(Icons.Filled.Visibility, "Lentes deportivos", checked = lentes, onChange = { lentes = it })
                    FilaInterruptor(Icons.Filled.Watch, "Reloj GPS", checked = reloj, onChange = { reloj = it })
                    FilaInterruptor(Icons.Filled.Headphones, "Audífonos", checked = audifonos, onChange = { audifonos = it })
                }
            }
        }
    }
}
